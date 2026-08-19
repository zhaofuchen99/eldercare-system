package com.zfc.eldercare.core.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档实体（knowledge_doc 表，AI 模块 - RAG 知识库）。
 * 记录上传的社区知识文档元数据；向量存 Redis，切片文本存 knowledge_chunk。
 */
@Data
public class KnowledgeDoc {

    private Long id;

    /** 文档标题（默认取原始文件名） */
    private String title;

    /** 原始文件名 */
    private String fileName;

    /** 存储相对路径 knowledge/yyyyMM/{uuid}.ext（重新解析用） */
    private String filePath;

    /** 文件类型：TXT/MD/PDF/DOCX（当前支持 TXT/MD） */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 切片数量 */
    private Integer chunkCount;

    /** 处理状态：PARSING解析中/READY可用/FAILED失败 */
    private String status;

    /** 上传管理员 ID */
    private Long createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
