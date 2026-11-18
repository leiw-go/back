/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmEmailGenerationEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * CRM 邮件生成历史数据库操作的 Mapper 接口.
 *
 * @since 2026-11-18
 */
@Mapper
public interface CrmEmailGenerationMapper {

    /**
     * 新增一条生成记录.
     *
     * @param generation 生成记录实体
     * @return 受影响的行数
     *     */
    int insert(CrmEmailGenerationEntity generation);

    /**
     * 按主键查询未删除的记录.
     *
     * @param id 记录 ID
     * @return 记录；不存在返回 null
     *     */
    CrmEmailGenerationEntity selectById(@Param("id") String id);

    /**
     * 分页查询未删除的记录，按生成时间倒排.
     * <p>
     * 分页由 PageHelper 拦截，这里只管排序与过滤。
     *
     * @return 记录列表
     *     */
    List<CrmEmailGenerationEntity> selectPage();

    /**
     * 逻辑删除一条记录.
     *
     * @param id        记录 ID
     * @param deletedAt 删除时刻的毫秒时间戳
     * @return 受影响的行数
     *     */
    int softDelete(@Param("id") String id, @Param("deletedAt") Long deletedAt);
}
