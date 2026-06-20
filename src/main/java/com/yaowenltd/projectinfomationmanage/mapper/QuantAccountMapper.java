/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.QuantAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis mapper for QuantAccount.
 */
@Mapper
public interface QuantAccountMapper {

    int insertAccount(QuantAccount account);

    int updateAccount(QuantAccount account);

    int updateLoginStatus(@Param("id") String id,
                          @Param("lastLoginTime") java.time.LocalDateTime lastLoginTime,
                          @Param("status") String status);

    int deleteAccountById(@Param("id") String id);

    QuantAccount findAccountById(@Param("id") String id);

    QuantAccount findAccountByName(@Param("accountName") String accountName);

    List<QuantAccount> findAllAccounts();

    List<QuantAccount> findActiveAccounts();
}
