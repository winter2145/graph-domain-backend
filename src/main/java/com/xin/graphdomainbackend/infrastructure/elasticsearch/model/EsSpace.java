package com.xin.graphdomainbackend.infrastructure.elasticsearch.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.io.Serializable;
import java.util.Date;

@Document(indexName = "space")
@Data
public class EsSpace implements Serializable {

    /**
     * id
     */
    @Id
    private Long id;

    /**
     * 空间名称 支持中英文混合搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "ik", type = FieldType.Text, analyzer = "ik_smart"),
                    @InnerField(suffix = "standard", type = FieldType.Text, analyzer = "standard")
            }
    )
    private String spaceName;

    /**
     * 空间类型：0-私有 1-团队
     */
    @Field(type = FieldType.Integer)
    private Integer spaceType;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time,  pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date createTime;

    /**
     * 是否删除
     */
    @Field(type = FieldType.Integer)
    private Integer isDelete;

}
