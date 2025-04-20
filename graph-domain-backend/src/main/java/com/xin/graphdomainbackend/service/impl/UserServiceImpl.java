package com.xin.graphdomainbackend.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.constant.EncryptConstant;
import com.xin.graphdomainbackend.mapper.UserMapper;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.service.UserService;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-04-20 19:48:52
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    /**
     * 获取加密后的密码
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        return SecureUtil.md5(EncryptConstant.DEFAULT_SALT + userPassword);
    }

    /**
     * 用户注册
     * @param email       邮箱
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @param code         验证码
     * @return 用户注册成功后的ID
     */
    @Override
    public long userRegister(String email, String userPassword, String checkPassword, String code) {
        return 0;
    }
}




