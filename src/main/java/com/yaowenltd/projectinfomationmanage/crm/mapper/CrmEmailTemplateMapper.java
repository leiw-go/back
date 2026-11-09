/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmEmailTemplateEntity;
import java.util.List;

/**
 * CRM 邮件模板数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmEmailTemplateMapper {

    /**
     * 按主键查询未删除的模板.
     *
     * @param id 模板 ID
     * @return 模板；不存在返回 null
     *     */
    CrmEmailTemplateEntity selectById(@Param("id") String id);

    /**
     * 按模板名查询未删除的模板，用于查重.
     *
     * @param name      模板名
     * @param excludeId 需要排除的模板 ID；可为空
     * @return 模板；不存在返回 null
     *     */
    CrmEmailTemplateEntity selectByName(@Param("name") String name, @Param("excludeId") String excludeId);

    /**
     * 新增模板.
     *
     * @param template 模板实体
     * @return 受影响的行数
     *     */
    int insert(CrmEmailTemplateEntity template);

    /**
     * 更新模板.
     *
     * @param template 模板实体
     * @return 受影响的行数
     *     */
    int update(CrmEmailTemplateEntity template);

    /**
     * 逻辑删除模板.
     *
     * @param id        模板 ID
     * @param deletedAt 删除时刻的毫秒时间戳
     * @return 受影响的行数
     *     */
    int softDelete(@Param("id") String id, @Param("deletedAt") Long deletedAt);

    /**
     * 查询全部未删除的模板.
     *
     * @return 模板列表
     *     */
    List<CrmEmailTemplateEntity> selectAll();

}
