package com.xin.graphdomainbackend.mapper;

import com.xin.graphdomainbackend.model.entity.Tag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author Administrator
* @description 针对表【tag(标签)】的数据库操作Mapper
* @createDate 2025-06-20 21:47:50
* @Entity com.xin.graphdomainbackend.model.entity.Tag
*/
public interface TagMapper extends BaseMapper<Tag> {

    @Select("select tagName from tag where isDelete = 0")
    List<String> listTag();
}




