/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service;

import com.yaowenltd.projectinfomationmanage.model.dto.QuantAccountDto;

import java.util.List;

/**
 * Service for JoinQuant account configuration.
 */
public interface QuantAccountService {

    QuantAccountDto createAccount(QuantAccountDto dto);

    QuantAccountDto updateAccount(QuantAccountDto dto);

    void deleteAccount(String id);

    QuantAccountDto findAccountById(String id);

    List<QuantAccountDto> findAllAccounts();

    /**
     * Test login against JoinQuant. Returns a status message; updates
     * {@code lastLoginTime} / {@code lastLoginStatus} on the account.
     */
    String testAccountLogin(String id);
}
