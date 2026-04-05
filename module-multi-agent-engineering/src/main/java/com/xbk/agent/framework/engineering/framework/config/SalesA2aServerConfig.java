package com.xbk.agent.framework.engineering.framework.config;

import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.engineering.framework.agent.SalesAgentFactory;
import com.xbk.agent.framework.engineering.framework.support.EngineeringAgentCardSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 销售顾问 A2A Server 配置。
 *
 * 职责：与 TechSupportA2aServerConfig 对称，为销售顾问 Provider 应用配置
 * AgentCard 端点和 A2A JSON-RPC 处理端点。
 *
 * <p>两个 Provider 应用在结构上完全对称，这体现了 A2A 协议的一个重要教学价值：
 * 同一套协议规范可以在不同能力的 Agent 上复用，Consumer 不需要针对每个专家写不同的调用代码。
 *
 * @author xiexu
 */
@Profile("a2a-sales-provider")
@Configuration
public class SalesA2aServerConfig {

    /**
     * 提供销售顾问 AgentCard。
     *
     * @param port 当前应用端口
     * @return AgentCardProvider 实例
     */
    @Bean
    public AgentCardProvider salesAgentCardProvider(@Value("${server.port:8082}") int port) {
        String agentUrl = "http://localhost:" + port;
        AgentCardWrapper wrapper = EngineeringAgentCardSupport.buildSalesAgentCard(agentUrl);
        return new AgentCardProvider() {
            @Override
            public AgentCardWrapper getAgentCard() {
                return wrapper;
            }
        };
    }

    /**
     * 销售顾问 Agent 工厂。
     *
     * @param agentLlmGateway 统一网关
     * @return 销售顾问工厂
     */
    @Bean
    public SalesAgentFactory salesAgentFactory(AgentLlmGateway agentLlmGateway) {
        return new SalesAgentFactory(agentLlmGateway);
    }

    /**
     * 销售顾问 A2A 控制器。
     *
     * <p>通过组件扫描注册，但额外用 {@code @Profile} 约束只在销售顾问 Provider
     * 场景下生效，避免在其他应用里误装配导致依赖缺失。
     */
    @Profile("a2a-sales-provider")
    @RestController
    public static class SalesA2aController {

        private final AgentCardProvider agentCardProvider;
        private final SalesAgentFactory agentFactory;

        public SalesA2aController(AgentCardProvider agentCardProvider,
                                   SalesAgentFactory agentFactory,
                                   ObjectMapper objectMapper) {
            this.agentCardProvider = agentCardProvider;
            this.agentFactory = agentFactory;
        }

        /**
         * 暴露 AgentCard。
         *
         * @return AgentCard JSON
         */
        @GetMapping(value = "/.well-known/agent.json", produces = MediaType.APPLICATION_JSON_VALUE)
        public Object getAgentCard() {
            return agentCardProvider.getAgentCard().getAgentCard();
        }

        /**
         * 处理 A2A JSON-RPC 请求（销售顾问问答入口）。
         *
         * @param requestBody A2A JSON-RPC 请求体
         * @return JSON-RPC 响应
         */
        @PostMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, Object> handleA2aRequest(@RequestBody Map<String, Object> requestBody) {
            String requestId = String.valueOf(requestBody.getOrDefault("id", UUID.randomUUID().toString()));
            String userText = extractUserText(requestBody);
            String contextId = extractContextId(requestBody);
            String answer = agentFactory.handle(contextId, userText);
            return buildJsonRpcResponse(requestId, contextId, answer);
        }

        @SuppressWarnings("unchecked")
        private String extractUserText(Map<String, Object> body) {
            try {
                Map<String, Object> params = (Map<String, Object>) body.get("params");
                if (params == null) return "";
                Map<String, Object> message = (Map<String, Object>) params.get("message");
                if (message == null) return "";
                List<Map<String, Object>> parts = (List<Map<String, Object>>) message.get("parts");
                if (parts == null || parts.isEmpty()) return "";
                return String.valueOf(parts.get(0).getOrDefault("text", ""));
            }
            catch (Exception ex) {
                return "";
            }
        }

        @SuppressWarnings("unchecked")
        private String extractContextId(Map<String, Object> body) {
            try {
                Map<String, Object> params = (Map<String, Object>) body.get("params");
                if (params == null) return "context-" + UUID.randomUUID();
                Map<String, Object> message = (Map<String, Object>) params.get("message");
                if (message == null) return "context-" + UUID.randomUUID();
                Object contextId = message.get("contextId");
                return contextId != null ? String.valueOf(contextId) : "context-" + UUID.randomUUID();
            }
            catch (Exception ex) {
                return "context-" + UUID.randomUUID();
            }
        }

        private Map<String, Object> buildJsonRpcResponse(String requestId, String contextId, String answerText) {
            Map<String, Object> textPart = new LinkedHashMap<>();
            textPart.put("kind", "text");
            textPart.put("text", answerText);

            Map<String, Object> artifact = new LinkedHashMap<>();
            artifact.put("artifactId", "artifact-" + UUID.randomUUID());
            artifact.put("parts", List.of(textPart));

            Map<String, Object> status = new LinkedHashMap<>();
            status.put("state", "completed");

            Map<String, Object> task = new LinkedHashMap<>();
            task.put("kind", "task");
            task.put("id", "task-" + UUID.randomUUID());
            task.put("contextId", contextId);
            task.put("status", status);
            task.put("artifacts", List.of(artifact));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("jsonrpc", "2.0");
            response.put("id", requestId);
            response.put("result", task);
            return response;
        }
    }
}
