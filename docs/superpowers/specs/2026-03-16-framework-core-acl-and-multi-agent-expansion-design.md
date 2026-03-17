# Framework Core ACL And Multi-Agent Expansion Design

**Date:** 2026-03-16

## Goal

在现有 `spring-ai-agent-framework` 多模块工程基础上，补齐 3 个多智能体协作模块骨架，并为 `framework-core` 建立第一版核心抽象设计，重点完成统一 LLM 门面、防腐层、统一消息协议、统一记忆协议以及工具注册协议的顶层收口。

## Scope

本轮设计覆盖两部分：

1. 多模块扩展
   - `module-multi-agent-conversation`
   - `module-multi-agent-engineering`
   - `module-multi-agent-roleplay`
2. `framework-core` 核心协议设计
   - `llm`
   - `memory`
   - `tool`

本轮设计不包含完整业务实现，不包含各范式模块的具体运行时逻辑。

## Design Principles

- `framework-core` 必须作为所有范式模块的协议中心，避免直接暴露 Spring AI 类型。
- 框架与底层 Spring AI / Spring AI Alibaba 之间通过 ACL（防腐层）隔离。
- `memory` 与 `tool` 作为一级领域协议，不依赖 `llm`。
- `llm` 只接受框架自定义请求/响应 DTO，不向上层泄露第三方库对象。
- 接口位点一步到位预留同步、流式、结构化输出、工具调用四类能力。
- 首版实现坚持 MVP：先打通同步对话与工具调用建模，其余高级能力保留接口与扩展点。

## Module Expansion

### Dependency Direction

新增 3 个模块与现有模块保持同样规则：

- 仅依赖 `framework-core`
- 不依赖其他范式模块
- 不直接依赖底层模型厂商 Starter

### Package Design

#### module-multi-agent-conversation

根包：`com.xbk.agent.framework.conversation`

```text
api
application.routing
application.handoff
domain.conversation
domain.expert
domain.takeover
domain.policy
infrastructure.registry
infrastructure.routing
infrastructure.handoff
config
support
```

职责：对话驱动、多专家接管、去中心化 Handoffs 路由。

#### module-multi-agent-engineering

根包：`com.xbk.agent.framework.engineering`

```text
api
application.context
application.pipeline
domain.context
domain.placeholder
domain.isolation
domain.handoff
domain.policy
infrastructure.template
infrastructure.memory
infrastructure.pipeline
config
support
```

职责：上下文工程、占位符接力、状态隔离与高并发消息编排。

#### module-multi-agent-roleplay

根包：`com.xbk.agent.framework.roleplay`

```text
api
application.sequence
application.workflow
domain.role
domain.stage
domain.prompt
domain.contract
domain.policy
infrastructure.sequence
infrastructure.prompt
infrastructure.agentframework
config
support
```

职责：顺序执行、多角色 Prompt 协作、角色扮演流水线。

## Framework Core Package Layout

根包：`com.xbk.agent.framework.core`

```text
core
├── llm
│   ├── HelloAgentsLLM
│   ├── DefaultHelloAgentsLLM
│   ├── model
│   ├── option
│   ├── spi
│   ├── support
│   └── adapter.springai
├── memory
│   ├── Message
│   ├── Memory
│   ├── MemorySession
│   └── support
├── tool
│   ├── Tool
│   ├── ToolContext
│   ├── ToolDefinition
│   ├── ToolRegistry
│   └── support
├── agent
│   ├── AgentContext
│   ├── AgentRuntime
│   └── support
└── common
    ├── enum
    ├── exception
    └── util
```

## LLM Design

### Entry Point

`HelloAgentsLLM` 作为对外唯一统一门面，`DefaultHelloAgentsLLM` 作为默认实现。

门面能力位点：

- 同步对话
- 流式输出
- 结构化输出
- 能力探测

不直接暴露 Spring AI 的 `ChatClient`、`ChatModel`、`Prompt`、`ChatResponse`。

### Request / Response Model

#### LlmRequest

建议字段：

- `requestId`
- `conversationId`
- `List<Message> messages`
- `List<ToolDefinition> availableTools`
- `ModelOptions modelOptions`
- `StreamingOptions streamingOptions`
- `StructuredOutputOptions structuredOutputOptions`
- `ToolCallingOptions toolCallingOptions`
- `Map<String, Object> metadata`

约束：

- 直接复用 `memory.Message` 作为统一消息协议
- 工具输入只接受 `ToolDefinition`，不耦合 `ToolRegistry`
- `metadata` 只承载链路与扩展信息

#### LlmResponse

建议字段：

- `requestId`
- `responseId`
- `Message outputMessage`
- `String rawText`
- `List<ToolCall> toolCalls`
- `LlmFinishReason finishReason`
- `LlmUsage usage`
- `Map<String, Object> metadata`

#### StructuredLlmResponse<T>

- `LlmResponse response`
- `T structuredOutput`
- `boolean schemaValidated`

#### LlmStreamEvent

建议字段：

- `eventId`
- `LlmStreamEventType type`
- `String textDelta`
- `ToolCallDelta toolCallDelta`
- `LlmUsage usage`
- `boolean completed`
- `Map<String, Object> metadata`

#### Supporting Model

- `LlmUsage`
- `LlmFinishReason`
- `ToolCall`
- `ToolCallDelta`

### Options Model

#### ModelOptions

- `modelName`
- `temperature`
- `topP`
- `maxTokens`
- `stopSequences`
- `timeout`
- `Map<String, Object> providerHints`

#### StreamingOptions

- `enabled`
- `emitTextDelta`
- `emitToolCallDelta`
- `emitUsageOnComplete`
- `aggregateFinalMessage`

#### StructuredOutputOptions

- `enabled`
- `schemaName`
- `schemaDescription`
- `strict`
- `includeRawTextFallback`

#### StructuredOutputSpec<T>

- `Class<T> targetType`
- `String schemaName`
- `String schemaDescription`
- `boolean strict`

#### ToolCallingOptions

- `enabled`
- `ToolChoiceMode toolChoiceMode`
- `boolean parallelToolCalls`
- `int maxToolRoundTrips`
- `boolean includeToolResultsInContext`

### SPI Design

采用“主 SPI + 可选能力 SPI”的组合方式。

#### Main SPI

- `LlmClient`

职责：

- 承担同步对话主能力
- 暴露能力集合 `capabilities()`

#### Optional Capability SPI

- `StreamingLlmClient`
- `StructuredOutputLlmClient`

规则：

- `DefaultHelloAgentsLLM` 必须依赖主 `LlmClient`
- 可选能力根据底层实现是否实现对应 SPI 决定
- 不支持的能力统一抛框架内异常

### Capability Enum

- `SYNC_CHAT`
- `STREAMING`
- `STRUCTURED_OUTPUT`
- `TOOL_CALLING`

### Spring AI Adapter Layout

`adapter.springai` 下拆分为：

- `SpringAiLlmClient`
- `SpringAiStreamingLlmClient`
- `SpringAiStructuredOutputLlmClient`
- `SpringAiModelResolver`
- `SpringAiMessageMapper`
- `SpringAiToolMapper`
- `SpringAiOptionsMapper`
- `SpringAiResponseMapper`
- `SpringAiStreamEventMapper`

价值：

- 适配复杂度局部化
- 避免上帝类
- 未来升级 Spring AI 时改动面可控

## Memory Design

### Message

`Message` 作为统一消息值对象，覆盖系统消息、用户消息、模型回复、工具结果等交互形态。

建议字段：

- `messageId`
- `conversationId`
- `role`
- `content`
- `name`
- `toolCallId`
- `metadata`
- `createdAt`

建议角色枚举：

- `SYSTEM`
- `USER`
- `ASSISTANT`
- `TOOL`

约束：

- `role` 必填
- `content` 默认可读；工具结果场景允许通过上下文字段补足
- `metadata` 承担扩展语义，不替代主字段
- `Message` 不依赖 `llm` 或 `tool` 的实现类型

### Memory

`Memory` 作为会话级记忆入口，用于打开或获取某个会话的记忆视图。

建议职责：

- 获取 `MemorySession`
- 判断会话是否存在
- 清理指定会话

### MemorySession

`MemorySession` 负责会话内消息读写和快照读取。

建议能力：

- 追加单条消息
- 批量追加消息
- 读取全部消息快照
- 按窗口或条件读取消息子集
- 返回最新一条消息
- 获取消息数量
- 清空当前会话

规则：

- 读取返回只读快照
- 写入顺序稳定
- 查询无副作用
- 首版不内置隐式摘要压缩

## Tool Design

### Tool

`Tool` 采用极简抽象，只定义元数据与执行入口。

核心职责：

- 暴露工具定义
- 执行工具请求

### ToolDefinition

建议字段：

- `name`
- `description`
- `inputSchema`
- `outputDescription`
- `tags`
- `idempotent`

### ToolRequest

建议字段：

- `toolName`
- `arguments`
- `invocationId`
- `metadata`

### ToolContext

建议字段：

- `conversationId`
- `agentId`
- `turnId`
- `MemorySession`
- `Map<String, Object> attributes`
- 链路追踪信息

### ToolResult

建议字段：

- `success`
- `content`
- `structuredData`
- `errorCode`
- `errorMessage`
- `metadata`

### ToolRegistry

建议职责：

- 注册单个工具
- 批量注册工具
- 按名称查询工具
- 导出工具定义列表
- 路由并执行工具请求

规则：

- 工具名必须全局唯一
- 重复注册直接拒绝
- 注册中心只做查找、校验、路由与委派
- 未找到工具与工具执行失败必须区分处理
- `ToolRegistry` 只依赖 `tool` 与 `memory` 协议

## Collaboration Flow

统一协作链路如下：

```text
Paradigm Module
    -> read messages from Memory
    -> collect tool definitions from ToolRegistry
    -> build LlmRequest
    -> invoke HelloAgentsLLM
    -> receive LlmResponse / StructuredLlmResponse / stream events
    -> write assistant output or tool result back to Memory
```

边界：

- `HelloAgentsLLM` 不管理长期记忆
- `Memory` 不负责执行工具
- `ToolRegistry` 不负责调用模型

## MVP Implementation Strategy

第一版代码实现建议：

- 完成新增 3 个模块的父子 POM 与基础目录
- 在 `framework-core` 中落首批协议和默认实现骨架
- 先打通同步文本对话入口
- 完成工具调用上下文和工具协议建模
- 流式与结构化输出先定义接口与扩展点，不追求完整运行时实现

## Verification Strategy

- 更新父 `pom.xml` 的 `<modules>` 和内部模块 `dependencyManagement`
- 新增 3 个子模块的 `pom.xml`
- 为 `framework-core` 新增 Java 骨架类
- 使用 `mvn -q -Dmaven.repo.local=.m2/repository -DskipTests verify` 做构建验证

## Notes

- 包名前缀统一使用 `com.xbk.agent.framework`
- 用户已确认采用“增强预留”的统一门面方案
- 提交和推送动作仍需显式指令触发
