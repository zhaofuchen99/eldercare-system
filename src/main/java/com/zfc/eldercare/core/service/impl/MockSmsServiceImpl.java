package com.zfc.eldercare.core.service.impl;

import com.zfc.eldercare.core.entity.SmsCode;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.SmsCodeMapper;
import com.zfc.eldercare.core.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 短信服务本地模拟实现（开发期）。
 * 验证码不真发短信，打印到日志方便本地测试；接入阿里云短信时替换本实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockSmsServiceImpl implements SmsService {

    /** 验证码有效期（分钟） */
    private static final long CODE_EXPIRE_MINUTES = 5;

    private final SmsCodeMapper smsCodeMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void sendCode(String phone) {
        rateLimit(phone);

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));

        SmsCode smsCode = new SmsCode();
        smsCode.setPhone(phone);
        smsCode.setCode(code);
        smsCode.setExpireTime(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES));
        smsCode.setUsed(0);
        smsCodeMapper.insert(smsCode);

        // 开发期模拟：验证码打印到日志。接入阿里云短信后改为真实发送。
        log.info("【模拟短信】向 {} 发送验证码：{}（{} 分钟内有效）", phone, code, CODE_EXPIRE_MINUTES);
    }

    @Override
    public void sendText(String phone, String content) {
        // 开发期模拟：通知内容打印到日志。接入阿里云短信后改为真实发送。
        log.info("【模拟短信】向 {} 发送通知：{}", phone, content);
    }

    @Override
    public void verifyCode(String phone, String code) {
        SmsCode record = smsCodeMapper.selectLatestUnused(phone);
        if (record == null || !record.getCode().equals(code)) {
            throw new BusinessException("验证码错误或已过期");
        }
        smsCodeMapper.markUsed(record.getId());
    }

    /** 限流：同一手机号 3 次/分钟、10 次/天（文档 8.4） */
    private void rateLimit(String phone) {
        // 3 次/分钟
        String minuteKey = "rl:sms:" + phone;
        Long minuteCount = redisTemplate.opsForValue().increment(minuteKey);
        if (minuteCount != null && minuteCount == 1L) {
            redisTemplate.expire(minuteKey, Duration.ofMinutes(1));
        }
        if (minuteCount != null && minuteCount > 3) {
            throw new BusinessException(429, "短信发送过于频繁，请 1 分钟后再试");
        }

        // 10 次/天
        String dayKey = "rl:sms:day:" + phone + ":" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Long dayCount = redisTemplate.opsForValue().increment(dayKey);
        if (dayCount != null && dayCount == 1L) {
            redisTemplate.expire(dayKey, Duration.ofDays(1));
        }
        if (dayCount != null && dayCount > 10) {
            throw new BusinessException(429, "今日短信发送次数已达上限");
        }
    }
}
