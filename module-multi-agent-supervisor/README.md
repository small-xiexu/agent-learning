# module-multi-agent-supervisor

## 新手导航

如果你是第一次接触这个模块，建议先读：

- [Supervisor范式从0到1掌握指南](../docs/Supervisor范式从0到1掌握指南.md)

这篇导读会优先把下面几件事讲清楚：

- Supervisor 为什么不是“复杂版顺序流水线”
- 手写版 Supervisor Loop 到底怎么维护全局控制权
- Spring AI Alibaba 里的 `SupervisorAgent`、`ReactAgent`、`OverAllState` 分别承担什么职责
- 本模块为什么要同时保留手写版和框架版
- 真实 OpenAI Demo 需要哪些配置、怎么启动

README 负责给你模块总览；专题导读负责把运行机制和框架实现过程讲透。

## 模块定位

`module-multi-agent-supervisor` 用于承载“中心化监督者”多智能体协作范式。

它解决的不是“多个 Agent 一起聊天”这么宽泛的问题，而是下面这些更具体的工程问题：

- 当任务包含多个步骤时，谁负责全局拆解与阶段性决策
- 子 Agent 做完之后，结果回收到哪里继续判断下一步
- 如何避免多 Agent 协作退化成失控的自由群聊

在 `spring-ai-agent-framework` 中，这个模块应被视为多智能体系统的“中控层”。
当任务需要统一拆解、动态分发、阶段性收敛和多轮路由时，优先考虑 Supervisor，而不是让多个 Agent 自由竞争控制权。

## 最佳实践场景

**企业级任务总调度**：由中心化 Supervisor 统筹全局，子专家处理完任务必须返回中心节点；状态流转、完成判断和终止权始终收拢在监督者手中。

## 模块里的最小对照样例

当前模块用一条很适合 Supervisor 的最小闭环来做双实现对照：

> 请以“Spring AI Alibaba的多智能体优势”为主题写一篇简短的博客，然后将其翻译成英文，最后对英文翻译进行语法和拼写审查。

这条任务之所以适合讲 Supervisor，是因为它天然满足三个条件：

- 有清晰的分工边界：写作、翻译、审校
- 每一阶段的结果都会影响下一阶段
- 最终必须由中心节点判断是否 `FINISH`

## 当前实现

模块里现在有两套正式实现，而且两套都统一走 `AgentLlmGateway`：

- `HandwrittenSupervisorCoordinator`
  手写版协调器，显式维护 Scratchpad、JSON 路由决策、Worker 分发和最大轮次控制。
- `AlibabaSupervisorFlowAgent`
  框架版协调器，使用 Spring AI Alibaba 原生 `SupervisorAgent + ReactAgent + OverAllState` 完成同一条多步监督闭环。

这两套实现解决的是同一件事，但学习重点不同：

- 手写版回答：Supervisor 的最小 runtime 本体到底是什么
- 框架版回答：Spring AI Alibaba 如何把中心化调度做成可执行的框架运行时

## 双实现总览

### 1. 手写版 Supervisor Loop

核心类：

- `HandwrittenSupervisorCoordinator`
- `WriterAgent`
- `TranslatorAgent`
- `ReviewerAgent`
- `SupervisorScratchpad`
- `SupervisorDecisionJsonParser`
- `SupervisorWorkflowState`

这一版的关键点是：

- `while` 循环由 Java 代码显式推进
- Supervisor 每轮都基于 Scratchpad 和当前状态重新做决策
- 子 Agent 完成任务后不会接管流程，而是把结果写回 Supervisor
- `CompletionPolicy` 显式限制最大调度轮次

### 2. Spring AI Alibaba 原生 Supervisor 版

核心类：

- `AlibabaSupervisorFlowAgent`
- `FrameworkSupervisorPromptTemplates`
- `SupervisorGatewayBackedChatModel`
- `SupervisorStateExtractor`
- `SupervisorStateKeys`

这一版的关键点是：

- 用 `ReactAgent` 声明 writer、translator、reviewer 三个专业 Worker
- 用 `SupervisorAgent` 负责中心化调度和多轮回环
- 用 `outputKey` 和 `OverAllState` 交接阶段状态
- 最终再把框架状态还原成统一的 `SupervisorRunResult`

## Spring AI / Spring AI Alibaba 在这里怎么落地

### 手写版：统一网关抽象，Java 代码自己维护调度循环

手写版业务代码依赖的是：

- `AgentLlmGateway`
- `LlmRequest`
- `LlmResponse`
- `Message`

也就是说，这一版研究的重点是 Supervisor 本身的控制流，而不是框架语法糖。

### 框架版：围绕 `SupervisorAgent + ReactAgent + OverAllState` 做中心化调度

框架版更贴近 Spring AI Alibaba 的企业级组织方式：

- `writer_agent` 负责输出中文博客
- `translator_agent` 负责输出英文译稿
- `reviewer_agent` 负责输出最终审校稿
- `supervisor_router_agent` 只负责判断下一跳或 `FINISH`
- `SupervisorAgent` 负责把这些角色真正编排成多轮动态路由闭环

这个模块最重要的学习价值，不是“少写几行代码”，而是看清楚：

**Supervisor 的控制流如何从手写 `while` 循环提升成框架可执行的原生运行时。**

## 与测试的对应关系

如果你想先看“这个模块到底保证了什么行为”，优先看：

- `src/test/java/com/xbk/agent/framework/supervisor/SupervisorPatternComparisonTest.java`

这组测试同时钉住了：

- 手写版必须完成 `WRITER -> TRANSLATOR -> REVIEWER -> FINISH` 闭环
- 框架版必须完成同一条路由轨迹
- 三阶段产物必须分别进入统一结果对象和框架状态

除此之外，模块还补了两类支撑测试：

- `OpenAiSupervisorDemoPropertySupportTest`
  验证真实 Demo 的配置读取、Key 判断和开关判断
- `OpenAiSupervisorDemoTestConfigTest`
  验证最小自动装配下可以得到 `AgentLlmGateway` 和 `ChatModel`

## 真实 OpenAI Demo

当前模块保留了两套真实模型 Demo：

- `src/test/java/com/xbk/agent/framework/supervisor/HandwrittenSupervisorOpenAiDemo.java`
- `src/test/java/com/xbk/agent/framework/supervisor/AlibabaSupervisorFlowOpenAiDemo.java`

对应配置：

- `src/test/resources/application-openai-supervisor-demo.yml`
- `src/test/resources/application-openai-supervisor-demo-local.yml.example`
- `src/test/resources/application-openai-supervisor-demo-local.yml`

运行真实 Demo 前至少要准备两件事：

1. 在本地配置文件里填入真实 `llm.api-key`
2. 显式开启 `demo.supervisor.openai.enabled=true`

默认情况下，真实 Demo 会被安全跳过，避免日常测试误打外网。

## 推荐阅读顺序

如果你想顺着源码快速吃透，推荐顺序如下：

1. 先看 `SupervisorPatternComparisonTest`
2. 再看手写版
   - `HandwrittenSupervisorCoordinator`
   - `SupervisorScratchpad`
   - `SupervisorDecisionJsonParser`
   - `WriterAgent / TranslatorAgent / ReviewerAgent`
3. 再看框架版
   - `AlibabaSupervisorFlowAgent`
   - `FrameworkSupervisorPromptTemplates`
   - `SupervisorGatewayBackedChatModel`
   - `SupervisorStateExtractor`
4. 最后看真实 Demo 和配置支撑类
   - `HandwrittenSupervisorOpenAiDemo`
   - `AlibabaSupervisorFlowOpenAiDemo`
   - `OpenAiSupervisorDemoPropertySupport`

## 工程落地建议

### 1. 监督者负责决策，不负责微观执行

如果 Supervisor 自己也做大量具体工作，它很快就会退化成一个“又要调度、又要干活”的上帝 Agent。

### 2. 子 Agent 职责必须正交

Supervisor 路由质量很大程度取决于子 Agent 的边界是否清楚。角色越模糊，监督者越容易反复试错。

### 3. `FINISH` 必须有清晰语义

`FINISH` 不应该是“看起来差不多了”，而应该对应明确的完成条件：

- 所有必要子任务已经完成
- 最终结果已经收敛
- 不再需要进一步路由

### 4. 必须限制最大循环次数

Supervisor 模式天然适合复杂多步任务，但也天然有回环膨胀风险。无论是手写版还是框架版，都应显式设置最大调度轮次和失败退出策略。

## 结论

`module-multi-agent-supervisor` 的价值，不是“再多加一个总控 Agent”，而是把多智能体系统里的全局治理能力显式工程化。

在本项目中，它承担的是“中心化编排与多轮受控收敛”的角色，是连接专业 Worker 与全局目标达成的关键枢纽。

## 延伸阅读

- 顶层导读：[`docs/项目整体架构导读.md`](../docs/项目整体架构导读.md)
- 专题导读：[`docs/Supervisor范式从0到1掌握指南.md`](../docs/Supervisor范式从0到1掌握指南.md)
