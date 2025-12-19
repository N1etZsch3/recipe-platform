package com.n1etzsch3.recipe.business.domain.query;

import lombok.Data;

@Data
public class RecipePageQuery {
    private Integer page = 1;
    private Integer size = 10;

    private Integer categoryId;
    private String keyword;

    // sort: new (最新), hot (最热)
    private String sort;

    private Long authorId;
    private Integer status; // 0: Auditing, 1: Published, 2: Rejected
}
