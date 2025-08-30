package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xin.graphdomainbackend.config.CosClientConfig;
import com.xin.graphdomainbackend.constant.CrawlerConstant;
import com.xin.graphdomainbackend.constant.RedisConstant;
import com.xin.graphdomainbackend.constant.TargetTypeConstant;
import com.xin.graphdomainbackend.constant.UserConstant;
import com.xin.graphdomainbackend.esdao.EsPictureDao;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.manager.cos.CosManager;
import com.xin.graphdomainbackend.manager.es.EsUpdateService;
import com.xin.graphdomainbackend.manager.upload.FilePictureUpload;
import com.xin.graphdomainbackend.manager.upload.PictureUploadTemplate;
import com.xin.graphdomainbackend.manager.upload.UrlPictureUpload;
import com.xin.graphdomainbackend.mapper.PictureMapper;
import com.xin.graphdomainbackend.model.dto.file.UploadPictureResult;
import com.xin.graphdomainbackend.model.dto.picture.*;
import com.xin.graphdomainbackend.model.entity.LikeRecord;
import com.xin.graphdomainbackend.model.entity.Picture;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.enums.PictureReviewStatusEnum;
import com.xin.graphdomainbackend.model.enums.SpaceTypeEnum;
import com.xin.graphdomainbackend.model.vo.PictureVO;
import com.xin.graphdomainbackend.model.vo.UserVO;
import com.xin.graphdomainbackend.service.*;
import com.xin.graphdomainbackend.utils.ColorSimilarUtils;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【picture】的数据库操作Service实现
* @createDate 2025-04-30 19:13:10
*/
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private EsPictureDao esPictureDao;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private UserFollowsService userFollowsService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    @Lazy
    private LikeRecordService likeRecordService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    @Lazy
    private EsUpdateService esUpdateService;

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id不能为空");
        // 如果传递了 url，才校验
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest uploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 校验空间是否存在
        Long spaceId = uploadRequest.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "已超出当前存储图片的最大数量");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间大小不足");
            }
        }

        // 判断是新增还是更新
        Long pictureId = uploadRequest.getId();
        Long oldSize = 0L;
        Long oldSpaceId = null;
        Picture oldPicture = new Picture();

        // 如果是更新操作,判断图片是否存在
        if (pictureId != null) {
            oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

            oldSize = oldPicture.getPicSize();
            oldSpaceId = oldPicture.getSpaceId();

            // 校验空间是否一致
            // 没传 spaceId，则复用原有图片的 spaceId（这样也兼容了公共图库）
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId，必须和原图片的空间 id 一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR);
                }
            }
        }

        // 按照用户 id 划分目录 => 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId != null) {
            uploadPathPrefix = String.format("space/%s", spaceId);
        } else {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        }

        // 根据 inputSource 的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate;
        if (inputSource instanceof MultipartFile) {
            pictureUploadTemplate = filePictureUpload; // 注入的 FilePictureUpload 实例
        } else if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload; // 注入的 UrlPictureUpload 实例
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的上传类型");
        }
        // 上传图片得到图片信息
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPictureResult(inputSource, uploadPathPrefix);

        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());

        // 补充webp url
        if (uploadPictureResult.getWebpUrl() != null) {
            picture.setWebpUrl(uploadPictureResult.getWebpUrl());
        }

        // 补充缩略图url
        if (uploadPictureResult.getThumbnailUrl() != null) {
            picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        }

        // 支持前端自定义名字前缀
        if(StrUtil.isNotBlank(uploadRequest.getPicName()) && uploadRequest.getPicName() != null) {
            picture.setName(uploadRequest.getPicName());
        }

        // 补充 空间Id
        if (spaceId != null) {
            picture.setSpaceId(spaceId);
        }

        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setPicColor(uploadPictureResult.getPicColor());

        // 补充审核参数
        this.fillReviewParams(picture, loginUser, space);
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        // 开启事务
        Long finalSpaceId = spaceId;
        Long finalOldSpaceId = oldSpaceId;
        Long finalOldSize = oldSize;
        Picture finalOldPicture = oldPicture;
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");

            // 注册事务提交后的回调（仅在更新操作时触发）
            if (pictureId != null) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 异步清理旧文件
                        clearPictureFile(finalOldPicture);
                    }
                });
            }

            // 先减少老额度（仅在更新操作时触发）
            if (finalOldSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize - " + finalOldSize)
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "老额度回滚失败");
            }
            // 再加新额度
            if (finalSpaceId != null) {
                // 更新空间的使用额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });

        // 异步更新 ES （不影响主事务提交）
        esUpdateService.updatePictureEs(picture.getId());

        return PictureVO.objToVo(picture);
    }

    /**
     * 审核参数的填充
     */
    private void fillReviewParams(Picture picture, User loginUser, Space space) {
        // （补充）后期私人空间上传图片不需要审核
        boolean isPrivateSpace = false;
        if (space != null) {
            isPrivateSpace = space.getSpaceType().equals(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 如果是管理员或是私人空间，则自动通过审核
        if (userService.isAdmin(loginUser) || isPrivateSpace) {
            int value = PictureReviewStatusEnum.PASS.getValue();
            // 管理员自动过审
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewStatus(value);
        } else {
            // 非管理员,无论是编辑还是创建默认都是待审核
            int value = PictureReviewStatusEnum.REVIEWING.getValue();
            picture.setReviewStatus(value);
        }
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id =pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String category = pictureQueryRequest.getCategory();
        String introduction = pictureQueryRequest.getIntroduction();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        List<Long> teamSpaceIdList = pictureQueryRequest.getTeamSpaceIdList();

        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                            .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        if (spaceId != null) {
            // 直接查指定空间
            queryWrapper.eq("spaceId", spaceId);
        } else if (nullSpaceId && CollUtil.isNotEmpty(teamSpaceIdList)) {
            // 管理员视角：公开图库 + 团队空间
            queryWrapper.and(qw -> qw.isNull("spaceId").or().in("spaceId", teamSpaceIdList));
        } else if (nullSpaceId) {
            // 普通视角：只查公开图库
            queryWrapper.isNull("spaceId");
        } else if (CollUtil.isNotEmpty(teamSpaceIdList)) {
            // 只查团队空间（例如管理员限制只能查部分团队）
            queryWrapper.in("spaceId", teamSpaceIdList);
        }
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);

        // >= startEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        // < endEditTime
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);

        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            /* and (tags like %"Java"% and like %"Python"%) */
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture) {
        if (picture == null) {
            return null;
        }

        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);

        // 只加 redis 计数，不写库
        incrementViewCount(picture.getId());

        // 浏览量 = 数据库值 + redis 增量
        long viewCount = getViewCount(picture.getId());
        pictureVO.setViewCount(viewCount);

        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }

        return pictureVO;
    }

    /**
     * 根据图片id，增加阅览量
     * @param pictureId 图片id
     */
    private void incrementViewCount(Long pictureId) {
        // 构建redis缓存key
        String viewCountKey = String.format("picture:viewCount:%d", pictureId);
        // 利用redis原子性，increment（计数）
        Long count = stringRedisTemplate.opsForValue().increment(viewCountKey);

        // 第一次写入时设置过期时间 1 h
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(viewCountKey, 1, TimeUnit.HOURS);
        }
    }

    // 阅览量 =  数据库值 + redis 增量
    private long getViewCount(Long pictureId) {
        Picture dbPicture = this.getById(pictureId);
        // 获取数据库中的 阅览数量
        long dbCount = (dbPicture != null && dbPicture.getViewCount() != null)
                ? dbPicture.getViewCount() : 0;
        // 获取redis 增量值
        String viewCountKey = String.format("picture:viewCount:%d", pictureId);
        String cacheValue = stringRedisTemplate.opsForValue().get(viewCountKey);
        long redisCount = (cacheValue != null) ? Long.parseLong(cacheValue) : 0;

        return dbCount + redisCount;

    }

    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void flushViewCountsToDb() {
        try {
            Set<String> keys = stringRedisTemplate.keys("picture:viewCount:*");
            if (keys == null || keys.isEmpty()) {
                return;
            }

            for (String key : keys) {
                try {
                    String val = stringRedisTemplate.opsForValue().get(key);
                    if (val == null) continue;

                    long count = Long.parseLong(val);
                    if (count <= 0) continue;

                    Long pictureId = Long.valueOf(key.split(":")[2]);

                    log.debug("刷库图片 {} 浏览量: {}", pictureId, count);

                    // 使用事务确保数据一致性
                    boolean success = this.update()
                            .setSql("viewCount = viewCount + " + count)
                            .eq("id", pictureId)
                            .update();

                    if (success) {
                        // 刷库成功后，减少 Redis 中的计数
                        Long remaining = stringRedisTemplate.opsForValue().decrement(key, count);

                        // 如果 Redis 中计数为 0 或负数，删除 key
                        if (remaining != null && remaining <= 0) {
                            stringRedisTemplate.delete(key);
                        } else {
                            // 刷新过期时间
                            stringRedisTemplate.expire(key, 1, TimeUnit.HOURS);
                        }

                        log.debug("图片 {} 浏览量刷库成功，剩余 Redis 计数: {}", pictureId, remaining);
                    }
                } catch (Exception e) {
                    log.error("刷库图片浏览量失败，key: {}, error: {}", key, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("刷库浏览量任务执行失败: {}", e.getMessage());
        }
    }

    @Override
    public List<PictureVO> getPictureVOList(List<Picture> pictureList) {
        if (pictureList.isEmpty()) {
            return new ArrayList<>();
        }

        //  1.收集含有点赞的图片id
        Set<Long> containLikePictureIds = pictureList.stream()
                .filter(picture -> picture.getLikeCount() > 0)
                .map(Picture::getId)
                .collect(Collectors.toSet());

        // 2.根据含有点赞的图片id，去批量查找点赞记录
        List<LikeRecord> likeRecords = likeRecordService.getLikeRecordsByTargetIds(containLikePictureIds, TargetTypeConstant.IMAGE);

        // 3.获取当前登录用户ID（如果未登录则返回空集合）
        Set<Long> likedPictureIds = new HashSet<>();
        try {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = requestAttributes.getRequest();
                User loginUser = userService.getLoginUser(request);

                // 提取当前用户点赞过的图片 ID
                likedPictureIds = likeRecords.stream()
                        .filter(likeRecord -> likeRecord.getUserId().equals(loginUser.getId()))
                        .filter(likeRecord -> likeRecord.getLikeStatus() == 1)
                        .map(LikeRecord::getTargetId)
                        .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            log.error("获取用户点赞状态失败", e);
        }

        // 4.批量查询图片作者信息
        Map<Long, User> userMap = getUserMap(pictureList);

        Set<Long> finalLikedPictureIds = likedPictureIds;
        return pictureList.stream()
                .map(picture -> {
                    PictureVO pictureVO = PictureVO.objToVo(picture);
                    // 设置作者信息
                    if (userMap.containsKey(picture.getUserId())) {
                        User targetUser = userMap.get(picture.getUserId());
                        UserVO userVO = userService.getUserVO(targetUser);
                        pictureVO.setUser(userVO);
                    }
                    if (finalLikedPictureIds.contains(picture.getId())) {
                        pictureVO.setIsLiked(1);
                    }
                    return pictureVO;
                }).collect(Collectors.toList());

    }

    // 批量查询图片作者信息
    private Map<Long, User> getUserMap(List<Picture> pictureList) {
        Set<Long> userIds = pictureList.stream()
                .map(Picture::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }
        return userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    @Override
    public Page<PictureVO> getPictureVOByPage(PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);

        long current = pictureQueryRequest.getCurrent(); // 当前页号
        long pageSize = pictureQueryRequest.getPageSize(); //每页大

        QueryWrapper<Picture> queryWrapper = this.getQueryWrapper(pictureQueryRequest);

        // Page<Picture> -> Page<PictureVO>
        // MyBatis-Plus分页查数据库（带查询条件）,得到原始Picture列表（含敏感数据）
        Page<Picture> page = this.page(new Page<>(current, pageSize), queryWrapper);

        // 创建一个与Picture一样大的分页PictureVO
        long total = page.getTotal();
        Page<PictureVO> pictureVOPage = new Page<>(current, pageSize, total);

        // Picture列表 -> PictureVO列表
        List<Picture> records = page.getRecords();
        List<PictureVO> pictureVOList = this.getPictureVOList(records);

        // PictureVO列表 存入 Page<PictureVO>
        pictureVOPage.setRecords(pictureVOList);

        return pictureVOPage;
    }

    @Override
    public Page<Picture> getPictureByPage(PictureQueryRequest pictureQueryRequest) {

        long current = pictureQueryRequest.getCurrent(); // 当前页号
        long pageSize = pictureQueryRequest.getPageSize(); //每页大
        QueryWrapper<Picture> queryWrapper = this.getQueryWrapper(pictureQueryRequest);

        return this.page(new Page<>(current, pageSize), queryWrapper);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean editPicture(PictureEditRequest pictureEditRequest, HttpServletRequest request) {

        // 校验参数
        if (pictureEditRequest == null ||
                pictureEditRequest.getId() < 0
                || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断图片是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 校验空间是否存在
        Long spaceId = pictureEditRequest.getSpaceId();
        Space space = spaceService.getById(spaceId);

        // 保留旧的数据
        Picture picture = new Picture();
        BeanUtil.copyProperties(oldPicture, picture);
        // 只覆盖PictureEditRequest中出现的字段
        // Hutool的BeanUtil.copyProperties默认会覆盖同名字段（包括null），不会影响Picture中没有的字段
        BeanUtil.copyProperties(pictureEditRequest, picture);

        // tags字段类型不同，单独处理
        if (pictureEditRequest.getTags() != null) {
            picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        } else {
            picture.setTags(null); // 明确置空
        }
        // 设置编辑时间
        picture.setEditTime(new Date());

        User loginUser = userService.getLoginUser(request);

        // 校验权限
        // this.checkPictureAuth(loginUser, picture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser, space);
        // 校验数据
        this.validPicture(picture);

        // 数据库更新
        boolean res = this.updateById(picture);

        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR);

        // 同步更新 ES 数据
        esUpdateService.updatePictureEs(picture.getId());

        return true;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 1.校验参数
        if (pictureReviewRequest == null
                || pictureReviewRequest.getId() == null
                || pictureReviewRequest.getReviewStatus() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);

        if (reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "审核状态非法");
        }

        // 2.判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 3.检验图片是否审核
        if (Objects.equals(oldPicture.getReviewStatus(), reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }

        // 4.数据库操作
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewTime(new Date());
        updatePicture.setReviewerId(loginUser.getId());
        boolean result = this.updateById(updatePicture);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        // 5.更新ES数据
        esUpdateService.updatePictureEs(id);

    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest uploadByBatchRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(uploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        Integer count = uploadByBatchRequest.getCount();
        String searchText = uploadByBatchRequest.getSearchText();
        // 校验数量限制
        ThrowUtils.throwIf(count == null || count <= 0 || count > 30, ErrorCode.PARAMS_ERROR, "下载数量范围应为 1~30");

        // 文件名前缀
        String namePrefix = Optional.ofNullable(uploadByBatchRequest.getNamePrefix())
                .filter(StrUtil::isNotBlank)
                .orElse(searchText);

        // 构造百度图片翻页URL
        String baseUrl = "https://image.baidu.com/search/flip?tn=baiduimage&ie=utf-8&word=" + searchText + "&pn=";

        int uploadCount = 0;
        int page = 0;
        Set<String> seenUrls = new HashSet<>();

        while (uploadCount < count) {
            String pageUrl = baseUrl + (page * 20);
            page++;

            Document doc;
            try {
                doc = Jsoup.connect(pageUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 "
                                + "(KHTML, like Gecko) Chrome/101.0.4951.67 Safari/537.36")
                        .timeout(10000)
                        .get();
            } catch (IOException e) {
                log.error("获取页面失败：{}", pageUrl, e);
                break;
            }

            String html = doc.html();
            List<String> picUrls = parseBaiduImageUrls(html);

            for (String fileUrl : picUrls) {
                // 去重
                if (seenUrls.contains(fileUrl)) continue; // 如果已经处理过这个图片链接，跳过本次循环
                seenUrls.add(fileUrl); // 把当前图片链接加入已处理集合

                // 修正图片链接
                fileUrl = fileUrl.replaceAll("(?i)(\\.(jpg|jpeg|png|webp|gif))(/.*)?$", ".$2");

                try {
                    PictureUploadRequest request = new PictureUploadRequest();
                    request.setFileUrl(fileUrl);
                    request.setPicName(namePrefix + (uploadCount + 1));
                    request.setCategoryName(uploadByBatchRequest.getCategoryName());
                    request.setTagName(JSONUtil.toJsonStr(uploadByBatchRequest.getTagName()));

                    // 上传图片
                    PictureVO pictureVO = this.uploadPicture(fileUrl, request, loginUser);
                    log.info("图片上传成功，id = {}", pictureVO.getId());
                    uploadCount++;
                } catch (Exception e) {
                    log.warn("图片上传失败：{}", fileUrl, e);
                }

                if (uploadCount >= count) break;
            }
        }
        return uploadCount;
    }

    /**
     * 从百度图片HTML中提取图片URL
     */
    private List<String> parseBaiduImageUrls(String html) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"objURL\":\"(.*?)\"");
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    @Override
    public boolean updatePicture(Picture picture, User loginUser) {
        // 校验参数
        if (picture == null || picture.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 补充审核参数
        this.fillReviewParams(picture, loginUser, null);

        // 检查图片是否符合规则
        this.validPicture(picture);

        // 更新数据库
        boolean success = this.updateById(picture);
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        // 异步更新 ES （不影响主事务提交）
        esUpdateService.updatePictureEs(picture.getId());

        return true;
    }

    @Override
    public Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        // 校验参数
        if (pictureQueryRequest == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        long size = pictureQueryRequest.getPageSize();

        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null ,ErrorCode.NOT_LOGIN_ERROR);
        String userRole = loginUser.getUserRole();
        ThrowUtils.throwIf(userRole.equals(UserConstant.BAN_ROLE), ErrorCode.NO_AUTH_ERROR, "封禁用户禁止获取数据");

        // 限制爬虫
        ThrowUtils.throwIf(size > CrawlerConstant.BAN_COUNT, ErrorCode.PARAMS_ERROR);

        // 普通用户默认只能查看已过审的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 构建缓存Key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = RedisConstant.PUBLIC_PIC_REDIS_KEY_PREFIX + hashKey;

        // 1. 先查本地缓存
        String cacheValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cacheValue != null) {
            Page<PictureVO> cachedPage = JSONUtil.toBean(cacheValue, Page.class);
            return cachedPage;
        }

        // 2. 本地缓存未命中，查Redis
        cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cacheValue != null) {
            Page<PictureVO> cachedPage  = JSONUtil.toBean(cacheValue, Page.class);
            // 更新本地缓存
            LOCAL_CACHE.put(cacheKey, cacheValue);
            return cachedPage;
        }

        // 3. Redis未命中，查数据库,将picture -> pictureVO
        Page<PictureVO> pictureVOPage = this.getPictureVOByPage(pictureQueryRequest);

        // 写入Redis和本地缓存
        cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        int cacheExpireTime = 300 + RandomUtil.randomInt(0,300); // 5 - 10 分钟随机过期，防止雪崩
        stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        LOCAL_CACHE.put(cacheKey, cacheValue);

        return pictureVOPage;
    }

    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10_000L) // 最大 10000 条
            .expireAfterWrite(Duration.ofMinutes(5))  // 缓存 5 分钟后移除
            .build();

    @Override
    public boolean deletePicture(Long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 判断图片是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 开启事务
        Long finalSpaceId = oldPicture.getSpaceId();
        transactionTemplate.execute(status -> {
            // 数据库删除
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

            // 更新空间的使用额度
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, oldPicture.getId())
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }

            // 从ES删除
            try {
                esPictureDao.deleteById(pictureId);
            } catch (Exception e) {
                log.error("Delete picture from ES failed, pictureId: {}", pictureId, e);
                throw new RuntimeException("ES 删除失败", e); // 关键点
            }
            return true;
        });

        // 异步清理文件
        this.clearPictureFile(oldPicture);
        return true;
    }

    @Override
    @Async("asyncExecutor")
    public void clearPictureFile(Picture oldPicture) {
        if (oldPicture == null) {
            // 若 oldPicture 为 null，直接返回，避免空指针异常
            return;
        }
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getWebpUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getWebpUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }

        // 从 URL 中提取原始 key（去掉域名前缀）
        String webpKey = pictureUrl.replace(cosClientConfig.getHost(), "");

        // 1. 删除WebP 压缩图
        cosManager.deleteObject(webpKey);

        // 2. 删除 原图
        String originalUrl = oldPicture.getUrl();
        String originalKey = originalUrl.replace(cosClientConfig.getHost(), "");
        cosManager.deleteObject(originalKey);

        // 3. 删除缩略图（如果存在）
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            String thumbnailKey = thumbnailUrl.replace(cosClientConfig.getHost(), "");
            cosManager.deleteObject(thumbnailKey);
        }
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {

        Long userId = picture.getUserId();
        Long loginUserId = loginUser.getId();

        // 身份判断
        boolean isAdmin = userService.isAdmin(loginUser);
        boolean isMySelf = userId.equals(loginUserId);

        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {// 公共图库,仅本人图片或是管理者才可操作
            if (!isAdmin && !isMySelf) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前用户，无删除他人图片的权限");
            }
            return; // 检查通过，直接返回
        }

        Space space = spaceService.getById(spaceId);
        boolean isSpaceMember = spaceUserService.isSpaceMember(loginUserId, spaceId);

        Integer spaceType = space.getSpaceType();
        boolean isPrivate = spaceType.equals(SpaceTypeEnum.PRIVATE.getValue());
        boolean isTeam = spaceType.equals(SpaceTypeEnum.TEAM.getValue());

        if (!isMySelf && isPrivate) { //私有空间, 仅空间本人可以操作
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅本人可以操作");
        } else if (!isSpaceMember && isTeam) { //团队空间, 仅团队内的成员 可以操作
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅团队内的成员可以操作");
        }
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 1.校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 2.校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        spaceService.checkPrivateSpaceAuth(loginUser, space);

        // 3.查询该空间下所有含主色调的图片
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        // 如果没有图片，直接返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return new ArrayList<>();
        }
        // 将颜色字符串转换为主色调
        Color targetColor = Color.decode(picColor);

        // 4. 计算相似度并排序
        List<Picture> sortedPictureList = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPicColor();
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);

                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                .limit(12) // 取前 12 个
                .collect(Collectors.toList());

        // 5.返回结果
        return sortedPictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

    @Override
    public boolean editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        // 1.获取、校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();

        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        if (spaceId == null || spaceId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2.校验空间权限
        Space space = spaceService.getById(spaceId);
        spaceService.checkPrivateSpaceAuth(loginUser, space);

        // 3.查询指定图片
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                //	再加一个过滤条件：只选出 ID 属于 pictureIdList 的图片
                .in(Picture::getId, pictureIdList)
                .list();
        if (pictureList.isEmpty()) {
            return false;
        }

        // 4.更新分类和标签
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });

        // 5.批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);

        // 6.更新数据库
        transactionTemplate.execute(status -> {
            boolean result = this.updateBatchById(pictureList);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量编辑失败");

            return null;
        });

        // 7.更新ES数据
        esUpdateService.BatchUpdatePictureEs(pictureIdList);

        return true;
    }

    @Override
    public Page<PictureVO> getFollowPicture(PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        ServletRequestAttributes attribute = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attribute.getRequest();

        Page<Picture> page = new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize());

        // 根据当前用户id，查询用户的关注列表所有用户id
        User loginUser = userService.getLoginUser(request);
        List<Long> followList = userFollowsService.getFollowList(loginUser.getId());

        if (CollectionUtils.isEmpty(followList)) {
            return new Page<>();
        }

        // 构建查询条件 (关注者们的图片)
        LambdaQueryWrapper<Picture> pictureLambdaQueryWrapper = new LambdaQueryWrapper<>();
        pictureLambdaQueryWrapper
                .in(Picture::getUserId, followList)
                .eq(Picture::getReviewStatus, PictureReviewStatusEnum.PASS.getValue())
                .and(
                        picture -> picture.isNull(Picture::getSpaceId)
                                .or()
                                .eq(Picture::getSpaceId, 0)
                ).orderByDesc(Picture::getEditTime);

        // 查询数据库
        Page<Picture> picturePage = this.page(page, pictureLambdaQueryWrapper);

        // 查询脱敏图片信息
        List<Picture> pictureList = picturePage.getRecords();
        List<PictureVO> pictureVOList = getPictureVOList(pictureList);
        //List<PictureVO> pictureVOList = pictureList.stream().map(this::getPictureVO).collect(Collectors.toList());

        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        pictureVOPage.setRecords(pictureVOList);

        return pictureVOPage;
    }

    @Override
    public List<PictureVO> getTop10PictureWithCache(Long id) {

        // 构建缓存Key
        String cacheKey = RedisConstant.TOP_10_PIC_REDIS_KEY_PREFIX + id;

        try {
            // 1.先查本地缓存
            String cacheValue = LOCAL_CACHE.getIfPresent(cacheKey);
            if (cacheValue != null) {
                return objectMapper.readValue(cacheValue, new TypeReference<List<PictureVO>>() {
                });
            }

            // 2.本地缓存未命中，查Redis
            cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cacheValue != null) {
                List<PictureVO> cachedPage = objectMapper.readValue(cacheValue, new TypeReference<List<PictureVO>>() {
                });

                // 更新本地缓存
                LOCAL_CACHE.put(cacheKey, cacheValue);
                return cachedPage;
            }
        } catch (JsonProcessingException e) {
            log.error("获取缓存失败：", e);
        }

        // 3. Redis未命中，查数据库,将picture -> pictureVO
        List<Picture> pictureList = this.getTop10PictureList(id);
        List<PictureVO> pictureVOList = this.getPictureVOList(pictureList);

        // 更新本地缓存、redis
        LOCAL_CACHE.put(cacheKey, JSONUtil.toJsonStr(pictureVOList));

        int cacheExpireTime = RedisConstant.TOP_100_PIC_REDIS_KEY_EXPIRE_TIME + RandomUtil.randomInt(0, 300); // 设置过期时间1天，加随机5分钟
        stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(pictureVOList),
                cacheExpireTime, TimeUnit.SECONDS);

        return pictureVOList;
    }

    /**
     * 获取 top10 图片列表
     */
    private List<Picture> getTop10PictureList(Long id) {
        LambdaQueryWrapper<Picture> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Picture::getReviewStatus, 1)
                .isNull(Picture::getSpaceId);

        // 根据不同时间查询
        LocalDate today = LocalDate.now();
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        switch (id.intValue()) {
            case 1: // 周榜（上周一 00:00 ~ 上周日 23:59:59）
                LocalDate lastWeekStart = today.minusWeeks(1).with(DayOfWeek.MONDAY);
                LocalDate lastWeekEnd = lastWeekStart.plusDays(6);
                startDateTime = lastWeekStart.atStartOfDay();
                endDateTime = lastWeekEnd.atTime(LocalTime.MAX);
                break;
            case 2: // 月榜（上月 1号 00:00 ~ 上月最后一天 23:59:59）
                LocalDate lastMonthStart = today.minusMonths(1).withDayOfMonth(1);
                LocalDate lastMonthEnd = lastMonthStart.withDayOfMonth(lastMonthStart.lengthOfMonth());
                startDateTime = lastMonthStart.atStartOfDay();
                endDateTime = lastMonthEnd.atTime(LocalTime.MAX);
                break;
            case 3: // 总榜
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 时间范围条件
        if (startDateTime != null && endDateTime != null) {
            lambdaQueryWrapper.between(Picture::getUpdateTime,
                    Timestamp.valueOf(startDateTime),
                    Timestamp.valueOf(endDateTime));
        }
        // 排序规则（权重公式）
        lambdaQueryWrapper.last(
                "ORDER BY (likeCount * 0.4 + commentCount * 0.2 + viewCount * 0.1 + shareCount * 0.3) DESC LIMIT 10"
        );

        return this.list(lambdaQueryWrapper);
    }

    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule) || CollUtil.isEmpty(pictureList)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }

}




