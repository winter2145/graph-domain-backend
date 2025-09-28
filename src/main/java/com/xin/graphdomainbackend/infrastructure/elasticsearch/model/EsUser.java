package com.xin.graphdomainbackend.infrastructure.elasticsearch.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.io.Serializable;
import java.util.Date;

@Data
@Document(indexName = "user")
public class EsUser implements Serializable {

    private static final long serialVersionUID = 3543206548686181425L;

    /**
     * id
     */
    @Id
    private Long id;

    /**
     * 账号：支持英文和数字搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)
    )
    private String userAccount;

    /**
     * 用户昵称：支持中英文混合搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "mix", type = FieldType.Text, analyzer = "text_analyzer"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)
            }
    )
    private String userName;

    /**
     * 用户简介：支持中英文混合搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "text_analyzer"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)
    )
    private String userProfile;

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
