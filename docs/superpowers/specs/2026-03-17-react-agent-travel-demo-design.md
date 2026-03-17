# ReAct Agent Travel Demo Design

## 目标

在 `module-react-paradigm` 中手写一个纯粹依赖 `framework-core` 契约的 `ReActAgent`，并提供“智能旅行助手”可运行 Demo。

## 约束

1. 代码中禁止出现 `org.springframework.ai.**` import。
2. 只能依赖 `framework-core` 中的 `HelloAgentsLLM`、`Message`、`Tool`、`ToolRegistry` 等统一协议。
3. 核心循环必须使用 `while (step < maxSteps)` 防止无限迭代。

## 设计选择

### 1. 响应判定策略

采用“框架 DTO 优先、文本协议兜底”的混合模式：

- 若 `LlmResponse.toolCalls` 非空，则视为本轮需要执行工具。
- 若 `toolCalls` 为空，则从 `outputMessage.content` 或 `rawText` 解析最终答案。
- 若文本包含 `Final Answer:` 前缀，则在返回给调用方时剥离此前缀。

### 2. 历史消息策略

`ReActAgent.run(String userQuery)` 内部维护 `List<Message> history`，并同步写入本地 `InMemoryMemory` 的 `MemorySession`，以便工具执行时能够获得统一上下文。

### 3. Demo LLM 策略

不接真实模型，测试和 Demo 中使用一个本地假实现 `TravelDemoHelloAgentsLLM`：

- 第 1 轮返回天气工具调用
- 第 2 轮返回景点搜索工具调用
- 第 3 轮返回最终答案

这样可以完整演示 ReAct 的 `Thought -> Action -> Observation -> Final Answer` 闭环，同时保持源码完全不受第三方类型污染。

## 文件设计

- `module-react-paradigm/src/main/java/com/xbk/agent/framework/react/application/executor/ReActAgent.java`
  - 纯生产代码
  - 负责 while 循环、消息维护、工具执行与最终答案返回

- `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/ReActTravelDemo.java`
  - 既是 Demo 入口，也是 TDD 测试载体
  - 内含本地假 LLM、天气工具、景点工具
  - 覆盖正常闭环和 `maxSteps` 安全阀

## 停止条件

循环会在以下任一条件触发时终止：

1. LLM 返回最终答案
2. `step` 到达 `maxSteps`
3. 解析不到工具调用且没有可用最终答案

## 交付边界

这一轮只实现：

- `ReActAgent`
- `ReActTravelDemo`

不扩展：

- Spring Bean 自动装配
- 真正的模型接入
- 其他范式模块
