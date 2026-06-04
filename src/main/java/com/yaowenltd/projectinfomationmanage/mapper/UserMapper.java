/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper interface for User database operations.
 */
@Mapper
public interface UserMapper {

    /**
     * Inserts a new user.
     *
     * @param user the user to insert
     * @return the number of affected rows
     */
    int insertUser(User user);

    /**
     * Updates an existing user.
     *
     * @param user the user with updated information
     * @return the number of affected rows
     */
    int updateUser(User user);

    /**
     * Deletes a user by ID.
     *
     * @param id the user ID
     * @return the number of affected rows
     */
    int deleteUserById(@Param("id") String id);

    /**
     * Finds a user by ID.
     *
     * @param id the user ID
     * @return the user, or null if not found
     */
    User findUserById(@Param("id") String id);

    /**
     * Finds a user by username.
     *
     * @param username the username
     * @return the user, or null if not found
     */
    User findUserByUsername(@Param("username") String username);

    /**
     * Returns all active users.
     *
     * @return list of all users
     */
    List<User> findAllUsers();
}
