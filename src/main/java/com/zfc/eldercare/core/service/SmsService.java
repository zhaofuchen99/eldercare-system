package com.zfc.eldercare.core.service;

/**
 * 短信服务接口（详细设计文档 5.1 / 6.1 外部依赖）。
 * 当前提供本地模拟实现；接入阿里云短信时新增实现类即可，业务代码无需改动。
 */
public interface SmsService {

    /**
     * 发送验证码到手机号。
     * 包含限流：同一手机号 3 次/分钟、10 次/天（文档 8.4）。
     */
    void sendCode(String phone);

    /**
     * 校验验证码，正确则标记该验证码为已使用（防止复用）。
     *
     * @throws com.zfc.eldercare.core.exception.BusinessException 验证码错误或已过期
     */
    void verifyCode(String phone, String code);

    /**
     * 发送业务通知短信（预约成功/取消等，文档 5.5）。
     * 开发期为日志模拟；接入真实短信后替换实现，业务代码无需改动。
     */
    void sendText(String phone, String content);
}
