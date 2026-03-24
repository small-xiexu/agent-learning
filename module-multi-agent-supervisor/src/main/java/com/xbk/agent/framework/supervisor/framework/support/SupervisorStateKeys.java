package com.xbk.agent.framework.supervisor.framework.support;

/**
 * Supervisor 框架版状态键
 *
 * 职责：统一声明 Writer、Translator、Reviewer 与 Supervisor 路由阶段使用的状态 key。
 * 这些 key 同时被 Prompt 模板、状态提取器和框架运行时消费，属于框架版最核心的协议常量。
 *
 * @author xiexu
 */
public final class SupervisorStateKeys {

    public static final String WRITER_OUTPUT = "writer_output";
    public static final String TRANSLATOR_OUTPUT = "translator_output";
    public static final String REVIEWER_OUTPUT = "reviewer_output";
    public static final String MESSAGES = "messages";
    public static final String INPUT = "input";

    private SupervisorStateKeys() {
    }
}
