/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.entity.MessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 站内信数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface MessageMapper {

    /**
     * 批量插入站内信（群发 fan-out 时一行一个收件人）.
     *
     * @param messages 待插入的消息列表
     * @return 受影响的行数
     */
    int insertBatch(@Param("messages") List<MessageEntity> messages);

    /**
     * 标记某条消息为已读（仅限指定收件人）.
     *
     * @param messageId  消息 ID
     * @param receiverId 收件人 ID
     * @return 受影响的行数
     */
    int markRead(@Param("messageId") String messageId, @Param("receiverId") String receiverId);

    /**
     * 将某收件人的消息全部标记为已读.
     *
     * @param receiverId  收件人 ID
     * @param messageType 消息类型；为空表示不限类型
     * @return 受影响的行数
     */
    int markAllRead(@Param("receiverId") String receiverId, @Param("messageType") String messageType);

    /**
     * 统计某收件人的未读消息数.
     *
     * @param receiverId 收件人 ID
     * @return 未读消息数
     */
    long countUnread(@Param("receiverId") String receiverId);

}
