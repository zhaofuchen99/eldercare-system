package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.Question;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 题目 Mapper（question 表）。
 */
public interface QuestionMapper {

    int insert(Question question);

    Question selectById(@Param("id") Long id);

    /** 某问卷下全部题目（按排序号） */
    List<Question> selectByQuestionnaireId(@Param("questionnaireId") Long questionnaireId);

    int update(Question question);

    int delete(@Param("id") Long id);

    int deleteByQuestionnaireId(@Param("questionnaireId") Long questionnaireId);
}
