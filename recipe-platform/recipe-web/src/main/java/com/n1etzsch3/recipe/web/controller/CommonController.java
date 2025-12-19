package com.n1etzsch3.recipe.web.controller;

import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.framework.manager.CosManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "通用接口")
@RestController
@RequestMapping("/api/v1/common")
@RequiredArgsConstructor
public class CommonController {

    private final CosManager cosManager;

    @Operation(summary = "文件上传")
    @PostMapping("/upload")
    public Result<String> upload(@RequestPart("file") MultipartFile file) {
        log.info("收到文件上传请求: {}, size: {}", file.getOriginalFilename(), file.getSize());
        String url = cosManager.uploadFile(file);
        return Result.ok(url);
    }
}
