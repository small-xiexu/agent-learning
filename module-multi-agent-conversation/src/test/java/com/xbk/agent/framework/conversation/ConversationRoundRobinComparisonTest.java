package com.xbk.agent.framework.conversation;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.xbk.agent.framework.core.common.enums.LlmCapability;
import com.xbk.agent.framework.core.common.enums.MessageRole;
import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import com.xbk.agent.framework.core.llm.model.LlmRequest;
import com.xbk.agent.framework.core.llm.model.LlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredLlmResponse;
import com.xbk.agent.framework.core.llm.model.StructuredOutputSpec;
import com.xbk.agent.framework.core.llm.spi.LlmStreamHandler;
import com.xbk.agent.framework.core.memory.Message;
import com.xbk.agent.framework.conversation.api.ConversationRunResult;
import com.xbk.agent.framework.conversation.application.coordinator.RoundRobinGroupChat;
import com.xbk.agent.framework.conversation.application.executor.CodeReviewerAgent;
import com.xbk.agent.framework.conversation.application.executor.EngineerAgent;
import com.xbk.agent.framework.conversation.application.executor.ProductManagerAgent;
import com.xbk.agent.framework.conversation.domain.memory.ConversationMemory;
import com.xbk.agent.framework.conversation.domain.memory.ConversationTurn;
import com.xbk.agent.framework.conversation.domain.role.ConversationRoleType;
import com.xbk.agent.framework.conversation.infrastructure.agentframework.AlibabaConversationFlowAgent;
import com.xbk.agent.framework.conversation.infrastructure.agentframework.support.ConversationStateKeys;
import com.xbk.agent.framework.conversation.infrastructure.agentframework.support.ConversationStateSupport;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AutoGen RoundRobin 双实现对照测试
 *
 * 职责：钉住“共享群聊上下文 + 固定轮询 + Reviewer 决定结束”的 handwritten 与 FlowAgent 双实现
 *
 * @author xiexu
 */
class ConversationRoundRobinComparisonTest {

    private static final String BITCOIN_TASK = """
            任务：开发一个能够获取并打印实时比特币价格的简易 Python 脚本。
            团队角色：ProductManager（产品经理，负责需求分析）、Engineer（工程师，负责编码）、CodeReviewer（代码审查员，负责检查）。
            """;

    /**
     * 验证手写版 RoundRobin 群聊可以完成“需求分析 -> 编码 -> 审查 -> 再次编码 -> 最终批准”的闭环。
     */
    @Test
    void shouldCompleteBitcoinTaskWithHandwrittenRoundRobinGroupChat() {
        ScriptedConversationGateway gateway = new ScriptedConversationGateway();
        RoundRobinGroupChat groupChat = new RoundRobinGroupChat(
                new ProductManagerAgent(gateway),
                new EngineerAgent(gateway),
                new CodeReviewerAgent(gateway),
                new ConversationMemory(),
                8);

        ConversationRunResult result = groupChat.run(BITCOIN_TASK);

        assertEquals(ConversationRoleType.CODE_REVIEWER, result.getStopRole());
        assertTrue(result.getStopReason().contains("<AUTOGEN_TASK_DONE>"));
        assertTrue(result.getFinalPythonScript().contains("requests.get"));
        assertTrue(result.getFinalPythonScript().contains("timeout=10"));
        assertEquals(6, result.getTranscript().size());
        assertTrue(result.getSharedMessages().size() >= 7);
        assertTurn(result.getTranscript().get(0), ConversationRoleType.PRODUCT_MANAGER, "先实现一个最小可运行版本");
        assertTurn(result.getTranscript().get(1), ConversationRoleType.ENGINEER, "def fetch_bitcoin_price");
        assertTurn(result.getTranscript().get(2), ConversationRoleType.CODE_REVIEWER, "阻塞问题");
        assertTurn(result.getTranscript().get(5), ConversationRoleType.CODE_REVIEWER, "<AUTOGEN_TASK_DONE>");
        assertTrue(result.getFlowState().isEmpty());
    }

    /**
     * 验证 FlowAgent 版群聊会通过共享状态完成至少一轮审查后的继续协作。
     */
    @Test
    void shouldCompleteBitcoinTaskWithAlibabaConversationFlowAgent() {
        ScriptedConversationGateway gateway = new ScriptedConversationGateway();
        AlibabaConversationFlowAgent agent = new AlibabaConversationFlowAgent(gateway, 8);

        ConversationRunResult result = agent.run(BITCOIN_TASK);
        OverAllState state = result.getFlowState().orElseThrow();

        assertEquals(ConversationRoleType.CODE_REVIEWER, result.getStopRole());
        assertTrue(result.getStopReason().contains("<AUTOGEN_TASK_DONE>"));
        assertTrue(result.getFinalPythonScript().contains("requests.get"));
        assertTrue(state.value(ConversationStateKeys.DONE, Boolean.class).orElse(Boolean.FALSE));
        assertEquals("approved", state.value(ConversationStateKeys.REVIEW_STATUS, ""));
        assertEquals(Integer.valueOf(6), state.value(ConversationStateKeys.TURN_COUNT, Integer.class).orElseThrow());
        assertTrue(state.value(ConversationStateKeys.CURRENT_PYTHON_SCRIPT, "").contains("timeout=10"));
        assertEquals("autogen-product-manager-agent", agent.getProductManagerAgent().name());
        assertEquals("autogen-engineer-agent", agent.getEngineerAgent().name());
        assertEquals("autogen-code-reviewer-agent", agent.getCodeReviewerAgent().name());
        assertFalse(agent.getCodeReviewerAgent().isReturnReasoningContents());
        assertEquals(6, result.getTranscript().size());
        assertFalse(result.getSharedMessages().isEmpty());
    }

    /**
     * 验证 Flow 状态中的 Map 形态 shared_messages 和 transcript 可以被恢复。
     */
    @Test
    void shouldRestoreMapBackedConversationState() {
        List<ConversationTurn> transcript = List.of(
                new ConversationTurn(1, ConversationRoleType.PRODUCT_MANAGER, "先实现最小脚本"),
                new ConversationTurn(2, ConversationRoleType.ENGINEER, "print('btc')"));
        List<Message> sharedMessages = List.of(
                Message.builder()
                        .messageId("message-1")
                        .conversationId("conversation-1")
                        .role(MessageRole.USER)
                        .name("Task")
                        .content("实现比特币价格脚本")
                        .build(),
                Message.builder()
                        .messageId("message-2")
                        .conversationId("conversation-1")
                        .role(MessageRole.ASSISTANT)
                        .name("Engineer")
                        .content("print('btc')")
                        .build());

        List<ConversationTurn> restoredTranscript = ConversationStateSupport.readTranscript(
                ConversationStateSupport.toStateTranscript(transcript));
        List<Message> restoredSharedMessages = ConversationStateSupport.readSharedMessages(
                ConversationStateSupport.toStateMessages(sharedMessages));

        assertEquals(2, restoredTranscript.size());
        assertEquals(2, restoredSharedMessages.size());
        assertEquals(ConversationRoleType.PRODUCT_MANAGER, restoredTranscript.get(0).getRoleType());
        assertEquals("Engineer", restoredSharedMessages.get(1).getName());
    }

    /**
     * 断言单轮群聊的角色和内容。
     *
     * @param turn 单轮群聊
     * @param role 预期角色
     * @param expectedSnippet 预期片段
     */
    private void assertTurn(ConversationTurn turn, ConversationRoleType role, String expectedSnippet) {
        assertEquals(role, turn.getRoleType());
        assertTrue(turn.getContent().contains(expectedSnippet));
    }

    /**
     * 群聊脚本化网关。
     *
     * 职责：按固定顺序模拟六轮 AutoGen RoundRobin 对话，并校验关键上下文协议
     *
     * @author xiexu
     */
    private static final class ScriptedConversationGateway implements AgentLlmGateway {

        private final AtomicInteger callCount = new AtomicInteger();

        /**
         * 返回脚本化响应。
         *
         * @param request LLM 请求
         * @return LLM 响应
         */
        @Override
        public LlmResponse chat(LlmRequest request) {
            int currentCall = callCount.incrementAndGet();
            String systemPrompt = request.getMessages().getFirst().getContent();
            String latestMessage = request.getMessages().getLast().getContent();
            if (currentCall == 1) {
                assertTrue(systemPrompt.contains("ProductManager"));
                assertTrue(latestMessage.contains("开发一个能够获取并打印实时比特币价格"));
                return buildResponse(request, """
                        第一步请先实现一个最小可运行版本：
                        1. 使用 Python 和 requests 调用公共 API 获取比特币实时价格；
                        2. 在控制台打印价格；
                        3. 先不做复杂重试，但需要保留基础异常处理空间。
                        """);
            }
            if (currentCall == 2) {
                assertTrue(systemPrompt.contains("Engineer"));
                assertTrue(latestMessage.contains("先实现一个最小可运行版本"));
                return buildResponse(request, """
                        ```python
                        import requests

                        def fetch_bitcoin_price():
                            response = requests.get("https://api.coindesk.com/v1/bpi/currentprice/BTC.json")
                            data = response.json()
                            return data["bpi"]["USD"]["rate"]

                        if __name__ == "__main__":
                            print("BTC price:", fetch_bitcoin_price())
                        ```
                        """);
            }
            if (currentCall == 3) {
                assertTrue(systemPrompt.contains("CodeReviewer"));
                assertTrue(latestMessage.contains("def fetch_bitcoin_price"));
                return buildResponse(request, """
                        存在两个阻塞问题：
                        1. 缺少超时设置，网络异常时可能长时间阻塞；
                        2. 缺少 requests 异常处理和状态码检查。
                        请下一轮补上 timeout、raise_for_status 和基础异常处理。
                        """);
            }
            if (currentCall == 4) {
                assertTrue(systemPrompt.contains("ProductManager"));
                assertTrue(latestMessage.contains("阻塞问题"));
                return buildResponse(request, """
                        请根据 CodeReviewer 的阻塞意见完善脚本：
                        1. 为 requests.get 增加 timeout=10；
                        2. 对请求失败和 JSON 结构异常给出明确错误提示；
                        3. 保持脚本入口简单，最终仍然只打印比特币价格结果。
                        """);
            }
            if (currentCall == 5) {
                assertTrue(systemPrompt.contains("Engineer"));
                assertTrue(latestMessage.contains("timeout=10"));
                return buildResponse(request, """
                        ```python
                        import requests

                        API_URL = "https://api.coindesk.com/v1/bpi/currentprice/BTC.json"

                        def fetch_bitcoin_price():
                            try:
                                response = requests.get(API_URL, timeout=10)
                                response.raise_for_status()
                                data = response.json()
                                price = data["bpi"]["USD"]["rate"]
                                return price
                            except requests.RequestException as exception:
                                raise RuntimeError(f"failed to fetch bitcoin price: {exception}") from exception
                            except (KeyError, TypeError, ValueError) as exception:
                                raise RuntimeError("invalid bitcoin price response") from exception

                        if __name__ == "__main__":
                            print("BTC price:", fetch_bitcoin_price())
                        ```
                        """);
            }
            assertTrue(systemPrompt.contains("CodeReviewer"));
            assertTrue(latestMessage.contains("timeout=10"));
            return buildResponse(request, """
                    审查通过：脚本已经能够获取并打印实时比特币价格，并补上了超时和基础异常处理。
                    <AUTOGEN_TASK_DONE>
                    """);
        }

        /**
         * 当前测试不需要流式响应。
         *
         * @param request 请求
         * @param handler 处理器
         */
        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("conversation test stub does not support stream");
        }

        /**
         * 当前测试不需要结构化输出。
         *
         * @param request 请求
         * @param spec 结构化定义
         * @param <T> 输出类型
         * @return 永不返回
         */
        @Override
        public <T> StructuredLlmResponse<T> structuredChat(LlmRequest request, StructuredOutputSpec<T> spec) {
            throw new UnsupportedOperationException("conversation test stub does not support structured chat");
        }

        /**
         * 返回空能力集合。
         *
         * @return 空能力集合
         */
        @Override
        public Set<LlmCapability> capabilities() {
            return Collections.emptySet();
        }

        /**
         * 构造统一响应。
         *
         * @param request 原始请求
         * @param text 输出文本
         * @return LLM 响应
         */
        private LlmResponse buildResponse(LlmRequest request, String text) {
            return LlmResponse.builder()
                    .requestId(request.getRequestId())
                    .responseId("response-" + UUID.randomUUID())
                    .outputMessage(Message.builder()
                            .messageId("message-" + UUID.randomUUID())
                            .conversationId(request.getConversationId())
                            .role(MessageRole.ASSISTANT)
                            .content(text)
                            .build())
                    .rawText(text)
                    .build();
        }
    }
}
