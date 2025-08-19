package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.dto.user.UserQueryRequest;
import com.xin.graphdomainbackend.model.dto.user.UserUpdateRequest;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.vo.LoginUserVO;
import com.xin.graphdomainbackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;


/**
* @author xin
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-04-20 19:48:52
*/
public interface UserService extends IService<User> {

    /**
     * 对密码进行加盐 加密
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 发送验证码
     * @param email 邮箱
     * @param type 验证码类型
     * @param request http请求
     * @return 是否发送成功
     */
    boolean sendEmailCode(String email, String type, HttpServletRequest request);

    /**
     * 用户注册
     * @param email 邮箱
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @param code 验证码
     * @return 新用户 id
     */
    long userRegister(String email, String userPassword, String checkPassword, String code);

    /**
     * 用户登录
     * @param accountOrEmail 账号或邮箱
     * @param userPassword 用户密码
     * @param request http请求
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String accountOrEmail, String userPassword, HttpServletRequest request);

    /**
     * 判断用户是否登录，获取当前登录用户信息
     * @param request http请求
     * @return 返回登录对象
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 验证用户输入的验证码是否正确
     * @param verifyCode 用户输入的验证码
     * @param serverIfCode 服务器端存储的加密后的验证码
     * @return 如果验证成功返回true，否则返回false
     */
    boolean validateCaptcha(String verifyCode, String serverIfCode);

    /**
     * 获得脱敏后的登录用户信息
     * @param currentUser 当前用户对象
     * @return 脱敏后的登录用户信息
     */
    LoginUserVO getLoginUserVO(User currentUser);

    /**
     * 获取脱敏后的单个用户信息
     * @param user 用户对象
     * @return 脱敏后的用户
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的多个用户信息
     * @param userList 用户列表
     * @return 脱敏后的用户列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户退出
     * @param request http请求
     * @return 如果退出成功返回true，否则返回false
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取用户的查询条件构造器
     * @param userQueryRequest 用户查询请求对象，包含筛选条件
     * @return MyBatis-Plus 提供的查询构造器 QueryWrapper，用于数据库查询条件构建
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 更新用户信息（包括权限校验和字段保护）
     * @param userUpdateRequest 用户更新请求对象，包含需要更新的信息
     * @param request 当前 HTTP 请求，用于获取登录用户信息
     * @return 更新是否成功
     */
    boolean updateUser(UserUpdateRequest userUpdateRequest, HttpServletRequest request);

    /**
     * 判断用户是否为管理员
     * @param loginUser 登录用户
     * @return true 为管理员
     */
    boolean isAdmin(User loginUser);

    /**
     * 生成一个算术类型的图形验证码，并将答案进行加密存储到 Redis。
     * 验证码为带有干扰的图片，内容是简单的加减乘法表达式。
     *
     * @return 包含 base64 编码的验证码图片和加密后的答案 key 的 Map。
     */
    Map<String, String> getCaptcha();

    /**
     * 判断用户时候登录
     * @param request Http请求
     */
    Boolean isLogin(HttpServletRequest request);
}
