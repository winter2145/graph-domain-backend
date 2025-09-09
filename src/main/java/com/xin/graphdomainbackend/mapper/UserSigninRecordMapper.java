package com.xin.graphdomainbackend.mapper;

import com.xin.graphdomainbackend.model.entity.UserSigninRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/**
* @author Administrator
* @description 针对表【user_signin_record(用户签到记录表)】的数据库操作Mapper
* @createDate 2025-09-07 15:21:49
* @Entity com.xin.graphdomainbackend.model.entity.UserSigninRecord
*/
public interface UserSigninRecordMapper extends BaseMapper<UserSigninRecord> {

    @Select("SELECT * " +
            "FROM user_signin_record " +
            "WHERE userId = #{userId}" +
            "  AND signDate >= #{today};")
    UserSigninRecord selectByUserIdAndDate(@Param("userId") Long userId,
                                           @Param("today") LocalDate today);
}




