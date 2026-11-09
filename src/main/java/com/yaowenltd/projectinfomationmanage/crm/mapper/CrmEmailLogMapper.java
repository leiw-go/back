/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmEmailLogEntity;
import java.util.List;

/**
 * CRM 邮件发送记录数据库操作的 Mapper 接口（流水表，物理删除）.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmEmailLogMapper {

    /**
     * 批量写入发送记录.
     *
     * @param logs 发送记录列表
     * @return 受影响的行数
     *     */
    int insertBatch(@Param("logs") List<CrmEmailLogEntity> logs);

    /**
     * 查询某群发任务的全部发送记录.
     *
     * @param campaignId 群发任务 ID
     * @return 发送记录列表
     *     */
    List<CrmEmailLogEntity> selectByCampaignId(@Param("campaignId") String campaignId);

}
