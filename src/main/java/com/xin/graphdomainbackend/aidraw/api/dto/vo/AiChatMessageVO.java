package com.xin.graphdomainbackend.aidraw.api.dto.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiChatMessageVO {
    private String role;

    private String content;

    private String imageUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}