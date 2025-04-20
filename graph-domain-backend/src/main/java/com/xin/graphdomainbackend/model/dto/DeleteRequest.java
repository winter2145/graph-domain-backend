package com.xin.graphdomainbackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 根据ID删除类
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
