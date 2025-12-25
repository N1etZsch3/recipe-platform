package com.n1etzsch3.recipe.web.controller;

import com.n1etzsch3.recipe.business.entity.TeamMember;
import com.n1etzsch3.recipe.business.service.TeamService;
import com.n1etzsch3.recipe.common.core.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 团队成员公开接口
 */
@RestController
@RequestMapping("/api/v1/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /**
     * 获取团队成员列表
     */
    @GetMapping("/members")
    public Result<List<TeamMember>> listMembers() {
        return teamService.listMembers();
    }
}
