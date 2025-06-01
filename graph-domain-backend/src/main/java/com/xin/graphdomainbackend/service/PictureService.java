package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.dto.picture.*;
import com.xin.graphdomainbackend.model.entity.Picture;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.vo.PictureVO;
import com.xin.graphdomainbackend.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Administrator
* @description 针对表【picture】的数据库操作Service
* @createDate 2025-04-30 19:13:10
*/
public interface PictureService extends IService<Picture> {

    /**
     * 校验图片
     *
     * @param picture         图片实体
     */
    void validPicture(Picture picture);

    /*
        */
/**
     * 上传图片
     *
     * @param multipartFile         上传的图片文件
     * @param pictureUploadRequest  图片上传请求参数
     * @param loginUser             当前登录用户
     * @return                      图片视图对象
     */    /*

    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);
    */

    /**
     * 上传图片
     * @param inputSource           文件输入源(图片或url)
     * @param uploadRequest         图片上传请求参数
     * @param loginUser             图片视图对象
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest uploadRequest, User loginUser);

    /**
     * 构建图片查询条件
     *
     * @param pictureQueryRequest   图片查询请求参数
     * @return                      查询条件包装器
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 更新图片信息
     *
     * @param picture   图片实体
     * @param loginUser             当前登录用户
     * @return          是否更新成功
     */
    boolean updatePicture(Picture picture, User loginUser);

    /**
     * 根据图片 ID 删除指定图片（仅限管理员或本人）。
     *
     * @param id 图片主键 ID
     * @param loginUser 当前登录用户
     */
    boolean deletePicture(Long id, User loginUser);

    /**
     * 清理图片文件
     *
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 获取图片视图对象
     *
     * @param picture   图片实体
     * @return          图片视图对象
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 获取图片视图对象列表
     *
     * @param pictureList   图片实体列表
     * @return              图片视图对象列表
     */
    List<PictureVO> getPictureVOList(List<Picture> pictureList);

    /**
     * 获取图片视图对象列表 (分页)
     *
     * @param pictureQueryRequest   图片查询请求
     * @return              图片视图对象列表分页版
     */
    Page<PictureVO> getPictureVOByPage(PictureQueryRequest pictureQueryRequest);

    /**
     * 分页获取图片列表，根据查询条件筛选。
     *
     * @param pictureQueryRequest 图片查询请求参数
     * @return 分页结果，包含图片列表
     */
    Page<Picture> getPictureByPage(PictureQueryRequest pictureQueryRequest);

    /**
     * 编辑图片信息（如标题、描述等）。
     *
     * @param pictureEditRequest 图片编辑请求参数
     * @param request HTTP 请求对象，用于鉴权或获取用户信息
     * @return 是否编辑成功
     */
    boolean editPicture(PictureEditRequest pictureEditRequest, HttpServletRequest request);

    /**
     * 图片审核
     * @param pictureReviewRequest 图片审核请求参数
     * @param loginUser 当前登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param uploadByBatchRequest  批量导入图片请求参数
     * @param loginUser 当前登录用户
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest uploadByBatchRequest, User loginUser);

    /**
     * 分页获取图片列表，根据查询条件筛选。(带缓存)
     *
     * @param pictureQueryRequest 图片查询请求参数
     * @param request     Http请求
     * @return 分页结果，包含图片列表
     */
    Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 校验空间图片的权限
     *
     * @param loginUser 登录用户
     * @param picture 图片对象
     */
    void checkPictureAuth(User loginUser, Picture picture);

}
