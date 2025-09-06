package com.xin.graphdomainbackend.model.entity.es;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.classgraph.json.Id;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.Date;

@Data
@Document(indexName = "search_keyword")
public class EsSearchKeyword {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Date createTime;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "ik_smart"),
            otherFields = {
                    @InnerField(suffix = "ik", type = FieldType.Text, analyzer = "ik_smart"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword, ignoreAbove = 256)
            })
    private String keyword;
}
