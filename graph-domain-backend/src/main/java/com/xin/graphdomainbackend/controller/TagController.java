package com.xin.graphdomainbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.annotation.AuthCheck;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.constant.UserConstant;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.model.dto.PageRequest;
import com.xin.graphdomainbackend.model.entity.Tag;
import com.xin.graphdomainbackend.model.vo.TagVO;
import com.xin.graphdomainbackend.service.TagService;
import com.xin.graphdomainbackend.utils.ResultUtils;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/tag")
public class TagController {

    @Resource
    private TagService tagService;

    /**
     * 获取所有标签
     */
    @PostMapping("list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<TagVO>> listTagByPage(@RequestBody PageRequest pageRequest) {
        Page<TagVO> tagVOPage = tagService.getTagByPage(pageRequest);
        return ResultUtils.success(tagVOPage);
    }

    /**
     * 添加标签
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> addTag(String tagName) {
        ThrowUtils.throwIf(tagName == null || tagName.isEmpty(), ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(tagService.addTag(tagName));

    }

    /**
     * 删除标签
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteTag(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        Tag tag = tagService.getById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "请求标签不存在");
        }
        return ResultUtils.success(tagService.deleteTag(tag));
    }

    /**
     * 查找标签
     */
    @GetMapping("/search")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<TagVO>> searchTag(String tagName) {
        ThrowUtils.throwIf(tagName == null || tagName.isEmpty(), ErrorCode.PARAMS_ERROR);
        List<TagVO> tags = tagService.searchByName(tagName);
        return ResultUtils.success(tags);
    }

}
