package com.xin.graphdomainbackend.picture.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xin.graphdomainbackend.picture.dao.entity.Picture;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
* @author Administrator
* @description 针对表【picture】的数据库操作Mapper
* @createDate 2025-04-30 19:13:10
* @Entity com.xin.graphdomainbackend.model.entity.Picture
*/
public interface PictureMapper extends BaseMapper<Picture> {

    @Update("UPDATE picture " +
            "SET likeCount = likeCount + #{delta} " +
            "WHERE id = #{targetId} " +
            "AND likeCount >= -#{delta}")
    int updateLikeCount(@Param("targetId") Long targetId, @Param("delta") int delta);


    @Update("UPDATE picture " +
            "SET shareCount = shareCount + #{delta} " +
            "WHERE id = #{targetId} " +
            "AND shareCount >= -#{delta}")
    int updateShareCount(@Param("targetId") Long targetId, @Param("delta") int delta);

}




