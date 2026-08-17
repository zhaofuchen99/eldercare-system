package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.AssessmentResult;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评测结果 Mapper（assessment_result 表）。
 */
public interface AssessmentResultMapper {

    int insert(AssessmentResult result);

    AssessmentResult selectById(@Param("id") Long id);

    /** 某用户评测历史（配合 PageHelper 分页） */
    List<AssessmentResult> selectPageByUserId(@Param("userId") Long userId);
}
