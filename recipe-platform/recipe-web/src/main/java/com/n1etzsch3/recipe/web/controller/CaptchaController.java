package com.n1etzsch3.recipe.web.controller;

import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 验证码接口
 */
@Tag(name = "验证码接口")
@RestController
@RequestMapping("/api/v1/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @Operation(summary = "获取图形验证码")
    @GetMapping
    public Result<Map<String, String>> getCaptcha() {
        Map<String, String> captchaData = captchaService.generateCaptcha();
        return Result.ok(captchaData);
    }
}
