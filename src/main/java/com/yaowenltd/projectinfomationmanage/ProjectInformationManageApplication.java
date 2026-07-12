/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application entry point for ProjectInfomationManage.
 * This is the Spring Boot application starter class.
 * <p>
 * {@link EnableDiscoveryClient} 激活 Spring Cloud Alibaba 的 Nacos 服务注册，
 * 配合 spring-cloud-starter-alibaba-nacos-discovery 自动向 Nacos server 注册本服务实例。
 * </p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ProjectInformationManageApplication {

    /**
     * Main method to launch the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ProjectInformationManageApplication.class, args);
    }

}
