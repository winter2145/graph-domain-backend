package com.xin.graphdomainbackend.manager.auth;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.manager.auth.model.SpaceUserPermissionConstant;
import com.xin.graphdomainbackend.model.entity.Picture;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.SpaceUser;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.enums.SpaceRoleEnum;
import com.xin.graphdomainbackend.model.enums.SpaceTypeEnum;
import com.xin.graphdomainbackend.service.PictureService;
import com.xin.graphdomainbackend.service.SpaceService;
import com.xin.graphdomainbackend.service.SpaceUserService;
import com.xin.graphdomainbackend.service.UserService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.xin.graphdomainbackend.constant.UserConstant.BAN_ROLE;
import static com.xin.graphdomainbackend.constant.UserConstant.USER_LOGIN_STATE;


/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 只处理团队空间权限类型
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }

        // 获取当前登录用户的 session
        SaSession session = StpKit.SPACE.getSessionByLoginId(loginId);
        if (session == null) {
            return new ArrayList<>();
        }

        // 从 session 中获取用户对象
        User loginUser = (User) session.get(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }

        // 检查用户是否被封禁
        if (BAN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "封禁用户禁止访问,请联系管理员");
        }

        Long loginUserId = loginUser.getId();
        List<String> adminPermissions = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());

        // 获取上下文信息
        SpaceUserAuthContext authContext = getAuthContextByRequest();

        // 如果上下文全部为空，表示查询公共图库，直接返回管理员权限
        if (isAllFieldsNull(authContext)) {
            return adminPermissions;
        }

        // 优先使用上下文中提供的空间成员对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }

        // 如果提供了 spaceUserId，则走团队空间的校验流程
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            // 校验目标空间成员是否存在
            SpaceUser targetUser = spaceUserService.getById(spaceUserId);
            if (targetUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到该团队空间");
            }

            // 校验当前登录用户是否在同一个空间中
            SpaceUser currentSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, targetUser.getSpaceId())
                    .eq(SpaceUser::getUserId, loginUserId)
                    .one();
            if (currentSpaceUser == null) {
                return new ArrayList<>();
            }

            return spaceUserAuthManager.getPermissionsByRole(currentSpaceUser.getSpaceRole());
        }

        // 如果没有 spaceUserId，尝试通过 spaceId 或 pictureId 获取 Space 对象并处理
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            // 尝试通过图片获取空间信息
            Long pictureId = authContext.getPictureId();
            // 图片 id 也没有，则默认通过权限校验
            if (pictureId == null) {
                return adminPermissions;
            }

            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();

            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            }

            spaceId = picture.getSpaceId();

            // 如果图片没有归属空间，公共图库，仅本人或管理员可操作
            if (spaceId == null) {
                if (picture.getUserId().equals(loginUserId) || userService.isAdmin(loginUser)) {
                    return adminPermissions;
                } else {
                    // 不是自己的图片，仅可查看
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }

        // 查询空间信息
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        }

        // 私有空间权限判断：本人或管理员可访问
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            if (space.getUserId().equals(loginUserId) || userService.isAdmin(loginUser)) {
                return adminPermissions;
            } else {
                return new ArrayList<>();
            }
        } else {
            // 团队空间权限判断：获取空间成员角色
            SpaceUser teamMember = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, loginUserId)
                    .one();

            if (teamMember == null) {
                return new ArrayList<>();
            }
            return spaceUserAuthManager.getPermissionsByRole(teamMember.getSpaceRole());
        }
    }

    /**
     * 判断对象的所有字段是否为空
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    /**
     * 从请求中回去上下文对象
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String contextType = request.getHeader(Header.CONTENT_TYPE.getValue());

        SpaceUserAuthContext authRequest;

        // 获取请求参数
        if (ContentType.JSON.getValue().equals(contextType)) { // 如果是post请求
            String body = ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else { // 如果是get请求
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }

        // 根据请求路径区分 id 字段的含义
        Long id = authRequest.getId();
        if (ObjectUtils.isNotEmpty(id)) {
            // 获取到请求路径的业务前缀，/api/picture/aaa?a=1
            String requestURI = request.getRequestURI();
            // 先替换掉上下文，剩下的就是前缀 picture/aaa?a=1
            String partURI = requestURI.replace(contextPath + "/", "");
            // 获取前缀的第一个斜杠前的字符串
            String moduleName = partURI.split("/", 2)[0];
            // parts = ["picture", "aaa/bbb?a=1"]
            // parts[0] = "picture"

            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                default:
            }
        }
        return authRequest;
    }
}
