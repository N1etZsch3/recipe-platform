package com.n1etzsch3.recipe.business.service;

import com.n1etzsch3.recipe.business.entity.TeamMember;
import com.n1etzsch3.recipe.common.core.domain.Result;

import java.util.List;

public interface TeamService {

    /**
     * 获取团队成员列表（公开接口）
     */
    Result<List<TeamMember>> listMembers();

    /**
     * 添加成员
     */
    Result<?> addMember(TeamMember member);

    /**
     * 更新成员
     */
    Result<?> updateMember(Integer id, TeamMember member);

    /**
     * 删除成员
     */
    Result<?> deleteMember(Integer id);
}
