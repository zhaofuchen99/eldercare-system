package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库切片实体（knowledge_chunk 表，AI 模块 - RAG 知识库）。
 * 文档切分后的文本片段，vector_id 关联 Redis 中的向量（删除文档时回删）。
 */
@Data
public class KnowledgeChunk {

    private Long id;

    /** 文档 ID（关联 knowledge_doc.id） */
    private Long docId;

    /** 切片序号（从 0 开始） */
    private Integer chunkIndex;

    /** 切片文本（与 Redis 向量关联，检索时直接返回） */
    private String chunkText;

    /** Redis 向量 ID（vectorStore.add 生成，删除文档时回删） */
    private String vectorId;

    /** 切片字符数 */
    private Integer tokenCount;

    private LocalDateTime createTime;

    private Integer deleted;
}
