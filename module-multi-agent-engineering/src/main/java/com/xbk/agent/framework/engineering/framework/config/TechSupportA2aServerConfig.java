package com.xbk.agent.framework.engineering.framework.config;

import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardProvider;
import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.engineering.framework.agent.TechSupportAgentFactory;
import com.xbk.agent.framework.engineering.framework.support.EngineeringAgentCardSupport;
import io.a2a.spec.Artifact;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
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
 * 技术专家 A2A Server 配置。
 *
 * 职责：在技术专家 Provider 应用启动时，配置并暴露两个关键端点：
 * <ol>
 *   <li>{@code GET /.well-known/agent.json} —— 返回技术专家的 AgentCard；
 *   <li>{@code POST /} —— 处理 A2A JSON-RPC message/send 请求。
 * </ol>
 *
 * <p>A2A 协议说明（教学重点）：
 * Consumer 发现 Provider 的流程：
 * <pre>
 *   1. Consumer 从 Nacos 获取 tech-support-agent 服务实例 URL
 *   2. GET {url}/.well-known/agent.json → 获取 AgentCard（了解能力）
 *   3. POST {url} → 发送 JSON-RPC message/send → 获取专家答复
 * </pre>
 *
 * @author xiexu
 */
@Profile("a2a-tech-provider")
@Configuration
public class TechSupportA2aServerConfig {

    /**
     * 提供技术专家 AgentCard。
     *
     * <p>AgentCard 的 url 字段由 server.port 动态构建，
     * 与 Nacos 注册的 endpoint 保持一致。
     *
     * @param port 当前应用端口（由 server.port 注入）
     * @return AgentCardProvider 实例
     */
    @Bean
    public AgentCardProvider techSupportAgentCardProvider(
            @Value("${server.port:8081}") int port) {
        String agentUrl = "http://localhost:" + port;
        AgentCardWrapper wrapper = EngineeringAgentCardSupport.buildTechSupportAgentCard(agentUrl);
        return new AgentCardProvider() {
            @Override
            public AgentCardWrapper getAgentCard() {
                return wrapper;
            }
        };
    }

    /**
     * 技术专家 Agent 工厂。
     *
     * @param agentLlmGateway 统一网关
     * @return 技术专家工厂
     */
    @Bean
    public TechSupportAgentFactory techSupportAgentFactory(AgentLlmGateway agentLlmGateway) {
        return new TechSupportAgentFactory(agentLlmGateway);
    }

    /**
     * 技术专家 A2A 控制器内部类。
     *
     * <p>通过组件扫描注册，但额外用 {@code @Profile} 约束只在技术专家 Provider
     * 场景下生效，避免在其他应用里误装配导致依赖缺失。
     */
    @Profile("a2a-tech-provider")
    @RestController
    public static class TechSupportA2aController {

        private final AgentCardProvider agentCardProvider;
        private final TechSupportAgentFactory agentFactory;
        private final ObjectMapper objectMapper;

        public TechSupportA2aController(AgentCardProvider agentCardProvider,
                                        TechSupportAgentFactory agentFactory,
                                        ObjectMapper objectMapper) {
            this.agentCardProvider = agentCardProvider;
            this.agentFactory = agentFactory;
            this.objectMapper = objectMapper;
        }

        /**
         * 暴露 AgentCard（A2A 能力描述端点）。
         *
         * <p>Consumer 通过访问 /.well-known/agent.json 获取 AgentCard，
         * 了解这个 Agent 的名称、能力描述和技能列表。
         *
         * @return AgentCard JSON
         */
        @GetMapping(value = "/.well-known/agent.json", produces = MediaType.APPLICATION_JSON_VALUE)
        public Object getAgentCard() {
            return agentCardProvider.getAgentCard().getAgentCard();
        }

        /**
         * 处理 A2A JSON-RPC 请求（技术专家问答入口）。
         *
         * <p>A2A 协议使用 JSON-RPC 2.0 格式，method 为 message/send。
         * 本方法：提取用户文本 → 调用技术专家 → 返回 Task（已完成状态）。
         *
         * @param requestBody A2A JSON-RPC 请求体
         * @return JSON-RPC 响应（包含 Task 结果）
         */
        @PostMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, Object> handleA2aRequest(@RequestBody Map<String, Object> requestBody) {
            String requestId = String.valueOf(requestBody.getOrDefault("id", UUID.randomUUID().toString()));
            String userText = extractUserText(requestBody);
            String contextId = extractContextId(requestBody);
            String answer = agentFactory.handle(contextId, userText);
            return buildJsonRpcResponse(requestId, contextId, answer);
        }

        /**
         * 从 JSON-RPC 请求体中提取用户文本。
         *
         * @param body 请求体
         * @return 用户文本
         */
        @SuppressWarnings("unchecked")
        private String extractUserText(Map<String, Object> body) {
            try {
                Map<String, Object> params = (Map<String, Object>) body.get("params");
                if (params == null) {
                    return "";
                }
                Map<String, Object> message = (Map<String, Object>) params.get("message");
                if (message == null) {
                    return "";
                }
                List<Map<String, Object>> parts = (List<Map<String, Object>>) message.get("parts");
                if (parts == null || parts.isEmpty()) {
                    return "";
                }
                return String.valueOf(parts.get(0).getOrDefault("text", ""));
            }
            catch (Exception ex) {
                return "";
            }
        }

        /**
         * 提取请求上下文标识。
         *
         * @param body 请求体
         * @return contextId
         */
        @SuppressWarnings("unchecked")
        private String extractContextId(Map<String, Object> body) {
            try {
                Map<String, Object> params = (Map<String, Object>) body.get("params");
                if (params == null) {
                    return "context-" + UUID.randomUUID();
                }
                Map<String, Object> message = (Map<String, Object>) params.get("message");
                if (message == null) {
                    return "context-" + UUID.randomUUID();
                }
                Object contextId = message.get("contextId");
                return contextId != null ? String.valueOf(contextId) : "context-" + UUID.randomUUID();
            }
            catch (Exception ex) {
                return "context-" + UUID.randomUUID();
            }
        }

        /**
         * 构建 JSON-RPC 成功响应（包含已完成 Task）。
         *
         * @param requestId JSON-RPC 请求 ID
         * @param contextId 上下文 ID
         * @param answerText 专家答复文本
         * @return 响应 Map（自动序列化为 JSON）
         */
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
