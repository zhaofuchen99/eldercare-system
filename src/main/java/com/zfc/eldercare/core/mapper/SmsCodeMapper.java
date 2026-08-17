package com.zfc.eldercare.core.mapper;

import com.zfc.eldercare.core.entity.SmsCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 短信验证码表 Mapper。
 */
@Mapper
public interface SmsCodeMapper {

    /** 新增验证码记录 */
    int insert(SmsCode smsCode);

    /** 查某手机号最新一条未使用且未过期的验证码 */
    SmsCode selectLatestUnused(@Param("phone") String phone);

    /** 标记验证码已使用 */
    int markUsed(@Param("id") Long id);
}
