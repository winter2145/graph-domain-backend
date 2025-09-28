package com.xin.graphdomainbackend.infrastructure.elasticsearch.service;

import cn.hutool.core.collection.CollUtil;
import com.xin.graphdomainbackend.common.util.ConvertObjectUtils;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.dao.EsPictureDao;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.model.EsPicture;
import com.xin.graphdomainbackend.picture.dao.entity.Picture;
import com.xin.graphdomainbackend.picture.service.PictureService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EsUpdateService {

    @Resource
    private EsPictureDao esPictureDao;

    @Resource
    private PictureService pictureService; // 获取数据库最新数据

    @Async("asyncExecutor")
    @Retryable(
            value = Exception.class,
            maxAttempts = 1,
            backoff = @Backoff(delay = 1000, multiplier = 1)
    )
    public void updatePictureEs(Long pictureId) {
        try {
            Picture dbPicture = pictureService.getById(pictureId);
            EsPicture esPicture = ConvertObjectUtils.toEsPicture(dbPicture);
            log.info("开始更新 ES 数据，pictureId={}", pictureId);
            esPictureDao.save(esPicture);
            log.info("ES 更新完成，pictureId={}", pictureId);
        } catch (Exception e) {
            log.error("保存Es图片信息失败：" + e.getMessage());
        }
    }

    @Async("asyncExecutor")
    @Retryable(
            value = {Exception.class},
            maxAttempts = 1,
            backoff = @Backoff(delay = 1000, multiplier = 1)
    )
    public void batchUpdatePictureEs(List<Long> pictureIds) {
        List<Picture> pictureList = pictureService.listByIds(pictureIds);
        if (CollUtil.isEmpty(pictureList)) {
            return;
        }
        // 批量转换
        List<EsPicture> esPictures = pictureList.stream()
                .map(ConvertObjectUtils::toEsPicture)
                .collect(Collectors.toList());

        // 使用 ElasticsearchRepository 的 saveAll 方法
        log.info("开始更新 ES 数据，esPictures={}", esPictures);
        esPictureDao.saveAll(esPictures);
        log.info("ES 更新完成，esPictures={}", esPictures);
    }

    @Async("asyncExecutor")
    @Retryable(
            value = Exception.class,
            maxAttempts = 1,
            backoff = @Backoff(delay = 1000, multiplier = 1)
    )
    public void updateSearchKeyEs(Long pictureId) {
        try {
            Picture dbPicture = pictureService.getById(pictureId);
            EsPicture esPicture = ConvertObjectUtils.toEsPicture(dbPicture);
            log.info("开始更新 ES 数据，pictureId={}", pictureId);
            esPictureDao.save(esPicture);
            log.info("ES 更新完成，pictureId={}", pictureId);
        } catch (Exception e) {
            log.error("保存Es图片信息失败：" + e.getMessage());
        }
    }

    @Recover
    public void recover(Exception e, Object target) {
        if (target instanceof Long) {
            Long Id = (Long) target;
            log.error("ES更新重试全部失败，Id: {}", Id, e);
        } else if (target instanceof List) {
            @SuppressWarnings("unchecked")
            List<Long> Ids = (List<Long>) target;
            log.error("ES批量更新重试全部失败，Ids: {}", Ids, e);
        } else {
            log.error("ES更新重试全部失败，未知目标类型: {}", target, e);
        }
    }
}