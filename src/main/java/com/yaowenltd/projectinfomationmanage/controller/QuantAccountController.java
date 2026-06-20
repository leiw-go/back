/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.model.dto.QuantAccountDto;
import com.yaowenltd.projectinfomationmanage.service.QuantAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for JoinQuant account configuration.
 */
@RestController
@RequestMapping("/api/quant/accounts")
@Tag(name = "Quant Account Management", description = "聚宽账户管理 API")
public class QuantAccountController {

    private final QuantAccountService accountService;

    public QuantAccountController(QuantAccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @Operation(summary = "Create JoinQuant account")
    public ResponseResult<QuantAccountDto> createAccount(@Valid @RequestBody QuantAccountDto dto) {
        return ResponseResult.created(accountService.createAccount(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update JoinQuant account")
    public ResponseResult<QuantAccountDto> updateAccount(@PathVariable String id,
                                                         @Valid @RequestBody QuantAccountDto dto) {
        dto.setId(id);
        return ResponseResult.success(accountService.updateAccount(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete JoinQuant account")
    public ResponseResult<Void> deleteAccount(@PathVariable String id) {
        accountService.deleteAccount(id);
        return ResponseResult.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by id")
    public ResponseResult<QuantAccountDto> getAccount(@PathVariable String id) {
        QuantAccountDto dto = accountService.findAccountById(id);
        if (dto == null) {
            return ResponseResult.notFound("account not found");
        }
        return ResponseResult.success(dto);
    }

    @GetMapping
    @Operation(summary = "List all accounts")
    public ResponseResult<List<QuantAccountDto>> listAccounts() {
        return ResponseResult.success(accountService.findAllAccounts());
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Test login against JoinQuant")
    public ResponseResult<String> testAccountLogin(@PathVariable String id) {
        return ResponseResult.success(accountService.testAccountLogin(id));
    }
}
