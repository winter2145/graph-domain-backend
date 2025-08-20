package com.xin.graphdomainbackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.annotation.AuthCheck;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.constant.UserConstant;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.manager.crawler.CrawlerManager;
import com.xin.graphdomainbackend.model.dto.DeleteRequest;
import com.xin.graphdomainbackend.model.dto.user.*;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.vo.LoginUserVO;
import com.xin.graphdomainbackend.model.vo.UserVO;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ResultUtils;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CrawlerManager crawlerManager;

    /**
     * 获取验证码
     */
    @GetMapping("/getCode")
    public BaseResponse<Map<String, String>> getCode() {
        Map<String, String> captchaData = userService.getCaptcha();
        return  ResultUtils.success(captchaData);
    }
    /**
     * 获取邮箱验证码
     */
    @PostMapping("/get_emailcode")
    public BaseResponse<Boolean> getEmailCode(@RequestBody EmailCodeRequest emailCodeRequest, HttpServletRequest request) {
        if (emailCodeRequest == null || StrUtil.hasBlank(emailCodeRequest.getEmail(),emailCodeRequest.getType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String email = emailCodeRequest.getEmail();
        String type = emailCodeRequest.getType();
        // 检测高频操作
        crawlerManager.detectFrequentRequest(request);

        boolean result = userService.sendEmailCode(email, type, request);

        return ResultUtils.success(result);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest registerRequest) {
        ThrowUtils.throwIf(registerRequest == null, ErrorCode.PARAMS_ERROR);
        String email = registerRequest.getEmail();
        String userPassword = registerRequest.getUserPassword();
        String code = registerRequest.getCode();
        String checkPassword = registerRequest.getCheckPassword();
        long result = userService.userRegister(email, userPassword, checkPassword, code);

        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String accountOrEmail = userLoginRequest.getAccountOrEmail();
        String password = userLoginRequest.getPassword();
        String verifyCode = userLoginRequest.getVerifyCode();
        String serverIfCode = userLoginRequest.getServerIfCode();

        if (StrUtil.hasBlank(accountOrEmail, password, verifyCode, serverIfCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.validateCaptcha(verifyCode, serverIfCode);
        LoginUserVO userVO = null;
        if (result) {
            userVO = userService.userLogin(accountOrEmail, password, request);
        }

        // 设置在线缓存（私聊使用）
        if (userVO != null && userVO.getId() != null) {
            stringRedisTemplate.opsForValue().set("online:" + userVO.getId(), "online", 10, TimeUnit.MINUTES);
        }

        return ResultUtils.success(userVO);
    }

    /**
     * 获取用户是否登录
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User currentUser = userService.getLoginUser(request);
        LoginUserVO loginUserVO = userService.getLoginUserVO(currentUser);

        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户退出登录
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        // 删除用户在线缓存（私聊使用）
        User user = userService.getLoginUser(request);
        stringRedisTemplate.delete("online:" + user.getId());

        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        boolean result = userService.updateUser(userUpdateRequest, request);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }

        return ResultUtils.success(true);
    }

    /**
     * 增加用户（管理员）
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);

        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);

        final String defaultPassword = UserConstant.DEFAULT_PASSWORD;

        // 对密码加密，保存至数据库中
        String encryptPassword = userService.getEncryptPassword(defaultPassword);
        user.setUserPassword(encryptPassword);
        boolean save = userService.save(user);

        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(user.getId());
    }

    /**
     * 删除用户（管理员）
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.removeById(deleteRequest.getId());

        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserId(long id) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);

        User user = userService.getById(id);

        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类 （主要面向普通用户）
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserId(id);

        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUesrVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);

        long current = userQueryRequest.getCurrent(); // 当前页号
        long pageSize = userQueryRequest.getPageSize(); // 每页大小

        // MyBatis-Plus分页查数据库（带查询条件）,得到原始User列表（含敏感数据）
        Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));

        // 创建一个新的分页对象 Page<UserVO>,跟Page<User> userPage一样大，是为了存储其信息
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());

        // 转换成 UserVO 列表
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());

        // 脱敏后的记录塞到分页对象
        userVOPage.setRecords(userVOList);

        return ResultUtils.success(userVOPage);
    }

}
