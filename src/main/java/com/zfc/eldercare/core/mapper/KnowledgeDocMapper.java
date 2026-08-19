package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.KnowledgeDoc;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库文档 Mapper（knowledge_doc 表，AI 模块 - RAG）。
 */
public interface KnowledgeDocMapper {

    int insert(KnowledgeDoc doc);

    KnowledgeDoc selectById(@Param("id") Long id);

    /** 文档分页（管理端，配合 PageHelper），按创建时间倒序 */
    List<KnowledgeDoc> selectPage();

    /** 更新处理状态与切片数 */
    int updateStatusAndChunkCount(@Param("id") Long id, @Param("status") String status,
                                  @Param("chunkCount") Integer chunkCount);

    /** 逻辑删除 */
    int delete(@Param("id") Long id);

    /** 可用（READY）文档数：RAG 检索开关判断用 */
    long countReady();
}
