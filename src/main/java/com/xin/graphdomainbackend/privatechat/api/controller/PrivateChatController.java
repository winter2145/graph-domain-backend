package com.xin.graphdomainbackend.privatechat.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.common.aop.annotation.LoginCheck;
import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.common.util.ResultUtils;
import com.xin.graphdomainbackend.common.util.ThrowUtils;
import com.xin.graphdomainbackend.privatechat.api.dto.request.PrivateChatQueryRequest;
import com.xin.graphdomainbackend.privatechat.api.dto.vo.PrivateChatVO;
import com.xin.graphdomainbackend.privatechat.service.PrivateChatService;
import com.xin.graphdomainbackend.user.dao.entity.User;
import com.xin.graphdomainbackend.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/private_chat")
@Slf4j
public class PrivateChatController {

    @Resource
    private PrivateChatService privateChatService;

    @Resource
    private UserService userService;

    /**
     * 创建或更新私聊
     */
    @PostMapping("/create_update")
    @LoginCheck
    public BaseResponse<PrivateChatVO> createOrUpdatePrivateChat(@RequestParam Long targetUserId,
                                                                 @RequestParam(required = false) String lastMessage,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(targetUserId == null || targetUserId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);

        // 不能和自己私聊
        ThrowUtils.throwIf(targetUserId.equals(loginUser.getId()), ErrorCode.PARAMS_ERROR, "不能和自己私聊");

        // 检查目标用户是否存在
        User targetUser = userService.getById(targetUserId);
        ThrowUtils.throwIf(targetUser == null, ErrorCode.NOT_FOUND_ERROR, "目标用户不存在");

        PrivateChatVO privateChatVO = privateChatService.sendPrivateMessage(loginUser.getId(), targetUserId, lastMessage);

        return ResultUtils.success(privateChatVO);

    }

    /**
     * 分页获取私聊列表
     */
    @PostMapping("/list/page")
    @LoginCheck
    public BaseResponse<Page<PrivateChatVO>> listPrivateChatByPage(@RequestBody PrivateChatQueryRequest privateChatQueryRequest,
                                                                 HttpServletRequest request) {
        if (privateChatQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long current = privateChatQueryRequest.getCurrent();
        long size = privateChatQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<PrivateChatVO> privateChatByPage = privateChatService.getPrivateChatByPage(privateChatQueryRequest, loginUser);

        return ResultUtils.success(privateChatByPage);
    }

    /**
     * 清空未读消息
     */
    @PostMapping("/clear_unread/{targetUserId}/{isSender}")
    @LoginCheck
    public BaseResponse<Boolean> clearUnreadCount(@PathVariable Long targetUserId,boolean isSender,
                                                  HttpServletRequest request) {
        ThrowUtils.throwIf(targetUserId == null || targetUserId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 清除当前用户的未读消息数
        privateChatService.clearUnreadCount(loginUser.getId(), targetUserId, isSender);
        return ResultUtils.success(true);
    }

    /**
     * 删除私聊
     */
    @PostMapping("/delete/{privateChatId}")
    @LoginCheck
    public BaseResponse<Boolean> deletePrivateChat(@PathVariable Long privateChatId,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(privateChatId == null || privateChatId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = privateChatService.deletePrivateChat(privateChatId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 修改私聊名称
     */
    @PostMapping("/update_name/{privateChatId}")
    @LoginCheck
    public BaseResponse<Boolean> updateChatName(@PathVariable Long privateChatId,
                                                @RequestParam String chatName,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(privateChatId == null || privateChatId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(chatName.length() > 50, ErrorCode.PARAMS_ERROR, "聊天名称过长");

        User loginUser = userService.getLoginUser(request);
        privateChatService.updateChatName(privateChatId, chatName, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新聊天类型（好友/私信）
     */
    @PostMapping("/update_type/{targetUserId}")
    @LoginCheck
    public BaseResponse<Boolean> updateChatType(@PathVariable Long targetUserId,
                                                @RequestParam Boolean isFriend,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(targetUserId == null || targetUserId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        privateChatService.updateChatType(loginUser.getId(), targetUserId, isFriend);
        return ResultUtils.success(true);
    }

}
