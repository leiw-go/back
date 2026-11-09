/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.crm.common.CurrentUserResolver;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageSendRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.MessageService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collections;
import java.util.Map;

/**
 * CRM 站内信 HTTP 接口.
 *
 * @since 2026-11-08
 */
@RestController
@RequestMapping("/api/crm/messages")
@Tag(name = "CRM Message", description = "CRM in-site message APIs")
public class CrmMessageController {

    private final MessageService messageService;
    private final CurrentUserResolver currentUserResolver;

    /**
     * 使用指定依赖构造 CrmMessageController.
     *
     * @param messageService      站内信服务
     * @param currentUserResolver 当前用户解析器
     */
    public CrmMessageController(MessageService messageService, CurrentUserResolver currentUserResolver) {
        this.messageService = messageService;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * 我的消息列表（未读优先）.
     *
     * @param query   筛选条件
     * @param request HTTP 请求
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "List messages", description = "我的消息列表，未读优先")
    public ResponseResult<PageResponse<MessageDto>> listMessages(MessageQueryRequest query,
                                                                HttpServletRequest request) {
        String receiverId = currentUserResolver.currentUserId(request);
        return ResponseResult.success(messageService.list(receiverId, query));
    }

    /**
     * 未读数（前端每 30~60 秒轮询）.
     *
     * @param request HTTP 请求
     * @return 未读数
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Unread count", description = "未读消息数")
    public ResponseResult<Map<String, Long>> unreadCount(HttpServletRequest request) {
        String receiverId = currentUserResolver.currentUserId(request);
        return ResponseResult.success(Collections.singletonMap("unreadCount", messageService.countUnread(receiverId)));
    }

    /**
     * 消息详情（并自动标记已读）.
     *
     * @param id      消息 ID
     * @param request HTTP 请求
     * @return 消息
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get message", description = "查询消息详情，并自动标记已读")
    public ResponseResult<MessageDto> getMessage(@PathVariable String id, HttpServletRequest request) {
        String receiverId = currentUserResolver.currentUserId(request);
        return ResponseResult.success(messageService.getById(id, receiverId));
    }

    /**
     * 标记已读.
     *
     * @param id      消息 ID
     * @param request HTTP 请求
     * @return 操作结果
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "Mark message read", description = "标记消息已读")
    public ResponseResult<Void> markRead(@PathVariable String id, HttpServletRequest request) {
        String receiverId = currentUserResolver.currentUserId(request);
        messageService.markRead(id, receiverId);
        return ResponseResult.success();
    }

    /**
     * 全部标记已读（可按类型限定）.
     *
     * @param messageType 消息类型；为空表示全部
     * @param request     HTTP 请求
     * @return 标记条数
     */
    @PutMapping("/read-all")
    @Operation(summary = "Mark all messages read", description = "全部标记已读，可按类型限定")
    public ResponseResult<Map<String, Integer>> markAllRead(@RequestBody(required = false)
                                                              MessageQueryRequest request,
                                                              HttpServletRequest httpRequest) {
        String receiverId = currentUserResolver.currentUserId(httpRequest);
        String messageType = request == null ? null : request.getMessageType();
        int count = messageService.markAllRead(receiverId, messageType);
        return ResponseResult.success(Collections.singletonMap("count", count));
    }

    /**
     * 发送人工消息.
     *
     * @param body    消息内容
     * @param request HTTP 请求
     * @return 发送条数
     */
    @PostMapping
    @Operation(summary = "Send message", description = "发送人工消息给同事")
    public ResponseResult<Map<String, Integer>> sendMessage(@RequestBody MessageSendRequest body,
                                                            HttpServletRequest request) {
        String senderId = currentUserResolver.currentUserId(request);
        int count = messageService.sendManual(body.getReceiverId(), body.getTitle(), body.getContent(), senderId);
        return ResponseResult.success(Collections.singletonMap("count", count));
    }

    /**
     * 删除自己的消息（逻辑删除）.
     *
     * @param id      消息 ID
     * @param request HTTP 请求
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete message", description = "删除自己的消息")
    public ResponseResult<Void> deleteMessage(@PathVariable String id, HttpServletRequest request) {
        String receiverId = currentUserResolver.currentUserId(request);
        messageService.delete(id, receiverId);
        return ResponseResult.success();
    }

}
