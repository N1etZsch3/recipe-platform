package com.n1etzsch3.recipe.business.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.n1etzsch3.recipe.business.domain.dto.UserDTO;
import com.n1etzsch3.recipe.business.domain.dto.UserStatusDTO;
import com.n1etzsch3.recipe.business.entity.RecipeInfo;
import com.n1etzsch3.recipe.business.service.AdminLogService;
import com.n1etzsch3.recipe.business.service.AdminUserService;
import com.n1etzsch3.recipe.common.constant.UserConstants;
import com.n1etzsch3.recipe.common.core.domain.Result;
import com.n1etzsch3.recipe.system.entity.SysUser;
import com.n1etzsch3.recipe.system.mapper.SysUserMapper;
import com.n1etzsch3.recipe.business.mapper.RecipeInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final SysUserMapper sysUserMapper;
    private final RecipeInfoMapper recipeInfoMapper;
    private final PasswordEncoder passwordEncoder;
    private final AdminLogService adminLogService;

    @Override
    public Result<IPage<UserDTO>> pageUsers(Integer page, Integer size, String keyword, String role, String sortBy) {
        Page<SysUser> p = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .ne(SysUser::getRole, UserConstants.ROLE_ADMIN)
                .eq(StrUtil.isNotBlank(role), SysUser::getRole, role)
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(SysUser::getNickname, keyword)
                        .or()
                        .like(SysUser::getUsername, keyword));

        if ("oldest".equals(sortBy)) {
            wrapper.orderByAsc(SysUser::getCreateTime);
        } else {
            wrapper.orderByDesc(SysUser::getCreateTime);
        }

        Page<SysUser> result = sysUserMapper.selectPage(p, wrapper);

        Map<Long, Long> recipeCountMap = new HashMap<>();
        List<SysUser> users = result.getRecords();
        if (!users.isEmpty()) {
            List<Long> userIds = new java.util.ArrayList<>();
            for (SysUser user : users) {
                userIds.add(user.getId());
            }

            QueryWrapper<RecipeInfo> countWrapper = new QueryWrapper<>();
            countWrapper.select("user_id", "COUNT(*) as cnt");
            countWrapper.in("user_id", userIds);
            countWrapper.groupBy("user_id");
            List<Map<String, Object>> countList = recipeInfoMapper.selectMaps(countWrapper);
            if (countList != null) {
                for (Map<String, Object> row : countList) {
                    Long uid = Convert.toLong(row.get("user_id"));
                    Long cnt = Convert.toLong(row.get("cnt"));
                    if (uid != null) {
                        recipeCountMap.put(uid, cnt != null ? cnt : 0L);
                    }
                }
            }
        }

        IPage<UserDTO> dtoPage = result.convert(user -> {
            UserDTO dto = new UserDTO();
            BeanUtil.copyProperties(user, dto);
            dto.setPassword(null);
            dto.setRecipeCount(recipeCountMap.getOrDefault(user.getId(), 0L));
            return dto;
        });

        return Result.ok(dtoPage);
    }

    @Override
    public Result<?> addUser(SysUser user) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) {
            return Result.fail("用户名已存在");
        }

        if (UserConstants.ROLE_ADMIN.equals(user.getRole())) {
            return Result.fail("无法创建超级管理员");
        }

        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        if (StrUtil.isNotBlank(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            return Result.fail("密码不能为空");
        }

        if (user.getStatus() == null) {
            user.setStatus(UserConstants.NORMAL);
        }

        sysUserMapper.insert(user);
        adminLogService.log("USER_ADD", "user", user.getId(), user.getUsername(), null);
        return Result.ok("添加成功");
    }

    @Override
    public Result<?> updateUser(Long id, SysUser user) {
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) {
            return Result.fail("用户不存在");
        }

        if (UserConstants.ROLE_ADMIN.equals(existing.getRole())) {
            return Result.fail("无法修改超级管理员信息");
        }

        if (UserConstants.ROLE_ADMIN.equals(user.getRole())) {
            return Result.fail("无法设置为超级管理员角色");
        }

        existing.setNickname(user.getNickname());
        existing.setRole(user.getRole());
        existing.setIntro(user.getIntro());
        existing.setUpdateTime(LocalDateTime.now());

        if (user.getAvatar() != null) {
            existing.setAvatar(user.getAvatar());
        }

        if (StrUtil.isNotBlank(user.getPassword())) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        sysUserMapper.updateById(existing);
        adminLogService.log("USER_UPDATE", "user", id, existing.getUsername(), null);
        return Result.ok("修改成功");
    }

    @Override
    public Result<?> updateUserStatus(Long userId, UserStatusDTO statusDTO) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        user.setStatus(statusDTO.getStatus());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        String operationType = statusDTO.getStatus() == UserConstants.DISABLE ? "USER_BAN" : "USER_UNBAN";
        adminLogService.log(operationType, "user", userId, user.getNickname(), null);

        return Result.ok("状态修改成功");
    }

    @Override
    public Result<?> batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("未选择用户");
        }

        Long adminCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, ids)
                .eq(SysUser::getRole, UserConstants.ROLE_ADMIN));
        if (adminCount > 0) {
            return Result.fail("无法更改超级管理员状态");
        }

        SysUser update = new SysUser();
        update.setStatus(status);
        update.setUpdateTime(LocalDateTime.now());

        sysUserMapper.update(update, new LambdaQueryWrapper<SysUser>().in(SysUser::getId, ids));

        String operationType = status == UserConstants.DISABLE ? "USER_BATCH_BAN" : "USER_BATCH_UNBAN";
        adminLogService.log(operationType, "user", 0L, "Batch count: " + ids.size(), null);

        return Result.ok("批量操作成功");
    }
}
