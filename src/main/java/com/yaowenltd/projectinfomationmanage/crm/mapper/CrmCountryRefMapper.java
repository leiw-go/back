/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCountryRefEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * CRM 国家参考数据 Mapper.
 *
 * @since 2026-11-09
 */
@Mapper
public interface CrmCountryRefMapper {

    /**
     * 按关键词查询国家（国家码 / 英文名 / 中文名，支持模糊）.
     * <p>
     * 命中优先级：国家码 &gt; 中文名精确 &gt; 英文名精确 &gt; 模糊包含；同级按"常用国家优先"排。
     *
     * @param keyword 关键词
     * @return 命中的国家；没有则返回 null
     */
    CrmCountryRefEntity selectByKeyword(@Param("keyword") String keyword);

    /**
     * 按国家码查询.
     *
     * @param countryCode 国家码
     * @return 国家；不存在返回 null
     */
    CrmCountryRefEntity selectByCode(@Param("countryCode") String countryCode);

    /**
     * 查询全部国家（按常用国家优先排序）.
     *
     * @return 国家列表
     */
    List<CrmCountryRefEntity> selectAll();

    /**
     * 查询国家列表（可按关键词过滤）.
     *
     * @param keyword 关键词；为空表示不限
     * @return 国家列表
     */
    List<CrmCountryRefEntity> selectPage(@Param("keyword") String keyword);

}
