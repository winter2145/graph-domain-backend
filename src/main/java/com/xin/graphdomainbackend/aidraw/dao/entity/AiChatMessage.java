package com.xin.graphdomainbackend.aidraw.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import lombok.Data;

/**
 * AI聊天消息表，存储用户与AI的对话记录
 * @TableName ai_chat_message
 */
@TableName(value ="ai_chat_message")
@Data
public class AiChatMessage implements Serializable {
    /**
     * 消息ID，主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID，外键关联ai_chat_session表
     */
    private Long sessionId;

    /**
     * 轮次Id
     */
    private Long roundId;

    /**
     * 消息角色：user / assistant / system
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}