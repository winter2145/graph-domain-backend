package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.mapper.SpaceMapper;
import com.xin.graphdomainbackend.model.dto.space.analyze.*;
import com.xin.graphdomainbackend.model.entity.Picture;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.vo.space.analyze.*;
import com.xin.graphdomainbackend.service.PictureService;
import com.xin.graphdomainbackend.service.SpaceAnalyzeService;
import com.xin.graphdomainbackend.service.SpaceService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceAnalyzeService {

    @Resource
    UserService userService;

    @Resource
    SpaceService spaceService;

    @Resource
    PictureService pictureService;

    @Override
    public void checkAnalyzeSpaceAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();

        boolean isAdmin = userService.isAdmin(loginUser);
        if (queryPublic || queryAll) {         // 全空间分析或者公共图库权限校验：仅管理员可访问
            ThrowUtils.throwIf(!isAdmin, ErrorCode.NO_AUTH_ERROR);
        } else {
            // 分析特定空间，空间管理员与系统管理员可以访问
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = this.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (!loginUser.getId().equals(space.getUserId()) && !isAdmin) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    public void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        // 全员空间分析
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll) return;

        // 公共图库
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        if (queryPublic) {
            queryWrapper.isNull("spaceId");
            return;
        }

        // 特定空间
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest usageAnalyzeRequest, User loginUser) {
        // 全空间或公共图库，需要从Picture 表查询
        if (usageAnalyzeRequest.isQueryAll() || usageAnalyzeRequest.isQueryPublic()) {
            // 校验权限
            this.checkAnalyzeSpaceAuth(usageAnalyzeRequest, loginUser);
            // 统计图库的使用空间
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");

            // 补充查询范围
            fillAnalyzeQueryWrapper(usageAnalyzeRequest, queryWrapper);
            // select picSize where spaceId = xxx  返回List每个元素是 picSize 的值
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);

            // 将 Object 转成 Long，并求和，得到总的存储空间使用量
            long usedSize = pictureObjList
                    .stream()
                    .mapToLong(obj -> (Long) obj)
                    .sum();

            // 得到符合条件的图片数量
            long usedCount = pictureObjList.size();

            // 封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);

            // 公共图库（或者全部空间）无数量和容量限制、也没有比例
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);

            return spaceUsageAnalyzeResponse;
        } else { //特定空间,可以从Space表查询
            Long spaceId = usageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = this.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
            // 校验权限
            this.checkAnalyzeSpaceAuth(usageAnalyzeRequest, loginUser);

            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());

            // 计算比例
            double sizeUsageRatio = BigDecimal
                    .valueOf(space.getTotalSize() * 100.0 /space.getMaxSize())
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
            double countUsageRatio = BigDecimal
                    .valueOf(space.getTotalCount() * 100.0 / space.getMaxCount())
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);

            return spaceUsageAnalyzeResponse;
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.NOT_FOUND_ERROR);
        this.checkAnalyzeSpaceAuth(spaceCategoryAnalyzeRequest, loginUser);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        queryWrapper.select("category", "count(*) as count", "sum(picSize) as totalSize")
                .groupBy("category");
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyzeResponseList = pictureService.getBaseMapper()
                .selectMaps(queryWrapper)
                .stream()
                .map(obj -> {
                    String category = Optional.ofNullable(obj.get("category"))
                            .map(Object::toString)
                            .orElse("未分类");

                    Long count = Optional
                            .ofNullable(obj.get("count"))
                            .map(o -> ((Number) o).longValue())
                            .orElse(0L);
                    Long totalSize = Optional
                            .ofNullable(obj.get("totalSize"))
                            .map(o -> ((Number) o).longValue())
                            .orElse(0L);
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());

        return spaceCategoryAnalyzeResponseList;
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 校验权限
        checkAnalyzeSpaceAuth(spaceTagAnalyzeRequest, loginUser);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("tags");
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);

        ObjectMapper objectMapper = new ObjectMapper();

        // 查询数据库,得到[["夏天","测试3"], Null]
        List<Object> tagObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
        // 统计每个标签的个数
        Map<String, Long> tagMap = tagObjList.stream()
                .filter(Objects::nonNull) // 过滤 null
                .map(Object::toString)    // 转为["[\"夏天\",\"测试3\"]"]
                .filter(str -> !str.trim().isEmpty()) // 过滤空字符串
                .map(tagStr -> { // JSON 字符串 解析成 Java 对象
                    try {
                        return objectMapper.readValue(tagStr, new TypeReference<List<String>>() {});
                    } catch (JsonProcessingException e) {
                        return Collections.<String>emptyList();
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(
                        tag -> tag, Collectors.counting()
                ));

        return tagMap.entrySet()
                .stream()
                .map(tag -> new SpaceTagAnalyzeResponse(
                        tag.getKey(),
                        tag.getValue()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest sizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(sizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        checkAnalyzeSpaceAuth(sizeAnalyzeRequest, loginUser);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(sizeAnalyzeRequest, queryWrapper);

        queryWrapper.select("picSize");
        List<Object> sizeObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);

        long lessThan100KB = 0;
        long between100And500KB = 0;
        long between500KBAnd1MB = 0;
        long moreThan1MB = 0;
        for (Object obj : sizeObjList) {
            long size = ((Number) obj).longValue();
            if (size < 100 * 1024) lessThan100KB++;
            else if (size < 500 * 1024) between100And500KB++;
            else if (size < 1024 * 1024) between500KBAnd1MB++;
            else moreThan1MB++;
        }
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", lessThan100KB);
        sizeRanges.put("100KB-500KB", between100And500KB);
        sizeRanges.put("500KB-1MB", between500KBAnd1MB);
        sizeRanges.put(">1MB", moreThan1MB);

        // 转为响应对象
        return sizeRanges.entrySet()
                .stream()
                .map(size -> new SpaceSizeAnalyzeResponse(
                        size.getKey(),
                        size.getValue()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest userAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(userAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        checkAnalyzeSpaceAuth(userAnalyzeRequest, loginUser);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(userAnalyzeRequest, queryWrapper);

        Long userId = userAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        // 按年、月、日
        String timeDimension = userAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') as period", "count(*) as count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) as period", "count(*) as count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') as period", "count(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }
        //分组排序
        queryWrapper.groupBy("period").orderByAsc("period");
        // 查询
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);

        return queryResult.stream()
                .map(result -> {
                    String period = Optional.ofNullable(result.get("period"))
                            .map(Object::toString)
                            .orElse("");
                    Long count = Optional.ofNullable(result.get("count"))
                            .map(val -> ((Number) val).longValue())
                            .orElse(0L);
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());

    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 检查权限，仅管理员可以查看
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        // 构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("limit " + spaceRankAnalyzeRequest.getTopN()); // 取前 N 名

        // 查询并封装结果
        return spaceService.list(queryWrapper);
    }
}
