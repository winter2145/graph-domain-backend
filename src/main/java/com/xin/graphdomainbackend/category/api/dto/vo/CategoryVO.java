package com.xin.graphdomainbackend.category.api.dto.vo;

import lombok.Data;

import java.util.Date;

/**
 * 分类 视图
 */
@Data
public class CategoryVO {

    /**
     * 分类id
     */
    private Long id;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类类型：0-图片分类
     */
    private Integer type;

    /**
     * 创建时间
     */
    private Date createTime;

}
