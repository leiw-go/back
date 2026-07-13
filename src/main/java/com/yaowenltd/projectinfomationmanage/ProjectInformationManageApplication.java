/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ProjectInfomationManage 项目的主应用入口。
 * 这是 Spring Boot 应用的启动类。
 * <p>
 * {@link EnableDiscoveryClient} 激活 Spring Cloud Alibaba 的 Nacos 服务注册，
 * 配合 spring-cloud-starter-alibaba-nacos-discovery 自动向 Nacos server 注册本服务实例。
 * </p>
 * <p>
 * {@link EnableScheduling} 开启 Spring 内置的 {@code @Scheduled} 调度器，
 * 用于驱动项目内的定时任务（如飞书机器人 hello world 推送等）。
 * </p>
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class ProjectInformationManageApplication {

    /**
     * 用于启动 Spring Boot 应用的主方法。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ProjectInformationManageApplication.class, args);
    }

}
