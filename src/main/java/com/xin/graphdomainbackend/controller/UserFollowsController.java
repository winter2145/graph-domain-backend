package com.xin.graphdomainbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.annotation.LoginCheck;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.model.dto.picture.PictureQueryRequest;
import com.xin.graphdomainbackend.model.dto.userfollows.UserFollowersQueryRequest;
import com.xin.graphdomainbackend.model.dto.userfollows.UserFollowsAddRequest;
import com.xin.graphdomainbackend.model.dto.userfollows.UserFollowsIsFollowsRequest;
import com.xin.graphdomainbackend.model.enums.PictureReviewStatusEnum;
import com.xin.graphdomainbackend.model.vo.FollowersAndFansVO;
import com.xin.graphdomainbackend.model.vo.PictureVO;
import com.xin.graphdomainbackend.model.vo.UserVO;
import com.xin.graphdomainbackend.service.PictureService;
import com.xin.graphdomainbackend.service.UserFollowsService;
import com.xin.graphdomainbackend.utils.ResultUtils;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/userfollows")
public class UserFollowsController {

    @Resource
    private UserFollowsService userFollowsService;

    @Resource
    private PictureService pictureService;

    /**
     * 关注、取关
     */
    @LoginCheck
    @PostMapping("/adduserfollows")
    public BaseResponse<Boolean> addUserFollows(@Valid @RequestBody UserFollowsAddRequest userFollowsAddRequest) {
        return ResultUtils.success(userFollowsService.addUserFollows(userFollowsAddRequest));
    }

    /**
     * 查找是否关注
     */
    @LoginCheck
    @PostMapping("/findisfollow")
    public BaseResponse<Boolean> findIsFollow(@RequestBody UserFollowsIsFollowsRequest isFollowsRequest) {

        return ResultUtils.success(userFollowsService.findIsFollow(isFollowsRequest));
    }

    /**
     * 得到关注,粉丝列表
     */
    @LoginCheck
    @PostMapping("/getfolloworfanlist")
    public BaseResponse<Page<UserVO>> getFollowOrFanList(@RequestBody UserFollowersQueryRequest queryRequest){
        return ResultUtils.success(userFollowsService.getFollowOrFanList(queryRequest));
    }

    /**
     * 查找关注和粉丝数量
     */
    @LoginCheck
    @PostMapping("/getfollowandfanscount/{id}")
    public BaseResponse<FollowersAndFansVO> getFollowAndFansCount(@PathVariable Long id){
        return ResultUtils.success(userFollowsService.getFollowAndFansCount(id));
    }

    /**
     * 得到关注或者粉丝的公共的图片数据
     */
    @LoginCheck
    @PostMapping("/getfolloworfanpicture")
    public BaseResponse<Page<PictureVO>> getFollowOrFanPicture(@RequestBody PictureQueryRequest pictureQueryRequest){
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 50, ErrorCode.PARAMS_ERROR);

        ThrowUtils.throwIf(pictureQueryRequest.getUserId() == null, ErrorCode.PARAMS_ERROR, "用户id不能为空");
        pictureQueryRequest.setUserId(pictureQueryRequest.getUserId());
        pictureQueryRequest.setNullSpaceId(true);
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOByPage(pictureQueryRequest));
    }
}
