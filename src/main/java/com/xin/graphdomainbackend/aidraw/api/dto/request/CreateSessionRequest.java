package com.xin.graphdomainbackend.aidraw.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author xin
 * @description 创建会话请求参数
 */
@Data
public class CreateSessionRequest {
    @NotBlank
    private String userId;

    private String title;
}