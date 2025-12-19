package com.n1etzsch3.recipe.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String intro;
    /**
     * 角色: admin-管理员, member-普通用户
     */
    private String role;
    /**
     * 状态: 1-正常, 0-封禁
     */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
