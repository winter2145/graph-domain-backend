package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.esdao.EsPictureDao;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.manager.FileManager;
import com.xin.graphdomainbackend.mapper.PictureMapper;
import com.xin.graphdomainbackend.model.dto.file.UploadPictureResult;
import com.xin.graphdomainbackend.model.dto.picture.PictureEditRequest;
import com.xin.graphdomainbackend.model.dto.picture.PictureQueryRequest;
import com.xin.graphdomainbackend.model.dto.picture.PictureUploadRequest;
import com.xin.graphdomainbackend.model.entity.Picture;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.es.EsPicture;
import com.xin.graphdomainbackend.model.vo.PictureVO;
import com.xin.graphdomainbackend.model.vo.UserVO;
import com.xin.graphdomainbackend.service.PictureService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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

        // MyBatis-Plus分页查数据库（带查询条件）,得到原始Picture列表（含敏感数据）
        Page<Picture> page = this.page(new Page<>(current, pageSize), queryWrapper);

        // 创建一个与Picture一样大的分页PictureVO
        long total = page.getTotal();
        Page<PictureVO> pictureVOPage = new Page<>(current, pageSize, total);

        List<Picture> records = page.getRecords();
        // 转成 PictureVO 列表
        List<PictureVO> pictureVOList = this.getPictureVOList(records);

        pictureVOPage.setRecords(pictureVOList);

        return pictureVOPage;
    }

    @Override
    public Page<Picture> getPictureByPage(PictureQueryRequest pictureQueryRequest) {

        long current = pictureQueryRequest.getCurrent(); // 当前页号
        long pageSize = pictureQueryRequest.getPageSize(); //每页大
        QueryWrapper<Picture> queryWrapper = this.getQueryWrapper(pictureQueryRequest);

        Page<Picture> picturePage = this.page(new Page<>(current, pageSize), queryWrapper);

        return picturePage;
    }

    @Override
    public boolean editPicture(PictureEditRequest pictureEditRequest, HttpServletRequest request) {

        // 判断数据库中是否存在
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
    public boolean updatePicture(Picture picture) {
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
    public void deletePicture(Long id, User loginUser) {

    }

    /**
     * 增加图片浏览量
     */
    private void incrementViewCount(Long pictureId, HttpServletRequest request) {
        // 检查是否需要增加阅览量

    }
}




