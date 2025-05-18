package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
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
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.es.EsSpace;
import com.xin.graphdomainbackend.model.enums.SpaceLevelEnum;
import com.xin.graphdomainbackend.model.enums.SpaceTypeEnum;
import com.xin.graphdomainbackend.model.vo.SpaceVO;
import com.xin.graphdomainbackend.model.vo.UserVO;
import com.xin.graphdomainbackend.service.SpaceService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
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

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        return 0;
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
        this.fillSpaceBySpaceLevel(space);
        // 数据校验
        this.validSpace(space, false);
        oldSpace.setEditTime(new Date());

        // 更新数据库
        boolean result = this.updateById(oldSpace);
        // 更新ES
        if (result) {
            // 转换为ES实体
            EsSpace esSpace = new EsSpace();
            BeanUtils.copyProperties(oldSpace, esSpace);
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
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "id", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "id", spaceType);

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
}




