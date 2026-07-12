/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 产品数据库操作的 Mapper 接口.
 */
@Mapper
public interface ProductMapper {

    /**
     * 新增一个产品.
     *
     * @param product 待插入的产品
     * @return 受影响的行数
     */
    int insertProduct(Product product);

    /**
     * 更新已有产品.
     *
     * @param product 带有更新信息的产品
     * @return 受影响的行数
     */
    int updateProduct(Product product);

    /**
     * 根据 ID 删除产品.
     *
     * @param id 产品 ID
     * @return 受影响的行数
     */
    int deleteProductById(@Param("id") String id);

    /**
     * 根据 ID 查询产品.
     *
     * @param id 产品 ID
     * @return 产品实体，未找到返回 null
     */
    Product findProductById(@Param("id") String id);

    /**
     * 查询所有产品.
     *
     * @return 产品列表
     */
    List<Product> findAllProducts();
}