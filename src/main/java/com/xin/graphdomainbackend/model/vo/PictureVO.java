package com.xin.graphdomainbackend.model.vo;

import cn.hutool.json.JSONUtil;
import com.xin.graphdomainbackend.model.entity.Picture;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class PictureVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * webp url
     */
    private String webpUrl;

    /**
     * 空间 id（为空表示公共空间）
     */
    private Long spaceId;

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 评论数
     */
    private Long commentCount;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 分享数
     */
    private Long shareCount;

    /**
     * 浏览量
     */
    private Long viewCount;

    /**
     * 当前用户是否点赞
     */
    private Integer isLiked;

    /**
     * 创建用户信息
     */
    private UserVO user;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();

    /**
     * VO ->　实体类
     * 封装类转对象
     */
   public static Picture voToObj(PictureVO pictureVO) {
       if (pictureVO == null) {
           return null;
       }
       Picture picture = new Picture();
       BeanUtils.copyProperties(pictureVO, picture);
       // 类型不同，需要转换
       picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));
       return picture;
   }

    /**
     * 实体类 -> VO
     * 对象转封装类
     */
    public static PictureVO objToVo(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtils.copyProperties(picture, pictureVO);
        // 类型不同，需要转换
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        return pictureVO;
    }

}
