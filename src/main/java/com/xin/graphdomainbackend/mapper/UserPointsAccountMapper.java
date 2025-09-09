package com.xin.graphdomainbackend.mapper;

import com.xin.graphdomainbackend.model.entity.UserPointsAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
* @author Administrator
* @description 针对表【user_points_account(用户积分账户表)】的数据库操作Mapper
* @createDate 2025-09-07 15:21:49
* @Entity com.xin.graphdomainbackend.model.entity.UserPointsAccount
*/
public interface UserPointsAccountMapper extends BaseMapper<UserPointsAccount> {

    @Select("SELECT * " +
            "FROM user_points_account " +
            "WHERE userId = #{userId};")
    UserPointsAccount selectByUserId(@Param("userId") Long userId);
}




