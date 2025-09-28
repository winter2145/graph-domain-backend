package com.xin.graphdomainbackend.infrastructure.elasticsearch.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Document(indexName = "picture")
public class EsPicture implements Serializable {

    private static final long serialVersionUID = 535891074015120043L;

    @Id // 主键
    private Long id;

    /**
     * 图片名称：支持中英文混合搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = {
                    @InnerField(suffix = "mix", type = FieldType.Text, analyzer = "text_analyzer"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)
            }
    )
    private String name;

    /**
     * 简介：支持中英文混合搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "text_analyzer"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)
            }
    )
    private String introduction;

    /**
     * 分类
     */
    @Field(type = FieldType.Keyword)
    private String category;

    /**
     * 标签：支持中英文混合搜索,精准搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "ik", type = FieldType.Text, analyzer = "ik_smart"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)
            }
    )
    private List<String> tags;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    @Field(type = FieldType.Integer)
    private Integer reviewStatus;

    /**
     * 空间 id
     */
    @Field(type = FieldType.Long)
    private Long spaceId;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date createTime;

    /**
     * 是否删除
     */
    @Field(type = FieldType.Integer)
    private Integer isDelete;

}
