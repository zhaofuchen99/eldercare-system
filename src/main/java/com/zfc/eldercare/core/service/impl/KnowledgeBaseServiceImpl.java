package com.zfc.eldercare.core.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zfc.eldercare.core.entity.KnowledgeChunk;
import com.zfc.eldercare.core.entity.KnowledgeDoc;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.KnowledgeChunkMapper;
import com.zfc.eldercare.core.mapper.KnowledgeDocMapper;
import com.zfc.eldercare.core.service.KnowledgeBaseService;
import com.zfc.eldercare.core.vo.KnowledgeChunkVO;
import com.zfc.eldercare.core.vo.KnowledgeDocVO;
import com.zfc.eldercare.core.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 知识库服务实现（AI 模块 - RAG）。
 * 上传/重新解析涉及 Redis 向量、磁盘文件与 MySQL 多资源，不使用单数据库事务（每步独立提交，失败可重试）；
 * 任一步异常将文档置为 FAILED 并回删已写入的向量，保证可重试。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final String STATUS_PARSING = "PARSING";
    private static final String STATUS_READY = "READY";
    private static final String STATUS_FAILED = "FAILED";

    /** 当前支持的文件类型 */
    private static final Set<String> SUPPORTED = Set.of("txt", "md");

    /** 切片目标长度与重叠 */
    private static final int CHUNK_MAX_LEN = 800;
    private static final int CHUNK_OVERLAP = 80;

    private final KnowledgeDocMapper knowledgeDocMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final VectorStore knowledgeVectorStore;
    private final KnowledgeTextParser knowledgeTextParser;
    private final KnowledgeChunker knowledgeChunker;

    @Value("${eldercare.upload-dir:data/upload}")
    private String uploadDir;

    @Override
    public Long upload(Long adminUserId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文档");
        }
        String original = file.getOriginalFilename() == null ? "untitled" : file.getOriginalFilename();
        String ext = extOf(original);
        if (!SUPPORTED.contains(ext)) {
            throw new BusinessException("当前仅支持 txt/md 文档（pdf/docx 暂不支持）");
        }

        // 1. 存盘 + 插入文档记录（PARSING）
        String relative = storeFile(file, ext);
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setTitle(original);
        doc.setFileName(original);
        doc.setFilePath(relative);
        doc.setFileType(ext.toUpperCase());
        doc.setFileSize(file.getSize());
        doc.setChunkCount(0);
        doc.setStatus(STATUS_PARSING);
        doc.setCreateBy(adminUserId);
        knowledgeDocMapper.insert(doc);

        // 2. 解析 → 切片 → 向量化 → 切片入库 → READY
        List<String> vectorIds = new ArrayList<>();
        try {
            String text = knowledgeTextParser.extractText(Paths.get(uploadDir, relative), ext);
            List<String> chunks = knowledgeChunker.chunk(text, CHUNK_MAX_LEN, CHUNK_OVERLAP);
            if (chunks.isEmpty()) {
                throw new BusinessException("文档内容为空，请检查文件");
            }
            List<Document> documents = buildDocuments(chunks, doc.getId(), doc.getTitle(), vectorIds);
            knowledgeVectorStore.add(documents);

            List<KnowledgeChunk> chunkEntities = new ArrayList<>(documents.size());
            for (int i = 0; i < documents.size(); i++) {
                KnowledgeChunk c = new KnowledgeChunk();
                c.setDocId(doc.getId());
                c.setChunkIndex(i);
                c.setChunkText(documents.get(i).getText());
                c.setVectorId(vectorIds.get(i));
                c.setTokenCount(chunks.get(i).length());
                chunkEntities.add(c);
            }
            if (!chunkEntities.isEmpty()) {
                knowledgeChunkMapper.batchInsert(chunkEntities);
            }
            knowledgeDocMapper.updateStatusAndChunkCount(doc.getId(), STATUS_READY, chunkEntities.size());
            return doc.getId();
        } catch (Exception e) {
            knowledgeDocMapper.updateStatusAndChunkCount(doc.getId(), STATUS_FAILED, 0);
            if (!vectorIds.isEmpty()) {
                try {
                    knowledgeVectorStore.delete(vectorIds);
                } catch (Exception ignored) {
                    // 向量清理失败不阻断主流程，可手动重试删除
                }
            }
            if (e instanceof BusinessException be) {
                throw be;
            }
            log.error("知识文档解析入库失败 docId={}", doc.getId(), e);
            throw new BusinessException("文档处理失败：" + e.getMessage());
        }
    }

    @Override
    public PageVO<KnowledgeDocVO> pageDocs(int page, int size) {
        PageHelper.startPage(page, size);
        List<KnowledgeDoc> list = knowledgeDocMapper.selectPage();
        PageInfo<KnowledgeDoc> info = new PageInfo<>(list);
        return new PageVO<>(info.getPageNum(), info.getPageSize(), info.getTotal(), info.getPages(),
                list.stream().map(KnowledgeDocVO::from).toList());
    }

    @Override
    public PageVO<KnowledgeChunkVO> pageChunks(Long docId, int page, int size) {
        knowledgeDocMapper.selectById(docId); // 404 校验
        PageHelper.startPage(page, size);
        List<KnowledgeChunk> list = knowledgeChunkMapper.selectPageByDocId(docId);
        PageInfo<KnowledgeChunk> info = new PageInfo<>(list);
        return new PageVO<>(info.getPageNum(), info.getPageSize(), info.getTotal(), info.getPages(),
                list.stream().map(KnowledgeChunkVO::from).toList());
    }

    @Override
    public void deleteDoc(Long docId) {
        KnowledgeDoc doc = knowledgeDocMapper.selectById(docId);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在");
        }
        deleteChunkVectors(docId);
        knowledgeChunkMapper.deleteByDocId(docId);
        knowledgeDocMapper.delete(docId);
    }

    @Override
    public void reparse(Long docId) {
        KnowledgeDoc doc = knowledgeDocMapper.selectById(docId);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在");
        }
        Path path = Paths.get(uploadDir, doc.getFilePath());
        if (!Files.exists(path)) {
            throw new BusinessException(404, "原文档文件不存在，请重新上传");
        }
        // 清理旧向量与切片，置解析中
        deleteChunkVectors(docId);
        knowledgeChunkMapper.deleteByDocId(docId);
        knowledgeDocMapper.updateStatusAndChunkCount(docId, STATUS_PARSING, 0);

        List<String> vectorIds = new ArrayList<>();
        try {
            String text = knowledgeTextParser.extractText(path, extOf(doc.getFileName()));
            List<String> chunks = knowledgeChunker.chunk(text, CHUNK_MAX_LEN, CHUNK_OVERLAP);
            if (chunks.isEmpty()) {
                throw new BusinessException("文档内容为空，请检查文件");
            }
            List<Document> documents = buildDocuments(chunks, docId, doc.getTitle(), vectorIds);
            knowledgeVectorStore.add(documents);

            List<KnowledgeChunk> chunkEntities = new ArrayList<>(documents.size());
            for (int i = 0; i < documents.size(); i++) {
                KnowledgeChunk c = new KnowledgeChunk();
                c.setDocId(docId);
                c.setChunkIndex(i);
                c.setChunkText(documents.get(i).getText());
                c.setVectorId(vectorIds.get(i));
                c.setTokenCount(chunks.get(i).length());
                chunkEntities.add(c);
            }
            if (!chunkEntities.isEmpty()) {
                knowledgeChunkMapper.batchInsert(chunkEntities);
            }
            knowledgeDocMapper.updateStatusAndChunkCount(docId, STATUS_READY, chunkEntities.size());
        } catch (Exception e) {
            knowledgeDocMapper.updateStatusAndChunkCount(docId, STATUS_FAILED, 0);
            if (!vectorIds.isEmpty()) {
                try {
                    knowledgeVectorStore.delete(vectorIds);
                } catch (Exception ignored) {
                    // 同上
                }
            }
            if (e instanceof BusinessException be) {
                throw be;
            }
            log.error("知识文档重新解析失败 docId={}", docId, e);
            throw new BusinessException("文档重新解析失败：" + e.getMessage());
        }
    }

    // ========== 私有辅助 ==========

    /**
     * 切片 → Document（Spring AI 2.0 的 add() 返回 void，需显式生成 vectorId 写入 Document.id，
     * 向量 ID 即为 Redis 中该条向量的键，回删/MySQL vector_id 都依赖它）。
     */
    private List<Document> buildDocuments(List<String> chunks, Long docId, String title, List<String> vectorIds) {
        List<Document> documents = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            String vid = UUID.randomUUID().toString().replace("-", "");
            vectorIds.add(vid);
            documents.add(Document.builder()
                    .id(vid)
                    .text(chunks.get(i))
                    .metadata("docId", String.valueOf(docId))
                    .metadata("chunkIndex", i)
                    .metadata("docTitle", title)
                    .build());
        }
        return documents;
    }

    /** 回删某文档在 Redis 中的全部向量 */
    private void deleteChunkVectors(Long docId) {
        List<String> vectorIds = knowledgeChunkMapper.selectVectorIdsByDocId(docId);
        if (!vectorIds.isEmpty()) {
            try {
                knowledgeVectorStore.delete(vectorIds);
            } catch (Exception e) {
                log.warn("删除知识库向量失败 docId={}: {}", docId, e.getMessage());
            }
        }
    }

    private String storeFile(MultipartFile file, String ext) {
        try {
            String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            Path dir = Paths.get(uploadDir, "knowledge", month);
            Files.createDirectories(dir);
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(dir.resolve(fileName).toFile());
            return "knowledge/" + month + "/" + fileName;
        } catch (IOException e) {
            log.error("知识文档保存失败", e);
            throw new BusinessException("文档保存失败");
        }
    }

    private String extOf(String name) {
        if (name == null) {
            return "";
        }
        int idx = name.lastIndexOf('.');
        return idx < 0 ? "" : name.substring(idx + 1).toLowerCase();
    }
}
