package com.xbk.agent.framework.engineering.handwritten.agent;

import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.engineering.domain.message.EngineeringMessage;
import com.xbk.agent.framework.engineering.handwritten.hub.MessageHub;

import java.util.List;
import java.util.UUID;

/**
 * 手写版 Agent 抽象基类。
 *
 * <p>所有手写版 Agent（前台、技术专家、销售顾问）都继承这个类。
 * 它提供三个公共能力，子类直接用，不用自己实现：
 * <ol>
 *   <li>记住自己叫什么名字（{@code agentName}），消息里的 fromAgent / toAgent 就填这个；
 *   <li>通过 {@code send()} 把消息发到 MessageHub，也就是把信投进邮局；
 *   <li>通过 {@code askModel()} 向 LLM 提问，拿到文字答复。
 * </ol>
 *
 * @author xiexu
 */
public abstract class AbstractHandwrittenAgent implements HandwrittenMessageAgent {

    /**
     * 这个 Agent 的名字，填在每条消息的 fromAgent 字段里，让其他 Agent 知道是谁发的。
     */
    private final String agentName;

    /**
     * 统一 LLM 网关，子类通过 askModel() 间接调用，不用自己构造 HTTP 请求。
     */
    private final AgentLlmGateway agentLlmGateway;

    /**
     * 邮局引用，子类通过 send() 发消息，通过 getMessageHub() 读取投递日志。
     */
    private final MessageHub messageHub;

    /**
     * 创建手写版 Agent。
     *
     * @param agentName       Agent 名称
     * @param agentLlmGateway 统一网关
     * @param messageHub      消息中心
     */
    protected AbstractHandwrittenAgent(String agentName, AgentLlmGateway agentLlmGateway, MessageHub messageHub) {
        this.agentName = agentName;
        this.agentLlmGateway = agentLlmGateway;
        this.messageHub = messageHub;
    }

    /**
     * 返回 Agent 名称。
     *
     * @return Agent 名称
     */
    @Override
    public String getAgentName() {
        return agentName;
    }

    /**
     * 发送消息到 MessageHub。
     *
     * @param message 工程消息
     */
    @Override
    public void send(EngineeringMessage message) {
        messageHub.publish(message);
    }

    /**
     * 返回统一网关。
     *
     * @return 统一网关
     */
    protected AgentLlmGateway getAgentLlmGateway() {
        return agentLlmGateway;
    }

    /**
     * 返回消息中心。
     *
     * @return 消息中心
     */
    protected MessageHub getMessageHub() {
        return messageHub;
    }

    /**
     * 向 LLM 提一个问题，同步等待文字答复。
     *
     * <p>子类（前台、技术专家、销售顾问）都用这个方法调用模型，
     * 只需传入系统提示词和用户提示词，不用关心 HTTP、token、会话管理这些细节。
     *
     * @param conversationId 本次会话 ID，模型用它区分不同对话
     * @param systemPrompt   系统提示词，告诉模型"你是谁、你的职责是什么"
     * @param userPrompt     用户提示词，具体的问题内容
     * @return 模型回复的文字
     */
    protected String askModel(String conversationId, String systemPrompt, String userPrompt) {
        if (agentLlmGateway == null) {
            throw new IllegalStateException("AgentLlmGateway is required for " + agentName);
        }
        LlmResponse response = agentLlmGateway.chat(LlmRequest.builder()
                .requestId(agentName + "-request-" + UUID.randomUUID())
                .conversationId(conversationId)
                .messages(List.of(
                        message(conversationId, MessageRole.SYSTEM, systemPrompt),
                        message(conversationId, MessageRole.USER, userPrompt)))
                .build());

        // 真实 LLM 网关返回的答复在 outputMessage.content 里；
        // Mock 网关直接返回裸字符串放在 rawText 里。
        // 先尝试结构化路径，取不到再回退，两种实现都能正常工作。
        if (response.getOutputMessage() != null && response.getOutputMessage().getContent() != null) {
            return response.getOutputMessage().getContent();
        }
        return response.getRawText();
    }

    /**
     * 构造统一消息。
     *
     * @param conversationId 会话标识
     * @param role           消息角色
     * @param content        消息文本
     * @return 统一消息
     */
    private Message message(String conversationId, MessageRole role, String content) {
        return Message.builder()
                .messageId("message-" + UUID.randomUUID())
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .build();
    }
}
