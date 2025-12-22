package com.n1etzsch3.recipe.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.n1etzsch3.recipe.business.domain.vo.UserVO;
import com.n1etzsch3.recipe.business.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    @Select("""
            SELECT u.id, u.nickname, u.avatar, u.intro
            FROM user_follow f
            JOIN sys_user u ON f.followed_id = u.id
            WHERE f.follower_id = #{followerId}
            ORDER BY f.create_time DESC
            """)
    IPage<UserVO> selectFollowedUsers(Page<?> page, @Param("followerId") Long followerId);
}
