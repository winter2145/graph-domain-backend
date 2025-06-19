package com.xin.graphdomainbackend.model.vo.message.chat;

import com.xin.graphdomainbackend.model.vo.message.chat.ChatMessageVO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 聊天历史分页响应类
 */
@Data
public class ChatHistoryPageResponse implements Serializable {

    private static final long serialVersionUID = -1728113847313942626L;

    /**
     * 消息记录
     */
    private List<ChatMessageVO> records;

    /**
     * 当前页号
     */
    private Long current;

    /**
     * 每页条数
     */
    private Long size;

    /**
     * 总数
     */
    private Long total;
}
