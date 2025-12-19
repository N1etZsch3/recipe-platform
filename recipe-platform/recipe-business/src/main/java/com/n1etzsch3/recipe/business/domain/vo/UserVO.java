package com.n1etzsch3.recipe.business.domain.vo;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String nickname;
    private String avatar;
    private String intro;
    // 是否互相关注 (可选)
    private Boolean isMutualFollow;
    // 是否已关注
    private Boolean isFollow;
}
