package com.xin.graphdomainbackend.infrastructure.elasticsearch.service;

import cn.hutool.core.collection.CollUtil;
import com.xin.graphdomainbackend.common.util.ConvertObjectUtils;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.dao.EsPictureDao;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.model.EsPicture;
import com.xin.graphdomainbackend.picture.dao.entity.Picture;
import com.xin.graphdomainbackend.picture.service.PictureService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EsUpdateService {

    /**
     * ES DAO：可选依赖
     */
    @Autowired(required = false)
    private EsPictureDao esPictureDao;

    @Resource
    private PictureService pictureService;

    /**
     * 单条图片同步到 ES
     */
    @Async("asyncExecutor")
    public void updatePictureEs(Long pictureId) {

        // ES 未启用，直接跳过
        if (esPictureDao == null) {
            log.warn("ES 未启用，跳过图片 ES 同步，pictureId={}", pictureId);
            return;
        }

        try {
            Picture dbPicture = pictureService.getById(pictureId);
            if (dbPicture == null) {
                log.warn("图片不存在，跳过 ES 同步，pictureId={}", pictureId);
                return;
            }

            EsPicture esPicture = ConvertObjectUtils.toEsPicture(dbPicture);
            esPictureDao.save(esPicture);

            log.info("图片同步 ES 成功，pictureId={}", pictureId);

        } catch (Exception e) {
            // 吃掉异常，避免异步线程反复失败
            log.error("图片同步 ES 失败，pictureId={}", pictureId, e);
        }
    }

    /**
     * 批量图片同步到 ES
     */
    @Async("asyncExecutor")
    public void batchUpdatePictureEs(List<Long> pictureIds) {

        // ES 未启用，直接跳过
        if (esPictureDao == null) {
            log.warn("ES 未启用，跳过批量图片 ES 同步，pictureIds={}", pictureIds);
            return;
        }

        try {
            List<Picture> pictureList = pictureService.listByIds(pictureIds);
            if (CollUtil.isEmpty(pictureList)) {
                return;
            }

            List<EsPicture> esPictures = pictureList.stream()
                    .map(ConvertObjectUtils::toEsPicture)
                    .collect(Collectors.toList());

            esPictureDao.saveAll(esPictures);

            log.info("批量图片同步 ES 成功，count={}", esPictures.size());

        } catch (Exception e) {
            log.error("批量图片同步 ES 失败，pictureIds={}", pictureIds, e);
        }
    }

    /**
     * 搜索关键词相关的 ES 更新
     */
    @Async("asyncExecutor")
    public void updateSearchKeyEs(Long pictureId) {

        // ES 未启用，直接跳过
        if (esPictureDao == null) {
            log.warn("ES 未启用，跳过搜索关键词 ES 同步，pictureId={}", pictureId);
            return;
        }

        try {
            Picture dbPicture = pictureService.getById(pictureId);
            if (dbPicture == null) {
                return;
            }

            EsPicture esPicture = ConvertObjectUtils.toEsPicture(dbPicture);
            esPictureDao.save(esPicture);

            log.info("搜索关键词 ES 更新成功，pictureId={}", pictureId);

        } catch (Exception e) {
            log.error("搜索关键词 ES 更新失败，pictureId={}", pictureId, e);
        }
    }
}
