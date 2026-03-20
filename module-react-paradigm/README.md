# module-react-paradigm

## 模块定位

`module-react-paradigm` 用于承载单体智能体中最经典的 ReAct（Reasoning + Acting）范式。  
它的目标不是“包装一个会调用工具的聊天模型”，而是把“思考、行动、观察、再思考”的闭环明确建模为可治理、可扩展、可替换的工程结构。

在 `spring-ai-agent-framework` 中，这个模块应当成为默认的单 Agent 起点：  
当任务还不需要显式规划、图编排或多智能体协作时，优先使用 ReAct，而不是过早引入复杂工作流。

## 🎯 最佳实践场景

**智能旅行助手**：能够处理“思考查询天气 -> 行动调用 API -> 观察结果后再思考推荐景点”这种需要动态与外部世界交互的连贯任务。

## 理论背景

ReAct 的核心价值在于把推理与行动显式耦合起来。

传统单轮 LLM 问答的问题在于：模型只能“想”，不能“证伪自己的想法”。  
而 ReAct 通过以下循环打破这一点：

1. `Thought`：模型分析当前上下文，决定下一步策略。
2. `Action`：模型调用外部工具或执行具体动作。
3. `Observation`：工具返回客观世界的反馈。
4. 回到新的 `Thought`：模型基于新事实修正判断。

这套机制的工程意义非常强：

- 推理不再是闭门造车，而是被外部事实持续校正。
- 工具不再只是外挂能力，而是推理过程的一部分。
- Agent 的正确性不再只依赖提示词，而依赖“推理 + 执行 + 反馈”的动态闭环。

## 运行机制

该模块建议将 ReAct 运行时拆成 4 个稳定阶段：

### 1. 上下文装配

从 `framework-core` 的 `Memory` 中读取当前会话消息；  
从 `ToolRegistry` 中导出当前可用工具定义；  
再配合系统提示、运行配置、停止条件组装出本轮上下文。

### 2. 推理决策

模型先判断当前是否已经具备足够信息：

- 如果信息不足，则产生下一步工具调用决策
- 如果信息充足，则直接生成最终答案

### 3. 工具执行

当模型发出工具调用请求后，由 `ToolRegistry` 路由到具体工具执行，并把结果写回消息流。

### 4. 观察回灌

工具结果被转化为新的 `Observation` 消息进入上下文，驱动下一轮推理。  
循环会持续到以下任一条件满足为止：

- 模型输出最终答案
- 达到最大迭代次数
- Hook 或拦截器提前终止流程
- 工具调用失败且策略决定中止

## Spring AI Alibaba 映射

这个模块与 Spring AI Alibaba 的映射关系非常直接。

### 1. 核心抽象

Spring AI Alibaba 官方提供了基于 ReAct 理念的 `ReactAgent`。  
它不是一个简单的“工具调用助手”，而是运行在 Graph Runtime 之上的生产级 Agent 抽象。

### 2. 底层执行图

官方文档明确指出 `ReactAgent` 基于 Graph 运行时构建，其核心节点包括：

- `Model Node`：负责推理、生成最终答案或发起工具调用
- `Tool Node`：负责执行工具
- `Hook Nodes`：负责在人机协同、审计、停止条件等关键位置插入自定义逻辑

因此，本模块的设计重点不应放在“如何手写 while 循环”，而应放在：

- 如何约束模型的思考方式
- 如何定义工具边界
- 如何利用 Graph Runtime 的节点流转能力实现可靠闭环

### 3. 对官方能力的工程使用建议

- 用 `ReactAgent` 作为对外的主要执行入口
- 用 `invoke` 获取完整状态，而不是只拿最终文本
- 用 Hooks 和 Interceptors 管理停止条件、审计、人工审批、工具错误恢复
- 用最大迭代次数控制成本和死循环风险

## 与 framework-core 的关系

本模块不应直接把业务逻辑写死在 `ReactAgent` Builder 中，而应建立在 `framework-core` 的稳定协议之上：

- `AgentLlmGateway`：统一模型入口，隔离底层 `ChatModel`
- `Message / Memory`：统一消息与会话上下文
- `Tool / ToolRegistry`：统一工具定义、发现与执行

推荐的职责分工是：

- `framework-core` 决定“消息、工具、模型如何被统一抽象”
- `module-react-paradigm` 决定“这些抽象如何组成 ReAct 闭环”

## 工程落地建议

### 1. 工具颗粒度要小而稳定

ReAct 最怕“一个万能工具做所有事情”。  
工具应该围绕单一职责设计，让模型容易选择、容易恢复、容易观测。

### 2. Prompt 不要承担全部控制职责

系统提示只负责给出角色、目标和行为约束。  
循环边界、最大迭代次数、错误处理、人工审批应由运行时机制控制，而不是寄希望于模型“自己懂得收敛”。

### 3. Observation 必须可回放

工具结果应写入统一消息协议，确保后续问题排查时可以还原 Agent 的决策轨迹。

### 4. 把 ReAct 当作默认范式，而不是万能范式

当任务明显需要显式规划、复杂状态机或多专家协作时，应升级到 Plan-Replan、Graph Flow 或 Multi-agent 模块，而不是把 ReAct 循环无限堆大。

## 真实 OpenAI 对照 Demo

当前模块同时保留了两套 Demo：

- 离线教学版：
  - `ReActTravelDemo`
  - `SpringAIReActTravelDemo`
- 真实 OpenAI 对照版：
  - `ReActTravelOpenAiDemo`
  - `SpringAIReActTravelOpenAiDemo`

真实对照版的设计目标是让手写版 `ReActAgent` 和官方 `ReactAgent` 共用同一个 `ChatModel` 与同一个 `OPENAI_API_KEY`，从而进行真正的控制变量对照学习。

运行真实 Demo 前需要准备：

- 环境变量：`OPENAI_API_KEY`
- 显式开关：`-Ddemo.react.openai.enabled=true`

如果你更习惯 Spring Boot 本地配置文件，也可以：

1. 复制 `src/test/resources/application-openai-react-demo-local.yml.example`
2. 重命名为 `src/test/resources/application-openai-react-demo-local.yml`
3. 把真实 `api-key` 写进这个本地文件

这个本地文件已经被 `.gitignore` 忽略，不会被误提交到仓库。

默认的 `mvn test` 只会运行离线教学版和配置装载测试；  
真实 OpenAI Demo 在没有 Key 或没有显式开启时会自动跳过，避免日常测试误打外网。

## 适用场景与边界

### 适用场景

- 单 Agent 工具调用
- 中等复杂度的信息检索、分析、执行任务
- 需要动态根据观察结果调整策略的任务

### 不适合的场景

- 必须先生成完整蓝图再逐步执行的复杂任务
- 强状态机、强条件路由、强审计要求的流程
- 需要多个专家角色长期协作的任务

## 结论

ReAct 不是“最简单的 Agent”，而是“最值得先落地的 Agent”。  
它用最少的结构引入了最关键的能力：让推理与现实世界发生闭环。

在本项目中，`module-react-paradigm` 应被视为单体智能体的默认能力基座，也是其它高级范式的认知起点。
