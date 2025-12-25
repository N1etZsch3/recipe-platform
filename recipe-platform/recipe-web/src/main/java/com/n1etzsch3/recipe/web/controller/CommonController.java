package com.n1etzsch3.recipe.web.controller;

import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.framework.annotation.RateLimit;
import com.n1etzsch3.recipe.framework.manager.CosManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Tag(name = "通用接口")
@RestController
@RequestMapping("/api/v1/common")
@RequiredArgsConstructor
public class CommonController {

    private final CosManager cosManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Operation(summary = "文件上传")
    @RateLimit(time = 60, count = 20, limitType = RateLimit.LimitType.USER)
    @PostMapping("/upload")
    public Result<String> upload(@RequestPart("file") MultipartFile file) {
        log.info("收到文件上传请求: {}, size: {}", file.getOriginalFilename(), file.getSize());
        String url = cosManager.uploadFile(file);
        return Result.ok(url);
    }

    @Operation(summary = "获取幂等性Token")
    @GetMapping("/idempotent/token")
    public Result<String> getIdempotentToken() {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("idempotent:" + token, "1", 5, TimeUnit.MINUTES);
        log.info("生成幂等Token: {}", token);
        return Result.ok(token);
    }
}
