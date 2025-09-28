package com.xin.graphdomainbackend.user.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.xin.graphdomainbackend.common.config.MailSendConfig;
import com.xin.graphdomainbackend.common.constant.EncryptConstant;
import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ConvertObjectUtils;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import com.xin.graphdomainbackend.infrastructure.auth.model.StpKit;
import com.xin.graphdomainbackend.infrastructure.cos.model.UploadPictureResult;
import com.xin.graphdomainbackend.infrastructure.cos.upload.FilePictureUpload;
import com.xin.graphdomainbackend.user.api.dto.request.UserQueryRequest;
import com.xin.graphdomainbackend.user.api.dto.request.UserUpdateRequest;
import com.xin.graphdomainbackend.user.api.dto.vo.LoginUserVO;
import com.xin.graphdomainbackend.user.api.dto.vo.UserVO;
import com.xin.graphdomainbackend.user.constant.UserConstant;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.model.EsUser;
import com.xin.graphdomainbackend.user.dao.entity.User;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.dao.EsUserDao;
import com.xin.graphdomainbackend.user.dao.mapper.UserMapper;
import com.xin.graphdomainbackend.user.enums.UserRoleEnum;
import com.xin.graphdomainbackend.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-04-20 19:48:52
*/
@Service

@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private MailSendConfig emailSendUtil;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private EsUserDao esUserDao;

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
     * 发送验证码
     * @param email 邮箱
     * @param type 验证码类型
     * @param request http请求
     * @return 是否发送成功
     */
    @Override
    public boolean sendEmailCode(String email, String type, HttpServletRequest request) {
        if (StrUtil.hasBlank(email, type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }

        // 限制同一邮箱短时间内重复发送
        String limitKey = String.format("email:code:limit:%s:%s", type, email);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(limitKey))) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码发送过于频繁，请稍后再试");
        }
        // 生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 发送验证码
        try {
            emailSendUtil.sendEmail(email, code);
        } catch (Exception e) {
            log.error("错误信息:{}",e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "邮件发送失败");
        }

        // 将验证码存入redis中，设置过期时间
        String verifyCodeKey = String.format("email:code:verify:%s:%s", type, email);
        // 如果之前设置的验证码还没过期，再次发送验证码，会把原来的覆盖掉，旧验证码会被替换成新的验证码，过期时间也会被刷新。
        stringRedisTemplate.opsForValue().set(verifyCodeKey, code, 5, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(limitKey, "1", 60, TimeUnit.SECONDS);
        return true;
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

        // 1.校验参数
        if(StrUtil.hasBlank(email, userPassword, checkPassword, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        if (!email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
        }
        if(userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能少于8位");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        }

        // 根据用户邮箱 email 构造出 Redis 的Key
        String verifyCodeKey = String.format("email:code:verify:register:%s", email);

        // 根据 Key 获取对应的 邮箱验证码code
        String correctCode = stringRedisTemplate.opsForValue().get(verifyCodeKey);
        if (correctCode == null || !correctCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不正确");
        }

        // 利用 redisson 构造分布式锁 key
        String lockKey = String.format("email:register:lock:%s", email);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean isLock = lock.tryLock(5, TimeUnit.SECONDS);
            // 尝试获取锁，最多等5秒，利用看门狗机制 自动续约，直到锁释放
            // isLock = lock.tryLock(5, -1, TimeUnit.SECONDS);
            if (!isLock) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
            }

            // 2.判断数据库中是否存在对应的邮箱
            QueryWrapper<User> qw = new QueryWrapper<>();
            qw.eq("email", email);
            long count = this.count(qw);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已存在");
            }

            // 使用邮箱前缀作为账号
            String userAccount = email.substring(0, email.indexOf("@"));
            qw = new QueryWrapper<>();
            qw.eq("userAccount", userAccount);
            count = this.count(qw);
            if (count > 0) { // 说明账号已存在
                userAccount = userAccount + RandomUtil.randomNumbers(4);
            }

            // 3.加密密码
            String encryptPassword = this.getEncryptPassword(userPassword);

            // 4.插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setEmail(email);
            user.setUserPassword(encryptPassword);
            user.setUserName(userAccount); // 使用账号作为默认用户名
            user.setUserRole(UserRoleEnum.USER.getValue());

            boolean saveResult = this.save(user);

            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }

            // 删除验证码
            stringRedisTemplate.delete(verifyCodeKey);

            // 数据同步到ES
            EsUser esUser = ConvertObjectUtils.toEsUser(user);
            esUserDao.save(esUser);

            return user.getId();
        } catch (BusinessException e) {
            // 保留原始业务异常信息
            throw e;
        } catch (Exception e) {
            log.error("doCacheRegisterUser error", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "系统异常，请稍后再试");
        } finally { // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    /**
     * 用户登录
     * @param accountOrEmail 账号或邮箱
     * @param userPassword 用户密码
     * @param request http请求
     * @return 脱敏后的用户信息
     */
    @Override
    public LoginUserVO userLogin(String accountOrEmail, String userPassword, HttpServletRequest request) {
        // 1.校验
        if (StrUtil.hasBlank(accountOrEmail, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "密码不能低于8位");
        }

        // 2.加密密码与后端对比，后端的密码是加密的
        String encryptPassword = this.getEncryptPassword(userPassword);

        // 3.查询用户信息
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("userAccount", accountOrEmail).or().eq("email", accountOrEmail);

        User user = this.baseMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("user login failed, accountOrEmail cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        if (!user.getUserPassword().equals(encryptPassword)) {
            log.info("密码错误，用户: {}", accountOrEmail);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        if (UserConstant.BAN_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "此账号处于封禁中");
        }

        // 4.记录用户状态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // (后补) 记录用户登录态到 Sa-token，便于空间鉴权时使用
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);

        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 判断用户是否登录，获取当前登录用户信息
     * @param request http请求
     * @return 返回登录对象
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {

        // 判断Session是否为空
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) attribute;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 查询用户信息
        Long id = currentUser.getId();
        currentUser = this.baseMapper.selectById(id);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 验证用户输入的验证码是否正确
     * @param userInputCaptcha 用户输入的验证码
     * @param serverIfCode 服务器端存储的加密后的验证码
     * @return 如果验证成功返回true，否则返回false
     */
    @Override
    public boolean validateCaptcha(String userInputCaptcha, String serverIfCode) {
        if (userInputCaptcha != null && serverIfCode != null) {
            // 使用Hu tool对用户输入的验证码进行MD5加密
            String encryptedVerifyCode = DigestUtil.md5Hex(userInputCaptcha);
            if(encryptedVerifyCode.equals(serverIfCode)){
                return true;
            }
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
    }

    /**
     * 获得脱敏后的登录用户信息
     * @param currentUser 当前用户登录对象
     * @return 脱敏后的登录用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User currentUser) {
        LoginUserVO loginUserVO = new LoginUserVO();
        if (currentUser != null) {
            BeanUtil.copyProperties(currentUser, loginUserVO);
        }
        return loginUserVO;
    }

    /**
     * 获取脱敏后的单个用户信息
     * @param user 用户对象
     * @return 脱敏后的用户
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取脱敏后的多个用户信息
     * @param userList 用户列表
     * @return 脱敏后的用户列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (userList.isEmpty()) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                //.map(user -> this.getUserVO(user))
                .collect(Collectors.toList());
    }

    /**
     * 用户退出
     * @param request http请求
     * @return 如果退出成功返回true，否则返回false
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户未登录");
        }

        // 1. 清理 Session
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        request.getSession().invalidate(); // 可选：彻底销毁 Session

        // 2. 清理 Sa-Token 登录态
        Object loginId = StpKit.SPACE.getLoginIdDefaultNull();
        if (loginId != null) {
            long userId = Long.parseLong(loginId.toString()); // 安全转换
            stringRedisTemplate.delete("online:" + userId);
        }
        return true;
    }

    /**
     * 获取用户的查询条件构造器
     * @param userQueryRequest 用户查询请求对象，包含筛选条件
     * @return MyBatis-Plus 提供的查询构造器 QueryWrapper，用于数据库查询条件构建
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();

        if (userQueryRequest == null) {
            return queryWrapper;
        }

        // 获取字段
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);

        return queryWrapper;

    }

    /**
     * 更新用户信息（包括权限校验和字段保护）
     * @param userUpdateRequest 用户更新请求对象，包含需要更新的信息
     * @param request 当前 HTTP 请求，用于获取登录用户信息
     * @return 更新是否成功
     */
    @Override
    public boolean updateUser(UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        // 获取用户登录信息
        User loginUser = this.getLoginUser(request);
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isMyself = userUpdateRequest.getId().equals(loginUser.getId());

        // 不是管理员,不能更改他人
        if (!isAdmin && !isMyself) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "当前用户，无修改他人的权限");
        }

        // 非管理员不能修改 userRole 字段
        if (!isAdmin) {
            userUpdateRequest.setUserRole(UserConstant.DEFAULT_ROLE);
        }

        User userToUpdate = new User();
        BeanUtil.copyProperties(userUpdateRequest, userToUpdate);

        boolean result = this.updateById(userToUpdate);

        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }

        // 3. 数据库更新成功后，再同步到 ES
        User dbUser = this.getById(userUpdateRequest.getId()); // 确保取最新数据
        EsUser esUser = ConvertObjectUtils.toEsUser(dbUser);
        esUserDao.save(esUser);

        return true;
    }

    /**
     * 判断用户是否为管理员
     * @param loginUser 登录用户
     * @return true 为管理员
     */
    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE);
    }

    @Override
    public Map<String, String> getCaptcha() {
        Random random = new Random();
        int a = random.nextInt(20);
        int b = random.nextInt(10);
        char op;
        int answer;

        // 生成表达式
        switch (random.nextInt(3)) {
            case 1:
                op = '-';
                if (a < b) {
                    int temp = a; a = b; b = temp;
                }
                answer = a - b;
                break;
            case 2:
                op = '*';
                answer = a * b;
                break;
            default:
                op = '+';
                answer = a + b;
        }

        String expression = a + " " + op + " " + b + " = ";

        // 创建干扰验证码（Shear类型，带扭曲）
        ShearCaptcha shearCaptcha = CaptchaUtil.createShearCaptcha(160, 60, 4, 5);
        // 设置自定义表达式文本
        shearCaptcha.setGenerator(new CodeGenerator() {
            @Override
            public String generate() {
                return expression;
            }

            @Override
            public boolean verify(String code, String input) {
                return code.equals(input);
            }
        });
        shearCaptcha.createCode();

        // 将图片转为 Base64
        String base64Captcha;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            shearCaptcha.write(outputStream);
            base64Captcha = Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("生成验证码失败", e);
        }

        // 加密答案
        String answerStr = String.valueOf(answer);
        String encryptedCaptcha = DigestUtil.md5Hex(answerStr);

        // 存入 Redis，过期时间 5 分钟
        stringRedisTemplate.opsForValue().set("captcha:" + encryptedCaptcha, answerStr, 300, TimeUnit.SECONDS);

        // 封装返回结果
        Map<String, String> result = new HashMap<>();
        result.put("base64Captcha", base64Captcha);
        result.put("encryptedCaptcha", encryptedCaptcha);
        return result;
    }

    @Override
    public Boolean isLogin(HttpServletRequest request) {
        // 判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        return currentUser != null && currentUser.getId() != null;
    }

    @Override
    public String uploadAvatar(MultipartFile multipartFile, Long id, HttpServletRequest request) {
        // 判断用户是否存在
        User user = this.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        //判断文件是否存在
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.NOT_FOUND_ERROR, "头像文件为空");

        // 按照用户id划分目录
        String uploadPathPrefix = String.format("avatar/%s", user.getId());
        // 上传用户头像图片
        UploadPictureResult uploadPictureResult = filePictureUpload.uploadPictureResult(multipartFile, uploadPathPrefix);

        String thumbnailUrl = uploadPictureResult.getThumbnailUrl();
        String originUrl = uploadPictureResult.getUrl();
        String webpUrl = uploadPictureResult.getWebpUrl();
        if(thumbnailUrl != null) {
            user.setUserAvatar(thumbnailUrl);
        } else if (webpUrl != null) {
            user.setUserAvatar(webpUrl);
        } else {
            user.setUserAvatar(originUrl);
        }

        // 更新用户头像
        boolean result = this.updateById(user);

        if (result) {
            // 更新ES
            EsUser esUser = new EsUser();
            BeanUtil.copyProperties(user, esUser);
            esUserDao.save(esUser);
        }

        return user.getUserAvatar();
    }

    @Override
    public boolean banOrUnbanUser(Long userId, Boolean isUnban, User admin) {
        // 1. 校验参数
        if (userId == null || userId <= 0 || isUnban == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 校验管理员权限
        if (!UserConstant.ADMIN_ROLE.equals(admin.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员不能执行此操作");
        }

        // 3. 获取目标用户信息
        User targetUser = this.getById(userId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 4. 检查当前状态是否需要变更
        boolean isBanned = UserConstant.BAN_ROLE.equals(targetUser.getUserRole());
        if (isUnban == isBanned) {
            // 5. 更新用户角色
            User updateUser = new User();
            updateUser.setId(userId);
            // true 解禁，false 封禁
            updateUser.setUserRole(isUnban ? UserConstant.DEFAULT_ROLE : UserConstant.BAN_ROLE);
            updateUser.setUpdateTime(new Date());
            boolean result = this.updateById(updateUser);

            if (result) {
                // 6. 记录操作日志
                log.info("管理员[{}]{}用户[{}]",
                        admin.getUserAccount(),
                        isUnban ? "解封" : "封禁",
                        targetUser.getUserAccount());
            }
            return result;
        } else {
            // 状态已经是目标状态
            String operation = isUnban ? "解封" : "封禁";
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    String.format("该用户当前%s不需要%s", isUnban ? "未被封禁" : "已被封禁", operation));
        }
    }

}




