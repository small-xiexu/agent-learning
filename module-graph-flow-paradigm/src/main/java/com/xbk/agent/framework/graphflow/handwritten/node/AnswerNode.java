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
 * 手写版 AnswerNode：综合搜索结果，总结回答
 *
 * 职责：将用户提问与搜索结果一起发送给 LLM，生成最终答案，
 * 写入 finalAnswer 并将 stepStatus 推进到 END，标志图执行结束。
 *
 * @author xiexu
 */
@Slf4j
public class AnswerNode {

    private final AgentLlmGateway gateway;

    /**
     * 创建 AnswerNode。
     *
     * @param gateway 统一大模型调用网关
     */
    public AnswerNode(AgentLlmGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * 综合搜索结果生成最终回答并结束图执行。
     *
     * @param state 当前全局状态
     * @return 更新后的状态（stepStatus=END，finalAnswer 已填充）
     */
    public GraphState process(GraphState state) {
        log.info("AnswerNode 开始执行，基于搜索结果生成回答");

        String prompt = "请根据以下搜索结果，对用户的问题给出简洁、准确的回答。\n\n"
                + "用户问题：" + state.getUserQuery() + "\n\n"
                + "搜索结果：\n" + state.getSearchResults();

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
        String finalAnswer = response.getRawText().trim();

        state.setFinalAnswer(finalAnswer);
        // 写入 END 是手写版状态机的终止信号，GraphRunner 的 while 循环在此退出
        state.setStepStatus(StepStatus.END);

        log.info("AnswerNode 执行完毕，finalAnswer 已生成");
        return state;
    }
}
