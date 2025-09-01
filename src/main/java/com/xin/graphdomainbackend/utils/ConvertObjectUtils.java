package com.xin.graphdomainbackend.utils;

import cn.hutool.json.JSONUtil;
import com.xin.graphdomainbackend.model.entity.Picture;
import com.xin.graphdomainbackend.model.entity.Space;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.es.EsPicture;
import com.xin.graphdomainbackend.model.entity.es.EsSpace;
import com.xin.graphdomainbackend.model.entity.es.EsUser;

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
        esUser.setUserAvatar(user.getUserAvatar());
        esUser.setUserProfile(user.getUserProfile());
        esUser.setUserRole(user.getUserRole());
        esUser.setCreateTime(user.getCreateTime());
        esUser.setUpdateTime(user.getUpdateTime());
        esUser.setIsDelete(user.getIsDelete());

        return esUser;
    }

    public static EsPicture toEsPicture(Picture picture) {
        if (picture == null) return null;

        EsPicture esPicture = new EsPicture();
        esPicture.setId(picture.getId());
        esPicture.setUrl(picture.getUrl());
        esPicture.setThumbnailUrl(picture.getThumbnailUrl());
        esPicture.setWebpUrl(picture.getWebpUrl());
        esPicture.setName(picture.getName());
        esPicture.setIntroduction(picture.getIntroduction());
        esPicture.setCategory(picture.getCategory());
        esPicture.setTags(JSONUtil.toList(picture.getTags(), String.class)); // 标签存储，转换格式
        esPicture.setPicSize(picture.getPicSize());
        esPicture.setPicWidth(picture.getPicWidth());
        esPicture.setPicHeight(picture.getPicHeight());
        esPicture.setPicScale(picture.getPicScale());
        esPicture.setPicFormat(picture.getPicFormat());
        esPicture.setPicColor(picture.getPicColor());
        esPicture.setUserId(picture.getUserId());
        esPicture.setCommentCount(picture.getCommentCount());
        esPicture.setLikeCount(picture.getLikeCount());
        esPicture.setShareCount(picture.getShareCount());
        esPicture.setReviewStatus(picture.getReviewStatus());
        esPicture.setReviewMessage(picture.getReviewMessage());
        esPicture.setReviewerId(picture.getReviewerId());
        esPicture.setSpaceId(picture.getSpaceId());
        esPicture.setReviewTime(picture.getReviewTime());
        esPicture.setCreateTime(picture.getCreateTime());
        esPicture.setEditTime(picture.getEditTime());
        esPicture.setUpdateTime(picture.getUpdateTime());
        esPicture.setIsDelete(picture.getIsDelete());

        return esPicture;
    }

    public static EsSpace toEsSpace(Space space) {
        if (space == null) return null;

        EsSpace esSpace = new EsSpace();
        esSpace.setId(space.getId());
        esSpace.setSpaceName(space.getSpaceName());
        esSpace.setSpaceType(space.getSpaceType());
        esSpace.setSpaceLevel(space.getSpaceLevel());
        esSpace.setUserId(space.getUserId());
        esSpace.setCreateTime(space.getCreateTime());
        esSpace.setEditTime(space.getEditTime());
        esSpace.setUpdateTime(space.getUpdateTime());
        esSpace.setIsDelete(space.getIsDelete());

        return esSpace;
    }
}
