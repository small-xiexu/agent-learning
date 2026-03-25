# Spring AI Alibaba 框架核心组件指南

> 适用版本：`spring-ai-alibaba-agent-framework 1.1.2.0`
> 目标读者：有 Java / Spring Boot 经验、正在学习 Agent 应用开发的工程师。
> 本文档覆盖项目中所有框架版实现所依赖的核心组件，可当参考手册随时查阅。

---

## 目录

1. [整体组件关系图](#1-整体组件关系图)
2. [ReactAgent：最核心的执行单元](#2-reactagent最核心的执行单元)
3. [SupervisorAgent：中心化调度容器](#3-supervisoragent中心化调度容器)
4. [OverAllState：所有 Agent 的共享状态板](#4-overallstate所有-agent-的共享状态板)
5. [StateGraph：用状态图描述复杂流程](#5-stategraph用状态图描述复杂流程)
6. [@Tool + methodTools：给 Agent 绑定工具](#6-tool--methodtools给-agent-绑定工具)
7. [Saver：状态持久化与线程隔离](#7-saver状态持久化与线程隔离)
8. [CompileConfig：图编译参数](#8-compileconfig图编译参数)
9. [Agent 的三种调用方式](#9-agent-的三种调用方式)

---

## 1. 整体组件关系图

在动手看每个组件之前，先建立全局视角：

```
你的业务代码
    ↓ 调用
ReactAgent / SupervisorAgent    ← 执行单元，负责驱动 LLM 推理和工具调用
    ↓ 读写
OverAllState                    ← 全局状态容器，所有 Agent 共享的"黑板"
    ↓ 基于
StateGraph                      ← 定义节点、边、条件路由的图结构
    ↓ 依赖
ChatModel ← SupervisorGatewayBackedChatModel ← AgentLlmGateway
                                ← 统一模型门面，屏蔽底层 SDK 差异
    ↓ 配置
CompileConfig / Saver           ← 控制运行时行为（递归上限、状态持久化）
```

对 Java 工程师来说，最直观的类比是：

| 框架概念 | Java 类比 |
| --- | --- |
| `ReactAgent` | 一个带有"自主决策"能力的 Service Bean |
| `OverAllState` | 一次请求中所有 Service 共享的 ThreadLocal Map |
| `StateGraph` | 带条件分支的流程编排定义（类似 Spring Batch 的 Job） |
| `CompileConfig` | 线程池配置 + 超时参数 |
| `Saver` | Session 持久化（MemorySaver ≈ HttpSession，RedisS aver ≈ Redis Session） |

---

## 2. ReactAgent：最核心的执行单元

`ReactAgent` 是框架里最常用的 Agent 类型，几乎所有框架版实现都围绕它展开。它的内部是一个 **ReAct 循环**（推理 → 调工具 → 观察 → 再推理），直到 LLM 判断任务完成为止。

### 2.1 Builder 全属性一览

```java
ReactAgent agent = ReactAgent.builder()
    .name("writer_agent")                          // 必填
    .description("撰写中文简短博客")                 // 必填
    .model(chatModel)                              // 必填
    .systemPrompt("你是 writer_agent，只输出正文...") // 可选
    .instruction("请基于原始任务写作：{input}")        // 可选，支持占位符
    .outputKey("writer_output")                    // 可选
    .methodTools(new TravelTools())                // 可选，绑定工具
    .saver(new MemorySaver())                      // 可选，状态持久化
    .includeContents(false)                        // 可选，默认 true
    .returnReasoningContents(false)                // 可选，默认 false
    .build();
```

| 属性 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `name` | String | 是 | Agent 唯一标识，用于日志、调试、框架内路由 |
| `description` | String | 是 | 职责描述，Supervisor 靠这个决定"该派谁" |
| `model` | ChatModel | 是 | 底层模型实例，项目里通常用 `*GatewayBackedChatModel` 适配统一网关 |
| `systemPrompt` | String | 否 | 角色设定，固定不变，详见 2.5 节 |
| `instruction` | String | 否 | 任务指令，支持 `{key}` 占位符动态替换，详见 2.6 节 |
| `outputKey` | String | 否 | Agent 输出写入 `OverAllState` 的键名，详见 2.7 节 |
| `methodTools` | Object | 否 | 绑定带 `@Tool` 注解的 Java 对象，详见第 6 节 |
| `saver` | Saver | 否 | 状态持久化实现，详见第 7 节 |
| `includeContents` | boolean | 否 | 是否继承父流程的消息历史，默认 `true`；设为 `false` 可避免上下文污染，详见 2.10 节 |
| `returnReasoningContents` | boolean | 否 | 是否返回 LLM 的思维链过程，生产环境通常关闭，详见 2.11 节 |

---

### 2.2 `name`：Agent 在图里的唯一标识

#### 作用

`name` 是 Agent 在框架运行时的唯一名字。它不是单纯给人看的注释，而是会参与日志打印、调试定位、框架内部节点命名，以及某些场景下的路由匹配。

如果把 `ReactAgent` 类比成 Spring 容器里的 Bean，`name` 就像这个 Bean 在图运行时的逻辑名称。

#### 默认行为与设置建议

- `name` 是必填字段，不设置通常无法完成 Builder 构建
- 同一张图里不要给两个 Agent 配相同的 `name`
- 建议使用稳定、能表达职责的名字，而不是 `agent1`、`workerA` 这类临时命名

#### 本项目示例

Supervisor 模块里四个 Agent 的名字都直接表达职责：

```java
.name("writer_agent")
.name("translator_agent")
.name("reviewer_agent")
.name("supervisor_router_agent")
```

这些名字定义在 [AlibabaSupervisorFlowAgent.java](/Users/xiexu/xiaofu/agent-learning/module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/framework/agent/AlibabaSupervisorFlowAgent.java)。

#### 扩展示例

如果你在做客服场景，可以这样命名：

```java
ReactAgent classifier = ReactAgent.builder()
    .name("ticket_classifier_agent")
    .description("负责识别工单类别")
    .model(chatModel)
    .build();
```

#### 常见误解

- `name` 不是写给 LLM 的提示词，LLM 真正看到的角色语义主要来自 `description`、`systemPrompt`、`instruction`
- `name` 也不等于状态键，状态键由 `outputKey` 决定

---

### 2.3 `description`：给路由器和人类看的职责说明

#### 作用

`description` 用来描述这个 Agent 的职责边界。对于单 Agent 场景，它主要提高可读性；对于 Supervisor 这类中心化调度场景，它还会直接影响主路由 Agent 对“该派谁”的理解。

你可以把它理解成“岗位说明书”。

#### 默认行为与设置建议

- `description` 是必填字段
- 建议用一句话说清“这个 Agent 负责什么，不负责什么”
- 描述应该聚焦职责，不要把整段执行步骤都塞进去

#### 本项目示例

Supervisor 的翻译 Agent 和审校 Agent 分工非常明确：

```java
.description("把中文博客翻译成英文")
.description("对英文译文做语法和拼写审校")
```

对应代码见 [AlibabaSupervisorFlowAgent.java](/Users/xiexu/xiaofu/agent-learning/module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/framework/agent/AlibabaSupervisorFlowAgent.java)。

#### 扩展示例

如果你希望一个 Agent 只做 SQL 审核而不执行 SQL，可以这样写：

```java
.description("只负责审查 SQL 风险，不执行任何数据库写操作")
```

#### 常见误解

- `description` 不是普通注释，在多 Agent 协作里它往往是路由语义的一部分
- `description` 也不能替代 `systemPrompt`，前者是概括职责，后者是细化行为规则

---

### 2.4 `model`：这个 Agent 到底调用哪个大模型

#### 作用

`model` 决定 `ReactAgent` 背后的模型节点最终调用哪个 `ChatModel`。这是 Agent 触达大模型的直接入口。

从框架视角看，`ReactAgent` 不直接依赖你的 `AgentLlmGateway`，而是要求注入 `ChatModel`。因此项目里普遍多做了一层适配器：把统一的 `AgentLlmGateway` 包装成框架要求的 `ChatModel`。

#### 默认行为与设置建议

- `model` 是必填字段
- 同一个流程里多个 Agent 通常可以共享同一个 `ChatModel`
- 如果你希望不同 Agent 使用不同模型，也可以分别注入不同 `ChatModel`

#### 本项目示例

Supervisor 模块通过 `SupervisorGatewayBackedChatModel` 把统一网关适配成 `ChatModel`：

```java
this(agentLlmGateway, new SupervisorGatewayBackedChatModel(agentLlmGateway), maxRounds);
```

然后再把这个 `chatModel` 注入每个 `ReactAgent`：

```java
.model(chatModel)
```

适配器实现见 [SupervisorGatewayBackedChatModel.java](/Users/xiexu/xiaofu/agent-learning/module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/framework/support/SupervisorGatewayBackedChatModel.java)。

Conversation 和 CAMEL 模块也采用了同样思路，分别对应：

- [ConversationGatewayBackedChatModel.java](/Users/xiexu/xiaofu/agent-learning/module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/support/ConversationGatewayBackedChatModel.java)
- [CamelGatewayBackedChatModel.java](/Users/xiexu/xiaofu/agent-learning/module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/roleplay/support/CamelGatewayBackedChatModel.java)

#### 扩展示例

如果你想让“规划 Agent”用更强模型、“执行 Agent”用更便宜模型，可以这样拆：

```java
ReactAgent planner = ReactAgent.builder()
    .name("planner")
    .description("负责复杂规划")
    .model(expensiveChatModel)
    .build();

ReactAgent executor = ReactAgent.builder()
    .name("executor")
    .description("负责按计划执行")
    .model(cheapChatModel)
    .build();
```

#### 常见误解

- `model` 不是模型名称字符串，而是已经构造好的 `ChatModel` 实例
- 在本项目里，不建议让业务模块直接依赖厂商 SDK，而是应继续通过 `AgentLlmGateway -> *GatewayBackedChatModel` 这条边界接入

---

### 2.5 `systemPrompt`：长期稳定的角色设定

#### 作用

`systemPrompt` 用来告诉 LLM：“你是谁，你长期遵循什么规则。”
它对应的是 system 角色消息，适合放角色身份、输出边界、行为禁令等“每次调用都不变”的内容。

可以把它理解成“入职培训手册”。

#### 默认行为与设置建议

- 可选字段
- 适合放固定规则，不适合放每轮都变化的任务上下文
- 如果 Agent 的行为边界很明确，建议显式设置

#### 本项目示例

Supervisor 的主路由 Agent 把固定规则全部写在 `systemPrompt` 里，例如“你只负责决定下一跳”“必须返回 JSON 数组”等。具体模板见：
[FrameworkSupervisorPromptTemplates.java](/Users/xiexu/xiaofu/agent-learning/module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/framework/prompt/FrameworkSupervisorPromptTemplates.java)

注入位置见：
[AlibabaSupervisorFlowAgent.java](/Users/xiexu/xiaofu/agent-learning/module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/framework/agent/AlibabaSupervisorFlowAgent.java)

#### 扩展示例

```java
.systemPrompt("""
    你是一名资深 SQL 审核员。
    你只能指出风险和修改建议，不允许生成执行结果伪造数据。
    输出必须使用中文项目符号列表。
    """)
```

#### 与 `instruction` 的区别

| 属性 | 发给 LLM 的消息类型 | 支持 `{}` 占位符 | 每轮变化 |
| --- | --- | --- | --- |
| `systemPrompt` | `SystemMessage`（system 角色） | 否 | 不变 |
| `instruction` | `UserMessage`（user 角色） | 是 | 每轮动态渲染 |

#### 常见误解

- 不要把动态状态塞进 `systemPrompt`
- 不要用它替代 `description`，两者面向的对象不同：前者面向 LLM，后者更多面向框架语义和人类理解

---

### 2.6 `instruction`：每一轮执行时的任务单

#### 作用

`instruction` 是发给 LLM 的当前任务说明。它通常会结合 `OverAllState` 里的状态键，通过 `{key}` 占位符动态渲染成每轮不同的用户消息。

如果说 `systemPrompt` 是“长期规则”，那 `instruction` 就是“这一次具体要做什么”。

#### 默认行为与设置建议

- 可选字段
- 适合放动态任务、上游输出、当前状态快照
- 如果 Agent 需要读取上游结果，通常就应该显式设置 `instruction`

#### 本项目示例

Supervisor 路由 Agent 的 `instruction` 会读取 `writer_output`、`translator_output`、`reviewer_output` 来判断下一跳，模板见：
[FrameworkSupervisorPromptTemplates.java](/Users/xiexu/xiaofu/agent-learning/module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/framework/prompt/FrameworkSupervisorPromptTemplates.java)

Reflection 模块里的 coder / reviewer 也都是通过 `instruction` 读取 `current_code`、`review_feedback`：
[AlibabaReflectionFlowAgent.java](/Users/xiexu/xiaofu/agent-learning/module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/infrastructure/agentframework/AlibabaReflectionFlowAgent.java)

#### 占位符替换原理

很多人好奇 `{writer_output}` 是怎么被替换成实际值的，背后的链路如下：

**第一步**：框架把 `instruction` 字符串包装成 `AgentInstructionMessage`，标记为“未渲染”。

**第二步**：Agent 执行前，`AgentLlmNode` 扫描消息列表，找到未渲染的指令消息，并把 `OverAllState.data()` 作为参数传给模板渲染器。

```java
if (message instanceof AgentInstructionMessage && !instructionMessage.isRendered()) {
    String rendered = renderPromptTemplate(
        instructionMessage.getText(),
        state.data()
    );
}
```

**第三步**：模板渲染完成后，真正发给 LLM 的是替换后的 `UserMessage`。

#### 扩展示例

```java
.instruction("""
    原始问题：{input}

    分类结果：{ticket_type}

    请只输出一条给客服主管的转派建议。
    """)
```

#### 常见误解

- 占位符名字必须和状态键完全一致，否则会渲染为空
- `instruction` 不是“越长越好”，它应该只保留这一轮真正需要的上下文

---

### 2.7 `outputKey`：Agent 输出写回状态的协议

#### 作用

`outputKey` 决定 Agent 执行完成后，输出内容写到 `OverAllState` 的哪个键下。
它是多 Agent 串联时最关键的协议之一，因为下游 Agent 往往就是通过 `{outputKey}` 对应的键来读取上游结果。

#### 默认行为与设置建议

- 可选字段
- 单 Agent 场景不一定必须配置
- 只要存在“上游写、下游读”的状态交接，就强烈建议显式设置

#### 本项目示例

Supervisor 三个 Worker 的状态交接链非常典型：

```java
writerAgent      -> outputKey("writer_output")
translatorAgent  -> outputKey("translator_output")
reviewerAgent    -> outputKey("reviewer_output")
```

随后路由 Agent 的 `instruction` 又会读取这些键来判断下一跳。相关代码见：

- [AlibabaSupervisorFlowAgent.java](/Users/xiexu/xiaofu/agent-learning/module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/framework/agent/AlibabaSupervisorFlowAgent.java)
- [FrameworkSupervisorPromptTemplates.java](/Users/xiexu/xiaofu/agent-learning/module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/framework/prompt/FrameworkSupervisorPromptTemplates.java)

Plan-and-Solve 模块也用同样方式把 `plan_result` 传给执行器：
[AlibabaSequentialPlanAndSolveAgent.java](/Users/xiexu/xiaofu/agent-learning/module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/infrastructure/agentframework/AlibabaSequentialPlanAndSolveAgent.java)

#### 数据流示意

```
writerAgent 执行完
    → 输出写入 OverAllState["writer_output"]
        → translatorAgent 的 instruction 里 {writer_output} 被替换为实际内容
            → translatorAgent 执行完后写入 OverAllState["translator_output"]
```

#### 扩展示例

```java
ReactAgent classifier = ReactAgent.builder()
    .name("classifier")
    .description("负责给工单分类")
    .model(chatModel)
    .outputKey("ticket_type")
    .build();
```

#### 常见误解

- `outputKey` 不是日志字段，它是状态协议字段
- `outputKey` 不要求和 `name` 一致，二者职责完全不同

---

### 2.8 `methodTools`：把 Java 方法注册成可调用工具

#### 作用

`methodTools` 用来绑定带 `@Tool` 注解的 Java 对象，让 LLM 在推理过程中可以主动调用这些方法。
这会把 Agent 从“只会生成文本”升级成“会调用工具执行动作”。

#### 默认行为与设置建议

- 可选字段
- 如果 Agent 只做纯文本推理，可以不配
- 只要需要查天气、调接口、执行计算等外部动作，就应该考虑配置

#### 本项目示例

ReAct Demo 里直接把 `TravelTools` 绑定到 `ReactAgent`：

```java
.methodTools(new TravelTools())
```

对应代码见 [SpringAIReActTravelDemo.java](/Users/xiexu/xiaofu/agent-learning/module-react-paradigm/src/test/java/com/xbk/agent/framework/react/SpringAIReActTravelDemo.java)。
工具定义与调用细节详见第 6 节。

#### 扩展示例

```java
.methodTools(new TicketQueryTools(), new CrmWriteTools())
```

这表示同一个 Agent 既能查工单，也能回写 CRM。

#### 常见误解

- `methodTools` 绑定的是对象，不是字符串工具名
- LLM 能不能正确调用工具，很大程度取决于 `@Tool.description` 是否清晰

---

### 2.9 `saver`：给单个 Agent 配状态持久化

#### 作用

`saver` 负责保存这个 Agent 执行过程中的状态快照，并按 `threadId` 隔离不同调用。
在教学场景里，它最常见的价值是让你观察完整消息历史；在生产场景里，它还能用于恢复执行、跨请求续跑。

#### 默认行为与设置建议

- 可选字段
- 单独给 `ReactAgent` 配 Saver 时，通常是最直接的接法
- 如果是 `SupervisorAgent` 这类更高层容器，也可以改用 `CompileConfig.saverConfig(...)` 统一注册

#### 本项目示例

ReAct 官方 Demo 里直接用了内存版 `MemorySaver`：

```java
.saver(new MemorySaver())
```

对应代码见：

- [SpringAIReActTravelDemo.java](/Users/xiexu/xiaofu/agent-learning/module-react-paradigm/src/test/java/com/xbk/agent/framework/react/SpringAIReActTravelDemo.java)
- [SpringAIReActTravelOpenAiDemo.java](/Users/xiexu/xiaofu/agent-learning/module-react-paradigm/src/test/java/com/xbk/agent/framework/react/SpringAIReActTravelOpenAiDemo.java)

#### 扩展示例

```java
ReactAgent agent = ReactAgent.builder()
    .name("qa-agent")
    .description("支持多轮问答")
    .model(chatModel)
    .saver(new MemorySaver())
    .build();
```

#### 常见误解

- `saver` 不是业务记忆系统，它保存的是图运行时状态快照
- 不配置 `saver` 不代表 Agent 不能运行，只是你失去了显式状态持久化能力

---

### 2.10 `includeContents`：是否继承父流程已有消息历史

#### 作用

`includeContents` 决定当前 Agent 执行时，是否把父流程已经积累的消息历史一并带进去。
它主要影响“这个 Agent 看到多少上下文”。

默认值是 `true`。也就是说，如果你不关掉它，Agent 可能会继承上游的整段消息链。

#### 默认行为与设置建议

- 默认 `true`
- 当你希望 Agent 只看“当前状态键 + 当前指令”时，建议设为 `false`
- 多 Agent 协作里，如果上下文太长或角色太容易被污染，通常应该显式关闭

#### 本项目示例

Plan-and-Solve 里，Planner 和 Executor 都显式设置了：

```java
.includeContents(false)
```

原因在代码注释里写得很清楚：不希望继承父流程无关消息，避免任务污染。对应代码见：
[AlibabaSequentialPlanAndSolveAgent.java](/Users/xiexu/xiaofu/agent-learning/module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/infrastructure/agentframework/AlibabaSequentialPlanAndSolveAgent.java)

Conversation 与 Reflection 模块也大量使用了同样配置，说明这在本项目里是主流选择。

#### 扩展示例

如果你在做会议纪要补全，希望 Agent 基于完整聊天记录继续总结，就可以保留默认值：

```java
ReactAgent summarizer = ReactAgent.builder()
    .name("meeting-summarizer")
    .description("基于完整对话历史生成纪要")
    .model(chatModel)
    .includeContents(true)
    .build();
```

#### 常见误解

- `includeContents(false)` 不是让 Agent 看不到状态键；它只是关闭“父流程消息历史继承”
- 关闭后，`instruction` 中通过 `{key}` 引入的状态仍然可以正常读取

---

### 2.11 `returnReasoningContents`：是否把中间思维链回传到状态

#### 作用

`returnReasoningContents` 用来控制 Agent 是否把模型返回的 reasoning 内容一起暴露给上层流程。
它主要影响“父流程能不能看到模型的中间思考过程”。

#### 默认行为与设置建议

- 可选字段，通常建议显式设为 `false`
- 教学、实验、可观测性调试场景可以考虑打开
- 企业生产场景一般关闭，避免无谓泄露隐藏推理过程或放大上下文体积

#### 本项目示例

这个项目的框架版实现基本都显式关闭了它，例如：

```java
.returnReasoningContents(false)
```

你可以在这些类里看到：

- [AlibabaSupervisorFlowAgent.java](/Users/xiexu/xiaofu/agent-learning/module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/framework/agent/AlibabaSupervisorFlowAgent.java)
- [AlibabaSequentialPlanAndSolveAgent.java](/Users/xiexu/xiaofu/agent-learning/module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/infrastructure/agentframework/AlibabaSequentialPlanAndSolveAgent.java)
- [AlibabaReflectionFlowAgent.java](/Users/xiexu/xiaofu/agent-learning/module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/infrastructure/agentframework/AlibabaReflectionFlowAgent.java)
- [AlibabaConversationFlowAgent.java](/Users/xiexu/xiaofu/agent-learning/module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/infrastructure/agentframework/AlibabaConversationFlowAgent.java)

#### 扩展示例

如果你要做一个教学版 Demo，专门观察模型推理过程，可以这样开：

```java
ReactAgent tutorAgent = ReactAgent.builder()
    .name("math-tutor")
    .description("展示解题思路")
    .model(chatModel)
    .returnReasoningContents(true)
    .build();
```

#### 常见误解

- 打开它不等于“模型一定会稳定返回完整思维链”，这还取决于底层模型能力和返回协议
- 这个字段主要是可观测性开关，不应被当作业务输出主渠道

---

## 3. SupervisorAgent：中心化调度容器

`SupervisorAgent` 是 Supervisor 范式的框架原生实现，它本身不做业务决策，而是驱动一个循环：每轮询问 `mainRoutingAgent` 下一步找谁，再调度对应的 Worker，直到收到 `FINISH` 指令。

> 详细的 `SupervisorAgent` vs `mainRoutingAgent` 分工解释，参见 `module-multi-agent-supervisor` 的 README。

### 3.1 Builder 全属性

```java
SupervisorAgent supervisorAgent = SupervisorAgent.builder()
    .name("blog_supervisor_agent")              // 必填
    .description("负责动态选择子 Agent 并 FINISH") // 必填
    .mainAgent(mainRoutingAgent)                // 必填，决策大脑
    .subAgents(List.of(                         // 必填，可调度的 Worker 列表
        writerAgent, translatorAgent, reviewerAgent
    ))
    .compileConfig(CompileConfig.builder()      // 可选，详见第 8 节
        .recursionLimit(32)
        .build())
    .build();
```

| 属性 | 必填 | 说明 |
| --- | --- | --- |
| `name` | 是 | 唯一标识 |
| `description` | 是 | 职责描述 |
| `mainAgent` | **是** | 每轮判断下一跳的路由 Agent，不设置会抛 `IllegalArgumentException` |
| `subAgents` | **是** | Worker 列表，`mainAgent` 返回的名字必须能在这里匹配到 |
| `compileConfig` | 否 | 递归上限等运行时配置 |

### 3.2 运行时内部循环

```
supervisorAgent.invoke(task)
    ↓
Loop:
    mainRoutingAgent 读取 OverAllState，返回 ["writer_agent"] 或 ["FINISH"]
    ↓
    如果是 FINISH → 结束循环，返回 OverAllState
    如果是 Agent 名 → 调度对应 Worker 执行
    Worker 执行完 → 把输出写入 OverAllState[outputKey]
    ↓
    回到 Loop 顶部
```

---

## 4. OverAllState：所有 Agent 的共享状态板

`OverAllState` 是整个图执行过程中的**全局状态容器**，本质上是一个有合并策略的 `Map<String, Object>`。所有 Agent 的输入来自它，所有 Agent 的输出也写回它。

> 类比：你可以把它理解为一次多 Agent 协作过程中共享的"白板"，任何 Agent 都能在上面读写。

### 4.1 核心 API

| 方法 | 签名 | 用途 |
| --- | --- | --- |
| `value(key)` | `Optional<Object> value(String key)` | 读取某个键的值，返回 Optional |
| `value(key, class)` | `<T> Optional<T> value(String key, Class<T> type)` | 读取并转换类型 |
| `value(key, defaultVal)` | `<T> T value(String key, T defaultValue)` | 读取，不存在时返回默认值 |
| `data()` | `Map<String, Object> data()` | 获取完整状态 Map 的副本（只读） |

### 4.2 常见内置键

| 键名 | 类型 | 说明 |
| --- | --- | --- |
| `messages` | `List<Message>` | Agent 的消息历史，ReAct 循环自动维护 |
| `input` | `String` | 用户输入文本 |

其他键（如 `writer_output`、`turn_count`、`active_role` 等）均由业务代码自定义，框架不做限制。

### 4.3 KeyStrategy：合并策略

当同一个键收到多次写入时，框架通过 `KeyStrategy` 决定如何合并：

| 策略 | 含义 | 适用场景 |
| --- | --- | --- |
| `ReplaceStrategy` | 新值直接覆盖旧值 | 单次输出（如 `writer_output`） |
| `AppendStrategy` | 新值追加到旧值列表 | 消息历史（`messages`） |

通常不需要手动配置，框架默认为 `ReplaceStrategy`，`messages` 键自动使用 `AppendStrategy`。

---

## 5. StateGraph：用状态图描述复杂流程

`StateGraph` 是框架提供的**图编排 API**，用于手动定义节点、边和条件路由，适合需要精确控制流程走向的场景（如 AutoGen 群聊的轮询调度）。

> `SupervisorAgent` 的内部就是一个 `StateGraph`，只是框架帮你搭好了，不需要手写。

### 5.1 核心 API

```java
StateGraph stateGraph = new StateGraph();

// 注册节点（节点 = 一个执行单元，返回写入 OverAllState 的 Map）
stateGraph.addNode("pm_node", state -> {
    // 执行逻辑，返回要写入状态的内容
    return Map.of("messages", newMessage);
});

// 添加固定边（A 执行完一定去 B）
stateGraph.addEdge(StateGraph.START, "pm_node");
stateGraph.addEdge("pm_node", "engineer_node");

// 添加条件边（A 执行完后由函数决定去哪）
stateGraph.addConditionalEdges(
    "engineer_node",                    // 来源节点
    state -> {                          // 路由函数，返回下一节点名
        boolean done = state.value("done", Boolean.FALSE);
        return done ? StateGraph.END : "reviewer_node";
    },
    Map.of(                             // 合法的目标节点声明
        StateGraph.END, StateGraph.END,
        "reviewer_node", "reviewer_node"
    )
);
```

### 5.2 特殊常量

| 常量 | 含义 |
| --- | --- |
| `StateGraph.START` | 图的入口，每次 `invoke` 从这里开始 |
| `StateGraph.END` | 图的出口，到达这里流程结束 |

### 5.3 NodeAction：节点的签名

节点本质上是一个函数：读取 `OverAllState`，返回需要写回状态的 `Map`：

```java
// 函数式写法
stateGraph.addNode("my_node", state -> {
    String input = state.value("input").map(Object::toString).orElse("");
    String result = doSomething(input);
    return Map.of("my_output", result);   // 写回状态
});

// 实现接口写法（复杂节点推荐）
public class ProductManagerNode implements NodeAction {
    @Override
    public Map<String, Object> apply(OverAllState state) {
        // ...
        return Map.of("messages", updatedMessages);
    }
}
stateGraph.addNode("pm_node", new ProductManagerNode(...));
```

---

## 6. @Tool + methodTools：给 Agent 绑定工具

工具（Tool）是让 LLM 能够**调用真实 Java 方法**的机制。有了工具，Agent 就不再只会"说话"，还能查数据库、调接口、执行计算。

### 6.1 定义工具

使用 Spring AI 的 `@Tool` 和 `@ToolParam` 注解：

```java
public class TravelTools {

    @Tool(name = "queryWeather", description = "查询指定城市的当前天气")
    public String queryWeather(
            @ToolParam(description = "需要查询天气的城市名称") String city) {
        // 真实场景里这里调天气 API
        return city + "：晴朗，微风，气温 25°C";
    }

    @Tool(name = "recommendAttraction", description = "根据城市和天气推荐旅游景点")
    public String recommendAttraction(
            @ToolParam(description = "旅游城市") String city,
            @ToolParam(description = "当前天气状况") String weather) {
        return "推荐去" + city + "的颐和园，适合" + weather + "时游览";
    }
}
```

**要点**：
- `description` 字段非常重要，LLM 就是靠这段描述判断"什么时候调这个工具、传什么参数"
- 方法参数类型支持 String、int、boolean 等基础类型和简单 POJO

### 6.2 绑定到 Agent

```java
ReactAgent agent = ReactAgent.builder()
    .name("travel-react-agent")
    .description("智能旅行助手")
    .model(chatModel)
    .systemPrompt("你是一名智能旅行助手，可以查天气和推荐景点")
    .methodTools(new TravelTools())   // ← 绑定工具对象，框架自动扫描 @Tool 方法
    .saver(new MemorySaver())
    .build();
```

### 6.3 工具调用的完整执行链

```
用户提问："北京今天适合去哪里玩？"
    ↓
LLM 推理：需要先查天气，调用 queryWeather("北京")
    ↓
框架 ToolNode 自动执行 TravelTools.queryWeather("北京")
    ↓
返回结果 "北京：晴朗，微风，气温 25°C" → 追加到 messages
    ↓
LLM 再次推理：天气不错，调用 recommendAttraction("北京", "晴朗微风")
    ↓
框架执行工具，返回景点推荐 → 追加到 messages
    ↓
LLM 判断信息足够，生成最终回答
```

这个循环由框架自动驱动，开发者只需要实现 `@Tool` 方法本身。

---

## 7. Saver：状态持久化与线程隔离

`Saver` 负责在 Agent 执行过程中**保存每一步的状态快照**，并按 `threadId` 隔离不同调用。

### 7.1 为什么需要 Saver？

同一个 `ReactAgent` 实例可能同时处理多个请求，没有隔离会导致状态串线。`Saver` 的 `threadId` 机制保证了每次调用的状态互不干扰。

```java
// 不同的 threadId = 完全隔离的状态空间
RunnableConfig config1 = RunnableConfig.builder().threadId("user-001").build();
RunnableConfig config2 = RunnableConfig.builder().threadId("user-002").build();

agent.invoke("北京天气", config1);   // 状态存在 "user-001" 空间
agent.invoke("上海天气", config2);   // 状态存在 "user-002" 空间，互不干扰
```

### 7.2 Saver 类型对比

| 类型 | 存储介质 | 适用场景 |
| --- | --- | --- |
| `MemorySaver` | JVM 内存（ConcurrentHashMap） | Demo、单元测试、本地开发 |
| `RedisSaver` | Redis | 生产环境，分布式部署 |
| `PGVectorSaver` | PostgreSQL + pgvector | 需要向量检索历史的场景 |

### 7.3 使用方式

```java
// 方式一：直接在 ReactAgent 上配置（推荐）
ReactAgent agent = ReactAgent.builder()
    .saver(new MemorySaver())
    .build();

// 方式二：通过 CompileConfig 配置（SupervisorAgent 等场景）
CompileConfig config = CompileConfig.builder()
    .saverConfig(SaverConfig.builder()
        .register(new MemorySaver())
        .build())
    .build();
```

---

## 8. CompileConfig：图编译参数

`CompileConfig` 在图被编译成 `CompiledGraph` 之前传入，控制运行时的关键参数。

### 8.1 核心配置项

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `recursionLimit` | int | `Integer.MAX_VALUE` | 图的最大执行步数，**必须设置**，否则死循环不会终止 |
| `saverConfig` | SaverConfig | 无 | 注册 Saver |

### 8.2 recursionLimit 怎么设

```java
CompileConfig.builder()
    .recursionLimit(Math.max(8, maxRounds * 4))
    .build()
```

- `maxRounds` 是你预期的最大业务轮次（如 3 个 Worker 各跑一次 = 3 轮）
- 乘以 4 是因为框架内部每个业务轮次实际消耗约 2～4 步图执行（路由判断 + Worker 执行）
- 建议下限设为 8，避免因计算偏差导致过早中止

---

## 9. Agent 的三种调用方式

`ReactAgent`、`SupervisorAgent` 等都继承自 `Agent` 基类，支持三种调用方式：

| 方法 | 返回类型 | 适用场景 |
| --- | --- | --- |
| `invoke(input)` | `Optional<OverAllState>` | 调试和学习：能拿到完整状态快照，看清每一步写了什么 |
| `call(input)` | `AssistantMessage` | 生产使用：只关心最终答案 |
| `stream(input)` | `Flux<NodeOutput>` | 流式场景：逐步获取执行进度 |

### 9.1 `invoke` —— 学习时推荐

```java
Optional<OverAllState> optionalState = supervisorAgent.invoke(task);
OverAllState state = optionalState.orElseThrow();

// 可以从状态里提取任意中间产物
String writerOutput     = state.value("writer_output").map(Object::toString).orElse("");
String translatorOutput = state.value("translator_output").map(Object::toString).orElse("");
```

### 9.2 `call` —— 生产时推荐

```java
AssistantMessage response = reactAgent.call("北京今天适合去哪里玩？");
System.out.println(response.getText());
```

### 9.3 带 threadId 的调用

```java
RunnableConfig config = RunnableConfig.builder()
    .threadId("session-user-001")   // 隔离不同用户/会话的状态
    .build();

Optional<OverAllState> state = agent.invoke(userInput, config);
```

---

## 附录：组件与项目模块对照表

如果你想结合项目里的真实代码来理解这些组件，可以参考下表找到对应实现：

| 你想理解的组件 | 推荐查看的文件 |
| --- | --- |
| `ReactAgent` 基础用法 + `@Tool` + `MemorySaver` | `module-react-paradigm/.../SpringAIReActTravelDemo.java` |
| `SupervisorAgent` + `mainRoutingAgent` + `outputKey` 联动 | `module-multi-agent-supervisor/.../AlibabaSupervisorFlowAgent.java` |
| `StateGraph` 手写节点和条件边 | `module-multi-agent-conversation/.../AlibabaConversationFlowAgent.java` |
| `instruction` 占位符 + 上下游状态传递 | `module-multi-agent-supervisor/.../FrameworkSupervisorPromptTemplates.java` |
| `SupervisorGatewayBackedChatModel`（Gateway → ChatModel 适配） | `module-multi-agent-supervisor/.../SupervisorGatewayBackedChatModel.java` |
