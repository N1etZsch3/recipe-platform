package com.n1etzsch3.recipe.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.CommentDetailDTO;
import com.n1etzsch3.recipe.common.core.domain.Result;

public interface AdminCommentService {

    Result<IPage<CommentDetailDTO>> pageComments(Integer page, Integer size, String keyword,
            Long userId, Long recipeId, String sortBy);

    Result<?> deleteComment(Long commentId);
}
