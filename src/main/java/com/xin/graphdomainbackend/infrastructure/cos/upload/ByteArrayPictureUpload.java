package com.xin.graphdomainbackend.infrastructure.cos.upload;

import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

/**
 * AI 生成图片上传实现类 (字节数组图片上传)
 */
@Service
public class ByteArrayPictureUpload extends PictureUploadTemplate{
    @Override
    protected void validPicture(Object inputSource) {
        byte[] imageBytes = (byte[]) inputSource;
        ThrowUtils.throwIf(imageBytes == null || imageBytes.length == 0, ErrorCode.PARAMS_ERROR, "图片数据不能为空");

        // 1. 校验文件大小 (5MB)
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(imageBytes.length > 5 * ONE_M, ErrorCode.PARAMS_ERROR, "图片大小不能超过 5MB");
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        // AI 生成的图片没有原始文件名，我们给它生成一个随机名
        return "Byte" + UUID.randomUUID().toString() +  ".png";
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        byte[] imageBytes = (byte[]) inputSource;
        // 使用 NIO 将字节写入文件
        Files.write(file.toPath(), imageBytes);
    }
}
