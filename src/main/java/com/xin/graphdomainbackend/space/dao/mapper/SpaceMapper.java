package com.xin.graphdomainbackend.space.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xin.graphdomainbackend.space.dao.entity.Space;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author Administrator
* @description 针对表【space(空间)】的数据库操作Mapper
* @createDate 2025-05-18 07:08:21
* @Entity com.xin.graphdomainbackend.model.entity.Space
*/
public interface SpaceMapper extends BaseMapper<Space> {
    @Select("SELECT id FROM space WHERE spaceType = #{spaceType}")
    List<Long> selectTeamSpaceIds(@Param("spaceType") Integer spaceType);
}




