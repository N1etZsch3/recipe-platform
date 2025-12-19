package com.n1etzsch3.recipe.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recipe_info")
public class RecipeInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Long userId;
    private Integer categoryId;
    private String coverImage;
    private String description;
    /**
     * 状态: 0-待审核, 1-已发布, 2-驳回
     */
    private Integer status;
    private String rejectReason;
    private Integer viewCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
