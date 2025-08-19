package com.xin.graphdomainbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.graphdomainbackend.constant.TargetTypeConstant;
import com.xin.graphdomainbackend.constant.UserConstant;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.mapper.CommentsMapper;
import com.xin.graphdomainbackend.mapper.PictureMapper;
import com.xin.graphdomainbackend.model.dto.comments.CommentsAddRequest;
import com.xin.graphdomainbackend.model.dto.comments.CommentsDeleteRequest;
import com.xin.graphdomainbackend.model.dto.comments.CommentsLikeRequest;
import com.xin.graphdomainbackend.model.dto.comments.CommentsQueryRequest;
import com.xin.graphdomainbackend.model.entity.Comments;
import com.xin.graphdomainbackend.model.entity.LikeRecord;
import com.xin.graphdomainbackend.model.entity.Picture;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.enums.MessageSourceEnum;
import com.xin.graphdomainbackend.model.vo.PictureVO;
import com.xin.graphdomainbackend.model.vo.comment.CommentUserVO;
import com.xin.graphdomainbackend.model.vo.comment.CommentsVO;
import com.xin.graphdomainbackend.service.CommentsService;
import com.xin.graphdomainbackend.service.LikeRecordService;
import com.xin.graphdomainbackend.service.PictureService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【comments】的数据库操作Service实现
* @createDate 2025-07-26 14:10:43
*/
@Service
@Slf4j
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comments>
    implements CommentsService {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private CommentsMapper commentsMapper;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    @Lazy
    private LikeRecordService likeRecordService;

    @Override
    public Boolean addComment(CommentsAddRequest commentsAddRequest, HttpServletRequest request) {
       User user = userService.getLoginUser(request);
       ThrowUtils.throwIf(user == null || commentsAddRequest == null, ErrorCode.PARAMS_ERROR);


       // 如果不是是图片下的评论
       if (commentsAddRequest.getTargetType() != 1) {
           throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的评论类型");
       }

        // 获取图片信息
        Picture picture = pictureService.getById(commentsAddRequest.getTargetId());
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        // 默认是给图片评论
        Long targetUserId = picture.getUserId();

        // 如果是评论下的回复
        Long parentCommentId = commentsAddRequest.getParentCommentId();
        Comments comment = this.lambdaQuery().select(parentCommentId!=null && parentCommentId > 0, Comments::getUserId)
                .eq(Comments::getCommentId, parentCommentId)
                .one();
        if (comment != null) {
            targetUserId = comment.getUserId();
        }

        Comments comments = new Comments();
        BeanUtil.copyProperties(commentsAddRequest, comments);
        comments.setUserId(user.getId());
        comments.setTargetUserId(targetUserId); // 设置操作目标的用户信息
        comments.setIsRead(0);
        comments.setLikeCount(0L);  // 设置初始点赞数
        comments.setDislikeCount(0L);  // 设置初始点踩数
        comments.setIsDelete(0);  // 设置未删除状态

        // 保存至数据库
        boolean result = this.save(comments);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "评论保存失败");
        }

        // 更新评论数
        operationCommentCount(commentsAddRequest.getTargetId(), 1, 1);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteComment(CommentsDeleteRequest commentsDeleteRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(commentsDeleteRequest == null, ErrorCode.PARAMS_ERROR);

        User user = userService.getLoginUser(request);

        // 获取评论信息
        Comments comment = this.getById(commentsDeleteRequest.getCommentId());
        if (comment == null || comment.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "评论不存在");
        }

        // 校验权限（只能删除自己的评论）
        if (!user.getId().equals(comment.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 递归获取所有子评论ID
        List<Long> deletedCommentList = findAllNestedCommentIds(comment.getCommentId());
        deletedCommentList.add(commentsDeleteRequest.getCommentId());// （包含自身）

        // 执行批量逻辑删除
        int affectedRows = commentsMapper.batchSoftDelete(deletedCommentList);

        // 更新评论数
        operationCommentCount(comment.getTargetId(), affectedRows, 0);

        return true;
    }

    @Override
    public Page<CommentsVO> queryComment(CommentsQueryRequest commentsQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(commentsQueryRequest == null || commentsQueryRequest.getTargetId() == null,
                ErrorCode.PARAMS_ERROR);

        // 1.创建分页对象
        long current = commentsQueryRequest.getCurrent();
        long size = commentsQueryRequest.getPageSize();
        Page<Comments> page = new Page<>(current, size);

        Long targetId = commentsQueryRequest.getTargetId();
        Integer targetType = commentsQueryRequest.getTargetType() != null
                ? commentsQueryRequest.getTargetType() : 1;

        // 2.构建查询顶级评论的 对象（parentCommentId = 0）
        LambdaQueryWrapper<Comments> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(Comments::getTargetId, targetId)
                .eq(Comments::getTargetType, targetType) // 默认查询图片评论
                .eq(Comments::getParentCommentId, 0) // 查询顶级评论
                .orderByDesc(Comments::getCreateTime);

        // 得到顶级评论列表
        Page<Comments> commentsPage = page(page, lambdaQueryWrapper);
        List<Comments> topCommentsList = commentsPage.getRecords();

        if (topCommentsList.isEmpty()) {
            return new Page<>(current, size, 0);
        }

        // 所有用户 ID 收集器（顶级 + 子评论）
        Set<Long> allUserIds = new HashSet<>();
        // 所有评论 ID 收集器（顶级 + 子评论）
        Set<Long> allCommentIds = new HashSet<>();

        // 3.构建顶级评论 VO，并填充子评论树
        List<CommentsVO> commentsVOList = topCommentsList.stream().map(comment -> {
            CommentsVO vo = CommentsVO.objToVo(comment);

            // 获取顶级评论的 用户id与评论id ，用于后续填充顶级评论的 用户id 和 评论id
            allUserIds.add(vo.getUserId());
            allCommentIds.add(vo.getCommentId());

            // 获取并构建子评论树
            List<CommentsVO> childCommentTree = getAllChildAsTreeWithIds(comment.getCommentId(), allUserIds, allCommentIds);
            vo.setChildren(childCommentTree);

            return vo;
        }).collect(Collectors.toList());

        // 4.批量查询用户信息
        List<User> users = userService.listByIds(allUserIds);
        Map<Long, CommentUserVO> userVOMap = users.stream()
                .map(user -> {
                    CommentUserVO vo = new CommentUserVO();
                    BeanUtils.copyProperties(user, vo);
                    return vo;
                })
                .collect(Collectors.toMap(CommentUserVO::getId, vo -> vo));

        // 5.填充用户信息
        commentsVOList.forEach(vo -> attachUser(vo, userVOMap));

        // 6.批量查询当前用户对所有评论的点赞状态
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) attribute;
        if (currentUser != null) { // 只有登录的用户才能查看点赞状态
            User loginUser = userService.getLoginUser(request);
            if (loginUser != null && !allCommentIds.isEmpty()) {
                List<LikeRecord> commentLikeRecords = likeRecordService.getLikeRecordsByTargetIds(allCommentIds, TargetTypeConstant.COMMENT);

                // 提取出当前用户 点赞记录的评论id
                Map<Long, Integer> likeStatusMap = commentLikeRecords.stream()
                        .filter(likeRecord -> likeRecord.getUserId().equals(loginUser.getId()))
                        .collect(Collectors.toMap(LikeRecord::getTargetId, LikeRecord::getLikeStatus));

                // 设置所有评论的点赞状态（包括子评论）
                setLikeStatusForAllComments(commentsVOList, likeStatusMap);
            }
        }


        // 构造分页结果
        Page<CommentsVO> resultPage = new Page<>(current, size, commentsPage.getTotal());
        resultPage.setRecords(commentsVOList);
        return resultPage;
    }

    /**
     * 获取子评论树，并收集用户ID和评论ID
     */
    private List<CommentsVO> getAllChildAsTreeWithIds(Long parentId, Set<Long> allUserIds, Set<Long> allCommentIds) {
        // 1. 一次性查询所有子评论（含完整数据）
        List<Comments> allChildCommentList = findAllNestedComments(parentId);

        // 2. 收集用户ID和评论ID（用于后续批量查询用户信息和点赞状态）
        allChildCommentList.forEach(c -> {
            allUserIds.add(c.getUserId());
            allCommentIds.add(c.getCommentId());
        });

        // 3. 按 parentId 分组
        Map<Long, List<CommentsVO>> parentMap = new HashMap<>();
        for (Comments c : allChildCommentList) {
            CommentsVO vo = new CommentsVO();
            BeanUtils.copyProperties(c, vo);
            parentMap.computeIfAbsent(c.getParentCommentId(), k -> new ArrayList<>()).add(vo);
        }

        // 4. 递归构建树形结构
        return buildCommentTree(parentId, parentMap);
    }

    /**
     * 构建评论树结构
     */
    private List<CommentsVO> buildCommentTree(Long parentId, Map<Long, List<CommentsVO>> parentMap) {
        List<CommentsVO> childrenCommentsVO = parentMap.getOrDefault(parentId, new ArrayList<>());
        for (CommentsVO child : childrenCommentsVO) {
            child.setChildren(buildCommentTree(child.getCommentId(), parentMap));
        }
        return childrenCommentsVO;
    }

    /**
     * BFS（广度优先遍历）设置所有评论的点赞状态
     */
    private void setLikeStatusForAllComments(List<CommentsVO> commentsVOList, Map<Long, Integer> likeStatusMap) {
        // 初始化队列
        Queue<CommentsVO> queue = new LinkedList<>(commentsVOList);
        while (!queue.isEmpty()) {
            // 1. 取出当前节点
            CommentsVO current = queue.poll();

            // 2. 设置点赞状态（若无记录则默认为0）
            Integer likeStatus = likeStatusMap.getOrDefault(current.getCommentId(), 0);
            current.setLikeStatus(likeStatus);

            // 3. 将子评论加入队列（确保非空判断）
            if (current.getChildren() != null) {
                queue.addAll(current.getChildren());
            }
        }
    }

    @Override
    public Page<CommentsVO> getCommentedHistory(CommentsQueryRequest commentsQueryRequest, String source, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(commentsQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();

        // 创建分页对象
        long current = commentsQueryRequest.getCurrent();
        long size = commentsQueryRequest.getPageSize();
        Page<Comments> page = new Page<>(current, size);

        Integer targetType = commentsQueryRequest.getTargetType() != null
                ? commentsQueryRequest.getTargetType() : 1;

        // 构建查询语句
        LambdaQueryWrapper<Comments> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (source.equals(MessageSourceEnum.FROM_ME.getValue())) {
            lambdaQueryWrapper.eq(Comments::getUserId, userId)
                    .eq(Comments::getTargetType, targetType)
                    .orderByDesc(Comments::getCreateTime);
        } else if (source.equals(MessageSourceEnum.TO_ME.getValue())) {
            lambdaQueryWrapper.eq(Comments::getTargetUserId, userId)
                    .ne(Comments::getUserId, userId)// 排除自己
                    .orderByDesc(Comments::getCreateTime);
        }

        Page<Comments> commentsPage = page(page, lambdaQueryWrapper);
        List<Comments> records = commentsPage.getRecords();

        // 所有用户 ID 收集器（顶级 + 子评论）
        Set<Long> allUserIds = new HashSet<>();

        // 所有图片 ID 收集器
        Set<Long> allPictureIds = new HashSet<>();

        // 转换成VO类comment
        List<CommentsVO> commentsVOList = records.stream().map(comment -> {
            CommentsVO vo = new CommentsVO();
            BeanUtils.copyProperties(comment, vo);

            allUserIds.add(vo.getUserId());
            allPictureIds.add(vo.getTargetId());
            return vo;
        }).collect(Collectors.toList());

        // 批量查询用户信息
        List<User> users = userService.listByIds(allUserIds);
        Map<Long, CommentUserVO> userVOMap = users.stream().map(user -> {
            CommentUserVO vo = new CommentUserVO();
            BeanUtils.copyProperties(user, vo);
            return vo;
        }).collect(Collectors.toMap(CommentUserVO::getId, vo -> vo));

        // 填充用户信息
        commentsVOList.forEach(vo -> attachUser(vo, userVOMap));

        // 批量查询图片信息
        List<Picture> pictures = pictureService.listByIds(allPictureIds);

        Map<Long, PictureVO> pictureVOMap = pictures.stream()
                .map(PictureVO::objToVo)
                .collect(
                        Collectors.toMap(PictureVO::getId, vo -> vo)
                );

        // 填充图片信息
        commentsVOList.forEach(vo -> attachPicture(vo, pictureVOMap));

        // 构造分页结果
        Page<CommentsVO> resultPage = new PageDTO<>(current, size, commentsPage.getTotal());
        resultPage.setRecords(commentsVOList);
        return resultPage;
    }

    @Override
    public long getUnreadCommentsCount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        return this.count(new LambdaQueryWrapper<Comments>()
                .eq(Comments::getTargetUserId, userId)
                .eq(Comments::getIsRead, 0)
                .ne(Comments::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllUnreadComments(Long userId) {
         this.lambdaUpdate().set(Comments::getIsRead, 1)
                .eq(Comments::getTargetUserId, userId)
                .eq(Comments::getIsRead, 0)
                .update();
    }

    @Override
    public List<CommentsVO> getAndClearUnreadComments(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long loginUserId = loginUser.getId();

        // 获取未读评论
        List<Comments> unreadComments = this.lambdaQuery().eq(Comments::getTargetUserId, loginUserId)
                .eq(Comments::getIsRead, 0)
                .ne(Comments::getUserId, loginUserId)
                .orderByDesc(Comments::getCreateTime)
                .list();
        if (CollUtil.isEmpty(unreadComments)) {
            return new ArrayList<>();
        }

        // 获取未读评论id列表
        List<Long> unreadCommentsIds = unreadComments.stream()
                .map(Comments::getCommentId)
                .collect(Collectors.toList());

        // 批量更新为已读
        this.lambdaUpdate()
                .set(Comments::getIsRead, 1)
                .in(Comments::getCommentId,unreadCommentsIds)
                .update();

        // 所有用户 ID 收集器（顶级 + 子评论）
        Set<Long> allUserIds = new HashSet<>();
        // 所有图片 ID 收集器
        Set<Long> allPictureIds = new HashSet<>();

        // 构建返回数据
        List<CommentsVO> unreadCommentsVOList = unreadComments.stream()
                .map(comment -> {
            CommentsVO vo = new CommentsVO();
            BeanUtils.copyProperties(comment, vo);

            allUserIds.add(vo.getUserId());
            allPictureIds.add(vo.getTargetId());
            return vo;
        }).collect(Collectors.toList());

        // 批量查询用户信息
        List<User> users = userService.listByIds(allUserIds);
        Map<Long, CommentUserVO> userVOMap = users.stream().map(user -> {
            CommentUserVO vo = new CommentUserVO();
            BeanUtils.copyProperties(user, vo);
            return vo;
        }).collect(Collectors.toMap(CommentUserVO::getId, vo -> vo));

        // 填充用户信息
        unreadCommentsVOList.forEach(vo -> attachUser(vo, userVOMap));

        // 批量查询图片信息
        List<Picture> pictures = pictureService.listByIds(allPictureIds);
        Map<Long, PictureVO> pictureVOMap = pictures.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toMap(PictureVO::getId, vo -> vo));

        // 填充图片信息
        unreadCommentsVOList.forEach(vo -> attachPicture(vo, pictureVOMap));

        return unreadCommentsVOList;
    }

    @Override
    public Boolean likeComment(CommentsLikeRequest commentsLikeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(commentsLikeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);

        // 查找要更新的评论
        LambdaQueryWrapper<Comments> commentsQueryWrapper = new LambdaQueryWrapper<>();
        commentsQueryWrapper.eq(Comments::getCommentId, commentsLikeRequest.getCommentId());
        Comments comments = getOne(commentsQueryWrapper);
        ThrowUtils.throwIf(comments == null, ErrorCode.NOT_FOUND_ERROR);

        Integer targetType = TargetTypeConstant.COMMENT;
        Boolean updateResult = false;

        // 计算新的点赞状态
        Integer newLikeStatus = calculateNewLikeStatus(commentsLikeRequest);

        // 处理点赞/点踩操作
        updateResult = likeRecordService.dealLikeOrDislike(
                loginUser.getId(), comments.getCommentId(), targetType, newLikeStatus, comments.getUserId()
        );

        if (updateResult) {
            // 更新评论表点赞、踩的数量
            updateCommentCounts(commentsLikeRequest,  comments.getCommentId());
        }

        return true;
    }

    // 计算新的点赞状态
    private Integer calculateNewLikeStatus(CommentsLikeRequest request) {
        // 根据前端传递的 likeCount 和 dislikeCount 计算新状态
        if (request.getLikeCount() != null && request.getLikeCount() > 0) {
            return 1; // 点赞
        } else if (request.getDislikeCount() != null && request.getDislikeCount() > 0) {
            return 2; // 点踩
        } else {
            return 0; // 取消操作
        }
    }

    // 更新评论表点赞、踩的数量
    private void updateCommentCounts(CommentsLikeRequest request, Long commentId) {
        if (request.getLikeCount() != null && request.getLikeCount() != 0) {
            this.lambdaUpdate()
                    .setSql("likeCount = likeCount + " + request.getLikeCount())
                    .eq(Comments::getCommentId, commentId)
                    .update();
        }

        if (request.getDislikeCount() != null && request.getDislikeCount() != 0) {
            this.lambdaUpdate()
                    .setSql("dislikeCount = dislikeCount + " + request.getDislikeCount())
                    .eq(Comments::getCommentId, commentId)
                    .update();
        }
    }

    /**
     * 批量查找子评论id
     * @param parentId 父评论id
     */
    private List<Long> findAllNestedCommentIds(Long parentId) {
        return commentsMapper.selectAllChildCommentIds(parentId);
    }

    /**
     * 批量查找子评论
     * @param parentId 父评论id
     */
    private List<Comments> findAllNestedComments(Long parentId){
        return commentsMapper.selectAllChildCommentsWithDetails(parentId);
    }

    // 绑定用户信息
    private void attachUser(CommentsVO vo, Map<Long, CommentUserVO> userVOMap) {
        if (vo == null) return;
        CommentUserVO userVO = userVOMap.get(vo.getUserId());
        if (userVO != null) {
            vo.setCommentUser(userVO);
        }
        if (vo.getChildren() != null) {
            vo.getChildren().forEach(child -> attachUser(child, userVOMap));
        }
    }

    // 绑定图片信息
    private void attachPicture(CommentsVO vo, Map<Long, PictureVO> pictureVOMap) {
        if (vo == null) return;
        PictureVO pictureVO = pictureVOMap.get(vo.getTargetId());
        if (pictureVO != null) {
            vo.setPicture(pictureVO);
        }
    }

    /**
     *  更新图片下的 评论数量
     * @param pictureId 图片id
     * @param count 评论数量
     * @param type 1 - 增加，0 - 减少
     */
    public void operationCommentCount(Long pictureId, int count, int type) {
        if (count <= 0) return;

        int updated = 0;
        if (type == 1) {
            updated = pictureMapper.update(null,
                    Wrappers.<Picture>lambdaUpdate()
                            .setSql("commentCount = commentCount + " + count)
                            .eq(Picture::getId, pictureId)
                            .le(Picture::getCommentCount, Long.MAX_VALUE - count) // 防止溢出
            );
        } else if (type == 0){
            updated = pictureMapper.update(null,
                    Wrappers.<Picture>lambdaUpdate()
                            .setSql("commentCount = commentCount - " + count)
                            .eq(Picture::getId, pictureId)
                            .ge(Picture::getCommentCount, count) // 防止负数
            );
        }

        if (updated == 0) {
            log.warn("图片评论数更新失败，pictureId: {}", pictureId);
        }
    }

}




