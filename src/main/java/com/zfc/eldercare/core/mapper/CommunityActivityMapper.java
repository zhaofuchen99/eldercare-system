package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.CommunityActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 社区活动表 Mapper（文档 6.3.11）。
 */
@Mapper
public interface CommunityActivityMapper {

    int insert(CommunityActivity activity);

    CommunityActivity selectById(@Param("id") Long id);

    /** 批量查询（我的活动 VO 组装用） */
    List<CommunityActivity> selectByIds(@Param("ids") List<Long> ids);

    /** 活动分页（管理端），配合 PageHelper */
    List<CommunityActivity> selectPage();

    /** 已发布活动列表（会员端：非草稿，按开始时间倒序） */
    List<CommunityActivity> selectPublished();

    int update(CommunityActivity activity);

    /** 逻辑删除 */
    int delete(@Param("id") Long id);

    /** 原子增加报名人数：仅未达到人数上限时成功（NULL 表示不限），文档 9.3 并发控制 */
    int incrementParticipants(@Param("id") Long id);

    /** 指定日期开始的活动（已发布，活动提醒推送用，文档 5.12） */
    List<CommunityActivity> selectStartingOn(@Param("date") java.time.LocalDate date);
}
