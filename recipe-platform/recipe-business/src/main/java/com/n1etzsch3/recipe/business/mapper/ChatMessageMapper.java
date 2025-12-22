package com.n1etzsch3.recipe.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.n1etzsch3.recipe.business.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("""
            SELECT m.*
            FROM sys_message m
            JOIN (
                SELECT other_id, MAX(id) AS max_id
                FROM (
                    SELECT id,
                           CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END AS other_id
                    FROM sys_message
                    WHERE sender_id = #{userId} OR receiver_id = #{userId}
                ) t
                GROUP BY other_id
            ) latest
            ON m.id = latest.max_id
            ORDER BY m.create_time DESC
            """)
    List<ChatMessage> selectLatestConversations(@Param("userId") Long userId);
}
