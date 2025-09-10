package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.esdao.EsSpaceDao;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.mapper.SpaceMapper;
import com.xin.graphdomainbackend.model.dto.DeleteRequest;
import com.xin.graphdomainbackend.model.dto.space.SpaceAddRequest;
import com.xin.graphdomainbackend.model.dto.space.SpaceQueryRequest;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.SpaceUser;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.es.EsSpace;
import com.xin.graphdomainbackend.model.entity.es.EsUser;
import com.xin.graphdomainbackend.model.enums.SpaceLevelEnum;
import com.xin.graphdomainbackend.model.enums.SpaceRoleEnum;
import com.xin.graphdomainbackend.model.enums.SpaceTypeEnum;
import com.xin.graphdomainbackend.model.vo.SpaceVO;
import com.xin.graphdomainbackend.model.vo.UserVO;
import com.xin.graphdomainbackend.model.vo.space.SpaceCreatedVO;
import com.xin.graphdomainbackend.service.SpaceService;
import com.xin.graphdomainbackend.service.SpaceUserService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ConvertObjectUtils;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-05-18 07:08:21
*/
@Service
@Slf4j
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    private UserService userService;

    @Resource
    private EsSpaceDao esSpaceDao;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private SpaceUserService spaceUserService;

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1.填充参数默认值
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }

        // 2.校验参数
        this.validSpace(space, true);
        if (loginUser.getId() == null || loginUser.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 3.校验权限, 非管理员只能创建普通级别的空间
        boolean admin = userService.isAdmin(loginUser);
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel()
        && !admin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }

        // 4.控制同一个用户只能创建一个私有空间
        // 利用 redisson 构造分布式锁 key
        long userId = loginUser.getId();
        String lockKey = String.format("space:create:private:lock:%s", userId);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean isLock = lock.tryLock(5, TimeUnit.SECONDS);
            if (!isLock) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
            }
            if (space.getSpaceType().equals(SpaceTypeEnum.PRIVATE.getValue())) {
                // 查询该用户是否已存在私有空间
                LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Space::getUserId, loginUser.getId())
                        .eq(Space::getSpaceType, SpaceTypeEnum.PRIVATE.getValue());
                long count = this.count(queryWrapper);
                if (count > 0) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "每个用户只能创建一个私有空间");
                }
            }
            // 5.填充信息，保存空间
            this.fillSpaceBySpaceLevel(space);
            space.setUserId(loginUser.getId());

            boolean result = this.save(space);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存空间到数据库失败");

            // 创建成功，如果是团队空间，关联新增团队成员记录
            if (SpaceTypeEnum.TEAM.getValue() == space.getSpaceType()) {
                SpaceUser spaceUser = new SpaceUser();
                spaceUser.setSpaceId(space.getId());
                spaceUser.setUserId(userId);
                spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());// 创建者为管理员
                spaceUser.setStatus(1);  // 设置为已通过状态
                result = spaceUserService.save(spaceUser);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
            }

            // 数据同步到ES
            Space dbSpace = this.getById(space.getId());
            EsSpace esSpace = ConvertObjectUtils.toEsSpace(dbSpace);
            esSpaceDao.save(esSpace);

            // 创建分表(学习用，非必要不用)
            // dynamicShardingManager.createSpacePictureTable(space);

            return space.getId();
        } catch (BusinessException e) {
            // 保留原始业务异常信息
            throw e;
        } catch (Exception e) {
            log.error("doCacheRegisterUser error", e);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "系统异常，请稍后再试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);

        // 创建时校验
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            if (spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类别不能为空");
            }
        }
        // 修改数据时，空间名称进行校验
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        // 修改数据时，空间级别进行校验
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        // 修改数据时，空间类别进行校验
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类别不存在");
        }


    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSpace(Long spaceId, User loginUser) {
        ThrowUtils.throwIf(spaceId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 判断空间是否存在
        Space oldSpace = this.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);

        // 数据库删除
        boolean result = this.removeById(spaceId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 从ES删除
        try {
            esSpaceDao.deleteById(spaceId);
        } catch (Exception e) {
            log.error("Delete picture from ES failed, spaceId: {}", spaceId, e);
            throw new RuntimeException("ES 删除失败", e); // 关键点
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSpaceByBatch(List<DeleteRequest> deleteRequests) {
        if (CollUtil.isEmpty(deleteRequests)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<Long> ids = deleteRequests
                .stream()
                .map(DeleteRequest::getId)
                .collect(Collectors.toList());
        boolean result = this.removeBatchByIds(ids);

        if (result) {
            // 批量删除ES数据
            try {
                esSpaceDao.deleteAllById(ids);
            } catch (Exception e) {
                log.error("ES 批量删除失败，ids: {}", ids, e);
                throw new RuntimeException("ES 删除失败", e); // 关键点
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSpace(Space space) {
        // 校验参数
        if (space == null || space.getId() == null || space.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查询旧空间，确保存在
        Long spaceId = space.getId();
        Space oldSpace = this.getById(spaceId);
        if (oldSpace == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }

        // 复制更新字段（防止覆盖空值）
        BeanUtils.copyProperties(space, oldSpace, "id", "createTime", "userId");

        // 自动填充数据
        this.fillSpaceBySpaceLevel(oldSpace);
        // 数据校验
        this.validSpace(oldSpace, false);
        oldSpace.setEditTime(new Date());

        // 更新数据库
        boolean result = this.updateById(oldSpace);
        // 更新ES
        if (result) {
            // 取db中最新的space 转换为 ES实体
            Space dbSpace = this.getById(spaceId);
            EsSpace esSpace = ConvertObjectUtils.toEsSpace(dbSpace);
            try {
                esSpaceDao.save(esSpace);
            } catch (Exception e) {
                log.error("ES 更新失败，spaceId: {}", spaceId, e);
                throw new RuntimeException("ES 更新失败", e); // 关键点
            }
        }

        return result;
    }

    @Override
    public SpaceVO getSpaceVO(Space space) {
        if (space == null) {
            return null;
        }

        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);

        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }

        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {

        List<Space> spaceList = spacePage.getRecords();

        long current = spacePage.getCurrent(); // 当前页号
        long pageSize = spacePage.getSize(); //每页大
        long total = spacePage.getTotal();

        // 创建一个相同大小的Page对象
        Page<SpaceVO> spaceVOPage = new Page<>(current, pageSize, total);
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }

        // 将List<Space> -> List<SpaceVO>
        List<SpaceVO> spaceVOList = spaceList
                .stream()
                .map(this::getSpaceVO)
                .collect(Collectors.toList());

        // SpaceVO列表 存入 Page<SpaceVO>
        spaceVOPage.setRecords(spaceVOList);

        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {

        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);

        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);

        return queryWrapper;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            long maxCount = spaceLevelEnum.getMaxCount();

            space.setMaxCount(maxCount);
            space.setMaxSize(maxSize);
        }

    }

    @Override
    public void checkPrivateSpaceAuth(User loginUser, Space space) {
        // 仅本人可以操作
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    @Override
    public Page<SpaceCreatedVO> getCreatedSpaceVOByPage(Page<Space> spacePage, Integer spaceLevel) {
        List<Space> spaceList = spacePage.getRecords();

        long current = spacePage.getCurrent(); // 当前页号
        long pageSize = spacePage.getSize(); //每页大
        long total = spacePage.getTotal();

        // 创建一个相同大小的Page对象
        Page<SpaceCreatedVO> spaceCreatedVOPage = new Page<>(current, pageSize, total);
        if (CollUtil.isEmpty(spaceList)) {
            return spaceCreatedVOPage;
        }

        // 将List<Space> -> List<spaceCreatedVOPage>
        List<SpaceCreatedVO> spaceCreatedVOList = spaceList
                .stream()
                .map(space -> {
                    SpaceCreatedVO spaceCreatedVO = new SpaceCreatedVO();
                    BeanUtils.copyProperties(space, spaceCreatedVO);

                    SpaceLevelEnum levelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
                    if (levelEnum != null) {
                        String spaceLevelName = levelEnum.getText();
                        spaceCreatedVO.setSpaceLevelName(spaceLevelName);
                    }
                    SpaceTypeEnum typeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
                    if (typeEnum != null) {
                        String spaceTypeName = typeEnum.getText();
                        spaceCreatedVO.setSpaceTypeName(spaceTypeName);
                    }

                    spaceCreatedVO.setCanExchange(Boolean.TRUE);
                    if (spaceLevel == space.getSpaceLevel()) {
                        spaceCreatedVO.setCanExchange(Boolean.FALSE);
                    }
                    return spaceCreatedVO;
                }).collect(Collectors.toList());

        // SpaceVO列表 存入 Page<SpaceVO>
        spaceCreatedVOPage.setRecords(spaceCreatedVOList);

        return spaceCreatedVOPage;
    }
}




