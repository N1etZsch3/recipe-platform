package com.n1etzsch3.recipe.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.n1etzsch3.recipe.business.entity.TeamMember;
import com.n1etzsch3.recipe.business.mapper.TeamMemberMapper;
import com.n1etzsch3.recipe.business.service.AdminLogService;
import com.n1etzsch3.recipe.business.service.TeamService;
import com.n1etzsch3.recipe.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamMemberMapper teamMemberMapper;
    private final AdminLogService adminLogService;

    @Override
    public Result<List<TeamMember>> listMembers() {
        List<TeamMember> members = teamMemberMapper.selectList(
                new LambdaQueryWrapper<TeamMember>()
                        .orderByAsc(TeamMember::getSortOrder)
                        .orderByAsc(TeamMember::getId));
        return Result.ok(members);
    }

    @Override
    public Result<?> addMember(TeamMember member) {
        member.setCreateTime(LocalDateTime.now());
        if (member.getSortOrder() == null) {
            member.setSortOrder(0);
        }
        teamMemberMapper.insert(member);
        adminLogService.log("TEAM_MEMBER_ADD", "team_member", Long.valueOf(member.getId()), member.getName(), null);
        return Result.ok("添加成功");
    }

    @Override
    public Result<?> updateMember(Integer id, TeamMember member) {
        TeamMember existing = teamMemberMapper.selectById(id);
        if (existing == null) {
            return Result.fail("成员不存在");
        }

        existing.setName(member.getName());
        existing.setRole(member.getRole());
        existing.setAvatar(member.getAvatar());
        existing.setEmoji(member.getEmoji());
        existing.setColor(member.getColor());
        existing.setBgColor(member.getBgColor());
        existing.setDescription(member.getDescription());
        existing.setSkills(member.getSkills());
        existing.setGitType(member.getGitType());
        existing.setGithub(member.getGithub());
        existing.setEmail(member.getEmail());
        existing.setSortOrder(member.getSortOrder());

        teamMemberMapper.updateById(existing);
        adminLogService.log("TEAM_MEMBER_UPDATE", "team_member", Long.valueOf(id), existing.getName(), null);
        return Result.ok("修改成功");
    }

    @Override
    public Result<?> deleteMember(Integer id) {
        TeamMember member = teamMemberMapper.selectById(id);
        String memberName = member != null ? member.getName() : "ID:" + id;
        teamMemberMapper.deleteById(id);
        adminLogService.log("TEAM_MEMBER_DELETE", "team_member", Long.valueOf(id), memberName, null);
        return Result.ok("删除成功");
    }
}
