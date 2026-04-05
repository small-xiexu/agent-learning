# module-plan-replan-paradigm

## 新手导航

如果你是第一次接触这个模块，建议先读：

- [Plan-and-Solve范式新手导读](../docs/Plan-and-Solve范式新手导读.md)

这篇导读会优先把下面几件事讲清楚：

- Plan-and-Solve 为什么不是“复杂版 ReAct”
- 手写版为什么依赖 `AgentLlmGateway` 和 `history`
- 框架版为什么依赖 `SequentialAgent`、`outputKey`、`OverAllState`
- Spring AI 和 Spring AI Alibaba 在这个模块里各自落在哪一层
- 真实 OpenAI Demo 需要哪些配置、怎么启动

README 负责给你模块级总览；导读负责把运行机制讲透。

## 模块定位

`module-plan-replan-paradigm` 用于承载“先规划、再执行”的复杂任务求解范式。

它适合的不是“当前一步怎么边想边做”，而是这类任务：

- 步骤长
- 前后依赖明显
- 错误成本高
- 需要先得到一份执行路线图

如果 ReAct 更关注动态工具调用，Reflection 更关注做完后的二次审查，那么 Plan-and-Solve 更关注：

**在行动之前，先把执行路线显式拆出来。**

## 最佳实践场景

**复杂数学求解、长链路推导、结构化研报生成**：先要求模型输出一份可执行计划，再让执行器按计划推进，而不是一开始就一路即兴。

## 模块里的最小对照样例

本模块用“买苹果问题”做最小对照：

> 一个水果店周一卖出了15个苹果。周二卖出的苹果数量是周一的两倍。周三卖出的数量比周二少了5个。请问这三天总共卖出了多少个苹果？

这道题看起来简单，但非常适合用来讲 Plan-and-Solve：

- 计划天然有顺序
- 每一步结果都会影响下一步
- 很适合观察手写 `history` 和状态键交接的差别

## 双实现总览

### 1. 手写版 Plan-and-Solve runtime

核心类：

- `HandwrittenPlanner`
- `HandwrittenExecutor`
- `HandwrittenPlanAndSolveAgent`
- `PlanStep`
- `StepExecutionRecord`

代码入口：

- `src/main/java/com/xbk/agent/framework/planreplan/application/executor/HandwrittenPlanner.java`
- `src/main/java/com/xbk/agent/framework/planreplan/application/executor/HandwrittenExecutor.java`
- `src/main/java/com/xbk/agent/framework/planreplan/application/coordinator/HandwrittenPlanAndSolveAgent.java`
- `src/main/java/com/xbk/agent/framework/planreplan/domain/plan/PlanStep.java`
- `src/main/java/com/xbk/agent/framework/planreplan/domain/execution/StepExecutionRecord.java`

这一版的关键点是：

- Planner 先生成步骤计划
- Executor 只负责执行当前步骤
- `history` 由 Java 代码显式累加
- `for` 循环由 Java 代码显式推进

它最适合用来理解：

- Plan-and-Solve 的最小 runtime 到底长什么样
- 为什么阶段执行要依赖计划和历史
- 为什么流程复杂后，协调器会越来越像调度器

### 2. Spring AI Alibaba 顺序编排版

核心类：

- `AlibabaSequentialPlanAndSolveAgent`
- `plannerAgent`
- `executorAgent`

代码入口：

- `src/main/java/com/xbk/agent/framework/planreplan/infrastructure/agentframework/AlibabaSequentialPlanAndSolveAgent.java`

这一版的关键点是：

- 通过 `ReactAgent` 声明 Planner 和 Executor 两个阶段
- 通过 `SequentialAgent` 顺序串联阶段
- 通过 `outputKey` 把阶段结果写入状态
- 通过 `instruction` 占位符读取上游阶段输出
- 最终通过 `OverAllState` 回放运行结果

## Spring AI / Spring AI Alibaba 在这里怎么落地

### 手写版：统一网关抽象，Spring AI 做底层适配

手写版业务代码依赖的是：

- `AgentLlmGateway`
- `LlmRequest`
- `LlmResponse`

真实模型接入仍然可以走统一链路：

1. `framework-llm-autoconfigure` 读取统一 `llm.*` 配置
2. 自动装配 `AgentLlmGateway`
3. `framework-llm-springai` 创建 Spring AI `ChatModel`
4. `SpringAiLlmClient` 把统一请求转成 Spring AI `Prompt`
5. 最终由 `ChatModel.call(...)` 发起真实调用

### 框架版：围绕 `SequentialAgent + OverAllState` 做阶段编排

框架版更贴近 Spring AI Alibaba 的企业级组织方式：

- `plannerAgent` 读取 `{input}` 并输出 `plan_result`
- `executorAgent` 读取 `{input}` 和 `{plan_result}` 并输出 `final_answer`
- `SequentialAgent` 负责按顺序推进这两个阶段
- `OverAllState` 负责持有整条链最终的状态快照

这说明框架版最重要的变化不是“少写几行代码”，而是：

**阶段之间开始靠状态协议交接，而不是靠手动拼字符串交接。**

## 与测试的对应关系

如果你想先看“这个模块到底保证了什么行为”，优先看：

- `src/test/java/com/xbk/agent/framework/planreplan/PlanAndSolveAppleProblemDemoTest.java`

这组测试同时钉住了：

- 手写版必须先规划，再按计划逐步执行
- 框架版必须通过 `outputKey` 状态流转完成同一问题
- `plan_result` 和 `final_answer` 必须真实进入状态

## 真实 OpenAI Demo

当前模块保留了两套真实模型 Demo：

- `src/test/java/com/xbk/agent/framework/planreplan/HandwrittenPlanAndSolveOpenAiDemo.java`
- `src/test/java/com/xbk/agent/framework/planreplan/AlibabaSequentialPlanAndSolveOpenAiDemo.java`

对应配置：

- `src/test/resources/application-openai-plan-solve-demo.yml`
- `src/test/resources/application-llm-local.yml.example`
- `src/test/resources/application-llm-local.yml`
- `src/test/resources/application-openai-plan-solve-demo-local.yml.example`
- `src/test/resources/application-openai-plan-solve-demo-local.yml`

运行真实 Demo 前至少要准备两件事：

1. 在 `application-llm-local.yml` 里填入真实 `llm.*` 参数
2. 在 `application-openai-plan-solve-demo-local.yml` 里显式开启 `demo.plan-solve.openai.enabled=true`

默认情况下，真实 Demo 会被安全跳过，避免日常测试误打外网。

## 推荐阅读顺序

如果你想顺着源码快速吃透，推荐顺序如下：

1. 先看 `PlanAndSolveAppleProblemDemoTest`
2. 再看手写版
   - `HandwrittenPlanner`
   - `HandwrittenExecutor`
   - `HandwrittenPlanAndSolveAgent`
3. 再看框架版
   - `AlibabaSequentialPlanAndSolveAgent`
4. 最后看真实 Demo 和配置支持类
   - `HandwrittenPlanAndSolveOpenAiDemo`
   - `AlibabaSequentialPlanAndSolveOpenAiDemo`
   - `OpenAiPlanSolveDemoPropertySupport`

## 工程落地建议

### 1. 计划必须真的可执行

如果 Planner 只输出空泛口号，Plan-and-Solve 就会退化成“先写一段总结，再继续瞎做”。

### 2. 阶段边界必须稳定

无论是手写 `history`，还是框架版 `outputKey`，都必须让阶段之间的输入输出关系足够清晰。

### 3. 不要把所有问题都硬套成 Plan-and-Solve

如果任务更像实时查工具，就该用 ReAct；
如果任务更像做完再审一次，就该用 Reflection。

### 4. 真正需要治理的是状态交接

Plan-and-Solve 的工程核心不是“先想一下”，而是“计划、历史和结果如何在阶段之间可靠传递”。

## 结论

这个模块最重要的价值，不是把买苹果问题做对，而是把同一个 Plan-and-Solve 闭环同时落成了两种 runtime：

- 手写版：帮助你理解范式本体
- 顺序编排版：帮助你理解工程化落地

把这两套实现对照着看，才能真正看清 Plan-and-Solve 在这个仓库里的完整位置。
