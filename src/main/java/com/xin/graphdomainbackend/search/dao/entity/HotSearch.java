package com.xin.graphdomainbackend.search.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 热门搜索记录表
 * @TableName hot_search
 */
@TableName(value ="hot_search")
@Data
public class HotSearch implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 搜索类型
     */
    private String type;

    /**
     * 搜索次数
     */
    private Long count;

    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}