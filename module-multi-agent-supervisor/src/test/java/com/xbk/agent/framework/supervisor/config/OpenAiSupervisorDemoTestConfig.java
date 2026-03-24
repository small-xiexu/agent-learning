package com.xbk.agent.framework.supervisor.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * OpenAI Supervisor Demo 测试配置
 *
 * 职责：为真实 OpenAI 对照 Demo 提供最小 Spring Boot 自动装配入口，
 * 把测试关注点压缩到“统一 llm 配置能否装起网关和 ChatModel”
 *
 * @author xiexu
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class OpenAiSupervisorDemoTestConfig {
}
