package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.constant.RedisConstant;
import com.xin.graphdomainbackend.esdao.EsSearchKeyDao;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.mapper.HotSearchMapper;
import com.xin.graphdomainbackend.model.entity.HotSearch;
import com.xin.graphdomainbackend.model.entity.es.EsSearchKeyword;
import com.xin.graphdomainbackend.service.HotSearchService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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
public class HotSearchServiceImpl extends ServiceImpl<HotSearchMapper, HotSearch>
    implements HotSearchService{

    @Resource
    private EsSearchKeyDao esSearchKeyDao;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<String> getHotSearchList(String type, int size) {
        // 先从redis中获取
        String cacheKey =RedisConstant.HOT_SEARCH_CACHE_KEY + type;
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


        // 获取ES是否存在对象数据
        List<EsSearchKeyword> esList  = esSearchKeyDao.findByTypeAndKeyword(type, searchText);

        if (CollectionUtils.isEmpty(esList)) {

            // 保存 MySQL
            HotSearch newHot = new HotSearch();
            newHot.setKeyword(searchText);
            newHot.setType(type);
            newHot.setCount(1L);
            newHot.setCreateTime(new Date());
            newHot.setLastUpdateTime(new Date());
            this.save(newHot);

            // ES中没有记录 -> 写 ES
            EsSearchKeyword newEs = new EsSearchKeyword();
            newEs.setId(newHot.getId()); // 用 MySQL 生成的 id 作为 ES 的 id, 强制保持一致
            newEs.setKeyword(searchText);
            newEs.setType(type);
            newEs.setCreateTime(new Date());
            esSearchKeyDao.save(newEs);
        } else {
            // ES 已存在 -> 拿主键
            Long esId = esList.get(0).getId();
            HotSearch hot = lambdaQuery()
                    .eq(HotSearch::getId, esId)
                    .one();
            if (hot != null) {
                // 5. 原子 +1（乐观锁防并发）
                boolean success = lambdaUpdate()
                        .set(HotSearch::getCount, hot.getCount() + 1)
                        .set(HotSearch::getLastUpdateTime, new Date())
                        .eq(HotSearch::getId, hot.getId())
                        .eq(HotSearch::getCount, hot.getCount()) // 乐观锁
                        .update();
                if (!success) {
                    // 版本冲突，简单重试一次即可（也可抛异常由上层重试）
                    recordSearchKeyword(searchText, type);
                }
            }
        }
    }
}




