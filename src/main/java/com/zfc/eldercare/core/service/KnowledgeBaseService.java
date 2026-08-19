package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.vo.KnowledgeChunkVO;
import com.zfc.eldercare.core.vo.KnowledgeDocVO;
import com.zfc.eldercare.core.vo.PageVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库服务（AI 模块 - RAG，管理端文档管理）。
 */
public interface KnowledgeBaseService {

    /**
     * 上传知识文档：存盘 → 解析 → 切片 → 向量化入库 → 状态 READY。
     *
     * @return 文档 ID
     */
    Long upload(Long adminUserId, MultipartFile file);

    /** 文档分页（管理端） */
    PageVO<KnowledgeDocVO> pageDocs(int page, int size);

    /** 某文档切片分页（管理端查看） */
    PageVO<KnowledgeChunkVO> pageChunks(Long docId, int page, int size);

    /** 删除文档：回删 Redis 向量 + 逻辑删除切片与文档 */
    void deleteDoc(Long docId);

    /** 重新解析并重建向量（覆盖式） */
    void reparse(Long docId);
}
