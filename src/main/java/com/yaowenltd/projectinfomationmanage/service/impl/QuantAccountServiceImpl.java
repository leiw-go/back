/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.yaowenltd.projectinfomationmanage.common.AesUtil;
import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.mapper.QuantAccountMapper;
import com.yaowenltd.projectinfomationmanage.model.dto.QuantAccountDto;
import com.yaowenltd.projectinfomationmanage.model.entity.QuantAccount;
import com.yaowenltd.projectinfomationmanage.service.JoinQuantInvocationException;
import com.yaowenltd.projectinfomationmanage.service.JoinQuantInvoker;
import com.yaowenltd.projectinfomationmanage.service.QuantAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link QuantAccountService}.
 */
@Service
public class QuantAccountServiceImpl implements QuantAccountService {

    private static final Logger log = LoggerFactory.getLogger(QuantAccountServiceImpl.class);

    private final QuantAccountMapper accountMapper;
    private final JoinQuantInvoker joinQuantInvoker;
    private final AesUtil aesUtil;

    public QuantAccountServiceImpl(QuantAccountMapper accountMapper,
                                   JoinQuantInvoker joinQuantInvoker,
                                   AesUtil aesUtil) {
        this.accountMapper = accountMapper;
        this.joinQuantInvoker = joinQuantInvoker;
        this.aesUtil = aesUtil;
    }

    @Override
    public QuantAccountDto createAccount(QuantAccountDto dto) {
        if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
            throw new IllegalArgumentException("password is required when creating an account");
        }
        QuantAccount existing = accountMapper.findAccountByName(dto.getAccountName());
        if (existing != null) {
            throw new IllegalArgumentException("account name already exists: " + dto.getAccountName());
        }
        QuantAccount account = new QuantAccount();
        account.setId(UUID.randomUUID().toString());
        account.setAccountName(dto.getAccountName());
        account.setUsername(dto.getUsername());
        account.setPassword(aesUtil.encrypt(dto.getPassword()));
        account.setPhone(dto.getPhone());
        account.setIsActive(dto.getIsActive() == null ? 1 : dto.getIsActive());
        LocalDateTime now = LocalDateTime.now();
        account.setCreateTime(now);
        account.setUpdateTime(now);
        accountMapper.insertAccount(account);
        return toDto(account, dto.getPassword());
    }

    @Override
    public QuantAccountDto updateAccount(QuantAccountDto dto) {
        QuantAccount current = accountMapper.findAccountById(dto.getId());
        if (current == null) {
            throw new IllegalArgumentException("account not found: " + dto.getId());
        }
        if (dto.getAccountName() != null) {
            current.setAccountName(dto.getAccountName());
        }
        if (dto.getUsername() != null) {
            current.setUsername(dto.getUsername());
        }
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            current.setPassword(aesUtil.encrypt(dto.getPassword()));
        }
        if (dto.getPhone() != null) {
            current.setPhone(dto.getPhone());
        }
        if (dto.getIsActive() != null) {
            current.setIsActive(dto.getIsActive());
        }
        current.setUpdateTime(LocalDateTime.now());
        accountMapper.updateAccount(current);
        return toDto(current, null);
    }

    @Override
    public void deleteAccount(String id) {
        accountMapper.deleteAccountById(id);
    }

    @Override
    public QuantAccountDto findAccountById(String id) {
        QuantAccount account = accountMapper.findAccountById(id);
        if (account == null) {
            return null;
        }
        return toDto(account, null);
    }

    @Override
    public List<QuantAccountDto> findAllAccounts() {
        List<QuantAccount> accounts = accountMapper.findAllAccounts();
        List<QuantAccountDto> out = new ArrayList<>(accounts.size());
        for (QuantAccount a : accounts) {
            out.add(toDto(a, null));
        }
        return out;
    }

    @Override
    public String testAccountLogin(String id) {
        QuantAccount account = accountMapper.findAccountById(id);
        if (account == null) {
            throw new IllegalArgumentException("account not found: " + id);
        }
        String plain = aesUtil.decrypt(account.getPassword());
        try {
            JsonNode resp = joinQuantInvoker.auth(account.getUsername(), plain);
            String status = "ok: " + resp.toString();
            accountMapper.updateLoginStatus(id, LocalDateTime.now(), status);
            return status;
        } catch (JoinQuantInvocationException ex) {
            log.warn("JoinQuant login failed for {}: {}", account.getUsername(), ex.getMessage());
            accountMapper.updateLoginStatus(id, LocalDateTime.now(), "fail: " + ex.getMessage());
            return "fail: " + ex.getMessage();
        } finally {
            // zero out plaintext ref
            plain = null;
        }
    }

    private QuantAccountDto toDto(QuantAccount account, String clearPassword) {
        QuantAccountDto dto = new QuantAccountDto();
        dto.setId(account.getId());
        dto.setAccountName(account.getAccountName());
        dto.setUsername(account.getUsername());
        // Never return the encrypted password; only return the (optional) cleartext the caller just sent
        if (clearPassword != null) {
            dto.setPassword(clearPassword);
        }
        dto.setPhone(account.getPhone());
        dto.setIsActive(account.getIsActive());
        dto.setLastLoginTime(account.getLastLoginTime());
        dto.setLastLoginStatus(account.getLastLoginStatus());
        dto.setCreateTime(account.getCreateTime());
        dto.setUpdateTime(account.getUpdateTime());
        return dto;
    }
}
