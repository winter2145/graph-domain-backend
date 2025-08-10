package com.xin.graphdomainbackend.model.vo.comment;

import lombok.Data;

import java.io.Serializable;

/**
 * 评论用户响应类
 */
@Data
public class CommentUserVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;
}
