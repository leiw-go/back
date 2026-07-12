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
 * 负责用户管理相关操作的 HTTP 接口.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "User CRUD management APIs")
public class UserController {

    private final UserService userService;

    /**
     * 使用给定的 UserService 构造 UserController.
     *
     * @param userService 用户服务
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 创建一个新用户.
     *
     * @param userDto 用户数据
     * @return 已创建的用户
     */
    @PostMapping
    @Operation(summary = "Create user", description = "Create a new user")
    public ResponseResult<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        UserDto created = userService.createUser(userDto);
        return ResponseResult.created(created);
    }

    /**
     * 更新已存在的用户.
     *
     * @param id      用户 ID
     * @param userDto 更新后的用户数据
     * @return 更新后的用户
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update an existing user")
    public ResponseResult<UserDto> updateUser(@PathVariable String id, @Valid @RequestBody UserDto userDto) {
        userDto.setId(id);
        UserDto updated = userService.updateUser(userDto);
        return ResponseResult.success(updated);
    }

    /**
     * 根据 ID 删除用户.
     *
     * @param id 用户 ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Delete a user by ID")
    public ResponseResult<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseResult.success();
    }

    /**
     * 根据 ID 查找用户.
     *
     * @param id 用户 ID
     * @return 用户数据
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Get user details by ID")
    public ResponseResult<UserDto> getUserById(@PathVariable String id) {
        UserDto userDto = userService.findUserById(id);
        return ResponseResult.success(userDto);
    }

    /**
     * 返回所有用户.
     *
     * @return 全部用户列表
     */
    @GetMapping
    @Operation(summary = "List all users", description = "Get all registered users")
    public ResponseResult<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.findAllUsers();
        return ResponseResult.success(users);
    }
}
