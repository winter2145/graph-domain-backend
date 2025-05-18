package com.xin.graphdomainbackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑空间请求
 */
@Data
public class SpaceEditRequest implements Serializable {

    private static final long serialVersionUID = 2514809815659713458L;

    // 空间id
    private Long id;

    // 空间名称
    private String spaceName;

}
