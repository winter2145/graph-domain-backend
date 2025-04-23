package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.config.MailSendConfig;
import com.xin.graphdomainbackend.constant.EncryptConstant;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.mapper.UserMapper;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.enums.UserRoleEnum;
import com.xin.graphdomainbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

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
        boolean isLock = false;
        try {
            isLock = lock.tryLock(5, TimeUnit.SECONDS);
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
}




