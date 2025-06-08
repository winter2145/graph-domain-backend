package com.xin.graphdomainbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 按照颜色搜索图片请求
 */
@Data
public class SearchPictureByColorRequest implements Serializable {

    private static final long serialVersionUID = 1114754932989389383L;

    // 图片主色调
    private String picColor;

    // 空间id
    private Long spaceId;
}
