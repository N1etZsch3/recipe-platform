package com.n1etzsch3.recipe.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.n1etzsch3.recipe.business.domain.dto.AdminOperationLogDTO;
import com.n1etzsch3.recipe.business.entity.AdminOperationLog;
import com.n1etzsch3.recipe.business.mapper.AdminOperationLogMapper;
import com.n1etzsch3.recipe.business.service.AdminLogService;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 管理员操作日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogServiceImpl implements AdminLogService {

    private final AdminOperationLogMapper logMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public void log(String operationType, String targetType, Long targetId, String targetName, String detail) {
        try {
            Long adminId = getCurrentUserId();
            if (adminId == null) {
                log.warn("无法记录日志：未获取到管理员ID");
                return;
            }

            SysUser admin = sysUserMapper.selectById(adminId);
            String adminName = admin != null ? admin.getUsername() : "unknown";

            AdminOperationLog logEntity = new AdminOperationLog();
            logEntity.setAdminId(adminId);
            logEntity.setAdminName(adminName);
            logEntity.setOperationType(operationType);
            logEntity.setTargetType(targetType);
            logEntity.setTargetId(targetId);
            logEntity.setTargetName(targetName);
            logEntity.setDetail(detail);
            logEntity.setIpAddress(getClientIp());
            logEntity.setCreateTime(LocalDateTime.now());

            logMapper.insert(logEntity);
            log.info("操作日志记录: admin={}, type={}, target={}#{}", adminName, operationType, targetType, targetId);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    /**
     * 从 Spring Security 上下文获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Long) {
                return (Long) auth.getPrincipal();
            }
            // 尝试从 principal 转换
            if (auth != null && auth.getPrincipal() != null) {
                Object principal = auth.getPrincipal();
                if (principal instanceof String) {
                    return Long.parseLong((String) principal);
                }
            }
        } catch (Exception e) {
            log.warn("获取当前用户ID失败: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public void log(String operationType, String detail) {
        log(operationType, null, null, null, detail);
    }

    @Override
    public Result<IPage<AdminOperationLogDTO>> pageLogs(Integer page, Integer size,
            String operationType, Long adminId,
            String startDate, String endDate) {
        Page<AdminOperationLog> p = new Page<>(page, size);
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<AdminOperationLog>()
                .eq(StrUtil.isNotBlank(operationType), AdminOperationLog::getOperationType, operationType)
                .eq(adminId != null, AdminOperationLog::getAdminId, adminId);

        // 日期范围筛选
        if (StrUtil.isNotBlank(startDate)) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            wrapper.ge(AdminOperationLog::getCreateTime, start);
        }
        if (StrUtil.isNotBlank(endDate)) {
            LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            wrapper.le(AdminOperationLog::getCreateTime, end);
        }

        wrapper.orderByDesc(AdminOperationLog::getCreateTime);

        Page<AdminOperationLog> resultPage = logMapper.selectPage(p, wrapper);

        IPage<AdminOperationLogDTO> dtoPage = resultPage.convert(logEntity -> {
            AdminOperationLogDTO dto = new AdminOperationLogDTO();
            BeanUtil.copyProperties(logEntity, dto);
            dto.setOperationTypeName(AdminOperationLogDTO.getOperationTypeName(logEntity.getOperationType()));
            return dto;
        });

        return Result.ok(dtoPage);
    }

    @Override
    public void logLogin(Long adminId, String adminName, boolean success, String detail) {
        try {
            AdminOperationLog logEntity = new AdminOperationLog();
            logEntity.setAdminId(adminId);
            logEntity.setAdminName(adminName);
            logEntity.setOperationType(success ? "ADMIN_LOGIN" : "ADMIN_LOGIN_FAILED");
            logEntity.setTargetType("auth");
            logEntity.setTargetId(null);
            logEntity.setTargetName(null);
            logEntity.setDetail(detail);
            logEntity.setIpAddress(getClientIp());
            logEntity.setCreateTime(LocalDateTime.now());

            logMapper.insert(logEntity);
            log.info("登录日志记录: admin={}, success={}, ip={}", adminName, success, logEntity.getIpAddress());
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }

    /**
     * 获取客户端 IP 地址
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null)
                return null;

            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            // 多个代理时取第一个
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            // 将 IPv6 localhost 转为 IPv4
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
                ip = "127.0.0.1";
            }
            return ip;
        } catch (Exception e) {
            log.warn("获取客户端IP失败", e);
            return null;
        }
    }
}
