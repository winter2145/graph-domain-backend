package com.xin.graphdomainbackend.category.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xin.graphdomainbackend.category.dao.entity.Category;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author Administrator
* @description 针对表【category(分类)】的数据库操作Mapper
* @createDate 2025-09-02 21:04:45
* @Entity com.xin.graphdomainbackend.model.entity.Category
*/
public interface CategoryMapper extends BaseMapper<Category> {

    @Select("select categoryName from category where isDelete = 0 and type = #{type}")
    List<String> listCategoryByType(@Param("type") Integer type);

}




