package com.xbk.agent.framework.reflection.domain.memory;

/**
 * Reflection 单轮记录
 *
 * 职责：将一轮迭代中的"代码产出"与"评审反馈"绑定在一起，
 * 形成可追溯的"执行 → 反思"快照，是 ReflectionMemory 历史列表的基本单元。
 *
 * <p>与 ReflectionMemory 的分工：
 * <pre>
 *   ReflectionTurnRecord  → 单轮快照：这一轮写了什么代码、收到了什么反馈
 *   ReflectionMemory      → 全轮容器：所有轮次的快照列表，支持完整历史回放
 * </pre>
 * 两者的关系类比于"单条日志行"与"日志文件"：
 * ReflectionTurnRecord 是不可变的单条记录，ReflectionMemory 负责累积管理。
 *
 * <p>设计为 final + 全字段 final：
 * 反思记录一旦产生就代表那一轮的客观事实，不允许事后修改，
 * 保证历史轨迹的可信度，也便于测试断言逐轮验证改进效果。
 *
 * <p>字段命名说明：
 * <ul>
 *   <li>{@code execution} — 这一轮 Coder 产出的完整代码（"执行产物"），
 *       不叫 {@code code} 是为了保持对 Reflection 范式语义的通用性，
 *       未来不局限于代码场景也能复用。</li>
 *   <li>{@code reflection} — 这一轮 Reviewer 给出的评审意见（"反思结果"），
 *       是下一轮 Coder 改进的直接依据。</li>
 * </ul>
 *
 * @author xiexu
 */
public final class ReflectionTurnRecord {

    /**
     * 本轮 Coder 产出的完整代码内容，对应 Reflection 范式中的"执行"阶段。
     */
    private final String execution;

    /**
     * 本轮 Reviewer 给出的评审反馈，对应 Reflection 范式中的"反思"阶段。
     * 若包含"无需改进"则代表本轮已收敛，HandwrittenReflectionAgent 将在此处终止迭代。
     */
    private final String reflection;

    /**
     * 创建单轮记录。
     *
     * @param execution  本轮代码内容
     * @param reflection 本轮评审反馈
     */
    public ReflectionTurnRecord(String execution, String reflection) {
        this.execution = execution;
        this.reflection = reflection;
    }

    /**
     * 返回本轮执行产出（代码内容）。
     *
     * @return 代码内容
     */
    public String getExecution() {
        return execution;
    }

    /**
     * 返回本轮反思结果（评审反馈）。
     *
     * @return 评审反馈
     */
    public String getReflection() {
        return reflection;
    }
}
