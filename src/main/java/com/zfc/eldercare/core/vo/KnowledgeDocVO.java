package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.KnowledgeDoc;

import java.time.LocalDateTime;

/**
 * 知识库文档 VO（管理端展示）。
 */
public record KnowledgeDocVO(
        Long id,
        String title,
        String fileType,
        Long fileSize,
        Integer chunkCount,
        String status,
        LocalDateTime createTime
) {
    public static KnowledgeDocVO from(KnowledgeDoc d) {
        return new KnowledgeDocVO(d.getId(), d.getTitle(), d.getFileType(), d.getFileSize(),
                d.getChunkCount(), d.getStatus(), d.getCreateTime());
    }
}
