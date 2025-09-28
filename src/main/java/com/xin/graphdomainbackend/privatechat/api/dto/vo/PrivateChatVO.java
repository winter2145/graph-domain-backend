package com.xin.graphdomainbackend.privatechat.api.dto.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.xin.graphdomainbackend.user.api.dto.vo.UserVO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class PrivateChatVO implements Serializable {

    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 目标用户id
     */
    private Long targetUserId;

    /**
     * 最后一条消息内容
     */
    private String lastMessage;

    /**
     * 最后一条消息时间
     */
    private Date lastMessageTime;

    /**
     * 用户未读消息数
     */
    private Integer userUnreadCount;

    /**
     * 目标用户未读消息数
     */
    private Integer targetUserUnreadCount;

    /**
     * 用户自定义的私聊名称
     */
    private String userChatName;

    /**
     * 目标用户自定义的私聊名称
     */
    private String targetUserChatName;

    /**
     * 聊天类型：0-私信 1-好友(双向关注)
     */
    private Integer chatType;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 目标用户对象
     */
    private UserVO targetUser;

    /**
     * 是否是发送者
     */
    private Boolean isSender;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
