/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.MessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
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
     * 按主键查询未删除的站内信.
     *
     * @param id 消息 ID
     * @return 站内信；不存在返回 null
     */
    MessageEntity selectById(@Param("id") String id);

    /**
     * 查询某收件人的消息列表（未读优先）.
     *
     * @param receiverId 收件人 ID
     * @param query      筛选条件
     * @return 站内信列表
     */
    List<MessageEntity> selectPage(@Param("receiverId") String receiverId,
                                   @Param("query") MessageQueryRequest query);

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

    /**
     * 逻辑删除某条消息（仅限自己的消息）.
     *
     * @param id         消息 ID
     * @param receiverId 收件人 ID
     * @param deletedAt  删除时刻的毫秒时间戳
     * @return 受影响的行数
     */
    int softDelete(@Param("id") String id, @Param("receiverId") String receiverId,
                   @Param("deletedAt") Long deletedAt);

    /**
     * 清理已读且超过保留期的消息（未读不清理）.
     *
     * @param cutoff     保留期截止时间
     * @param deletedAt  删除时刻的毫秒时间戳
     * @return 受影响的行数
     */
    int deleteReadBefore(@Param("cutoff") LocalDateTime cutoff, @Param("deletedAt") Long deletedAt);

    /**
     * 统计某条业务下指定消息类型的已发消息数（定时任务幂等判据）.
     *
     * @param bizType     关联业务类型
     * @param bizId       关联业务主键
     * @param messageType 消息类型
     * @return 命中的消息行数
     */
    int countByBiz(@Param("bizType") String bizType, @Param("bizId") String bizId,
                   @Param("messageType") String messageType);

}
