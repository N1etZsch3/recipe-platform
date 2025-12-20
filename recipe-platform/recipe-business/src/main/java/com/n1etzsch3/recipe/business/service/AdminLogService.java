package com.n1etzsch3.recipe.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.AdminOperationLogDTO;
import com.n1etzsch3.recipe.common.core.domain.Result;

/**
 * 管理员操作日志服务
 */
public interface AdminLogService {

    /**
     * 记录操作日志
     *
     * @param operationType 操作类型
     * @param targetType    目标类型
     * @param targetId      目标ID
     * @param targetName    目标名称
     * @param detail        操作详情
     */
    void log(String operationType, String targetType, Long targetId, String targetName, String detail);

    /**
     * 记录简单日志（无目标）
     */
    void log(String operationType, String detail);

    /**
     * 分页查询操作日志
     */
    Result<IPage<AdminOperationLogDTO>> pageLogs(Integer page, Integer size,
            String operationType, Long adminId,
            String startDate, String endDate);
}
