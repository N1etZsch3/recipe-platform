package com.n1etzsch3.recipe.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.n1etzsch3.recipe.system.entity.SysUser;

public interface UserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     */
    SysUser getByUsername(String username);
}
