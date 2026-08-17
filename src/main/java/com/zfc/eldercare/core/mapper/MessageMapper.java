package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.Message;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 站内消息 Mapper（message 表，详细设计文档 6.3.16）。
 */
public interface MessageMapper {

    /** 新增消息，回填自增主键 */
    int insert(Message message);

    /** 按 ID 查消息（未删除） */
    Message selectById(@Param("id") Long id);

    /** 会员端：我的消息分页（未读在前，新的在前） */
    List<Message> selectPageByUserId(@Param("userId") Long userId);

    /** 管理端：消息分页（可按用户/类型筛选） */
    List<Message> selectPage(@Param("userId") Long userId, @Param("type") String type);

    /** 用户消息总数（未删除） */
    long countByUserId(@Param("userId") Long userId);

    /** 用户未读消息数 */
    long countUnreadByUserId(@Param("userId") Long userId);

    /** 标记已读（仅本人、未删除、未读成功，返回 0 表示已是已读态） */
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    /** 删除消息（逻辑删除） */
    int delete(@Param("id") Long id);
}
