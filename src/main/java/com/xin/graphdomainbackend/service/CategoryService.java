package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.model.dto.PageRequest;
import com.xin.graphdomainbackend.model.entity.Category;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.vo.CategoryVO;

import java.util.List;

/**
* @author Administrator
* @description 针对表【category(分类)】的数据库操作Service
* @createDate 2025-09-02 21:04:45
*/
public interface CategoryService extends IService<Category> {

    /**
     * 获取所有分类名称列表
     */
    List<String> listCategoryName(Integer type);

    /**
     * 分页获取 分类
     */
    Page<CategoryVO> getCategoryVOByPage(PageRequest pageRequest);

    /**
     * 分类列表脱敏
     */
    List<CategoryVO> getCategoryVOByList(List<Category> categories);

    /**
     * 增加分类
     */
    Boolean addCategory(String categoryName);

    /**
     * 删除分类
     */
    Boolean deleteCategory(long id);

    /**
     * 根据分类 名字，查找分类
     */
    List<CategoryVO> searchByName(String name);

}
