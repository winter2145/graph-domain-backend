package com.xin.graphdomainbackend.aidraw.api.dto.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author xin
 * @description 绘画视图
 */
@Data
public class AiChatSessionVO {
    private Long id;

    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
