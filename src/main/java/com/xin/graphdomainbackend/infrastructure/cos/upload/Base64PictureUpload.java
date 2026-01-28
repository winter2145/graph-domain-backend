package com.xin.graphdomainbackend.infrastructure.cos.upload;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

/**
 * AI 生成图片上传实现类 (处理 Base64)
 */
@Service
public class Base64PictureUpload extends PictureUploadTemplate{
    /**
     * 校验输入源
     * @param inputSource 这里规定必须是 Base64 字符串
     */
    @Override
    protected void validPicture(Object inputSource) {
        if (!(inputSource instanceof String)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "AI图片必须是Base64字符串格式");
        }
        String base64Str = (String) inputSource;
        if (StrUtil.isBlank(base64Str)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片数据为空");
        }


    }


    @Override
    protected String getOriginalFilename(Object inputSource) {
        return "Base64_" + UUID.randomUUID().toString() +  ".png";
    }

    /**
     * 处理输入源，将 Base64 转换为文件
     */
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String base64Str = (String) inputSource;

        // 1. 如果 Base64 字符串包含前缀 (如 "data:image/png;base64,")，需要去掉
        if (base64Str.contains(",")) {
            String[] parts = base64Str.split(",");
            if (parts.length > 1) {
                base64Str = parts[1];
            }
        }

        // 2. 解码 Base64 为字节数组
        byte[] decodedBytes = Base64.decode(base64Str);
        // 校验文件大小 (5MB)
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(decodedBytes.length > 5 * ONE_M, ErrorCode.PARAMS_ERROR, "图片大小不能超过 5MB");

        // 3.将字节写入模板类创建的临时 file 对象中
        Files.write(file.toPath(), decodedBytes);
    }
}
