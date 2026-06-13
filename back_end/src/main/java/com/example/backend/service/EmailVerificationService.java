package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码服务
 * 使用 Redis 存储验证码，支持分布式部署、自动过期
 */
@Service
public class EmailVerificationService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /** Redis key 前缀 */
    private static final String CODE_KEY_PREFIX = "email:code:";
    private static final String LOCK_KEY_PREFIX = "email:lock:";

    /** 验证码有效期：5 分钟 */
    private static final long CODE_EXPIRE_SECONDS = 5 * 60;

    /** 发送间隔限制：60 秒 */
    private static final long SEND_INTERVAL_SECONDS = 60;

    public EmailVerificationService(JavaMailSender mailSender, StringRedisTemplate redisTemplate) {
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 发送验证码到指定邮箱
     *
     * @param email 目标邮箱
     * @return 发送结果描述，null 表示成功
     */
    public String sendVerificationCode(String email) {
        // 检查发送频率（Redis 频率锁）
        String lockKey = LOCK_KEY_PREFIX + email;
        Boolean lockExists = redisTemplate.hasKey(lockKey);
        if (Boolean.TRUE.equals(lockExists)) {
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            long remaining = ttl != null && ttl > 0 ? ttl : 1;
            return "发送过于频繁，请 " + remaining + " 秒后重试";
        }

        // 生成 6 位数字验证码
        String code = generateCode();

        try {
            // 发送邮件
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("【气象数据平台】注册验证码");
            message.setText("您好！\n\n"
                    + "您正在注册气象数据管理与分析平台账户，您的验证码为：\n\n"
                    + "    " + code + "\n\n"
                    + "验证码有效期为 5 分钟，请尽快完成注册。\n"
                    + "如果这不是您的操作，请忽略此邮件。\n\n"
                    + "—— 气象数据管理与分析系统");
            mailSender.send(message);

            // 验证码写入 Redis，5 分钟过期
            String codeKey = CODE_KEY_PREFIX + email;
            redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

            // 写入频率锁，60 秒过期
            redisTemplate.opsForValue().set(lockKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

            return null; // null 表示成功
        } catch (Exception e) {
            return "邮件发送失败: " + e.getMessage();
        }
    }

    /**
     * 校验验证码
     *
     * @param email 邮箱
     * @param code  用户输入的验证码
     * @return true=校验通过
     */
    public boolean verifyCode(String email, String code) {
        if (email == null || code == null) {
            return false;
        }

        String codeKey = CODE_KEY_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            return false;
        }

        // 比较验证码（忽略大小写）
        boolean match = storedCode.equalsIgnoreCase(code.trim());
        if (match) {
            // 验证成功后删除，防止重复使用
            redisTemplate.delete(codeKey);
        }
        return match;
    }

    /**
     * 生成 6 位随机数字验证码
     */
    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
