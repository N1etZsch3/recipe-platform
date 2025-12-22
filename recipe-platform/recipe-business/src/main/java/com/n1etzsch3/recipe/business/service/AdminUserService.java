package com.n1etzsch3.recipe.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.n1etzsch3.recipe.business.domain.dto.UserDTO;
import com.n1etzsch3.recipe.business.domain.dto.UserStatusDTO;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;

import java.util.List;

public interface AdminUserService {

    Result<IPage<UserDTO>> pageUsers(Integer page, Integer size, String keyword, String role, String sortBy);

    Result<?> addUser(SysUser user);

    Result<?> updateUser(Long id, SysUser user);

    Result<?> updateUserStatus(Long userId, UserStatusDTO statusDTO);

    Result<?> batchUpdateStatus(List<Long> ids, Integer status);
}
