/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.customer;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 客户导出用的 Excel 行对象（不复用实体，避免把内部字段带出去）.
 *
 * @since 2026-11-08
 */
@Data
public class CustomerExportDto {

    /**
     * 客户 ID.
     */
    @ExcelProperty("客户ID")
    private String id;

    /**
     * 公司全称.
     */
    @ExcelProperty("公司全称")
    private String companyName;

    /**
     * 简称.
     */
    @ExcelProperty("简称")
    private String shortName;

    /**
     * 国家码.
     */
    @ExcelProperty("国家")
    private String countryCode;

    /**
     * 城市.
     */
    @ExcelProperty("城市")
    private String city;

    /**
     * 地址.
     */
    @ExcelProperty("地址")
    private String address;

    /**
     * 行业.
     */
    @ExcelProperty("行业")
    private String industry;

    /**
     * 来源渠道.
     */
    @ExcelProperty("来源渠道")
    private String source;

    /**
     * 客户等级.
     */
    @ExcelProperty("客户等级")
    private String level;

    /**
     * 客户状态.
     */
    @ExcelProperty("客户状态")
    private String customerStatus;

    /**
     * 负责人.
     */
    @ExcelProperty("负责人")
    private String ownerId;

    /**
     * 备注.
     */
    @ExcelProperty("备注")
    private String remark;
}
