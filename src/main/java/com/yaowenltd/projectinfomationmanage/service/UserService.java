/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service;

import com.yaowenltd.projectinfomationmanage.model.dto.UserDto;

import java.util.List;

/**
 * Service interface for user management operations.
 */
public interface UserService {

    /**
     * Creates a new user.
     *
     * @param userDto the user data
     * @return the created user
     */
    UserDto createUser(UserDto userDto);

    /**
     * Updates an existing user.
     *
     * @param userDto the user data with updates
     * @return the updated user
     */
    UserDto updateUser(UserDto userDto);

    /**
     * Deletes a user by ID.
     *
     * @param id the user ID
     */
    void deleteUser(String id);

    /**
     * Finds a user by ID.
     *
     * @param id the user ID
     * @return the user data
     */
    UserDto findUserById(String id);

    /**
     * Returns all users.
     *
     * @return list of all users
     */
    List<UserDto> findAllUsers();
}
