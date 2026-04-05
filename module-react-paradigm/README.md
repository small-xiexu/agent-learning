# module-react-paradigm

## 新手导航

如果你是第一次接触这个模块，建议先读：

- [ReAct范式新手导读](../docs/ReAct范式新手导读.md)

扩展阅读：

- [ReAct学习路径](../docs/ReAct学习路径.md)
- [ReAct手写版与官方版对照](../docs/ReAct手写版与官方版对照.md)

这篇导读会优先把下面几件事讲清楚：

- ReAct 为什么不是“会调工具的聊天模型”
- 手写版为什么依赖 `AgentLlmGateway`、`Message`、`ToolRegistry`
- 官方版为什么依赖 Spring AI Alibaba `ReactAgent`
- `call()` 和 `invoke()` 分别适合看什么
- 真实 OpenAI Demo 需要哪些配置、怎么启动

README 负责给你模块级总览；导读负责把运行机制讲透；扩展阅读负责更细的源码对照。

## 模块定位

`module-react-paradigm` 用于承载单体智能体里最经典的 ReAct 范式。

它解决的核心问题不是“怎么多说几句”，而是：

- 模型缺信息时，怎么显式决定调用工具
- 工具返回结果后，怎么驱动模型继续思考
- 整个推理和行动闭环，怎么被程序治理而不是只靠 prompt

如果 Reflection 更关注“做完以后怎么再审一次”，Plan-and-Solve 更关注“先拆计划再执行”，那么 ReAct 更关注：

**当前这一步缺什么信息，我要不要先行动，再基于新事实继续推理。**

## 最佳实践场景

**智能旅行助手**：先判断是否需要查天气，再调用天气工具，再基于天气结果调用景点推荐工具，最后输出完整建议。

## 模块里的双实现对照

### 1. 手写版 ReAct runtime

核心类：

- `ReActAgent`

离线 Demo：

- `ReActTravelDemo`

它的关键点是：

- 通过 `AgentLlmGateway` 调模型
- 通过 `ToolRegistry` 管理工具
- 用 `List<Message> history` 手动维护上下文
- 通过 `ToolCall` 判断是否继续行动
- 用 `while (step < maxSteps)` 做死循环保护

这一版最适合用来理解：

- ReAct 的最小 runtime 到底长什么样
- 工具调用如何真正进入消息流
- 为什么安全阀必须由程序显式控制

### 2. Spring AI Alibaba 官方版

核心类：

- `ReactAgent`
- `MemorySaver`
- `RunnableConfig`

离线 Demo：

- `SpringAIReActTravelDemo`

它的关键点是：

- 使用 `ReactAgent.builder()` 声明式组装 Agent
- 用 `methodTools(...)` 把 Java 方法自动暴露成工具
- 用 `MemorySaver` 保存线程态
- 用 `call()` 拿最终答案，用 `invoke()` 拿完整状态
- 不再手写 `while`，而是交给 Graph Runtime 自动流转

## Spring AI / Spring AI Alibaba 在这里怎么落地

### 手写版：统一网关抽象，Spring AI 做底层适配

手写版业务代码直接依赖的是：

- `AgentLlmGateway`
- `LlmRequest`
- `LlmResponse`
- `ToolRegistry`

但真实模型接入仍然可以走 Spring AI：

1. `framework-llm-autoconfigure` 读取统一 `llm.*` 配置
2. 自动装配 `AgentLlmGateway`
3. `framework-llm-springai` 创建 Spring AI `ChatModel`
4. `SpringAiLlmClient` 把统一请求转成 Spring AI `Prompt`
5. 最终由 `ChatModel.call(...)` 发起真实调用

所以手写版是在统一协议层上学习 ReAct，本质上并不排斥 Spring AI。

### 官方版：直接围绕 Graph Runtime 组织闭环

官方版更贴近 Spring AI Alibaba 的原生能力：

- `ReactAgent` 负责组织模型节点和工具节点
- `MemorySaver` 负责保存线程状态
- `RunnableConfig.threadId` 负责隔离不同调用
- `OverAllState` 负责暴露完整运行结果

这说明官方版的重点不是“如何自己造 runtime”，而是“如何声明式地使用 runtime”。

## 与测试和 Demo 的对应关系

如果你想先看行为，再看实现，推荐优先看：

- `src/test/java/com/xbk/agent/framework/react/ReActTravelDemo.java`
- `src/test/java/com/xbk/agent/framework/react/SpringAIReActTravelDemo.java`
- `src/test/java/com/xbk/agent/framework/react/ReActAgentToolCallingTest.java`

这几组代码分别钉住了：

- 手写版的旅行助手闭环
- 官方版的旅行助手闭环
- 工具调用与最大步数控制

## 真实 OpenAI Demo

当前模块保留了两套真实模型 Demo：

- `src/test/java/com/xbk/agent/framework/react/ReActTravelOpenAiDemo.java`
- `src/test/java/com/xbk/agent/framework/react/SpringAIReActTravelOpenAiDemo.java`

对应配置：

- `src/test/resources/application-openai-react-demo.yml`
- `src/test/resources/application-llm-local.yml.example`
- `src/test/resources/application-llm-local.yml`
- `src/test/resources/application-openai-react-demo-local.yml.example`
- `src/test/resources/application-openai-react-demo-local.yml`

运行真实 Demo 前至少要准备两件事：

1. 在 `application-llm-local.yml` 里填入真实 `llm.*` 参数
2. 在 `application-openai-react-demo-local.yml` 里显式开启 `demo.react.openai.enabled=true`

默认情况下，真实 Demo 会被安全跳过，避免日常测试误打外网。

## 推荐阅读顺序

如果你想顺着源码快速吃透，推荐顺序如下：

1. 先看 `ReActAgent`
2. 再看 `ReActTravelDemo`
3. 再看 `SpringAIReActTravelDemo`
4. 最后看真实 Demo 和配置支持类
   - `ReActTravelOpenAiDemo`
   - `SpringAIReActTravelOpenAiDemo`
   - `OpenAiReactDemoPropertySupport`

这样看，你会更容易把：

- 范式本体
- 统一协议层
- Spring AI 模型接入
- Spring AI Alibaba 图运行时

这 4 层真正串起来。

## 工程落地建议

### 1. 工具颗粒度要小而稳定

ReAct 最怕“一个万能工具做所有事”。
工具职责越清晰，模型越容易选对，也越容易恢复和观测。

### 2. 停止条件必须显式

不要指望模型“自己懂得停”。
最大步数、错误处理和早停策略都应该由 runtime 明确治理。

### 3. Observation 必须可回放

工具结果一定要可靠进入上下文。
一旦 Observation 丢失，后续决策链路就会不可追踪。

### 4. 把 ReAct 当成默认范式，但不要把它当万能范式

当任务更像“先拆计划”或“再做一轮审查”时，就应该升级到其它模块，而不是把 ReAct 循环无限堆大。

## 结论

这个模块最重要的价值，不是证明 ReAct 会调工具，而是把同一个 ReAct 闭环同时落成了两种 runtime：

- 手写版：帮助你理解范式本体
- 官方版：帮助你理解框架运行时

把这两套代码对照着看，才能真正看清 ReAct 在这个仓库里的完整位置。
