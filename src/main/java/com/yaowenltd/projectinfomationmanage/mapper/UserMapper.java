/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据库操作的 Mapper 接口.
 */
@Mapper
public interface UserMapper {

    /**
     * 新增一个用户.
     *
     * @param user 待插入的用户
     * @return 受影响的行数
     */
    int insertUser(User user);

    /**
     * 更新已有用户.
     *
     * @param user 带有更新信息的用户
     * @return 受影响的行数
     */
    int updateUser(User user);

    /**
     * 根据 ID 删除用户.
     *
     * @param id 用户 ID
     * @return 受影响的行数
     */
    int deleteUserById(@Param("id") String id);

    /**
     * 根据 ID 查询用户.
     *
     * @param id 用户 ID
     * @return 用户实体，未找到返回 null
     */
    User findUserById(@Param("id") String id);

    /**
     * 根据用户名查询用户.
     *
     * @param username 用户名
     * @return 用户实体，未找到返回 null
     */
    User findUserByUsername(@Param("username") String username);

    /**
     * 查询所有启用的用户.
     *
     * @return 用户列表
     */
    List<User> findAllUsers();
}