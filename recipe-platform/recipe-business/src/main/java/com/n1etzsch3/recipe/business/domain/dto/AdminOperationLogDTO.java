package com.n1etzsch3.recipe.business.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志查询返回 DTO
 */
@Data
public class AdminOperationLogDTO {

    private Long id;

    private Long adminId;

    private String adminName;

    private String operationType;

    private String operationTypeName;

    private String targetType;

    private Long targetId;

    private String targetName;

    private String detail;

    private String ipAddress;

    private LocalDateTime createTime;

    /**
     * 获取操作类型的中文名称
     */
    public static String getOperationTypeName(String type) {
        if (type == null)
            return "";
        return switch (type) {
            case "ADMIN_LOGIN" -> "管理员登录";
            case "USER_BAN" -> "封禁用户";
            case "USER_UNBAN" -> "解封用户";
            case "RECIPE_APPROVE" -> "审核通过菜谱";
            case "RECIPE_REJECT" -> "驳回菜谱";
            case "RECIPE_DELETE" -> "删除菜谱";
            case "CATEGORY_ADD" -> "新增分类";
            case "CATEGORY_UPDATE" -> "修改分类";
            case "CATEGORY_DELETE" -> "删除分类";
            case "COMMENT_DELETE" -> "删除评论";
            default -> type;
        };
    }
}
