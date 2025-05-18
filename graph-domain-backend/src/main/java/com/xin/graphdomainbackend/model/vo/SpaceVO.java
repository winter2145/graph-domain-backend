package com.xin.graphdomainbackend.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.xin.graphdomainbackend.model.entity.Space;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 空间视图
 */
@Data
public class SpaceVO implements Serializable {

    private static final long serialVersionUID = 2407196458511716583L;

    // id
    private Long id;

    // 空间名称
    private String spaceName;

    // 空间级别  0-普通版 1-专业版 2-旗舰版
    private Integer spaceLevel;

    // 空间类型 0-私有 1-团队
    private  Integer spaceType;;

    // 空间图片的最大总大小
    private Long maxSize;

    // 空间图片的最大数量
    private Long maxCount;

    // 当下空间图片的总大小
    private Long totalSize;

    // 当下空间的图片数量
    private Long totalCount;

    // 创建用户ID
    private Long useId;

    // 创建时间
    private Date createTime;

    // 编辑时间
    private Date editTime;

    // 更新时间
    private Date updateTime;

    // 创建用户信息
    private UserVO user;

    // 空间成员数量
    private Long memberCount;

    /**
     * spaceVO -> space
     * @param spaceVO 视图封装类
     * @return 视图对象
     */
    public static Space voToObj(SpaceVO spaceVO) {
        if (spaceVO == null) {
            return null;
        }
        Space space = new Space();
        BeanUtil.copyProperties(spaceVO, space);
        return space;
    }

    /**
     * space -> spaceVO
     *
     * @param space 视图对象
     * @return 视图封装类
     */
    public static SpaceVO objToVo(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = new SpaceVO();
        BeanUtil.copyProperties(space, spaceVO);
        return spaceVO;
    }
}
