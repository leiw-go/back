/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerContactMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerContactRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerContactEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CustomerContactServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CustomerContactService} 的单元测试。
 * <p>
 * 契约来自需求文档 §3.2、§5.3.2、§8.2：
 * 电话与 WhatsApp 必须是 E.164 格式；邮箱在同一客户下唯一；
 * 电话号码在同一客户下唯一（跨客户允许重复）。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CustomerContactServiceTests {

    @Mock
    private CustomerContactMapper customerContactMapper;

    @Mock
    private CustomerMapper customerMapper;

    private CustomerContactService contactService;

    @BeforeEach
    void setUp() {
        contactService = new CustomerContactServiceImpl(customerContactMapper, customerMapper);
    }

    private static CustomerEntity customer() {
        CustomerEntity entity = new CustomerEntity();
        entity.setId("C1");
        entity.setCountryCode("US");
        entity.setDeleted(0L);
        return entity;
    }

    private static CustomerContactRequest request() {
        CustomerContactRequest request = new CustomerContactRequest();
        request.setName("John Smith");
        request.setEmail("john@acme.com");
        request.setPhone("+14155552671");
        request.setWhatsapp("+14155552671");
        return request;
    }

    @Test
    @DisplayName("联系人必须归属一个存在的客户")
    void testCustomerMustExist() {
        when(customerMapper.selectById("C404")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> contactService.create("C404", request()));
        verify(customerContactMapper, never()).insert(any());
    }

    @Test
    @DisplayName("电话必须是 E.164 格式")
    void testPhoneMustBeE164() {
        when(customerMapper.selectById("C1")).thenReturn(customer());
        CustomerContactRequest bad = request();
        bad.setPhone("13800138000");

        assertThrows(IllegalArgumentException.class, () -> contactService.create("C1", bad));
        verify(customerContactMapper, never()).insert(any());
    }

    @Test
    @DisplayName("WhatsApp 必须是 E.164 格式")
    void testWhatsappMustBeE164() {
        when(customerMapper.selectById("C1")).thenReturn(customer());
        CustomerContactRequest bad = request();
        bad.setWhatsapp("+1 415 555 2671");

        assertThrows(IllegalArgumentException.class, () -> contactService.create("C1", bad));
        verify(customerContactMapper, never()).insert(any());
    }

    @Test
    @DisplayName("同一客户下邮箱不允许重复")
    void testDuplicateEmailWithinCustomerIsRejected() {
        when(customerMapper.selectById("C1")).thenReturn(customer());
        when(customerContactMapper.selectByEmail(eq("C1"), eq("john@acme.com"))).thenReturn(existingContact());

        assertThrows(IllegalArgumentException.class, () -> contactService.create("C1", request()));
        verify(customerContactMapper, never()).insert(any());
    }

    @Test
    @DisplayName("同一客户下电话号码不允许重复")
    void testDuplicatePhoneWithinCustomerIsRejected() {
        when(customerMapper.selectById("C1")).thenReturn(customer());
        when(customerContactMapper.selectByPhone(eq("C1"), eq("+14155552671"))).thenReturn(existingContact());

        assertThrows(IllegalArgumentException.class, () -> contactService.create("C1", request()));
        verify(customerContactMapper, never()).insert(any());
    }

    @Test
    @DisplayName("合法联系人应写入")
    void testCreatePersistsContact() {
        when(customerMapper.selectById("C1")).thenReturn(customer());
        when(customerContactMapper.selectByEmail(eq("C1"), any())).thenReturn(null);
        when(customerContactMapper.selectByPhone(eq("C1"), any())).thenReturn(null);
        when(customerContactMapper.insert(any())).thenReturn(1);

        contactService.create("C1", request());

        ArgumentCaptor<CustomerContactEntity> captor = ArgumentCaptor.forClass(CustomerContactEntity.class);
        verify(customerContactMapper).insert(captor.capture());
        assertEquals("C1", captor.getValue().getCustomerId());
        assertEquals("John Smith", captor.getValue().getName());
        assertEquals(0, captor.getValue().getIsPrimary());
    }

    @Test
    @DisplayName("邮箱与电话都允许为空")
    void testEmailAndPhoneAreOptional() {
        when(customerMapper.selectById("C1")).thenReturn(customer());
        when(customerContactMapper.insert(any())).thenReturn(1);
        CustomerContactRequest minimal = new CustomerContactRequest();
        minimal.setName("Only Name");

        contactService.create("C1", minimal);

        verify(customerContactMapper).insert(any());
    }

    @Test
    @DisplayName("删除不存在的联系人应被拒绝")
    void testDeleteMissingContactIsRejected() {
        when(customerContactMapper.selectById("X404")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> contactService.delete("X404"));
        verify(customerContactMapper, never()).softDelete(any(), any());
    }

    @Test
    @DisplayName("删除联系人应逻辑删除并写入删除时间戳")
    void testDeleteSoftDeletes() {
        when(customerContactMapper.selectById("X1")).thenReturn(existingContact());
        when(customerContactMapper.softDelete(eq("X1"), any())).thenReturn(1);

        contactService.delete("X1");

        verify(customerContactMapper).softDelete(eq("X1"), any());
    }

    private static CustomerContactEntity existingContact() {
        CustomerContactEntity entity = new CustomerContactEntity();
        entity.setId("X1");
        entity.setCustomerId("C1");
        entity.setName("John Smith");
        entity.setEmail("john@acme.com");
        entity.setPhone("+14155552671");
        entity.setDeleted(0L);
        return entity;
    }
}
