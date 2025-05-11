package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xin.graphdomainbackend.constant.CrawlerConstant;
import com.xin.graphdomainbackend.constant.RedisConstant;
import com.xin.graphdomainbackend.constant.UserConstant;
import com.xin.graphdomainbackend.esdao.EsPictureDao;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.manager.FileManager;
import com.xin.graphdomainbackend.manager.upload.FilePictureUpload;
import com.xin.graphdomainbackend.manager.upload.PictureUploadTemplate;
import com.xin.graphdomainbackend.manager.upload.UrlPictureUpload;
import com.xin.graphdomainbackend.mapper.PictureMapper;
import com.xin.graphdomainbackend.model.dto.file.UploadPictureResult;
import com.xin.graphdomainbackend.model.dto.picture.*;
import com.xin.graphdomainbackend.model.entity.Picture;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.es.EsPicture;
import com.xin.graphdomainbackend.model.enums.PictureReviewStatusEnum;
import com.xin.graphdomainbackend.model.vo.PictureVO;
import com.xin.graphdomainbackend.model.vo.UserVO;
import com.xin.graphdomainbackend.service.PictureService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Resource
    private EsPictureDao esPictureDao;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
/* 初版仅支持文件上传图片
   @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新,判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR,"图片不存在");
        }
        // 上传图片得到图片信息
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPictureResult(multipartFile, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());

        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }*/

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest uploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 判断是新增还是更新
        Long pictureId = null;
        if (uploadRequest != null) {
            pictureId = uploadRequest.getId();
        }

        // 如果是更新操作,判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        }

        // 上传图片得到图片信息
        // 根据 inputSource 的类型区分上传方式
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        PictureUploadTemplate pictureUploadTemplate;
        if (inputSource instanceof MultipartFile) {
            pictureUploadTemplate = filePictureUpload; // 注入的 FilePictureUpload 实例
        } else if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload; // 注入的 UrlPictureUpload 实例
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的上传类型");
        }

        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPictureResult(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());

        // 补充缩略图url
        if (uploadPictureResult.getThumbnailUrl() != null) {
            picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        }

        // 支持前端自定义名字前缀
        if(StrUtil.isNotBlank(uploadRequest.getPicName()) && uploadRequest.getPicName() != null) {
            picture.setName(uploadRequest.getPicName());
        }

        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());

        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }

    /**
     * 审核参数的填充
     */
    private void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
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
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
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

        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }

        return pictureVO;
    }

    @Override
    public List<PictureVO> getPictureVOList(List<Picture> pictureList) {
        if (pictureList.isEmpty()) {
            return new ArrayList<>();
        } else {
            return pictureList
                    .stream()
                    .map(this::getPictureVO)
                    .collect(Collectors.toList());
        }
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

        // 保留旧的数据
        Picture picture = new Picture();
        BeanUtil.copyProperties(oldPicture, picture);
        // 只覆盖PictureEditRequest中出现的字段
        // Hutool的BeanUtil.copyProperties默认会覆盖同名字段（包括null），不会影响Picture中没有的字段
        BeanUtil.copyProperties(pictureEditRequest, picture);

        // tags字段类型不同，单独处理
        if (pictureEditRequest.getTags() != null) {
            picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        } else if (pictureEditRequest.getTags() == null) {
            picture.setTags(null); // 明确置空
        }
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 补充审核参数
        User loginUser = userService.getLoginUser(request);
        this.fillReviewParams(picture, loginUser);
        // 校验数据
        this.validPicture(picture);

        // 数据库更新
        boolean res = this.updateById(picture);

        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR);

        // 同步更新 ES 数据
        try {
            // 先查询 ES 中是否存在该数据
            Optional<EsPicture> esOptional = esPictureDao.findById(id);
            EsPicture esPicture;
            if (esOptional.isPresent()) {
                // 如果存在，获取现有数据
                esPicture = esOptional.get();
                // 只更新需要修改的字段
                esPicture.setName(picture.getName());
                esPicture.setIntroduction(picture.getIntroduction());
                esPicture.setCategory(picture.getCategory());
                esPicture.setTags(picture.getTags());
                esPicture.setEditTime(picture.getEditTime());
            } else {
                // 如果不存在，创建新的 ES 文档
                esPicture = new EsPicture();
                BeanUtil.copyProperties(picture, esPicture);
            }
            // 保存或更新到 ES
            esPictureDao.save(esPicture);
        } catch (Exception e) {
            log.error("Failed to sync picture to ES during edit, pictureId: {}", picture.getId(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步 ES 数据失败");
        }
        return res;
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
        String reviewMessage = pictureReviewRequest.getReviewMessage();
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

        // 5.同步更新ES数据
        try {
            Optional<EsPicture> esOptional = esPictureDao.findById(id);
            EsPicture esPicture;
            if (esOptional.isPresent()) {
                esPicture = esOptional.get();
                esPicture.setReviewerId(loginUser.getId());
                esPicture.setReviewTime(updatePicture.getReviewTime());
                esPicture.setReviewMessage(reviewMessage);
            } else {
                // 如果不存在，从 MySQL 获取完整数据并创建新的 ES 文档
                Picture fullPicture = this.getById(id);
                esPicture = new EsPicture();
                BeanUtils.copyProperties(fullPicture, esPicture);
            }
        } catch (Exception e) {
            log.error("Failed to sync picture review status to ES, pictureId: {}", id, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步 ES 数据失败");
        }


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
                if (seenUrls.contains(fileUrl)) continue;
                seenUrls.add(fileUrl);

                try {
                    PictureUploadRequest request = new PictureUploadRequest();
                    request.setFileUrl(fileUrl);
                    request.setPicName(namePrefix + (uploadCount + 1));
                    request.setCategoryName(uploadByBatchRequest.getCategoryName());
                    request.setTagName(JSONUtil.toJsonStr(uploadByBatchRequest.getTagName()));

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
        this.fillReviewParams(picture, loginUser);

        // 检查图片是否符合规则
        this.validPicture(picture);

        // 更新数据库
        boolean success = this.updateById(picture);
        if (!success) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        // 同步更新 ES 数据
        try {
            Optional<EsPicture> esPictureOptional = esPictureDao.findById(picture.getId());
            EsPicture esPicture;
            if (esPictureOptional.isPresent()) {
                esPicture = esPictureOptional.get();
                esPicture.setName(picture.getName());
                esPicture.setIntroduction(picture.getIntroduction());
                esPicture.setCategory(picture.getCategory());
                esPicture.setTags(picture.getTags());
                esPicture.setEditTime(picture.getEditTime());
                esPicture.setReviewStatus(picture.getReviewStatus());
                esPicture.setReviewMessage(picture.getReviewMessage());
            } else {
                esPicture = new EsPicture();
                BeanUtil.copyProperties(picture, esPicture);
            }
            esPictureDao.save(esPicture);
            return true;
        } catch (Exception e) {
            log.error("同步ES数据失败, pictureId: {}", picture.getId());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "同步ES失败");
        }

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
    public void deletePicture(Long id, User loginUser) {

    }

    /**
     * 增加图片浏览量
     */
    private void incrementViewCount(Long pictureId, HttpServletRequest request) {
        // 检查是否需要增加阅览量

    }
}




