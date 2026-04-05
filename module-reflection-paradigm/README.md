# module-reflection-paradigm

## 新手导航

如果你是第一次接触这个模块，建议先读：

- [Reflection范式新手导读](../docs/Reflection范式新手导读.md)

这篇导读会先把下面几件事讲清楚：

- Reflection 为什么不是“多问几次模型”
- 手写版为什么依赖 `AgentLlmGateway`
- 图编排版为什么依赖 `ChatModel`、`FlowAgent`、`StateGraph`、`OverAllState`
- Spring AI 和 Spring AI Alibaba 在这个例子里分别落在哪一层
- 真实 OpenAI Demo 需要哪些配置、怎么启动

README 负责给你一个模块级总览；导读负责把底层运行机制讲透。

## 模块定位

`module-reflection-paradigm` 用于承载“先产出初稿，再显式评审，再按反馈修订，最后决定是否停止”的 Reflection 范式。

它解决的不是“第一次完全不会做”的问题，而是这类更常见的场景：

- 第一次结果基本正确，但质量不够高
- 已经能运行，但复杂度太差
- 结构大体可用，但需要再做一轮严格审视

如果 ReAct 更关注“边想边做”，Plan-and-Solve 更关注“先拆计划再执行”，那么 Reflection 更关注：

**第一次做完以后，怎么再从评审者视角把质量往上推一轮。**

## 最佳实践场景

**高质量代码生成与算法调优**：先让生成者给出可运行初稿，再让评审者从时间复杂度和性能瓶颈角度挑错，最后推动系统把实现优化到更优解。

## 模块里的最小对照样例

本模块用一道素数题把 Reflection 钉成了可运行、可测试、可对照的样子：

> 编写一个 Java 方法，找出 1 到 n 之间所有的素数，并返回一个 `List<Integer>`。

这道题非常适合 Reflection：

- 初稿很容易写成暴力试除法
- reviewer 很容易从时间复杂度切入
- 优化方向会自然收敛到埃拉托斯特尼筛法
- “是否还要继续优化”也容易设计成显式停止条件

## 双实现总览

### 1. 手写版 Reflection runtime

核心类：

- `HandwrittenJavaCoder`
- `HandwrittenJavaReviewer`
- `ReflectionMemory`
- `ReflectionTurnRecord`
- `HandwrittenReflectionAgent`

代码入口：

- `src/main/java/com/xbk/agent/framework/reflection/application/executor/HandwrittenJavaCoder.java`
- `src/main/java/com/xbk/agent/framework/reflection/application/executor/HandwrittenJavaReviewer.java`
- `src/main/java/com/xbk/agent/framework/reflection/domain/memory/ReflectionMemory.java`
- `src/main/java/com/xbk/agent/framework/reflection/domain/memory/ReflectionTurnRecord.java`
- `src/main/java/com/xbk/agent/framework/reflection/application/coordinator/HandwrittenReflectionAgent.java`

这一版的关键点是：

- coder 和 reviewer 都通过 `AgentLlmGateway` 调模型
- prompt 由 Java 代码自己拼装
- 每轮结果由 `ReflectionMemory` 显式记录
- 停止条件由 `HandwrittenReflectionAgent` 在运行时检查
- 整个回环由 Java 控制流直接推进

它适合用来理解：

- Reflection 的最小 runtime 到底长什么样
- 为什么“生成”和“评审”要拆成两个角色
- 为什么停止条件必须由程序治理，而不能只靠 prompt 礼貌请求

### 2. Spring AI Alibaba 图编排版

核心类：

- `JavaCoderNode`
- `JavaReviewerNode`
- `AlibabaReflectionFlowAgent`

代码入口：

- `src/main/java/com/xbk/agent/framework/reflection/infrastructure/agentframework/node/JavaCoderNode.java`
- `src/main/java/com/xbk/agent/framework/reflection/infrastructure/agentframework/node/JavaReviewerNode.java`
- `src/main/java/com/xbk/agent/framework/reflection/infrastructure/agentframework/AlibabaReflectionFlowAgent.java`

这一版的关键点是：

- 直接使用 Spring AI `ChatModel`
- 用 Spring AI Alibaba `FlowAgent` 封装状态图运行时
- 节点只负责“读状态 -> 调模型 -> 写状态”
- 回环不再手写 `while`，而是通过条件边控制
- 最终运行事实通过 `OverAllState` 回放

图编排版里最关键的状态键有 3 个：

- `current_code`
- `review_feedback`
- `iteration_count`

这说明图编排版最核心的工程变化不是“代码更短”，而是：

**关键运行事实进入状态，而不是只藏在模型上下文里。**

## Spring AI / Spring AI Alibaba 在这里怎么落地

### 手写版：统一网关抽象，Spring AI 做底层适配

手写版业务代码依赖的是：

- `AgentLlmGateway`
- `LlmRequest`
- `LlmResponse`

但这不代表它脱离了 Spring AI。

真实接入链路是：

1. `framework-llm-autoconfigure` 读取统一 `llm.*` 配置
2. 根据 `llm.provider` 自动选择 provider adapter
3. `framework-llm-springai` 创建 Spring AI `ChatModel`
4. `SpringAiLlmClient` 把统一请求转换成 Spring AI `Prompt`
5. 最终由 `ChatModel.call(...)` 发起真实调用

所以手写版的定位是：

- 范式层：用统一网关抽象
- 模型接入层：仍然可以由 Spring AI 驱动

### 图编排版：直接围绕 `ChatModel + StateGraph` 组织运行时

图编排版更贴近 Spring AI Alibaba 的运行时模型：

- `AlibabaReflectionFlowAgent` 负责定义 flow、compile config 和条件边
- `JavaCoderNode` / `JavaReviewerNode` 实现 `AsyncNodeAction`
- `OverAllState` 持有当前整张图的共享事实
- 条件边根据 `review_feedback` 和 `iteration_count` 决定下一跳

当前实现里还保留了两个 `ReactAgent`：

- `java-reflection-coder-agent`
- `java-reflection-reviewer-agent`

它们主要承担角色元数据和行为约束，例如：

- agent 名称
- agent 描述
- `returnReasoningContents(false)`

而真正节点执行则显式写在 `JavaCoderNode` / `JavaReviewerNode` 里，目的是让“状态如何映射到 prompt”更直观。

## 与测试的对应关系

如果你想先看“这个模块到底保证了什么行为”，优先看：

- `src/test/java/com/xbk/agent/framework/reflection/ReflectionPrimeGenerationDemoTest.java`

这组测试同时钉住了两套实现：

- 手写版必须完成“初稿 -> 评审 -> 优化 -> 再评审”
- 图编排版必须通过条件边形成受控回环
- 最终都要从暴力思路收敛到更优实现
- reviewer 明确给出“无需改进”时，流程应停止

这也是这个模块最好的阅读入口。

## 真实 OpenAI Demo

当前模块除了离线测试，还保留了两套真实模型对照 Demo：

- `src/test/java/com/xbk/agent/framework/reflection/HandwrittenReflectionOpenAiDemo.java`
- `src/test/java/com/xbk/agent/framework/reflection/AlibabaReflectionFlowOpenAiDemo.java`

对应配置：

- `src/test/resources/application-openai-reflection-demo.yml`
- `src/test/resources/application-llm-local.yml.example`
- `src/test/resources/application-llm-local.yml`
- `src/test/resources/application-openai-reflection-demo-local.yml.example`
- `src/test/resources/application-openai-reflection-demo-local.yml`

运行真实 Demo 前，至少要准备两件事：

1. 在 `application-llm-local.yml` 里填入真实 `llm.*` 参数
2. 在 `application-openai-reflection-demo-local.yml` 里显式开启 `demo.reflection.openai.enabled=true`

默认情况下，真实 Demo 会被安全跳过，避免日常测试误打外网。

## 推荐阅读顺序

如果你想顺着源码快速吃透，推荐顺序如下：

1. 先看 `ReflectionPrimeGenerationDemoTest`
2. 再看手写版
   - `HandwrittenJavaCoder`
   - `HandwrittenJavaReviewer`
   - `ReflectionMemory`
   - `HandwrittenReflectionAgent`
3. 再看图编排版
   - `JavaCoderNode`
   - `JavaReviewerNode`
   - `AlibabaReflectionFlowAgent`
4. 最后看真实 Demo
   - `OpenAiReflectionDemoPropertySupport`
   - `OpenAiReflectionDemoTestConfig`
   - `HandwrittenReflectionOpenAiDemo`
   - `AlibabaReflectionFlowOpenAiDemo`

这样看，你会更容易把：

- 范式本体
- 统一 LLM 抽象
- Spring AI 模型接入
- Spring AI Alibaba 图编排

这 4 层真正串起来。

## 工程落地建议

### 1. reviewer 必须真的比 coder 更苛刻

如果两个角色的 prompt 太像，系统只是在重复生成，不是在做 Reflection。

### 2. 停止条件必须显式

Reflection 最大的风险不是不会优化，而是无限优化。
必须同时有业务停止条件和运行时保护条件。

### 3. 把关键事实写进 memory 或 state

不要把“当前代码”“评审意见”“轮次计数”全部藏进长 prompt。
一旦它们不能显式回放，调试和治理成本会迅速升高。

### 4. 把 Reflection 用在真正值得反思的问题上

Reflection 适合高价值、高质量要求的任务；
低价值任务一旦强行加 Reflection，往往只会增加成本和延迟。

## 结论

这个模块最重要的价值，不是多写了一套“评审后再改”的样例，而是把同一个 Reflection 闭环同时落成了两种 runtime：

- 手写版：帮助你理解范式本体
- 图编排版：帮助你理解工程化落地

把这两套代码对照着看，才能真正看清 Reflection 在这个仓库里的完整位置。
