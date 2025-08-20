package com.xin.graphdomainbackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.annotation.AuthCheck;
import com.xin.graphdomainbackend.annotation.LoginCheck;
import com.xin.graphdomainbackend.annotation.SaSpaceCheckPermission;
import com.xin.graphdomainbackend.api.imagesearch.ImageSearchByCrawlerApi;
import com.xin.graphdomainbackend.api.imagesearch.model.ImageSearchResult;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.constant.UserConstant;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.manager.auth.SpaceUserAuthManager;
import com.xin.graphdomainbackend.manager.auth.StpKit;
import com.xin.graphdomainbackend.manager.auth.model.SpaceUserPermissionConstant;
import com.xin.graphdomainbackend.manager.crawler.CrawlerManager;
import com.xin.graphdomainbackend.mapper.SpaceMapper;
import com.xin.graphdomainbackend.model.dto.DeleteRequest;
import com.xin.graphdomainbackend.model.dto.picture.*;
import com.xin.graphdomainbackend.model.entity.Picture;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.enums.PictureReviewStatusEnum;
import com.xin.graphdomainbackend.model.enums.SpaceTypeEnum;
import com.xin.graphdomainbackend.model.vo.PictureTagCategory;
import com.xin.graphdomainbackend.model.vo.PictureVO;
import com.xin.graphdomainbackend.service.PictureService;
import com.xin.graphdomainbackend.service.SpaceService;
import com.xin.graphdomainbackend.service.TagService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ResultUtils;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private TagService tagService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceMapper spaceMapper;

    @Resource
    private CrawlerManager crawlerManager;


    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture( @RequestPart("file") MultipartFile multipartFile,
                                                  PictureUploadRequest pictureUploadRequest,
                                                  HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 通过URL上传图片
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest uploadRequest,
                                                      HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = uploadRequest.getFileUrl();

        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, uploadRequest, loginUser);

        return ResultUtils.success(pictureVO);

    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_LOGIN_ERROR);
        if (deleteRequest == null || deleteRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(request);
        Long PictureId = deleteRequest.getId();

        // 校验图片权限 (已修改为SaToken校验权限)
        // Picture targetPicture = pictureService.getById(PictureId);
        // pictureService.checkPictureAuth(loginUser, targetPicture);

        // 操作数据库
        boolean result = pictureService.deletePicture(PictureId, loginUser);

        return ResultUtils.success(result);
    }

    /**
     * 更新图片（仅管理员可用）
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest updateRequest,
                                               HttpServletRequest request) {
        if (updateRequest == null
                || updateRequest.getId() < 0
                ||request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 将 DTO -> 实体类
        Picture picture = new Picture();
        BeanUtil.copyProperties(updateRequest, picture);
        // 将tags的List -> String
        picture.setTags(JSONUtil.toJsonStr(updateRequest.getTags()));
        // 更新
        pictureService.updatePicture(picture, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片（面向普通人）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_LOGIN_ERROR);
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(pictureService.editPicture(pictureEditRequest, request));
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);

        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            User loginUser = userService.getLoginUser(request);
            pictureService.checkPictureAuth(loginUser, picture);
        }
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片包装类 （主要面向普通用户）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        BaseResponse<Picture> pictureById = getPictureById(id, request);
        Picture picture = pictureById.getData();

        assert picture != null;
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            // 修改为SaToken 编程式校验权限
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }

        // 为权限塞值
        Space space = spaceService.getById(spaceId);
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);

        PictureVO pictureVO = pictureService.getPictureVO(picture);
        pictureVO.setPermissionList(permissionList);

        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {

        Long spaceId = pictureQueryRequest.getSpaceId();
        // 管理员没有传 spaceId，则查公开图库 + 所有团队空间图片
        if (spaceId == null) {
            // 查 spaceId 为 null 的公开图库
            pictureQueryRequest.setNullSpaceId(true);

            // 加上团队空间的 ID 列表
            List<Long> teamSpaceIdList = spaceMapper.selectTeamSpaceIds(SpaceTypeEnum.TEAM.getValue());
            pictureQueryRequest.setTeamSpaceIdList(teamSpaceIdList); // 塞入是团队空间的id
        }
        Page<Picture> pictureByPage = pictureService.getPictureByPage(pictureQueryRequest);

        return ResultUtils.success(pictureByPage);
    }

    /**
     * 分页获取图片列表封装类（主要面向普通用户）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 公开图库
        if (spaceId == null) { // 普通用户默认只能查看已过审的公开数据
            // 如果传了 reviewStatus，说明是用户主动筛选，比如“我的发布”
            if (pictureQueryRequest.getReviewStatus() == null) {
                pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            }
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            Integer spaceType = space.getSpaceType();
            if (spaceType.equals(SpaceTypeEnum.PRIVATE.getValue())) { // 私有空间,不需要审核（只有本人可以查看）
                // 修改为SaToken 编程式校验权限
                boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
                ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR, "仅本人可以操作");
            }
            if (spaceType.equals(SpaceTypeEnum.TEAM.getValue())) { // 团队空间只能查看已审核的
                pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            }
        }

        // 反爬检测
        crawlerManager.detectNormalRequest(request);

        Page<PictureVO> pictureVOByPage = pictureService.getPictureVOByPage(pictureQueryRequest);
        return ResultUtils.success(pictureVOByPage);
    }

    /**
     * 分页获取图片列表封装类（"我的发布"）
     */
    @PostMapping("/list/my/page/vo")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<Page<PictureVO>> listMyPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                               HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();

        // 强制绑定当前用户
        pictureQueryRequest.setUserId(userId);
        // 如果传了 reviewStatus
        Page<PictureVO> pictureVOByPage = pictureService.getPictureVOByPage(pictureQueryRequest);

        return ResultUtils.success(pictureVOByPage);
    }

    /**
     * 审核图片
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/review")
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量抓取并创建图片
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest uploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(uploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(uploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);

    }

    /**
     * 分页获取图片列表（封装类，有缓存）
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                                      HttpServletRequest request) {
        return ResultUtils.success(
                pictureService.listPictureVOByPageWithCache(pictureQueryRequest, request)
        );
    }

    /**
     * 以图搜图(爬取百度识图url)
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest, HttpServletRequest request) {
        userService.getLoginUser(request);

        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImageSearchByCrawlerApi.searchImage(picture.getThumbnailUrl());

        return ResultUtils.success(resultList);
    }

    /**
     * 以图搜图(bing)
     * TODO
     */

    /**
     * 按照颜色搜索
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest pictureByColorRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(pictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.NOT_LOGIN_ERROR);

        String picColor = pictureByColorRequest.getPicColor();
        Long spaceId = pictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> pictureVOList = pictureService.searchPictureByColor(spaceId, picColor, loginUser);

        return ResultUtils.success(pictureVOList);
    }

    /**
     * 批量编辑图片
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        boolean result = pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);

        return ResultUtils.success(result);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();

        List<String> tagList = tagService.listTag();
        pictureTagCategory.setTagList(tagList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 关注列表照片
     */
    @PostMapping("/follow")
    @LoginCheck
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<Page<PictureVO>> getFollowPicture(@RequestBody PictureQueryRequest pictureQueryRequest) {
        return ResultUtils.success(pictureService.getFollowPicture(pictureQueryRequest));
    }
}
