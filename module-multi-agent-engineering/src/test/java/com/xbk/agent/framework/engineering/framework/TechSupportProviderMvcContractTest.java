package com.xbk.agent.framework.engineering.framework;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.engineering.framework.config.TechSupportA2aServerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 技术专家 Provider MVC 合同测试。
 *
 * 职责：以程序化 Web 上下文验证 `/.well-known/agent.json`
 * 端点已经注册成功，并能返回技术专家 AgentCard。
 *
 * @author xiexu
 */
class TechSupportProviderMvcContractTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withPropertyValues(
                    "spring.profiles.active=a2a-tech-provider",
                    "spring.cloud.nacos.discovery.enabled=false")
            .withUserConfiguration(TestApplication.class);

    /**
     * 验证技术专家 Provider 会暴露 AgentCard HTTP 端点。
     */
    @Test
    void shouldExposeAgentCardEndpoint() {
        contextRunner.run(context -> {
            try {
                MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
                mockMvc.perform(get("/.well-known/agent.json"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.name").value("tech-support-agent"))
                        .andExpect(jsonPath("$.url").value("http://localhost:8081"));
            }
            catch (Exception ex) {
                fail(ex);
            }
        });
    }

    /**
     * 最小测试应用。
     *
     * 职责：扫描 Provider 配置包并补齐最少依赖，避免接入外部基础设施。
     *
     * @author xiexu
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = TechSupportA2aServerConfig.class)
    static class TestApplication {

        /**
         * 提供一个最小的 LLM 网关 stub，满足 Provider Bean 装配。
         *
         * @return LLM 网关 stub
         */
        @Bean
        AgentLlmGateway agentLlmGateway() {
            return new StubAgentLlmGateway();
        }
    }

    /**
     * 仅用于装配测试上下文的 LLM 网关 stub。
     *
     * @author xiexu
     */
    private static class StubAgentLlmGateway implements AgentLlmGateway {

        /**
         * 当前测试不会走到同步对话调用。
         *
         * @param request LLM 请求
         * @return 不返回任何结果
         */
        @Override
        public LlmResponse chat(LlmRequest request) {
            throw new UnsupportedOperationException("当前测试不验证 POST / A2A 调用");
        }

        /**
         * 当前测试不会走到流式对话调用。
         *
         * @param request LLM 请求
         * @param handler 流式处理器
         */
        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("当前测试不验证流式调用");
        }

        /**
         * 当前测试不会走到结构化输出调用。
         *
         * @param request LLM 请求
         * @param spec 输出规格
         * @param <T> 结构化输出类型
         * @return 不返回任何结果
         */
        @Override
        public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
            throw new UnsupportedOperationException("当前测试不验证结构化调用");
        }

        /**
         * 返回空能力集即可满足当前测试装配需求。
         *
         * @return 空能力集
         */
        @Override
        public Set<LlmCapability> capabilities() {
            return Collections.emptySet();
        }
    }
}
