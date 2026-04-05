package com.xbk.agent.framework.engineering.framework.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 技术专家 Provider 应用。
 *
 * 职责：作为独立 Spring Boot 进程，把技术专家 Agent 的能力通过 A2A 协议暴露给 Consumer 应用。
 *
 * <p>运行方式：
 * <pre>
 *   mvn spring-boot:run -Dspring-boot.run.profiles=a2a-tech-provider,a2a-nacos-local
 *   # 或指定配置文件
 *   java -jar xxx.jar --spring.profiles.active=a2a-tech-provider,a2a-nacos-local,llm-local
 * </pre>
 *
 * <p>启动后：
 * <ul>
 *   <li>向 Nacos 注册服务名 {@code tech-support-agent}，端口默认 8081；
 *   <li>暴露 {@code GET /.well-known/agent.json} 返回 AgentCard；
 *   <li>暴露 {@code POST /} 处理 A2A JSON-RPC 技术问答请求。
 * </ul>
 *
 * <p>前置条件：本地 Nacos 已启动（见 application-a2a-nacos-local.yml 说明）。
 *
 * @author xiexu
 */
@SpringBootApplication(scanBasePackages = "com.xbk.agent.framework")
@EnableDiscoveryClient
public class TechSupportProviderApplication {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "a2a-tech-provider,a2a-nacos-local,llm-local");
        SpringApplication.run(TechSupportProviderApplication.class, args);
    }
}
