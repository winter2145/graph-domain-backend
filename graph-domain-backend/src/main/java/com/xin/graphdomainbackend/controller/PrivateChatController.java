package com.xin.graphdomainbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xin.graphdomainbackend.common.BaseResponse;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import com.xin.graphdomainbackend.model.dto.privatechat.PrivateChatQueryRequest;
import com.xin.graphdomainbackend.model.entity.User;
import com.xin.graphdomainbackend.model.entity.websocket.PrivateChat;
import com.xin.graphdomainbackend.model.vo.message.PrivateChatVO;
import com.xin.graphdomainbackend.service.PrivateChatService;
import com.xin.graphdomainbackend.service.UserService;
import com.xin.graphdomainbackend.utils.ResultUtils;
import com.xin.graphdomainbackend.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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

}
