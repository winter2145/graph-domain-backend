package com.xin.graphdomainbackend.picture.api.dto.requeset;

import lombok.Data;

import java.io.Serializable;

/**
 * 以图搜图请求
 */
@Data
public class SearchPictureByPictureRequest implements Serializable {

    private static final long serialVersionUID = -1027108696949258028L;

    /**
     * 图片 id
     */
    private Long pictureId;
}
