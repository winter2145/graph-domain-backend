package com.xin.graphdomainbackend.model.vo.comment;

import cn.hutool.json.JSONUtil;
import com.xin.graphdomainbackend.model.dto.PageRequest;
import com.xin.graphdomainbackend.model.entity.Comments;
import com.xin.graphdomainbackend.model.entity.Picture;
import com.xin.graphdomainbackend.model.vo.PictureVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 评论响应类
 */
@Data
public class CommentsVO extends PageRequest implements Serializable {
    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 目标类型（1-图片 2-帖子）
     */
    private Integer targetType;

    /**
     * 目标用户ID
     */
    private Long targetUserId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论ID
     */
    private Long parentId;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 点踩数
     */
    private Long dislikeCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 评论用户信息
     */
    private CommentUserVO commentUser;

    /**
     * 图片信息（当 targetType = 1 时）
     */
    private PictureVO picture;


    /**
     * 子评论列表
     */
    private List<CommentsVO> children;


    /**
     * VO ->　实体类
     * 封装类转对象
     */
    public static Comments voToObj(CommentsVO commentsVO) {
        if (commentsVO == null) {
            return null;
        }
        Comments comments = new Comments();
        BeanUtils.copyProperties(commentsVO, comments);

        return comments;
    }

    /**
     * 实体类 -> VO
     * 对象转封装类
     */
    public static CommentsVO objToVo(Comments comments) {
        if (comments == null) {
            return null;
        }
        CommentsVO commentsVO = new CommentsVO();
        BeanUtils.copyProperties(comments, commentsVO);

        return commentsVO;
    }
}
