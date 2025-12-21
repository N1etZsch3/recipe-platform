package com.n1etzsch3.recipe.system.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.n1etzsch3.recipe.common.constant.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 生成图形验证码
     * 
     * @return 包含 captchaId 和 imageBase64 的 Map
     */
    public Map<String, String> generateCaptcha() {
        // 生成验证码 (宽、高、字符数、干扰线数)
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 50);

        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String code = captcha.getCode().toLowerCase();
        String imageBase64 = captcha.getImageBase64Data();

        // 存入 Redis，5分钟有效
        String key = CacheConstants.KEY_CAPTCHA + captchaId;
        redisTemplate.opsForValue().set(key, code, CacheConstants.TTL_CAPTCHA, TimeUnit.SECONDS);

        log.debug("生成验证码: captchaId={}, code={}", captchaId, code);

        Map<String, String> result = new HashMap<>();
        result.put("captchaId", captchaId);
        result.put("imageBase64", imageBase64);
        return result;
    }

    /**
     * 验证验证码
     * 
     * @param captchaId 验证码ID
     * @param code      用户输入的验证码
     * @return 是否验证通过
     */
    public boolean validateCaptcha(String captchaId, String code) {
        if (captchaId == null || code == null) {
            return false;
        }

        String key = CacheConstants.KEY_CAPTCHA + captchaId;
        String storedCode = (String) redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            log.debug("验证码不存在或已过期: captchaId={}", captchaId);
            return false;
        }

        // 验证后立即删除，防止重复使用
        redisTemplate.delete(key);

        boolean result = storedCode.equalsIgnoreCase(code.toLowerCase());
        log.debug("验证码验证结果: captchaId={}, result={}", captchaId, result);
        return result;
    }
}
