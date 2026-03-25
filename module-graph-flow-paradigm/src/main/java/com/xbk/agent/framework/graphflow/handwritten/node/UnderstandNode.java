package com.xbk.agent.framework.graphflow.handwritten.node;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.graphflow.common.state.GraphState;
import com.xbk.agent.framework.graphflow.common.state.StepStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * 手写版 UnderstandNode：理解意图，提取搜索关键词
 *
 * 职责：接收 GraphState，调用 LLM 从用户原始提问中提取搜索关键词，
 * 将关键词写入 searchQuery，并将 stepStatus 推进到 UNDERSTOOD。
 *
 * @author xiexu
 */
@Slf4j
public class UnderstandNode {

    private final AgentLlmGateway gateway;

    /**
     * 创建 UnderstandNode。
     *
     * @param gateway 统一大模型调用网关
     */
    public UnderstandNode(AgentLlmGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * 执行意图理解，提取搜索关键词并更新状态。
     *
     * @param state 当前全局状态
     * @return 更新后的状态（stepStatus=UNDERSTOOD，searchQuery 已填充）
     */
    public GraphState process(GraphState state) {
        log.info("UnderstandNode 开始执行，userQuery={}", state.getUserQuery());

        String prompt = "请从以下用户提问中提取最适合用于搜索引擎查询的关键词，只输出关键词本身，不要有任何解释：\n"
                + state.getUserQuery();

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

        state.setSearchQuery(searchQuery);
        state.setStepStatus(StepStatus.UNDERSTOOD);

        log.info("UnderstandNode 执行完毕，searchQuery={}", searchQuery);
        return state;
    }
}
