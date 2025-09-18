package com.xin.graphdomainbackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.xin.graphdomainbackend.config.CosClientConfig;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.manager.cos.CosManager;
import com.xin.graphdomainbackend.model.dto.file.UploadPictureResult;
import com.xin.graphdomainbackend.utils.HexColorExpanderUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * 图片上传模板
 */
@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;
    /**
     * 上传图片 并获取 图片信息
     *
     * @param inputSource    文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传图片的结果
     */
    public UploadPictureResult uploadPictureResult(Object inputSource, String uploadPathPrefix) {
        // 1.校验图片
        validPicture(inputSource);

        // 2.图片上传地址
        String uuid = RandomUtil.randomString(12);
        String originalFilename = getOriginalFilename(inputSource);
        // 自己拼接文件上传路径，而不是使用原始文件名称，可以增强安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 3.服务器本地创建一个临时文件对象（还没有内容）
            file = File.createTempFile(uploadPath, null);
            // 图片数据（可能是前端上传的文件，也可能是图片URL）写入到上面创建的临时文件里
            // inputSource -> file
            processFile(inputSource, file);

            // 4. 上传到对象存储（自动生成webp和缩略图）
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);

            // 5. 获取原图信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 6. 获取处理结果列表
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();


            // 7. 处理webp和缩略图
            if (CollUtil.isNotEmpty(objectList)) {
                // 获取压缩之后得到的文件信息
                CIObject compressedCiObject = objectList.get(0);
                // 缩略图默认等于压缩图
                CIObject thumbnailCiObject = compressedCiObject;
                // 有生成缩略图，才获取缩略图
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                // 封装结果（传入原图路径 uploadPath）
                UploadPictureResult uploadPictureResult = buildResult(originalFilename, uploadPath, compressedCiObject, thumbnailCiObject, imageInfo);
                uploadPictureResult.setPicSize(FileUtil.size(file)); // 图片大小一直为原图大小
                return uploadPictureResult;
            }

            // 未生成 WebP 和缩略图时，直接返回原图信息
            return getUploadPictureResult(originalFilename, uploadPath, file, imageInfo);

        } catch (Exception e) {
            log.error("图片上传失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            this.deleteTempFile(file);
        }

    }

    /**
     * 封装原图结果（未生成 WebP 和缩略图）
     */
    private UploadPictureResult getUploadPictureResult(String originalFilename, String uploadPath, File file, ImageInfo imageInfo) {
        // 计算宽高
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        // round 四舍五入
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

        // 封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setWebpUrl(null); // 无 WebP
        uploadPictureResult.setThumbnailUrl(null); // 无缩略图
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        // 设置图片主色调
        String picColor = HexColorExpanderUtils.normalizeHexColor(imageInfo.getAve());
        uploadPictureResult.setPicColor(picColor);
        return uploadPictureResult;
    }

    /**
     * 封装 WebP 和缩略图结果
     */
    private UploadPictureResult buildResult(String originalFilename, String originalUploadPath, CIObject compressedCiObject, CIObject thumbnailCiObject,
                                            ImageInfo imageInfo) {
        // 计算宽高
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        // 封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        // 原图 URL
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + originalUploadPath);
        // 设置压缩后的wepb地址
        uploadPictureResult.setWebpUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        // 设置缩略图地址
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        // uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        // 设置图片主色调
        String picColor = HexColorExpanderUtils.normalizeHexColor(imageInfo.getAve());
        uploadPictureResult.setPicColor(picColor);

        // 返回可访问的地址
        return uploadPictureResult;
    }
    /**
     * 清理临时文件
     *
     * @param file 文件对象
     */
    private void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean result = file.delete();
        if (!result) {
            log.error("file delete error, filepath = " + file.getAbsolutePath());
        }
    }

    /**
     * 校验输入源（本地文件或 URL）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

}
