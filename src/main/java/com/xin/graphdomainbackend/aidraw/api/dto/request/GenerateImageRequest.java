package com.xin.graphdomainbackend.aidraw.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author xin
 * @description 图片生成请求
 */
@Data
public class GenerateImageRequest {
    @NotBlank
    private String userId;

    @NotBlank
    private String prompt;
}