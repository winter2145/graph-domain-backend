package com.xin.graphdomainbackend.category.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.category.api.dto.request.CategoryQueryRequest;
import com.xin.graphdomainbackend.category.api.dto.vo.CategoryVO;
import com.xin.graphdomainbackend.category.service.CategoryService;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.common.aop.annotation.AuthCheck;
import com.xin.graphdomainbackend.common.util.ResultUtils;
import com.xin.graphdomainbackend.user.constant.UserConstant;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    /**
     * 分页获取分类列表（管理员）
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<CategoryVO>> listCategoryVO(CategoryQueryRequest categoryQueryRequest) {
        Page<CategoryVO> categoryVOByPage = categoryService.getCategoryVOByPage(categoryQueryRequest);

        return ResultUtils.success(categoryVOByPage);
    }

    /**
     * 获取指定类型的分类列表
     */
    @GetMapping("/list/type/{type}")
    public BaseResponse<List<String>> listCategoryByType(@PathVariable Integer type) {
        return ResultUtils.success(categoryService.listCategoryName(type));
    }

    /**
     * 添加新分类（管理员）
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> addCategory(@RequestParam String categoryName) {
        return ResultUtils.success(categoryService.addCategory(categoryName));
    }

    /**
     * 删除分类（管理员）
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteCategory(@RequestParam Long id) {
        return ResultUtils.success(categoryService.deleteCategory(id));
    }

    /**
     * 搜索分类（管理员）
     */
    @PostMapping("/search")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<CategoryVO>> findCategory(@RequestParam String categoryName) {
        return ResultUtils.success(categoryService.searchByName(categoryName));
    }
}
