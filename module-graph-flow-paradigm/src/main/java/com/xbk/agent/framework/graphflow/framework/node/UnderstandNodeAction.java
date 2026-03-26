package com.xbk.agent.framework.graphflow.framework.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import lombok.extern.slf4j.Slf4j;

import com.xbk.agent.framework.graphflow.framework.support.GraphFlowStateKeys;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 框架版 UnderstandNodeAction：意图理解节点
 *
 * 职责：实现 AsyncNodeAction 接口，从 OverAllState 中读取用户提问，
 * 调用 LLM 提取搜索关键词，以增量 Map 的形式返回更新字段。
 *
 * ══ 与手写版对照 ══════════════════════════════════════════════════════
 * 手写版：GraphState process(GraphState state)
 *         → 接收完整 POJO，返回修改后的完整 POJO
 * 框架版：CompletableFuture<Map<String,Object>> apply(OverAllState state)
 *         → 只返回"本节点变更的字段增量"，框架自动做 State Merge
 *
 * 手写版：state.setSearchQuery(xxx); state.setStepStatus(UNDERSTOOD);
 * 框架版：return Map.of("search_query", xxx, "step_status", "UNDERSTOOD")
 *         → 框架将此 Map 合并进全局 OverAllState，其他字段保持不变
 * ════════════════════════════════════════════════════════════════════
 *
 * @author xiexu
 */
@Slf4j
public class UnderstandNodeAction implements AsyncNodeAction {

    private final AgentLlmGateway gateway;

    /**
     * 创建 UnderstandNodeAction。
     *
     * @param gateway 统一大模型调用网关
     */
    public UnderstandNodeAction(AgentLlmGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * 执行意图理解，提取搜索关键词。
     *
     * @param state 框架全局状态（等价于手写版 GraphState POJO）
     * @return 本节点产生的状态增量（框架自动 merge 进 OverAllState）
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        // 从 OverAllState 中读取字段 —— 等价于手写版 state.getUserQuery()
        String userQuery = state.value(GraphFlowStateKeys.USER_QUERY, String.class).orElse("");
        log.info("UnderstandNodeAction 开始执行，user_query={}", userQuery);

        String prompt = "请从以下用户提问中提取最适合用于搜索引擎查询的关键词，只输出关键词本身，不要有任何解释：\n"
                + userQuery;

        String conversationId = "graph-flow-" + UUID.randomUUID();
        LlmRequest request = LlmRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .messages(List.of(
                        Message.builder()
                                .messageId(UUID.randomUUID().toString())
                                .conversationId(conversationId)
                                .role(MessageRole.USER)
                                .content(prompt)
                                .build()))
                .build();

        LlmResponse response = gateway.chat(request);
        String searchQuery = response.getRawText().trim();

        log.info("UnderstandNodeAction 执行完毕，search_query={}", searchQuery);

        // 只返回本节点变更的字段，框架做增量 merge ——
        // 等价于手写版：state.setSearchQuery(searchQuery); state.setStepStatus(UNDERSTOOD);
        // 注意：框架版无需显式设置 step_status，路由由 addEdge 声明决定，不依赖状态字段
        return CompletableFuture.completedFuture(Map.of(GraphFlowStateKeys.SEARCH_QUERY, searchQuery));
    }
}
