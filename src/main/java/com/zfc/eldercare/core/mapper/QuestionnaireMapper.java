package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.Questionnaire;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 问卷 Mapper（questionnaire 表）。
 */
public interface QuestionnaireMapper {

    int insert(Questionnaire questionnaire);

    Questionnaire selectById(@Param("id") Long id);

    List<Questionnaire> selectByIds(@Param("ids") List<Long> ids);

    /** 已发布问卷（会员端展示，按创建时间倒序） */
    List<Questionnaire> selectPublished();

    /** 全部问卷（管理端，配合 PageHelper 分页） */
    List<Questionnaire> selectPage();

    int update(Questionnaire questionnaire);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int delete(@Param("id") Long id);
}
