package com.xin.graphdomainbackend.utils;

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

    public static User toUser(EsUser esUser) {
        if (esUser == null) {
            return null;
        }
        User user = new User();
        user.setId(esUser.getId());
        user.setUserAccount(esUser.getUserAccount());
        user.setUserName(esUser.getUserName());
        user.setUserAvatar(esUser.getUserAvatar());
        user.setUserProfile(esUser.getUserProfile());
        user.setUserRole(esUser.getUserRole());
        user.setCreateTime(esUser.getCreateTime());
        user.setUpdateTime(esUser.getUpdateTime());
        user.setIsDelete(esUser.getIsDelete());

        return user;
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
        esPicture.setTags(picture.getTags());
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

    public static Picture toPicture(EsPicture esPicture) {
        if (esPicture == null) {
            return null;
        }

        Picture picture = new Picture();
        picture.setId(esPicture.getId());
        picture.setName(esPicture.getName());
        picture.setUserId(picture.getUserId());
        picture.setUrl(esPicture.getUrl());
        picture.setThumbnailUrl(esPicture.getThumbnailUrl());
        picture.setWebpUrl(esPicture.getWebpUrl());
        picture.setIntroduction(esPicture.getIntroduction());
        picture.setCategory(esPicture.getCategory());
        picture.setTags(esPicture.getTags());
        picture.setPicSize(esPicture.getPicSize());
        picture.setPicHeight(esPicture.getPicHeight());
        picture.setPicScale(esPicture.getPicScale());
        picture.setPicColor(esPicture.getPicColor());
        picture.setCommentCount(esPicture.getCommentCount());
        picture.setLikeCount(esPicture.getLikeCount());
        picture.setShareCount(esPicture.getShareCount());
        picture.setSpaceId(esPicture.getSpaceId());
        picture.setReviewStatus(esPicture.getReviewStatus());
        picture.setReviewMessage(esPicture.getReviewMessage());
        picture.setReviewerId(esPicture.getReviewerId());
        picture.setReviewTime(esPicture.getReviewTime());
        picture.setCreateTime(esPicture.getCreateTime());
        picture.setEditTime(esPicture.getEditTime());
        picture.setUpdateTime(esPicture.getUpdateTime());
        picture.setIsDelete(esPicture.getIsDelete());

        return picture;
    }

    public static Space toSpace(EsSpace esSpace) {
        if (esSpace == null) {
            return null;
        }

        Space space = new Space();
        space.setId(esSpace.getId());
        space.setSpaceName(esSpace.getSpaceName());
        space.setSpaceLevel(esSpace.getSpaceLevel());
        space.setSpaceType(esSpace.getSpaceType());
        space.setUserId(esSpace.getUserId());
        space.setEditTime(esSpace.getEditTime());
        space.setCreateTime(esSpace.getCreateTime());
        space.setUpdateTime(esSpace.getUpdateTime());
        space.setIsDelete(esSpace.getIsDelete());

        return space;
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
