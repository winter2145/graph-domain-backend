package com.xin.graphdomainbackend.model.vo.share;

import com.xin.graphdomainbackend.model.vo.UserVO;
import lombok.Data;

import java.util.Date;

/**
 * 分享记录视图
 */
@Data
public class ShareRecordVO {
    /**
     * 分享ID
     */
    private Long id;

    /**
     * 分享时间
     */
    private Date shareTime;

    /**
     * 分享用户信息
     */
    private UserVO user;

    /**
     * 内容类型：1-图片
     */
    private Integer targetType;

    /**
     * 被分享的内容（根据targetType可能是PictureVO/Post/SpaceVO）
     */
    private Object target;
}
