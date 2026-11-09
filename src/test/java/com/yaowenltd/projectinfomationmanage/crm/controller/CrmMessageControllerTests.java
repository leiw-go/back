/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.common.CurrentUserResolver;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageDto;
import com.yaowenltd.projectinfomationmanage.crm.service.MessageService;
import com.yaowenltd.projectinfomationmanage.mapper.UserMapper;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import com.yaowenltd.projectinfomationmanage.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link CrmMessageController} 的单元测试.
 * <p>
 * 契约来自需求文档 §4.13：7 个端点，未读数是前端轮询用的轻量接口。
 *
 * @since 2026-11-08
 */
@ExtendWith(MockitoExtension.class)
class CrmMessageControllerTests {

    @Mock
    private MessageService messageService;

    @Mock
    private UserMapper userMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId("U1");
        user.setUsername("admin");
        lenient().when(userMapper.selectByUsername(anyString())).thenReturn(user);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CrmMessageController(messageService, new CurrentUserResolver(userMapper)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static MessageDto message() {
        MessageDto dto = new MessageDto();
        dto.setId("M1");
        dto.setMessageType("QUOTATION_REJECTED");
        dto.setLevel("IMPORTANT");
        dto.setTitle("报价被拒绝：QT20261108000001");
        dto.setReceiverId("U1");
        dto.setRead(false);
        return dto;
    }

    @Test
    @DisplayName("消息列表应返回统一响应体")
    void testListMessages() throws Exception {
        PageResponse<MessageDto> page = new PageResponse<>();
        page.setData(Collections.singletonList(message()));
        page.setTotal(1);
        page.setPage(1);
        page.setSize(20);
        when(messageService.list(anyString(), any())).thenReturn(page);

        mockMvc.perform(get("/api/crm/messages")
                        .requestAttr(CurrentUserResolver.AUTH_ATTR_USERNAME, "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].messageType").value("QUOTATION_REJECTED"));
    }

    @Test
    @DisplayName("未读数接口返回未读条数")
    void testUnreadCount() throws Exception {
        when(messageService.countUnread("U1")).thenReturn(3L);

        mockMvc.perform(get("/api/crm/messages/unread-count")
                        .requestAttr(CurrentUserResolver.AUTH_ATTR_USERNAME, "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(3));
    }

    @Test
    @DisplayName("消息详情自动标记已读")
    void testGetMessage() throws Exception {
        when(messageService.getById("M1", "U1")).thenReturn(message());

        mockMvc.perform(get("/api/crm/messages/M1")
                        .requestAttr(CurrentUserResolver.AUTH_ATTR_USERNAME, "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("M1"));
        verify(messageService).getById("M1", "U1");
    }

    @Test
    @DisplayName("标记单条已读")
    void testMarkRead() throws Exception {
        mockMvc.perform(put("/api/crm/messages/M1/read")
                        .requestAttr(CurrentUserResolver.AUTH_ATTR_USERNAME, "admin"))
                .andExpect(status().isOk());
        verify(messageService).markRead("M1", "U1");
    }

    @Test
    @DisplayName("全部标记已读返回处理条数")
    void testMarkAllRead() throws Exception {
        when(messageService.markAllRead("U1", "ORDER_SHIPPED")).thenReturn(2);

        mockMvc.perform(put("/api/crm/messages/read-all")
                        .requestAttr(CurrentUserResolver.AUTH_ATTR_USERNAME, "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messageType\":\"ORDER_SHIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2));
    }

    @Test
    @DisplayName("发送人工消息")
    void testSendMessage() throws Exception {
        when(messageService.sendManual(eq("U2"), anyString(), anyString(), eq("U1"))).thenReturn(1);

        mockMvc.perform(post("/api/crm/messages")
                        .requestAttr(CurrentUserResolver.AUTH_ATTR_USERNAME, "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\":\"U2\",\"title\":\"你好\",\"content\":\"有空对一下报价\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));
    }

    @Test
    @DisplayName("删除自己的消息")
    void testDeleteMessage() throws Exception {
        mockMvc.perform(delete("/api/crm/messages/M1")
                        .requestAttr(CurrentUserResolver.AUTH_ATTR_USERNAME, "admin"))
                .andExpect(status().isOk());
        verify(messageService).delete("M1", "U1");
    }

}
