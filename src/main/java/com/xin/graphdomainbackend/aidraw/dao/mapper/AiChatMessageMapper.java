package com.xin.graphdomainbackend.aidraw.dao.mapper;

import com.xin.graphdomainbackend.aidraw.dao.entity.AiChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
* @author Administrator
* @description 针对表【ai_chat_message(AI聊天消息表，存储用户与AI的对话记录)】的数据库操作Mapper
* @createDate 2025-10-05 10:48:14
* @Entity com.xin.graphdomainbackend.infrastructure.ai.dao.entity.AiChatMessage
*/
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {

    @Update("UPDATE ai_chat_message " +
            "SET imageUrl = #{imageUrl}, "+
            "content = #{content} " +
            "WHERE id = #{id}")
    Boolean updateAssistantById(@Param("id") Long id, @Param("content") String content, @Param("imageUrl") String imageUrl);

    @Select("SELECT MAX(roundId) FROM ai_chat_message WHERE sessionId = #{sessionId}")
    Long findMaxRoundIdBySessionId(@Param("sessionId") Long sessionId);

}




