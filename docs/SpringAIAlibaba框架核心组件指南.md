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
| `model` | ChatModel | 是 | 底层模型实例，项目里统一用 `SupervisorGatewayBackedChatModel` 适配 |
| `systemPrompt` | String | 否 | 角色设定，固定不变，详见 2.2 节 |
| `instruction` | String | 否 | 任务指令，支持 `{key}` 占位符动态替换，详见 2.3 节 |
| `outputKey` | String | 否 | Agent 输出写入 `OverAllState` 的键名，详见 2.4 节 |
| `methodTools` | Object | 否 | 绑定带 `@Tool` 注解的 Java 对象，详见第 6 节 |
| `saver` | Saver | 否 | 状态持久化实现，详见第 7 节 |
| `includeContents` | boolean | 否 | 是否继承父流程的消息历史，默认 `true`；设为 `false` 可避免上下文污染 |
| `returnReasoningContents` | boolean | 否 | 是否返回 LLM 的思维链过程，生产环境通常关闭 |

---

### 2.2 `systemPrompt` vs `instruction`：两者有什么区别？

这是最容易混淆的地方，一句话区分：

> `systemPrompt` 是"入职培训手册"（角色设定，固定不变）；`instruction` 是"每次开会的任务单"（动态内容，支持占位符）。

#### 对应 LLM 的消息角色

| 属性 | 发给 LLM 的消息类型 | 支持 `{}` 占位符 | 每轮变化 |
| --- | --- | --- | --- |
| `systemPrompt` | `SystemMessage`（system 角色） | 否 | 不变 |
| `instruction` | `UserMessage`（user 角色） | **是** | 每轮动态渲染 |

#### 实际例子（`supervisor_router_agent`）

```java
// systemPrompt：告诉 LLM "你是谁，你的行为准则是什么"
// 每次调用都一样，LLM 靠这个维持角色稳定
systemPrompt:
    "你是中心化 Supervisor 的主路由 Agent。
     你只负责决定下一步应该调用哪个子 Agent。
     你必须严格只返回 JSON 数组，严禁输出解释..."

// instruction：告诉 LLM "这一轮具体的上下文和任务是什么"
// {writer_output} 等占位符会在每轮执行时从 OverAllState 里取值填入
instruction:
    "原始任务：{input}
     当前状态：
     writer_output={writer_output}
     translator_output={translator_output}
     reviewer_output={reviewer_output}
     路由规则：..."
```

---

### 2.3 `instruction` 占位符替换原理

很多人好奇 `{writer_output}` 是怎么被替换成实际值的，背后的链路如下：

**第一步**：框架把 `instruction` 字符串包装成 `AgentInstructionMessage`（一种特殊的消息类型），标记为"未渲染"。

**第二步**：Agent 执行前，`AgentLlmNode` 扫描消息列表，找到未渲染的 `AgentInstructionMessage`，调用 `renderPromptTemplate` 方法：

```java
// AgentLlmNode.java 源码（简化）
if (message instanceof AgentInstructionMessage && !instructionMessage.isRendered()) {
    String rendered = renderPromptTemplate(
        instructionMessage.getText(),
        state.data()          // ← OverAllState 里的全部键值对作为替换参数
    );
    // 渲染后标记为"已渲染"，替换原消息
}
```

**第三步**：`renderPromptTemplate` 内部使用 Spring AI 的 `PromptTemplate` 完成占位符替换：

```java
private String renderPromptTemplate(String prompt, Map<String, Object> params) {
    return PromptTemplate.builder()
        .template(prompt)
        .build()
        .render(params);   // params 就是 OverAllState.data()
}
```

**完整数据流**：

```
OverAllState.data() = {
    "input":              "写一篇关于 Java 的博客",
    "writer_output":      "Java 是一门面向对象的编程语言...",
    "translator_output":  "",
    "reviewer_output":    ""
}
    ↓ 作为 params 传入
PromptTemplate.render(params)
    ↓ 替换占位符
渲染后的 UserMessage:
    "原始任务：写一篇关于 Java 的博客
     当前状态：
     writer_output=Java 是一门面向对象的编程语言...
     translator_output=
     reviewer_output=
     路由规则：..."
    ↓ 发给 LLM
```

**关键结论**：占位符的名字必须和 `OverAllState` 里的键名完全一致，否则替换为空字符串。这就是为什么 `outputKey`（见 2.4 节）需要与 `instruction` 里的占位符名对齐。

---

### 2.4 `outputKey`：Agent 输出写入状态的协议

`outputKey` 决定了 Agent 执行完后，把输出内容写到 `OverAllState` 的哪个键下。

```java
ReactAgent writerAgent = ReactAgent.builder()
    .name("writer_agent")
    .outputKey("writer_output")    // ← 执行完后输出写入 state["writer_output"]
    .build();

ReactAgent translatorAgent = ReactAgent.builder()
    .name("translator_agent")
    .outputKey("translator_output")
    .instruction("请把下面的中文博客翻译成英文：{writer_output}")
    //                                            ↑ 读取上一个 Agent 的输出
    .build();
```

**三个角色联动**：

```
writerAgent 执行完
    → 输出写入 OverAllState["writer_output"]
        → translatorAgent 的 instruction 里 {writer_output} 被替换为实际内容
            → translatorAgent 执行完后写入 OverAllState["translator_output"]
                → ...
```

这就是框架版多 Agent 状态交接的核心协议：**上游用 `outputKey` 写，下游用 `{key}` 读。**

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
