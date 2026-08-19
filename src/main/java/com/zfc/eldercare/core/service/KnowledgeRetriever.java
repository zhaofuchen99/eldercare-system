package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.mapper.KnowledgeDocMapper;
import com.zfc.eldercare.core.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库检索器（AI 模块 - RAG，与聊天链路唯一的耦合点）。
 *
 * <p>fail-open：满足全部条件才返回检索文本，否则返回空串（调用方据此决定是否注入）：
 * 1. 开关 knowledge_enabled=true；2. 存在 READY 文档；3. 相似度命中 ≥ 阈值；4. 调用不抛异常。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KnowledgeRetriever {

    private static final String KEY_ENABLED = "knowledge_enabled";
    private static final String KEY_TOP_K = "knowledge_top_k";
    private static final String KEY_THRESHOLD = "knowledge_search_threshold";

    private final VectorStore knowledgeVectorStore;
    private final SysConfigMapper sysConfigMapper;
    private final KnowledgeDocMapper knowledgeDocMapper;

    /** 命中返回拼好的知识段（含来源标题），未命中/异常返回空串 */
    public String retrieve(String query) {
        try {
            if (!"true".equalsIgnoreCase(sysConfigMapper.selectValueByKey(KEY_ENABLED))) {
                return "";
            }
            if (knowledgeDocMapper.countReady() == 0) {
                return "";
            }
            int topK = intConfig();
            double threshold = doubleConfig();
            List<Document> docs = knowledgeVectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(topK).similarityThreshold(threshold).build());
            if (docs.isEmpty()) {
                return "";
            }
            return docs.stream()
                    .map(d -> d.getMetadata().getOrDefault("docTitle", "知识库") + "：\n" + d.getText())
                    .collect(Collectors.joining("\n\n---\n\n"));
        } catch (Exception e) {
            log.warn("RAG 检索失败，跳过知识库增强：{}", e.getMessage());
            return "";
        }
    }

    private int intConfig() {
        try {
            return Integer.parseInt(sysConfigMapper.selectValueByKey(KnowledgeRetriever.KEY_TOP_K));
        } catch (Exception e) {
            return 3;
        }
    }

    private double doubleConfig() {
        try {
            return Double.parseDouble(sysConfigMapper.selectValueByKey(KnowledgeRetriever.KEY_THRESHOLD));
        } catch (Exception e) {
            return 0.6;
        }
    }
}
