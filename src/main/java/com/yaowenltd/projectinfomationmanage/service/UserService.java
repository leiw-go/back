/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service;

import com.yaowenltd.projectinfomationmanage.model.dto.UserDto;

import java.util.List;

/**
 * 服务接口，定义用户管理业务能力.
 */
public interface UserService {

    /**
     * 创建新用户.
     *
     * @param userDto 用户数据
     * @return 已创建的用户
     */
    UserDto createUser(UserDto userDto);

    /**
     * 更新已存在的用户.
     *
     * @param userDto 包含更新字段的用户数据
     * @return 更新后的用户
     */
    UserDto updateUser(UserDto userDto);

    /**
     * 根据 ID 删除用户.
     *
     * @param id 用户 ID
     */
    void deleteUser(String id);

    /**
     * 根据 ID 查找用户.
     *
     * @param id 用户 ID
     * @return 用户数据
     */
    UserDto findUserById(String id);

    /**
     * 返回所有用户.
     *
     * @return 所有用户列表
     */
    List<UserDto> findAllUsers();
}