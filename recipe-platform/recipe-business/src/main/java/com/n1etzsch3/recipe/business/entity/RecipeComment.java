package com.n1etzsch3.recipe.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recipe_comment")
public class RecipeComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recipeId;
    private Long userId;
    private Long parentId; // 父评论ID，null表示顶级评论
    private String content;
    private Integer likeCount; // 点赞数
    private LocalDateTime createTime;
}
