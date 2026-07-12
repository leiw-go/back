/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

/**
 * 分页请求参数.
 */
public class PageRequest {

    private int page = 1;

    private int size = 10;

    public PageRequest() {
    }

    public PageRequest(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    /**
     * 计算数据库查询的偏移量.
     */
    public int getOffset() {
        return (page - 1) * size;
    }
}
