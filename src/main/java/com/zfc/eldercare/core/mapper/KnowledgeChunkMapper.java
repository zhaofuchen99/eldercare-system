package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.KnowledgeChunk;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库切片 Mapper（knowledge_chunk 表，AI 模块 - RAG）。
 */
public interface KnowledgeChunkMapper {

    int insert(KnowledgeChunk chunk);

    /** 批量插入切片 */
    int batchInsert(@Param("list") List<KnowledgeChunk> chunks);

    /** 某文档切片分页（管理端查看，配合 PageHelper） */
    List<KnowledgeChunk> selectPageByDocId(@Param("docId") Long docId);

    /** 某文档的全部 Redis 向量 ID（删除文档时回删向量） */
    List<String> selectVectorIdsByDocId(@Param("docId") Long docId);

    /** 逻辑删除某文档全部切片 */
    int deleteByDocId(@Param("docId") Long docId);
}
