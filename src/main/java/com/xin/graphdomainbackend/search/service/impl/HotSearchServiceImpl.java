package com.xin.graphdomainbackend.search.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.dao.EsSearchKeyDao;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.model.EsSearchKeyword;
import com.xin.graphdomainbackend.infrastructure.redis.constant.RedisConstant;
import com.xin.graphdomainbackend.search.dao.entity.HotSearch;
import com.xin.graphdomainbackend.search.dao.mapper.HotSearchMapper;
import com.xin.graphdomainbackend.search.service.HotSearchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
* @author Administrator
* @description 针对表【hot_search(热门搜索记录表)】的数据库操作Service实现
* @createDate 2025-09-06 09:30:40
*/
@Service
@Slf4j
public class HotSearchServiceImpl extends ServiceImpl<HotSearchMapper, HotSearch>
    implements HotSearchService {

    @Autowired(required = false)
    private EsSearchKeyDao esSearchKeyDao;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<String> getHotSearchList(String type, int size) {
        // 先从redis中获取
        String cacheKey = RedisConstant.HOT_SEARCH_CACHE_KEY + type;
        String emptyMarkerKey = cacheKey + ":empty";  // 使用特殊标记键记录空状态
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(emptyMarkerKey))) {
            return Collections.emptyList();
        }

        List<String> cacheValue = stringRedisTemplate.opsForList()
                .range(cacheKey, 0, size - 1);

        if (CollectionUtils.isNotEmpty(cacheValue)) {
            return cacheValue;
        }

        // Redis未命中，查数据库
        Date startTime = DateUtil.beginOfDay(new Date());  // 获取今天零点的时间
        List<String> hotSearchKeywords = this.baseMapper.getHotSearch(type, startTime, size);
        if (CollectionUtils.isEmpty(hotSearchKeywords)) {
            // 防止缓存穿透
            stringRedisTemplate.opsForValue().set(emptyMarkerKey, "1");
            stringRedisTemplate.expire(emptyMarkerKey, 300 + RandomUtil.randomInt(0,120), TimeUnit.SECONDS);
            return Collections.emptyList();
        }

        int cacheExpireTime = RedisConstant.CACHE_EXPIRE_TIME + RandomUtil.randomInt(0, 120); // 设置过期时间15分钟，加随机2分钟
        // 使用管道执行
        try {
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                // 清空旧数据
                connection.del(cacheKey.getBytes());
                // 批量添加数据 [keyword1, keyword2, keyword3, ...]
                for (String kw : hotSearchKeywords) {
                    connection.rPush(cacheKey.getBytes(), kw.getBytes(StandardCharsets.UTF_8));
                }
                // 设置过期时间
                connection.expire(cacheKey.getBytes(), cacheExpireTime);
                return null;
            });
        } catch (RedisConnectionFailureException e) {
            log.error("Redis连接失败，直接返回数据库结果", e);
            return hotSearchKeywords; // 降级方案：直接返回数据库结果
        } catch (Exception e) {
            log.error("Redis管道操作异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "系统繁忙，请稍后重试");
        }

        return hotSearchKeywords;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordSearchKeyword(String searchText, String type) {
        // 校验参数
        ThrowUtils.throwIf(searchText.isBlank(), ErrorCode.PARAMS_ERROR, "搜索关键字不能为空");
        ThrowUtils.throwIf(type.isBlank(), ErrorCode.PARAMS_ERROR, "搜索类型不能为空");


        // 查询数据库是否存在关键词
        HotSearch hot = lambdaQuery()
                .eq(HotSearch::getKeyword, searchText)
                .eq(HotSearch::getType, type)
                .one();
        if(hot == null) {
            // 新关键词
            hot = new HotSearch();
            hot.setKeyword(searchText);
            hot.setType(type);
            hot.setCount(1L);
            hot.setCreateTime(new Date());
            hot.setLastUpdateTime(new Date());
            this.save(hot);
        } else {
            boolean success = lambdaUpdate()
                    .set(HotSearch::getCount, hot.getCount() + 1)
                    .set(HotSearch::getLastUpdateTime, new Date())
                    .eq(HotSearch::getId, hot.getId())
                    .eq(HotSearch::getCount, hot.getCount())
                    .update();

            if (!success) {
                recordSearchKeyword(searchText, type);
                return;
            }
        }

        // ES 同步
        if (esSearchKeyDao != null) {
            try {
                EsSearchKeyword es = new EsSearchKeyword();
                es.setId(hot.getId());
                es.setKeyword(hot.getKeyword());
                es.setType(hot.getType());
                esSearchKeyDao.save(es);
            } catch (Exception e) {
                log.warn("ES 同步失败，已忽略，keyword={}", searchText, e);
            }
        }
    }
}




