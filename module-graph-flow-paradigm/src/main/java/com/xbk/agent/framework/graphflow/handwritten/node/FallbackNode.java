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
 * 手写版 FallbackNode：搜索失败时的降级回答节点
 *
 * 职责：当 SearchNode 失败（stepStatus=SEARCH_FAILED）时由条件边路由到此节点。
 * 不依赖搜索结果，仅凭用户原始提问让 LLM 给出"无工具退化回答"，
 * 写入 finalAnswer 并将 stepStatus 推进到 END。
 *
 * @author xiexu
 */
@Slf4j
public class FallbackNode {

    private final AgentLlmGateway gateway;

    /**
     * 创建 FallbackNode。
     *
     * @param gateway 统一大模型调用网关
     */
    public FallbackNode(AgentLlmGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * 执行降级回答，仅凭用户提问生成兜底答案。
     *
     * @param state 当前全局状态
     * @return 更新后的状态（stepStatus=END，finalAnswer 已填充降级回答）
     */
    public GraphState process(GraphState state) {
        log.warn("FallbackNode 触发，搜索失败原因：{}", state.getErrorMessage());

        String prompt = "搜索工具当前不可用，请仅凭你的知识对以下问题给出尽可能准确的回答，"
                + "并在回答开头注明\"（注：本次回答未使用实时搜索，信息可能不是最新的）\"。\n\n"
                + "用户问题：" + state.getUserQuery();

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
        String fallbackAnswer = response.getRawText().trim();

        state.setFinalAnswer(fallbackAnswer);
        // 降级回答同样以 END 终止，GraphRunner 的 while 循环在此退出
        state.setStepStatus(StepStatus.END);

        log.info("FallbackNode 执行完毕，降级回答已生成");
        return state;
    }
}
