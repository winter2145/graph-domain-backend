package com.xin.graphdomainbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.model.dto.PageRequest;
import com.xin.graphdomainbackend.model.entity.Category;
import com.xin.graphdomainbackend.model.vo.CategoryVO;
import com.xin.graphdomainbackend.service.CategoryService;
import com.xin.graphdomainbackend.mapper.CategoryMapper;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【category(分类)】的数据库操作Service实现
* @createDate 2025-09-02 21:04:45
*/
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
    implements CategoryService{

    @Override
    public List<String> listCategoryName(Integer type) {
        return this.baseMapper.listCategoryByType(type);
    }

    @Override
    public Page<CategoryVO> getCategoryVOByPage(PageRequest pageRequest) {
        // 校验参数
        ThrowUtils.throwIf(pageRequest == null, ErrorCode.PARAMS_ERROR);
        long current = pageRequest.getCurrent();
        long pageSize = pageRequest.getPageSize();

        // 创建与数据库查询到的category一样大的空分页
        Page<Category> categoryPage = this.page(new Page<>(current, pageSize));
        Page<CategoryVO> categoryPageVO = new Page<>(current, pageSize, categoryPage.getTotal());
        List<Category> categories = categoryPage.getRecords();

        // 获取脱敏数据
        List<CategoryVO> categoryVOList = this.getCategoryVOByList(categories);

        // 脱敏数据存放到空分页中
        categoryPageVO.setRecords(categoryVOList);

        return categoryPageVO;
    }

    @Override
    public List<CategoryVO> getCategoryVOByList(List<Category> categories) {
        if (CollectionUtils.isEmpty(categories)) {
            return Collections.emptyList();
        }

        return categories.stream()
                .filter(Objects::nonNull)
                .map(category -> {
                    CategoryVO categoryVO = new CategoryVO();
                    BeanUtils.copyProperties(category, categoryVO);
                    return categoryVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Boolean addCategory(String categoryName) {
        ThrowUtils.throwIf(StringUtils.isBlank(categoryName), ErrorCode.PARAMS_ERROR, "分类名不能为空");

        Category category = new Category();
        if (StringUtils.isNotBlank(categoryName)) {
            category.setCategoryName(categoryName);
            return this.save(category);
        }

        return false;
    }

    @Override
    public Boolean deleteCategory(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 先检查是否存在
        Category category = this.getById(id);
        ThrowUtils.throwIf(category == null, ErrorCode.NOT_FOUND_ERROR, "分类不存在");

        return this.removeById(id);
    }

    @Override
    public List<CategoryVO> searchByName(String name) {

        if (StringUtils.isBlank(name)) {
            return Collections.emptyList();
        }

        List<Category> list = this.lambdaQuery()
                .like(Category::getCategoryName, name)
                .list();

        return this.getCategoryVOByList(list);
    }
}




