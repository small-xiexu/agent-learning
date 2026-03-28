package com.xbk.agent.framework.engineering.framework.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 接待员 Consumer 应用。
 *
 * 职责：作为独立 Spring Boot 进程，接收用户请求，通过本地意图分类后，
 * 用 A2A 协议调用远端专家 Provider，并返回最终结果。
 *
 * <p>运行方式：
 * <pre>
 *   java -jar xxx.jar --spring.profiles.active=a2a-receptionist-consumer,a2a-nacos-local
 * </pre>
 *
 * <p>完整集成场景（三进程本地模式）：
 * <pre>
 *   # 终端 1：启动技术专家 Provider（端口 8081）
 *   java -jar xxx.jar --spring.profiles.active=a2a-tech-provider,a2a-nacos-local
 *
 *   # 终端 2：启动销售顾问 Provider（端口 8082）
 *   java -jar xxx.jar --spring.profiles.active=a2a-sales-provider,a2a-nacos-local
 *
 *   # 终端 3：启动接待员 Consumer（端口 8080）
 *   java -jar xxx.jar --spring.profiles.active=a2a-receptionist-consumer,a2a-nacos-local
 *
 *   # 发送测试请求（通过 HTTP 或单元测试）
 *   POST http://localhost:8080/engineering/handle
 *   {"request": "我的服务报 NullPointerException，帮我排查"}
 * </pre>
 *
 * @author xiexu
 */
@SpringBootApplication(scanBasePackages = "com.xbk.agent.framework")
@EnableDiscoveryClient
public class ReceptionistConsumerApplication {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "a2a-receptionist-consumer,a2a-nacos-local");
        SpringApplication.run(ReceptionistConsumerApplication.class, args);
    }
}
