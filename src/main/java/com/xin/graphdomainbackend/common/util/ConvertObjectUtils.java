package com.xin.graphdomainbackend.common.util;

import cn.hutool.json.JSONUtil;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.model.EsPicture;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.model.EsSpace;
import com.xin.graphdomainbackend.infrastructure.elasticsearch.model.EsUser;
import com.xin.graphdomainbackend.picture.dao.entity.Picture;
import com.xin.graphdomainbackend.space.dao.entity.Space;
import com.xin.graphdomainbackend.user.dao.entity.User;

/**
 * 用于 ES 与 实体类之间的转换 工具类
 */
public class ConvertObjectUtils {

    public static EsUser toEsUser(User user) {
        if (user == null) {
            return null;
        }

        EsUser esUser = new EsUser();
        esUser.setId(user.getId());
        esUser.setUserAccount(user.getUserAccount());
        esUser.setUserName(user.getUserName());
        esUser.setUserProfile(user.getUserProfile());
        esUser.setCreateTime(user.getCreateTime());
        esUser.setIsDelete(user.getIsDelete());

        return esUser;
    }

    public static EsPicture toEsPicture(Picture picture) {
        if (picture == null) return null;

        EsPicture esPicture = new EsPicture();
        esPicture.setId(picture.getId());
        esPicture.setName(picture.getName());
        esPicture.setIntroduction(picture.getIntroduction());
        esPicture.setCategory(picture.getCategory());
        esPicture.setTags(JSONUtil.toList(picture.getTags(), String.class)); // 标签存储，转换格式
        esPicture.setReviewStatus(picture.getReviewStatus());
        esPicture.setSpaceId(picture.getSpaceId());
        esPicture.setCreateTime(picture.getCreateTime());
        esPicture.setIsDelete(picture.getIsDelete());

        return esPicture;
    }

    public static EsSpace toEsSpace(Space space) {
        if (space == null) return null;

        EsSpace esSpace = new EsSpace();
        esSpace.setId(space.getId());
        esSpace.setSpaceName(space.getSpaceName());
        esSpace.setSpaceType(space.getSpaceType());
        esSpace.setCreateTime(space.getCreateTime());
        esSpace.setIsDelete(space.getIsDelete());

        return esSpace;
    }
}

