/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmEmailCampaignEntity;
import java.util.List;

/**
 * CRM 邮件群发任务数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmEmailCampaignMapper {

    /**
     * 按主键查询未删除的群发任务.
     *
     * @param id 群发任务 ID
     * @return 群发任务；不存在返回 null
     *     */
    CrmEmailCampaignEntity selectById(@Param("id") String id);

    /**
     * 新增群发任务.
     *
     * @param campaign 群发任务实体
     * @return 受影响的行数
     *     */
    int insert(CrmEmailCampaignEntity campaign);

    /**
     * 更新群发任务.
     *
     * @param campaign 群发任务实体
     * @return 受影响的行数
     *     */
    int update(CrmEmailCampaignEntity campaign);

    /**
     * 分页查询群发任务.
     *
     * @param ownerId 创建人；为空表示不限
     * @param status 状态；为空表示不限
     * @return 群发任务列表
     *     */
    List<CrmEmailCampaignEntity> selectPage(@Param("ownerId") String ownerId, @Param("status") String status);

}
