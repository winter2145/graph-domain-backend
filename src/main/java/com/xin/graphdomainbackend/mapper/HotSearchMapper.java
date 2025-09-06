package com.xin.graphdomainbackend.mapper;

import com.xin.graphdomainbackend.model.entity.HotSearch;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
* @author Administrator
* @description 针对表【hot_search(热门搜索记录表)】的数据库操作Mapper
* @createDate 2025-09-06 09:30:40
* @Entity com.xin.graphdomainbackend.model.entity.HotSearch
*/
public interface HotSearchMapper extends BaseMapper<HotSearch> {

    @Select("SELECT keyword " +
            "FROM hot_search " +
            "WHERE type = #{type} " +
            "  AND lastUpdateTime >= #{startTime} " +
            "  AND isDelete = 0 " +
            "ORDER BY count DESC " +
            "LIMIT #{size}")
    List<String> getHotSearch(@Param("type") String type,
                                 @Param("startTime") Date startTime,
                                 @Param("size") int size);

}




