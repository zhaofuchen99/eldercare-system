package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 刷新令牌表 Mapper。
 * 删除策略为物理删除（详细设计文档 6.3.2）。
 */
@Mapper
public interface RefreshTokenMapper {

    /** 新增 Refresh Token */
    int insert(RefreshToken refreshToken);

    /** 按 Token 值查询 */
    RefreshToken selectByToken(@Param("token") String token);

    /** 删除某用户全部 Refresh Token（登出/强制下线） */
    int deleteByUserId(@Param("userId") Long userId);

    /** 按 Token 值删除 */
    int deleteByToken(@Param("token") String token);
}
