package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.entity.User;

import javax.servlet.http.HttpServletRequest;


/**
* @author xin
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-04-20 19:48:52
*/
public interface UserService extends IService<User> {

    /**
     * 获取加密后的密码
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
<<<<<<< HEAD
     * 发送验证码
     * @param email 邮箱
     * @param type 验证码类型
     * @param request http请求
     * @return 是否发送成功
     */
    boolean sendEmailCode(String email, String type, HttpServletRequest request);

    /**
=======
>>>>>>> 3c4d9bdfcda0ce38735af2264f321ce95210a592
     * 用户注册
     * @param email 邮箱
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @param code 验证码
     * @return 新用户 id
     */
    long userRegister(String email, String userPassword, String checkPassword, String code);

}
