# module-reflection-paradigm

## 新手导航

如果你是第一次接触这个模块，建议先读：

- [Reflection范式新手导读](../docs/Reflection范式新手导读.md)

这篇导读会先用“素数生成代码优化”讲清：

- Reflection 到底在解决什么问题
- 手写版运行时和图编排版运行时有什么区别
- `ReflectionMemory` 和 `OverAllState` 分别承担什么角色
- `while` 循环回环与 `StateGraph` 条件边回环的工程差异是什么

看完导读，再回来看当前 README 和源码，会更容易把这两套实现对应起来。

## 模块定位

`module-reflection-paradigm` 用于承载“执行后自我批判，再继续修正”的反思型智能体范式。  
它的目标不是让 Agent 反复重写答案，而是让系统具备一种工程化的“二次审视能力”：先产出方案，再以评审视角检查漏洞，最后决定是否继续迭代。

如果 ReAct 解决的是“如何边做边想”，那么 Reflection 解决的是“做完之后如何挑错并优化”。

## 🎯 最佳实践场景

**高质量代码生成与算法调优**：执行者先写出暴力解法，评审者再从时间复杂度角度指出瓶颈，并推动生成者把实现优化到筛法级别。

## 素数生成双实现对照

为了把 Reflection 真正看懂，这个模块里落了一道最小对照样例：

> 编写一个 Java 方法，找出 1 到 n 之间所有的素数，并返回一个 `List<Integer>`。

两套实现最终都会把初稿从暴力试除法优化到埃拉托斯特尼筛法，但工程组织方式不同。

### 1. 纯手写运行时版

这套实现对应“Reflection 的原始 runtime 思路”。

核心类有：

- `HandwrittenJavaCoder`
- `HandwrittenJavaReviewer`
- `ReflectionMemory`
- `HandwrittenReflectionAgent`

对应代码入口：

- `src/main/java/com/xbk/agent/framework/reflection/application/executor/HandwrittenJavaCoder.java`
- `src/main/java/com/xbk/agent/framework/reflection/application/executor/HandwrittenJavaReviewer.java`
- `src/main/java/com/xbk/agent/framework/reflection/domain/memory/ReflectionMemory.java`
- `src/main/java/com/xbk/agent/framework/reflection/application/coordinator/HandwrittenReflectionAgent.java`

这一版的关键观察点是：

- 初稿和优化版 Prompt 都由 Java 代码自己拼
- 每轮评审结果由 `ReflectionMemory` 手动累加
- 停止条件由 `HandwrittenReflectionAgent` 自己判断
- 整个回环完全由 `while` 控制

换句话说，这一版里：

**模型只负责“生成”和“评审”，运行时控制权仍然在 Java 代码手里。**

### 2. Spring AI Alibaba 图编排版

这套实现对应“显式状态图 runtime 思路”。

核心类有：

- `JavaCoderNode`
- `JavaReviewerNode`
- `AlibabaReflectionFlowAgent`

对应代码入口：

- `src/main/java/com/xbk/agent/framework/reflection/infrastructure/agentframework/node/JavaCoderNode.java`
- `src/main/java/com/xbk/agent/framework/reflection/infrastructure/agentframework/node/JavaReviewerNode.java`
- `src/main/java/com/xbk/agent/framework/reflection/infrastructure/agentframework/AlibabaReflectionFlowAgent.java`

这一版的关键观察点是：

- `current_code`、`review_feedback`、`iteration_count` 都进入状态图
- 节点只负责“读状态 -> 调模型 -> 写状态”
- 是否继续优化不再由 `while` 写死，而是由条件边决定
- 运行结果最终以 `OverAllState` 快照形式回放

换句话说，这一版里：

**Java 代码主要在声明节点、状态键和路由规则，而不是亲自推进循环。**

### 3. `ReflectionMemory` 与 `OverAllState` 的工程差异

这是两套实现最值得对照看的地方。

#### 手写版：`ReflectionMemory` 是显式历史容器

在 `HandwrittenReflectionAgent` 中，每做完一轮评审，Java 代码都会：

1. 把当前代码和评审意见封装成 `ReflectionTurnRecord`
2. 追加到 `ReflectionMemory`
3. 下一轮再把旧代码和新反馈重新拼进 Prompt

这意味着：

- 控制力最强
- 每一轮上下文格式完全可控
- 非常适合理解 Reflection 的底层闭环
- 但流程复杂后，协调器会越来越像手写调度器

#### 图编排版：`OverAllState` 是状态图里的事实容器

在 `AlibabaReflectionFlowAgent` 中：

- `JavaCoderNode` 更新 `current_code`
- `JavaReviewerNode` 更新 `review_feedback` 和 `iteration_count`
- 条件边再根据这些状态判断下一步去哪里

这意味着：

- 阶段之间通过状态键交接，而不是靠手写字符串拼接
- 路由逻辑和业务节点天然分离
- 更适合扩展成更长的企业级编排链路

## 理论背景

很多复杂任务的失败，并不是因为模型第一次完全不会做，而是因为：

- 初稿存在结构性漏洞
- 论证不够严谨
- 工具调用链条虽然完成，但结果质量不够高
- 输出看起来合理，但没有经过批判性审视

Reflection 范式正是把“批判者”显式引入系统中。

它包含一个基本闭环：

1. 生成初稿
2. 站在严格评审者视角指出问题
3. 基于问题修订答案
4. 判断是否继续迭代或停止

这让 Agent 不再满足于“一次生成”，而是具备“有限次数的自我纠错”能力。

## 与 framework-core 的关系

Reflection 模块依赖 `framework-core` 的核心原因在于：  
反思并不是字符串拼接，而是结构化状态流转。

- `AgentLlmGateway`：驱动手写版生成者与评审者的统一调用
- `Message / Memory`：保存每一轮草稿、评审意见、修订结果
- 统一的 LLM 协议层：让手写版和 Spring AI 版都能围绕同一套调用约定组织代码

推荐实践是把“草稿”“评审意见”“修订版”“轮次计数”都作为显式状态，而不是隐藏在长 Prompt 中。

## 工程落地建议

### 1. 评审者必须比生成者更苛刻

如果两个角色的提示词太接近，系统只是在重复生成，而不是反思。

### 2. 停止条件必须显式

Reflection 很容易陷入“越修越多”的成本黑洞。  
必须设置上限，并允许在达到阈值后输出“当前最佳版本”。

### 3. 状态键名要稳定

如果图编排版里的 `current_code`、`review_feedback`、`iteration_count` 经常变化，后续节点和测试都会变得脆弱。

### 4. 把反思用于高价值场景

Reflection 适合高质量、高风险输出，不适合所有日常问答。  
低价值任务引入反思，只会徒增成本与延迟。
