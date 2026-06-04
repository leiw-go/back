/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.model.dto.UserDto;
import com.yaowenltd.projectinfomationmanage.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for user management operations.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "User CRUD management APIs")
public class UserController {

    private final UserService userService;

    /**
     * Constructs a UserController with the given UserService.
     *
     * @param userService the user service
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Creates a new user.
     *
     * @param userDto the user data
     * @return the created user
     */
    @PostMapping
    @Operation(summary = "Create user", description = "Create a new user")
    public ResponseResult<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        UserDto created = userService.createUser(userDto);
        return ResponseResult.created(created);
    }

    /**
     * Updates an existing user.
     *
     * @param id      the user ID
     * @param userDto the updated user data
     * @return the updated user
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update an existing user")
    public ResponseResult<UserDto> updateUser(@PathVariable String id, @Valid @RequestBody UserDto userDto) {
        userDto.setId(id);
        UserDto updated = userService.updateUser(userDto);
        return ResponseResult.success(updated);
    }

    /**
     * Deletes a user by ID.
     *
     * @param id the user ID
     * @return success response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Delete a user by ID")
    public ResponseResult<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseResult.success();
    }

    /**
     * Finds a user by ID.
     *
     * @param id the user ID
     * @return the user data
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Get user details by ID")
    public ResponseResult<UserDto> getUserById(@PathVariable String id) {
        UserDto userDto = userService.findUserById(id);
        return ResponseResult.success(userDto);
    }

    /**
     * Returns all users.
     *
     * @return list of all users
     */
    @GetMapping
    @Operation(summary = "List all users", description = "Get all registered users")
    public ResponseResult<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.findAllUsers();
        return ResponseResult.success(users);
    }
}
