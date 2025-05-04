package com.xin.graphdomainbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xin.graphdomainbackend.model.dto.picture.PictureEditRequest;
import com.xin.graphdomainbackend.model.dto.picture.PictureQueryRequest;
import com.xin.graphdomainbackend.model.dto.picture.PictureUploadRequest;
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

    /**
     * 上传图片
     *
     * @param multipartFile         上传的图片文件
     * @param pictureUploadRequest  图片上传请求参数
     * @param loginUser             当前登录用户
     * @return                      图片视图对象
     */
    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 构建图片查询条件
     *
     * @param pictureQueryRequest   图片查询请求参数
     * @return                      查询条件包装器
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

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
     * 更新图片信息
     *
     * @param picture   图片实体
     * @return          是否更新成功
     */
    boolean updatePicture(Picture picture);

    void deletePicture(Long id, User loginUser);

    Page<Picture> getPictureByPage(PictureQueryRequest pictureQueryRequest);

    boolean editPicture(PictureEditRequest pictureEditRequest, HttpServletRequest request);
}
