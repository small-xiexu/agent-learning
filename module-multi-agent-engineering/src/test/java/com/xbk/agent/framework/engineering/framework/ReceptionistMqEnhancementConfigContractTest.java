package com.xbk.agent.framework.engineering.framework;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.engineering.framework.agent.FrameworkReceptionistService;
import com.xbk.agent.framework.engineering.framework.config.ReceptionistA2aClientConfig;
import com.xbk.agent.framework.engineering.framework.messaging.RoutingAuditEventPublisher;
import com.xbk.agent.framework.engineering.framework.messaging.SpecialistEscalationPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 接待员 MQ 增强层装配测试。
 *
 * 职责：验证在没有 RocketMQTemplate Bean 的情况下，Consumer 侧仍能装配
 * FrameworkReceptionistService 与两个 MQ 增强发布者，确保本地默认环境可正常启动。
 *
 * @author xiexu
 */
class ReceptionistMqEnhancementConfigContractTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues(
                    "spring.profiles.active=a2a-receptionist-consumer",
                    "engineering.mq.enabled=true")
            .withUserConfiguration(TestApplication.class, ReceptionistA2aClientConfig.class);

    /**
     * 验证没有 RocketMQTemplate 时仍可完成最小装配。
     */
    @Test
    void shouldLoadMqEnhancementBeansWithoutRocketMqTemplate() {
        contextRunner.run(context -> {
            assertNotNull(context.getBean(FrameworkReceptionistService.class));
            assertNotNull(context.getBean(RoutingAuditEventPublisher.class));
            assertNotNull(context.getBean(SpecialistEscalationPublisher.class));
        });
    }

    /**
     * 最小测试应用。
     *
     * @author xiexu
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        /**
         * 提供最小统一网关 stub。
         *
         * @return 统一网关 stub
         */
        @Bean
        AgentLlmGateway agentLlmGateway() {
            return new StubAgentLlmGateway();
        }

        /**
         * 提供最小服务发现 stub。
         *
         * @return 服务发现 stub
         */
        @Bean
        DiscoveryClient discoveryClient() {
            return new StubDiscoveryClient();
        }
    }

    /**
     * 不触发真实调用的网关 stub。
     *
     * @author xiexu
     */
    private static class StubAgentLlmGateway implements AgentLlmGateway {

        /**
         * 当前测试不触发真实 chat。
         *
         * @param request 请求
         * @return 不返回结果
         */
        @Override
        public LlmResponse chat(LlmRequest request) {
            throw new UnsupportedOperationException("当前装配测试不触发 LLM 调用");
        }

        /**
         * 当前测试不触发 stream。
         *
         * @param request 请求
         * @param handler 处理器
         */
        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("当前装配测试不触发 LLM 流式调用");
        }

        /**
         * 当前测试不触发 structuredChat。
         *
         * @param request 请求
         * @param spec 规格
         * @param <T> 类型
         * @return 不返回结果
         */
        @Override
        public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
            throw new UnsupportedOperationException("当前装配测试不触发结构化 LLM 调用");
        }

        /**
         * 声明支持能力。
         *
         * @return 能力集合
         */
        @Override
        public Set<LlmCapability> capabilities() {
            return Set.of(LlmCapability.SYNC_CHAT);
        }
    }

    /**
     * 最小服务发现 stub。
     *
     * @author xiexu
     */
    private static class StubDiscoveryClient implements DiscoveryClient {

        /**
         * 返回空服务列表。
         *
         * @param serviceId 服务名
         * @return 空实例列表
         */
        @Override
        public List<ServiceInstance> getInstances(String serviceId) {
            return Collections.emptyList();
        }

        /**
         * 返回空服务名列表。
         *
         * @return 空列表
         */
        @Override
        public List<String> getServices() {
            return Collections.emptyList();
        }

        /**
         * 描述当前客户端。
         *
         * @return mock 描述
         */
        @Override
        public String description() {
            return "stub-discovery-client";
        }
    }
}
