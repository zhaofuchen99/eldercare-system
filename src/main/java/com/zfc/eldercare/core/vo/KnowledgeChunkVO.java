package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.KnowledgeChunk;

import java.time.LocalDateTime;

/**
 * 知识库切片 VO（管理端查看某文档切片）。
 */
public record KnowledgeChunkVO(
        Long id,
        Integer chunkIndex,
        String chunkText,
        Integer tokenCount,
        String vectorId,
        LocalDateTime createTime
) {
    public static KnowledgeChunkVO from(KnowledgeChunk c) {
        return new KnowledgeChunkVO(c.getId(), c.getChunkIndex(), c.getChunkText(),
                c.getTokenCount(), c.getVectorId(), c.getCreateTime());
    }
}
