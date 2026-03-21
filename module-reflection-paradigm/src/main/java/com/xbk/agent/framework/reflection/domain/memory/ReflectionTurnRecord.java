package com.xbk.agent.framework.reflection.domain.memory;

/**
 * Reflection 单轮记录
 *
 * 职责：保存某一轮代码执行结果与对应的评审反馈
 *
 * @author xiexu
 */
public final class ReflectionTurnRecord {

    private final String execution;
    private final String reflection;

    /**
     * 创建单轮记录。
     *
     * @param execution 当前轮代码内容
     * @param reflection 当前轮评审反馈
     */
    public ReflectionTurnRecord(String execution, String reflection) {
        this.execution = execution;
        this.reflection = reflection;
    }

    public String getExecution() {
        return execution;
    }

    public String getReflection() {
        return reflection;
    }
}
