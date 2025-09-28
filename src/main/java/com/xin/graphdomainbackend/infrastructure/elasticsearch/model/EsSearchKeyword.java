package com.xin.graphdomainbackend.infrastructure.elasticsearch.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

@Data
@Document(indexName = "search_keyword")
public class EsSearchKeyword {

    @Id
    private Long id;

    /**
     * 类型  user space picture
     */
    @Field(type = FieldType.Keyword)
    private String type;

    /**
     * 关键词  支持中英文混合搜索
     */
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "ik_smart"),
            otherFields = {
                    @InnerField(suffix = "ik", type = FieldType.Text, analyzer = "ik_smart"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)
            })
    private String keyword;
}
