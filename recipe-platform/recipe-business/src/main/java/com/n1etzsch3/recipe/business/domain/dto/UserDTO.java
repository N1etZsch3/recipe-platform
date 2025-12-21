package com.n1etzsch3.recipe.business.domain.dto;

import com.n1etzsch3.recipe.system.entity.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserDTO extends SysUser {
    private Long recipeCount;
}
