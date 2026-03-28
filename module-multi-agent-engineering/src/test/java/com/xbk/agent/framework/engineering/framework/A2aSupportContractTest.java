package com.xbk.agent.framework.engineering.framework;

import com.alibaba.cloud.ai.graph.agent.a2a.AgentCardWrapper;
import com.xbk.agent.framework.engineering.framework.support.A2aResponseExtractor;
import com.xbk.agent.framework.engineering.framework.support.EngineeringAgentCardSupport;
import io.a2a.spec.Artifact;
import io.a2a.spec.EventKind;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A2A 支持层契约测试。
 *
 * 职责：钉住框架版 Agent 命名规范、AgentCard 元信息约定和响应提取规则，
 * 确保 Consumer 与 Provider 之间的命名协议不会静默漂移。
 *
 * @author xiexu
 */
class A2aSupportContractTest {

    /**
     * 验证技术专家 AgentCard 的命名和核心元信息符合约定。
     */
    @Test
    void techSupportAgentCardShouldHaveCorrectNamingAndMetadata() {
        AgentCardWrapper wrapper = EngineeringAgentCardSupport.buildTechSupportAgentCard(
                "http://localhost:8081");

        assertEquals("tech-support-agent", wrapper.name());
        assertNotNull(wrapper.description());
        assertFalse(wrapper.description().isBlank());
        assertEquals("http://localhost:8081", wrapper.url());
        assertNotNull(wrapper.skills());
        assertFalse(wrapper.skills().isEmpty());
        assertEquals("tech-support", wrapper.skills().get(0).id());
    }

    /**
     * 验证销售顾问 AgentCard 的命名和核心元信息符合约定。
     */
    @Test
    void salesAgentCardShouldHaveCorrectNamingAndMetadata() {
        AgentCardWrapper wrapper = EngineeringAgentCardSupport.buildSalesAgentCard(
                "http://localhost:8082");

        assertEquals("sales-agent", wrapper.name());
        assertNotNull(wrapper.description());
        assertEquals("http://localhost:8082", wrapper.url());
        assertNotNull(wrapper.skills());
        assertEquals("sales-consulting", wrapper.skills().get(0).id());
    }

    /**
     * 验证两个专家 Agent 命名不重复（Consumer 通过名字区分路由目标）。
     */
    @Test
    void techAndSalesAgentNamesShouldBeDifferent() {
        String techName = EngineeringAgentCardSupport.buildTechSupportAgentCard("http://localhost:8081").name();
        String salesName = EngineeringAgentCardSupport.buildSalesAgentCard("http://localhost:8082").name();

        assertFalse(techName.equals(salesName));
    }

    /**
     * 验证响应提取器能从 Task.artifacts 中正确提取文本。
     */
    @Test
    void responseExtractorShouldExtractTextFromTaskArtifacts() {
        TextPart textPart = new TextPart("这是技术专家的回答：请检查 Bean 注入配置。");
        Artifact artifact = new Artifact("artifact-1", "answer", null, List.of(textPart), Map.of());
        Task task = new Task(
                "task-1",
                "context-1",
                new TaskStatus(TaskState.COMPLETED),
                List.of(artifact),
                null,
                Map.of());
        SendMessageResponse response = new SendMessageResponse("req-1", (EventKind) task);

        String text = A2aResponseExtractor.extractText(response);

        assertTrue(text.contains("技术专家的回答"));
        assertTrue(A2aResponseExtractor.isCompleted(response));
    }

    /**
     * 验证响应提取器在响应为 null 时返回空字符串，不抛出异常。
     */
    @Test
    void responseExtractorShouldReturnEmptyStringOnNullResponse() {
        assertEquals("", A2aResponseExtractor.extractText(null));
        assertFalse(A2aResponseExtractor.isCompleted(null));
    }

    /**
     * 验证错误响应场景下 isCompleted 返回 false。
     */
    @Test
    void responseExtractorShouldReturnNotCompletedOnErrorResponse() {
        SendMessageResponse errorResponse = new SendMessageResponse("req-1",
                new JSONRPCError(-32603, "internal error", null));

        assertFalse(A2aResponseExtractor.isCompleted(errorResponse));
        assertEquals("", A2aResponseExtractor.extractText(errorResponse));
    }
}
