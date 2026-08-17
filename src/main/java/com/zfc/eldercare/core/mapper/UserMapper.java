package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户表 Mapper。
 */
@Mapper
public interface UserMapper {

    /** 按手机号查用户（未删除） */
    User selectByPhone(@Param("phone") String phone);

    /** 按 ID 查用户（未删除） */
    User selectById(@Param("id") Long id);

    /** 批量查询（管理端列表 VO 组装用） */
    java.util.List<User> selectByIds(@Param("ids") java.util.List<Long> ids);

    /** 新增用户，回填自增主键 */
    int insert(User user);

    /** 更新密码 */
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /** 原子增减积分（文档 9.3）：UPDATE user SET points = points + ? WHERE id = ? */
    int updatePoints(@Param("id") Long id, @Param("delta") int delta);

    /** 原子扣减积分（消费）：points >= amount 才成功，返回受影响行数（0=积分不足） */
    int deductPoints(@Param("id") Long id, @Param("amount") int amount);

    /** 会员总数（role=MEMBER 且未删除，文档 5.9 仪表盘） */
    long countMembers();

    /** 今日新增会员数（role=MEMBER、未删除、当日创建） */
    long countTodayNewMembers();

    /** 会员分页（管理端，可按关键字/状态/等级筛选，role=MEMBER），配合 PageHelper */
    java.util.List<User> selectMemberPage(@Param("keyword") String keyword,
                                          @Param("status") String status,
                                          @Param("memberLevel") String memberLevel);

    /** 更新用户状态（启用/禁用） */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 更新会员等级 */
    int updateMemberLevel(@Param("id") Long id, @Param("memberLevel") String memberLevel);

    /** 更新个人信息（仅更新非 null 字段，会员中心） */
    int updateProfile(User user);
}
