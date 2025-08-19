package com.xin.graphdomainbackend.model.vo.comment;

import cn.hutool.core.bean.BeanUtil;
import com.xin.graphdomainbackend.model.vo.UserVO;
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

    /**
     * UserVO -> CommentUserVO
     * @param userVO 用户VO对象
     */
    public static CommentUserVO objToCommentUserVO(UserVO userVO) {
        if (userVO == null) {
            return null;
        }
        CommentUserVO commentUserVO = new CommentUserVO();
        BeanUtil.copyProperties(userVO, commentUserVO);

        return commentUserVO;
    }
}
