package com.xbk.agent.framework.graphflow.common.tool;

/**
 * 模拟搜索工具
 *
 * 职责：为双版本实现提供统一的搜索工具桩，避免真实网络调用影响演示与测试。
 * 可通过构造器注入 shouldFail=true 来模拟搜索失败，触发 FallbackNode 分支。
 *
 * @author xiexu
 */
public class MockSearchTool {

    private final boolean shouldFail;

    /**
     * 创建默认（成功）搜索工具。
     */
    public MockSearchTool() {
        this(false);
    }

    /**
     * 创建可控失败模式的搜索工具。
     *
     * @param shouldFail true 表示每次搜索都会抛出异常，用于测试 FallbackNode 分支
     */
    public MockSearchTool(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    /**
     * 执行模拟搜索，返回与关键词匹配的固定结果。
     *
     * @param query 搜索关键词
     * @return 模拟搜索结果文本
     * @throws RuntimeException shouldFail=true 时抛出，模拟搜索服务不可用
     */
    public String search(String query) {
        if (shouldFail) {
            throw new RuntimeException("模拟搜索服务不可用：连接超时");
        }
        // 根据关键词返回固定的演示结果，供 AnswerNode 进行总结回答
        return "【模拟搜索结果】关键词：" + query + "\n"
                + "- 相关资料1：Spring AI 是 Spring 官方提供的 AI 集成框架，支持多种大模型。\n"
                + "- 相关资料2：LangGraph 是一个基于有向图的 Agent 编排框架，支持状态机工作流。\n"
                + "- 相关资料3：智能体（Agent）通过工具调用与外部系统交互，实现复杂任务自动化。";
    }
}
