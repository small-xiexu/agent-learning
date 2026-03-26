package com.xbk.agent.framework.roleplay;

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
import com.xbk.agent.framework.roleplay.api.CamelRunResult;
import com.xbk.agent.framework.roleplay.application.coordinator.HandwrittenCamelAgent;
import com.xbk.agent.framework.roleplay.application.executor.CamelProgrammerAgent;
import com.xbk.agent.framework.roleplay.application.executor.CamelTraderAgent;
import com.xbk.agent.framework.roleplay.domain.memory.CamelDialogueTurn;
import com.xbk.agent.framework.roleplay.domain.memory.CamelConversationMemory;
import com.xbk.agent.framework.roleplay.domain.role.CamelRoleType;
import com.xbk.agent.framework.roleplay.infrastructure.agentframework.AlibabaCamelFlowAgent;
import com.xbk.agent.framework.roleplay.infrastructure.agentframework.support.CamelStateKeys;
import com.xbk.agent.framework.roleplay.infrastructure.agentframework.support.CamelTranscriptStateSupport;
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
 * CAMEL 股票分析双实现对照测试
 *
 * 职责：先用测试钉住“只有交易员能终止”的角色接力，再补 Spring AI Alibaba 图编排版 handoff 回环
 *
 * @author xiexu
 */
class CamelStockAnalysisComparisonTest {

    private static final String STOCK_TASK = """
            目标：编写一个 Java 程序，通过调用公共 API 获取特定股票的实时价格，并计算其移动平均线。
            角色 A（AI 用户）：一位资深的股票交易员，负责提出具体的业务需求和审查结果。
            角色 B（AI 助理）：一位资深的 Java 程序员，负责根据需求编写代码。
            """;

    /**
     * 验证手写版 CAMEL 会完成“需求提出 -> 代码生成 -> 审查补充 -> 再次实现 -> 交易员验收”的角色接力。
     */
    @Test
    void shouldCompleteStockAnalysisTaskWithHandwrittenCamelAgent() {
        ScriptedCamelGateway gateway = new ScriptedCamelGateway();
        CamelTraderAgent traderAgent = new CamelTraderAgent(gateway);
        CamelProgrammerAgent programmerAgent = new CamelProgrammerAgent(gateway);
        CamelConversationMemory memory = new CamelConversationMemory();
        HandwrittenCamelAgent agent = new HandwrittenCamelAgent(traderAgent, programmerAgent, memory, 6);

        CamelRunResult result = agent.run(STOCK_TASK);

        assertTrue(result.getFinalJavaCode().contains("calculateMovingAverage"));
        assertTrue(result.getFinalJavaCode().contains("HttpClient"));
        assertEquals(CamelRoleType.TRADER, result.getStopRole());
        assertTrue(result.getStopReason().contains("<CAMEL_TASK_DONE>"));
        assertEquals(5, result.getTranscript().size());
        assertTurn(result.getTranscript().get(0), CamelRoleType.TRADER, "先完成第一阶段");
        assertTurn(result.getTranscript().get(1), CamelRoleType.PROGRAMMER, "fetchQuote");
        assertTurn(result.getTranscript().get(2), CamelRoleType.TRADER, "补齐 5 日移动平均线");
        assertTurn(result.getTranscript().get(3), CamelRoleType.PROGRAMMER, "calculateMovingAverage");
        assertTurn(result.getTranscript().get(4), CamelRoleType.TRADER, "<CAMEL_TASK_DONE>");
        assertTrue(result.getFlowState().isEmpty());
    }

    /**
     * 验证 FlowAgent 版 CAMEL 会通过共享状态完成至少一轮审查和修订后的 handoff 回环。
     */
    @Test
    void shouldCompleteStockAnalysisTaskWithAlibabaCamelFlowAgent() {
        ScriptedCamelGateway gateway = new ScriptedCamelGateway();
        AlibabaCamelFlowAgent agent = new AlibabaCamelFlowAgent(gateway, 6);

        CamelRunResult result = agent.run(STOCK_TASK);
        OverAllState state = result.getFlowState().orElseThrow();

        assertTrue(result.getFinalJavaCode().contains("calculateMovingAverage"));
        assertEquals(CamelRoleType.TRADER, result.getStopRole());
        assertTrue(result.getStopReason().contains("<CAMEL_TASK_DONE>"));
        assertTrue(state.value(CamelStateKeys.DONE, Boolean.class).orElse(Boolean.FALSE));
        assertEquals(CamelRoleType.TRADER.getStateValue(), state.value(CamelStateKeys.ACTIVE_ROLE, ""));
        assertTrue(state.value(CamelStateKeys.CURRENT_JAVA_CODE, "").contains("calculateMovingAverage"));
        assertTrue(state.value(CamelStateKeys.LAST_PROGRAMMER_OUTPUT, "").contains("HttpClient"));
        assertEquals(Integer.valueOf(5), state.value(CamelStateKeys.TURN_COUNT, Integer.class).orElseThrow());
        assertEquals("camel-stock-trader-agent", agent.getTraderAgent().name());
        assertEquals("camel-stock-programmer-agent", agent.getProgrammerAgent().name());
        assertFalse(agent.getTraderAgent().isReturnReasoningContents());
        assertFalse(agent.getProgrammerAgent().isReturnReasoningContents());
    }

    /**
     * 验证 Flow 状态中的 Map 形态 transcript 可以被稳定恢复。
     */
    @Test
    void shouldRestoreTranscriptFromMapBackedState() {
        List<CamelDialogueTurn> transcript = List.of(
                new CamelDialogueTurn(1, CamelRoleType.TRADER, "请先实现实时价格查询"),
                new CamelDialogueTurn(2, CamelRoleType.PROGRAMMER, "public class StockPriceFetcher {}"));

        List<Map<String, Object>> stateTranscript = CamelTranscriptStateSupport.toStateTranscript(transcript);
        List<CamelDialogueTurn> restoredTranscript = CamelTranscriptStateSupport.readTranscript(stateTranscript);

        assertEquals(2, restoredTranscript.size());
        assertEquals(CamelRoleType.TRADER, restoredTranscript.get(0).getRoleType());
        assertEquals("请先实现实时价格查询", restoredTranscript.get(0).getContent());
        assertEquals(CamelRoleType.PROGRAMMER, restoredTranscript.get(1).getRoleType());
        assertTrue(restoredTranscript.get(1).getContent().contains("StockPriceFetcher"));
    }

    /**
     * 断言对话轮次的角色和内容。
     *
     * @param turn 对话轮次
     * @param role 预期角色
     * @param expectedSnippet 预期内容片段
     */
    private void assertTurn(CamelDialogueTurn turn, CamelRoleType role, String expectedSnippet) {
        assertEquals(role, turn.getRoleType());
        assertTrue(turn.getContent().contains(expectedSnippet));
    }

    /**
     * CAMEL 统一网关脚本桩。
     *
     * 职责：按固定顺序模拟交易员和程序员的五轮对话，并校验提示词中包含关键协议
     *
     * @author xiexu
     */
    private static final class ScriptedCamelGateway implements AgentLlmGateway {

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
            String prompt = request.getMessages().getLast().getContent();
            if (currentCall == 1) {
                assertTrue(systemPrompt.contains("你是一位资深的股票交易员"), "交易员系统提示缺少角色定义");
                assertTrue(systemPrompt.contains("必须一问一答"), "交易员系统提示缺少一问一答约束");
                assertTrue(systemPrompt.contains("<CAMEL_TASK_DONE>"), "交易员系统提示缺少终止标记协议");
                assertTrue(prompt.contains("第一轮只能要求程序员先完成"), "首轮交易员提示缺少分阶段协作约束");
                return buildResponse(request, """
                        请先完成第一阶段：
                        1. 接收股票代码参数；
                        2. 通过公共 API 获取实时价格；
                        3. 输出最新价格；
                        4. 暂时不要计算移动平均线，下一轮我会继续审查。
                        """);
            }
            if (currentCall == 2) {
                assertTrue(systemPrompt.contains("你是一位资深的 Java 程序员"), "程序员系统提示缺少角色定义");
                assertTrue(systemPrompt.contains("每次只写一段代码"), "程序员系统提示缺少单段代码约束");
                assertTrue(systemPrompt.contains("你绝对不能输出 <CAMEL_TASK_DONE>"), "程序员系统提示缺少终止权限限制");
                assertTrue(prompt.contains("暂时不要计算移动平均线"), "程序员提示缺少第一阶段约束");
                return buildResponse(request, firstStageJavaCode());
            }
            if (currentCall == 3) {
                assertTrue(systemPrompt.contains("只有在你至少完成过一次“审查 -> 程序员修订”的回合后"), "交易员系统提示缺少最小审查轮次约束");
                assertTrue(prompt.contains("fetchQuote"), "交易员二轮提示缺少第一版代码");
                return buildResponse(request, """
                        继续完善这版 Java 程序：
                        1. 请补齐 5 日移动平均线计算；
                        2. 增加对收盘价列表为空的异常处理；
                        3. 最终同时输出实时价格和 5 日移动平均线。
                        """);
            }
            if (currentCall == 4) {
                assertTrue(systemPrompt.contains("你是一位资深的 Java 程序员"), "程序员二轮系统提示缺少角色定义");
                assertTrue(prompt.contains("补齐 5 日移动平均线计算"), "程序员二轮提示缺少审查意见");
                return buildResponse(request, finalStageJavaCode());
            }
            assertTrue(systemPrompt.contains("你是一位资深的股票交易员"), "终审交易员系统提示缺少角色定义");
            assertTrue(prompt.contains("calculateMovingAverage"), "终审交易员提示缺少修订后代码");
            return buildResponse(request, """
                    验收通过，这次修订已经覆盖实时价格、5 日移动平均线和基础异常处理。
                    <CAMEL_TASK_DONE>
                    """);
        }

        /**
         * 当前测试不需要流式响应。
         *
         * @param request 请求
         * @param handler 流式处理器
         */
        @Override
        public void stream(LlmRequest request, LlmStreamHandler handler) {
            throw new UnsupportedOperationException("camel test stub does not support stream");
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
            throw new UnsupportedOperationException("camel test stub does not support structured chat");
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
         * @return 统一响应
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

        /**
         * 返回第一阶段 Java 代码。
         *
         * @return Java 代码
         */
        private String firstStageJavaCode() {
            return """
                    import java.io.IOException;
                    import java.net.URI;
                    import java.net.http.HttpClient;
                    import java.net.http.HttpRequest;
                    import java.net.http.HttpResponse;
                    import java.util.List;

                    public class StockAnalysisApp {

                        private final HttpClient httpClient = HttpClient.newHttpClient();

                        public String fetchQuote(String symbol) throws IOException, InterruptedException {
                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create("https://query1.finance.yahoo.com/v7/finance/quote?symbols=" + symbol))
                                    .GET()
                                    .build();
                            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
                        }
                    }
                    """;
        }

        /**
         * 返回第二阶段 Java 代码。
         *
         * @return Java 代码
         */
        private String finalStageJavaCode() {
            return """
                    import java.io.IOException;
                    import java.net.URI;
                    import java.net.http.HttpClient;
                    import java.net.http.HttpRequest;
                    import java.net.http.HttpResponse;
                    import java.util.List;

                    public class StockAnalysisApp {

                        private final HttpClient httpClient = HttpClient.newHttpClient();

                        public double calculateMovingAverage(List<Double> prices) {
                            if (prices == null || prices.isEmpty()) {
                                throw new IllegalArgumentException("prices must not be empty");
                            }
                            return prices.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
                        }

                        public String fetchQuote(String symbol) throws IOException, InterruptedException {
                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create("https://query1.finance.yahoo.com/v7/finance/quote?symbols=" + symbol))
                                    .GET()
                                    .build();
                            return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
                        }

                        public String buildSummary(String symbol, List<Double> recentCloses)
                                throws IOException, InterruptedException {
                            String quote = fetchQuote(symbol);
                            double movingAverage = calculateMovingAverage(recentCloses);
                            return "symbol=" + symbol + ", quote=" + quote + ", ma5=" + movingAverage;
                        }
                    }
                    """;
        }
    }
}
