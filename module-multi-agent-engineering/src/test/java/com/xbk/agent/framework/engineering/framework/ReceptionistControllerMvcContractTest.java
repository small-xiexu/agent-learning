package com.xbk.agent.framework.engineering.framework;

import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.engineering.api.EngineeringRunResult;
import com.xbk.agent.framework.engineering.domain.routing.CustomerIntentType;
import com.xbk.agent.framework.engineering.domain.routing.RoutingDecision;
import com.xbk.agent.framework.engineering.domain.routing.SpecialistType;
import com.xbk.agent.framework.engineering.framework.agent.FrameworkReceptionistService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接待员 HTTP Controller MVC 合同测试。
 *
 * 职责：验证接待员 Consumer 暴露 `POST /engineering/handle` 端点，
 * 并把技术类、销售类请求都委托给 FrameworkReceptionistService 返回统一结果。
 *
 * @author xiexu
 */
class ReceptionistControllerMvcContractTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withPropertyValues(
                    "spring.profiles.active=a2a-receptionist-consumer",
                    "spring.cloud.nacos.discovery.enabled=false")
            .withUserConfiguration(TestApplication.class);

    /**
     * 验证技术类请求可通过 HTTP 入口返回技术专家结果。
     */
    @Test
    void shouldHandleTechQuestionViaHttpEndpoint() {
        contextRunner.run(context -> {
            try {
                MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
                mockMvc.perform(post("/engineering/handle")
                                .contentType(APPLICATION_JSON)
                                .content("{\"request\":\"我的 Spring Boot 服务启动时报 NullPointerException，帮我排查原因。\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.requestText").value("我的 Spring Boot 服务启动时报 NullPointerException，帮我排查原因。"))
                        .andExpect(jsonPath("$.specialistType").value("TECH_SUPPORT"))
                        .andExpect(jsonPath("$.finalResponse").value("技术专家建议：请先检查 Bean 注入链路和启动日志。"));
            }
            catch (Exception ex) {
                fail(ex);
            }
        });
    }

    /**
     * 验证销售类请求可通过 HTTP 入口返回销售顾问结果。
     */
    @Test
    void shouldHandleSalesQuestionViaHttpEndpoint() {
        contextRunner.run(context -> {
            try {
                MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
                mockMvc.perform(post("/engineering/handle")
                                .contentType(APPLICATION_JSON)
                                .content("{\"request\":\"我想了解企业版的购买方案和报价。\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.requestText").value("我想了解企业版的购买方案和报价。"))
                        .andExpect(jsonPath("$.specialistType").value("SALES"))
                        .andExpect(jsonPath("$.finalResponse").value("销售顾问建议：企业版支持按节点规模分层报价。"));
            }
            catch (Exception ex) {
                fail(ex);
            }
        });
    }

    /**
     * 最小测试应用。
     *
     * 职责：只扫描接待员 HTTP 包，并提供一个可预测的接待员服务 stub。
     *
     * @author xiexu
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = "com.xbk.agent.framework.engineering.framework.web")
    static class TestApplication {

        /**
         * 提供最小 LLM 网关 stub，覆盖自动配置依赖。
         *
         * @return LLM 网关 stub
         */
        @Bean
        AgentLlmGateway agentLlmGateway() {
            return new StubAgentLlmGateway();
        }

        /**
         * 提供接待员服务 stub，避免接入真实 Nacos/A2A/LLM 基础设施。
         *
         * @return 接待员服务 stub
         */
        @Bean
        FrameworkReceptionistService frameworkReceptionistService() {
            return new StubFrameworkReceptionistService();
        }
    }

    /**
     * 用于 MVC 合同测试的接待员服务 stub。
     *
     * @author xiexu
     */
    private static class StubFrameworkReceptionistService extends FrameworkReceptionistService {

        /**
         * 创建 stub 服务。
         */
        StubFrameworkReceptionistService() {
            super(null, null, null);
        }

        /**
         * 根据请求文本返回固定的技术或销售结果。
         *
         * @param userRequest 用户请求
         * @return 统一运行结果
         */
        @Override
        public EngineeringRunResult handle(String userRequest) {
            if (userRequest.contains("报价") || userRequest.contains("购买")) {
                return EngineeringRunResult.builder()
                        .conversationId("conversation-sales")
                        .requestText(userRequest)
                        .intentType(CustomerIntentType.SALES_CONSULTING)
                        .specialistType(SpecialistType.SALES)
                        .routingDecision(new RoutingDecision(
                                CustomerIntentType.SALES_CONSULTING,
                                SpecialistType.SALES,
                                "用户在咨询企业版方案与报价",
                                "sales-topic",
                                "sales-agent"))
                        .specialistResponse("销售顾问建议：企业版支持按节点规模分层报价。")
                        .finalResponse("销售顾问建议：企业版支持按节点规模分层报价。")
                        .routeTrail(List.of("framework_receptionist", "sales-agent"))
                        .build();
            }
            return EngineeringRunResult.builder()
                    .conversationId("conversation-tech")
                    .requestText(userRequest)
                    .intentType(CustomerIntentType.TECH_SUPPORT)
                    .specialistType(SpecialistType.TECH_SUPPORT)
                    .routingDecision(new RoutingDecision(
                            CustomerIntentType.TECH_SUPPORT,
                            SpecialistType.TECH_SUPPORT,
                            "用户在咨询技术异常排查",
                            "tech-topic",
                            "tech-support-agent"))
                    .specialistResponse("技术专家建议：请先检查 Bean 注入链路和启动日志。")
                    .finalResponse("技术专家建议：请先检查 Bean 注入链路和启动日志。")
                    .routeTrail(List.of("framework_receptionist", "tech-support-agent"))
                    .build();
        }
    }

    /**
     * 用于关闭自动配置依赖的最小 LLM 网关 stub。
     *
     * @author xiexu
     */
    private static class StubAgentLlmGateway implements AgentLlmGateway {

        /**
         * 当前 MVC 合同测试不会触发真实 LLM 调用。
         *
         * @param request LLM 请求
         * @return 不返回任何结果
         */
        @Override
        public LlmResponse chat(LlmRequest request) {
            throw new UnsupportedOperationException("当前测试不验证 LLM 调用");
        }

        /**
         * 当前 MVC 合同测试不会触发流式调用。
         *
         * @param request LLM 请求
         * @param handler 流式处理器
         */
        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("当前测试不验证流式调用");
        }

        /**
         * 当前 MVC 合同测试不会触发结构化调用。
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
         * 返回空能力集即可满足当前装配需求。
         *
         * @return 空能力集
         */
        @Override
        public Set<LlmCapability> capabilities() {
            return Collections.emptySet();
        }
    }
}
