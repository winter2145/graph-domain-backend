package com.xin.graphdomainbackend.aidraw.dao.mapper;

import com.xin.graphdomainbackend.aidraw.dao.entity.AiChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
* @author Administrator
* @description 针对表【ai_chat_session(AI聊天会话表，记录用户的聊天会话信息)】的数据库操作Mapper
* @createDate 2025-10-05 10:48:14
* @Entity com.xin.graphdomainbackend.infrastructure.ai.dao.entity.AiChatSession
*/
public interface AiChatSessionMapper extends BaseMapper<AiChatSession> {

    @Update("UPDATE ai_chat_session " +
            "SET title = #{title} " +
            "WHERE id = #{sessionId}")
    Boolean updateTitle(@Param("sessionId") Long sessionId, @Param("title") String title);
}




