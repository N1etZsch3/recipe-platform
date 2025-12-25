package com.n1etzsch3.recipe.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("team_member")
public class TeamMember {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    private String role;

    private String avatar;

    private String emoji;

    private String color;

    @TableField("bg_color")
    private String bgColor;

    private String description;

    /** JSON 格式的技能标签数组 */
    private String skills; // JSON 格式的技能标签数组

    @TableField("git_type")
    private String gitType; // github 或 gitee

    private String github;

    private String email;

    private Integer sortOrder;

    private LocalDateTime createTime;
}
