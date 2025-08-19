package com.xin.graphdomainbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片更新请求
 */
@Data
public class PictureUpdateRequest implements Serializable {

    private static final long serialVersionUID = 5797846678650628418L;

    private Long id;

    private String name;

    private String introduction;

    private String category;

    private List<String> tags;

}
