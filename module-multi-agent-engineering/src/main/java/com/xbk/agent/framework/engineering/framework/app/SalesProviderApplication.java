package com.xbk.agent.framework.engineering.framework.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 销售顾问 Provider 应用。
 *
 * 职责：作为独立 Spring Boot 进程，把销售顾问 Agent 的能力通过 A2A 协议暴露给 Consumer 应用。
 *
 * <p>运行方式：
 * <pre>
 *   mvn spring-boot:run -Dspring-boot.run.profiles=a2a-sales-provider,a2a-nacos-local
 *   java -jar xxx.jar --spring.profiles.active=a2a-sales-provider,a2a-nacos-local
 * </pre>
 *
 * <p>三进程本地集成架构说明：
 * 本模块同时包含三个 Spring Boot 应用入口：
 * <ol>
 *   <li>{@link TechSupportProviderApplication} — 端口 8081，技术专家 Provider；
 *   <li>{@link SalesProviderApplication} — 端口 8082，销售顾问 Provider；
 *   <li>{@link ReceptionistConsumerApplication} — 端口 8080，接待员 Consumer。
 * </ol>
 * 这种"本地三进程"架构是 A2A 协议的最小完整演示：
 * Nacos 负责注册发现，Agent 之间通过标准 A2A 协议通信，而不是单 JVM 内部调用。
 *
 * @author xiexu
 */
@SpringBootApplication(scanBasePackages = "com.xbk.agent.framework")
@EnableDiscoveryClient
public class SalesProviderApplication {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "a2a-sales-provider,a2a-nacos-local");
        SpringApplication.run(SalesProviderApplication.class, args);
    }
}
