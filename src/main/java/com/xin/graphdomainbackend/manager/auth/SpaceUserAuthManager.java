package com.xin.graphdomainbackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.manager.auth.model.SpaceUserAuthConfig;
import com.xin.graphdomainbackend.manager.auth.model.SpaceUserRole;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.SpaceUser;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.enums.SpaceRoleEnum;
import com.xin.graphdomainbackend.model.enums.SpaceTypeEnum;
import com.xin.graphdomainbackend.service.SpaceUserService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 空间成员权限管理
 */
@Component
public class SpaceUserAuthManager {

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;


    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) {
            return Collections.emptyList();
        }
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> r.getKey().equals(spaceUserRole))
                .findFirst()
                .orElse(null);

        if (role == null) {
            return Collections.emptyList();
        }
        return role.getPermissions();
    }

    /**
     * 获取权限列表
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR);

        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 阅览权限
        List<String> VIEWER_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.VIEWER.getValue());

        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            return VIEWER_PERMISSIONS;
        }

        // 获取空间类型
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }

        // 根据空间类型，获取相应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间, 仅空间本人可以操作
                if (space.getUserId().equals(loginUser.getId())) {
                    return ADMIN_PERMISSIONS;
                }
                return new ArrayList<>();

            case TEAM:
                // 团队空间, 需要查询SpaceUser 并获取相应的权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                }
                return getPermissionsByRole(spaceUser.getSpaceRole());
        }

        return new ArrayList<>();
    }

}
