package com.xin.graphdomainbackend.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.xin.graphdomainbackend.model.vo.comment.CommentUserVO;
import com.xin.graphdomainbackend.model.vo.comment.CommentsVO;
import lombok.Data;
import org.springframework.context.annotation.Bean;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视图（脱敏）
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = -3113775889295732572L;

    // id
    private Long id;

    // 账号
    private String userAccount;

    // 用户昵称
    private String userName;

    // 用户头像
    private String userAvatar;

    // 邮箱
    private String email;

    // 用户简介
    private String userProfile;

    // 用户角色
    private String userRole;

    // 创建时间
    private Date createTime;

    // 在线状态
    private Boolean online;

    public static CommentUserVO objToCommentUserVO(UserVO userVO) {
        if (userVO == null) {
            return null;
        }
        CommentUserVO commentUserVO = new CommentUserVO();
        BeanUtil.copyProperties(userVO, commentUserVO);

        return commentUserVO;
    }

}
