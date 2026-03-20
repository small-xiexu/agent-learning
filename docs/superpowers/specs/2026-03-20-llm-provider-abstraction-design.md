# 统一 LLM Provider 抽象与接入层重构设计

## 1. 背景与问题

当前仓库已经具备较清晰的 Agent 运行时抽象，例如 `AgentLlmGateway`、`LlmRequest`、`LlmResponse`、`ToolCall` 与 `ModelOptions`。`ReActAgent` 等上层运行时本身并不依赖具体厂商协议，这一点设计方向是正确的。

但在真实模型接入层面，当前工程仍存在几个明显问题：

- 真实模型 demo 直接依赖 `spring.ai.openai.*` 配置。
- `framework-core` 中混入了 Spring AI 适配实现，核心层与厂商接入层边界不够纯净。
- 上层虽然通过统一接口发起调用，但配置层和装配层仍然暴露 OpenAI/Spring AI 细节。
- 如果后续继续接入 OpenAI 兼容网关、DashScope、Anthropic、Gemini 等不同 provider，扩展成本会持续上升。

当前的主要问题不在 ReAct 主流程，而在于模型配置、provider 选择与底层适配尚未形成统一边界。

## 2. 设计目标

本次设计的目标如下：

- 让业务层、Agent 运行时和范式模块不感知底层厂商实现。
- 引入统一的 `llm.*` 配置前缀，替代上层直接依赖 `spring.ai.openai.*`。
- 将 provider 差异收敛到独立的 adapter 层处理。
- 为多 provider 扩展提供标准入口，但避免过度抽象。
- 以 Spring AI 作为第一版统一接入底座，优先完成边界收敛与抽象落地。
- 在不改写 `ReActAgent` 主流程的前提下，完成真实模型接入边界重构。

## 3. 非目标

以下内容不在本次设计范围内：

- 不在第一阶段支持所有厂商能力的完全对齐。
- 不追求把所有 provider 特性都抽象成统一一等字段。
- 不修改 `ReActAgent` 的 Thought -> Action -> Observation 主执行逻辑。
- 不在第一阶段重构所有业务模块，仅先覆盖真实模型接入链路。
- 不要求一次性完成全部目录迁移和所有 demo 改造。

## 4. 总体架构

目标架构分为三层：

### 4.1 framework-core

职责：

- 定义统一的 LLM 协议与运行时抽象。
- 定义统一请求、响应、能力和工具调用模型。
- 作为业务层与底层 provider 的稳定边界。

应保留在该层的对象包括但不限于：

- `AgentLlmGateway`
- `DefaultAgentLlmGateway`
- `LlmClient`
- `LlmRequest`
- `LlmResponse`
- `ToolCall`
- `ModelOptions`

该层不应继续承载以下内容：

- Spring AI 具体实现
- OpenAI/Anthropic/Gemini 等厂商命名
- `base-url`、`api-key` 一类接入配置
- Spring Boot 自动装配与 `@ConfigurationProperties`

### 4.2 framework-llm-springai

职责：

- 承载所有基于 Spring AI 的 provider 接入实现。
- 完成统一协议对象与 Spring AI 模型对象之间的映射。
- 作为 OpenAI compatible 或其他 Spring AI provider 的实现支点。

该层适合放置：

- `SpringAiLlmClient`
- `SpringAiMessageMapper`
- `SpringAiOptionsMapper`
- `SpringAiResponseMapper`
- `SpringAiModelResolver`
- 基于 Spring AI 的具体 provider adapter

### 4.3 framework-llm-autoconfigure

职责：

- 承载统一的 `llm.*` 配置模型。
- 收集并管理可用的 provider adapter。
- 自动装配统一的 `AgentLlmGateway` 或 `LlmClient` Bean。

该层负责把统一配置翻译成具体 provider 所需的接入对象，但不承载业务逻辑。

### 4.4 业务与范式模块

如 `module-react-paradigm`、`module-plan-replan-paradigm` 等模块只应依赖统一入口，不应再直接感知 OpenAI、Spring AI 或具体聊天路径。

目标调用链如下：

`业务模块 / Agent Runtime -> AgentLlmGateway -> LlmClient -> ProviderAdapter -> 底层 SDK / 模型服务`

## 5. 配置模型设计

建议统一引入 `llm.*` 配置，并以稳定共性字段为主：

- `llm.provider`
- `llm.base-url`
- `llm.api-key`
- `llm.model`
- `llm.chat-completions-path`
- `llm.timeout`
- `llm.capabilities.*`
- `llm.provider-options.*`

### 5.1 字段语义

`provider`

- 表示 provider 标识，而不是要求上层感知具体实现类。
- 第一阶段建议先从稳定的字符串标识开始，例如：
  - `openai-compatible`
  - `dashscope`
  - `anthropic`
  - `gemini`
- 不要求第一阶段就把所有 provider 都真正实现。

`base-url`

- 统一表示模型服务根地址。
- 对 OpenAI compatible provider，可映射到 Spring AI 的 `spring.ai.openai.base-url`。

`api-key`

- 统一表示认证凭证。
- 具体认证头、鉴权形式由 provider adapter 负责解释。

`model`

- 统一表示目标模型名。
- 真正传给底层 provider 的模型值由 adapter 负责处理。

`chat-completions-path`

- 用于兼容聊天路径非默认值的 OpenAI compatible 服务。
- 该字段属于接入层配置，不应泄漏到业务语义中。

`timeout`

- 统一超时配置。

`capabilities`

- 表示当前 provider 在当前配置下声称支持的能力，例如：
  - `tool-calling`
  - `streaming`
  - `structured-output`

`provider-options`

- 用于承载厂商特有参数。
- 例如 `extra-body`、租户信息、附加 header 或特定采样参数。

### 5.2 设计原则

- 统一配置层只抽稳定共性，不追求覆盖所有厂商特性。
- 超出统一协议范围的能力与参数，统一进入 `provider-options`。
- 上层默认使用公共能力，provider 差异通过 adapter 层显式处理。

### 5.3 OpenAI compatible 接入说明

第一阶段优先支持 OpenAI compatible 模型服务，因此需要明确统一配置的填写方式。

当接入方提供类似如下调用方式时：

```bash
curl https://example.com/v1/chat/completions \
  -H "Authorization: Bearer <api-key>" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "1+1"}]
  }'
```

统一配置应按如下规则填写：

- `llm.provider=openai-compatible`
- `llm.base-url=https://example.com`
- `llm.api-key=<api-key>`
- `llm.model=gpt-4o`
- `llm.chat-completions-path=/v1/chat/completions`

关键约束如下：

- `llm.base-url` 应填写服务根地址，而不是完整聊天接口地址。
- `llm.chat-completions-path` 默认值可约定为 `/v1/chat/completions`。
- 只有当服务端聊天路径不是默认值时，才显式覆盖 `llm.chat-completions-path`。
- `llm.api-key` 表示逻辑认证凭证，不要求上层关心具体 header 拼装方式。
- Bearer Token、兼容头或其他认证细节由 `OpenAiCompatibleProviderAdapter` 负责处理。

### 5.4 `llm.*` 到 Spring AI 配置的映射

在第一阶段，`OpenAiCompatibleProviderAdapter` 基于 Spring AI 实现，因此需要明确统一配置与底层配置的映射关系。

建议映射如下：

- `llm.base-url` -> `spring.ai.openai.base-url`
- `llm.api-key` -> `spring.ai.openai.api-key`
- `llm.model` -> `spring.ai.openai.chat.options.model`
- `llm.chat-completions-path` -> `spring.ai.openai.chat.completions-path`

如有需要，也可将统一超时和扩展参数进一步映射到 Spring AI 对应配置或运行时选项对象。

这一映射规则的目标是：

- 上层统一写 `llm.*`
- Spring AI 相关细节只存在于 adapter 层
- 后续如果底层从 Spring AI 切换到其他接入底座，仅调整 adapter 模块，不影响上层配置语义

## 6. 核心对象设计

建议新增以下核心对象：

### 6.1 `provider-id`

职责：

- 作为统一配置中的 provider 标识。
- 作为 adapter 发现和匹配依据。
- 第一阶段优先使用字符串标识，而不是硬编码枚举，避免每新增一个 provider 都修改中心代码。

### 6.2 `LlmProperties`

职责：

- 承载统一的 `llm.*` 配置。
- 作为自动装配与 provider adapter 的统一输入。

### 6.3 `LlmCapabilitiesProperties`

职责：

- 描述 provider 当前声明支持的能力。
- 为后续能力校验和降级提供基础数据。

### 6.4 `ProviderAdapter`

职责：

- 定义统一的 provider 接入接口。
- 屏蔽底层 SDK、starter 与协议差异。

建议该接口至少回答两个问题：

- 是否支持某个 `provider-id`
- 如何根据 `LlmProperties` 构建统一 `LlmClient` 或 `AgentLlmGateway`

### 6.5 `ProviderAdapterRegistry`

职责：

- 收集所有已注册的 `ProviderAdapter`。
- 根据 `provider-id` 选择匹配的 adapter。
- 避免业务层直接依赖某个 provider 实现。
- 避免通过 `switch`、硬编码枚举或中心工厂把 provider 路由写死。

### 6.6 `AgentLlmGatewayAutoConfiguration`

职责：

- 基于 `LlmProperties` 和 `ProviderAdapterRegistry` 自动装配统一网关。
- 作为业务模块的默认入口。

### 6.7 `OpenAiCompatibleProviderAdapter`

职责：

- 第一阶段优先支持 OpenAI compatible 模型服务。
- 基于 Spring AI 完成第一版统一接入实现。
- 负责把统一配置映射到 Spring AI OpenAI compatible 调用方式。

### 6.8 可插拔扩展原则

后续如果引入新的接入底座，例如 `framework-llm-googleadk` 或 `framework-llm-langchain4j`，目标应是：

- 新增一个接入模块
- 在该模块中提供新的 `ProviderAdapter` 实现
- 通过自动发现机制被 `framework-llm-autoconfigure` 收集
- 尽量不改 `framework-core`、ReAct Runtime 与业务模块

只有在新增 provider 需要引入新的公共能力语义时，才考虑调整 core 协议。

## 7. Provider 选择与调用流程

运行时流程建议如下：

1. 启动阶段读取 `llm.*` 配置。
2. 自动装配层收集所有 `ProviderAdapter` 实现。
3. `ProviderAdapterRegistry` 根据 `llm.provider` 选择具体 adapter。
4. adapter 根据统一配置构建底层 `LlmClient`。
5. `DefaultAgentLlmGateway` 对外暴露统一调用入口。
6. 业务模块和 Agent Runtime 仅依赖 `AgentLlmGateway`。

该流程的关键收益在于：

- 新增 provider 时不需要修改 ReAct 主流程。
- 业务模块无需感知底层 SDK 与具体聊天路径。
- provider 之间的差异被限制在 adapter 层内部。
- 如果接入层设计为自动发现，新 provider 通常以“新增模块”为主，而不是修改中心路由代码。

## 8. 兼容性与迁移策略

本次设计建议采用“两阶段迁移”策略，而不是一次性重构。

### 8.1 第一阶段：建立新边界

目标：

- 引入 `llm.*` 统一配置。
- 明确 Spring AI 是第一版统一接入底座。
- 新增 `LlmProperties`、`ProviderAdapter`、`ProviderAdapterRegistry`。
- 新增统一自动装配入口。
- 优先支持 `openai-compatible`。
- 让 `module-react-paradigm` 之类的真实模型 demo 摆脱 `spring.ai.openai.*` 暴露。

这一阶段允许：

- Spring AI 相关类暂时仍位于现有位置。
- 旧 demo 与旧配置短期并存，以降低迁移风险。

### 8.2 第二阶段：净化 core 边界

目标：

- 将 Spring AI 相关 adapter 从 `framework-core` 迁移到独立模块。
- 让 `framework-core` 仅保留纯协议与运行时抽象。
- 为后续非 Spring AI provider 实现留出清晰扩展位。

该阶段完成后，`framework-core` 不应继续承载任何具体厂商接入实现。

## 9. 风险与权衡

### 9.1 能力不一致风险

不同 provider 在 tool calling、streaming、structured output 等能力上并不完全一致。设计上不能假设所有 provider 拥有完全相同语义。

应对策略：

- 统一抽象仅覆盖公共能力。
- 厂商差异通过 adapter 和 `provider-options` 显式收口。

### 9.2 过度抽象风险

如果过早把所有厂商特性都抽象成统一字段，会导致配置模型膨胀、语义失真和维护成本上升。

应对策略：

- 只抽稳定共性。
- 把非共性参数放入扩展位。

### 9.3 一次性重构风险

如果立即完成目录迁移、配置切换、demo 改造和 provider 扩展，变更面会过大。

应对策略：

- 架构目标一次定清。
- 迁移步骤分阶段落地。

## 10. 验证方案

以下结果可作为本次设计落地后的验收标准：

- `module-react-paradigm` 的真实模型 demo 可以通过统一 `AgentLlmGateway` 跑通。
- 上层模块不再直接出现 `spring.ai.openai.*` 配置依赖。
- 新增 provider adapter 时，无需修改 `ReActAgent` 主流程。
- 新增 provider adapter 时，优先通过新增接入模块与注册 adapter 完成扩展。
- `framework-core` 最终不再承载具体厂商接入实现。
- OpenAI compatible 接口支持通过 `base-url`、`api-key`、`model` 和可选 `chat-completions-path` 完成接入。

## 11. 第一阶段实施范围

第一阶段建议聚焦以下范围：

- 新增统一 `llm.*` 配置模型。
- 新增 provider 能力配置对象。
- 新增 `ProviderAdapter` 与 `ProviderAdapterRegistry`。
- 新增 `OpenAiCompatibleProviderAdapter`。
- 新增统一自动装配入口。
- 改造真实模型 demo 使用统一网关入口。

第一阶段暂不包含：

- 多 provider 的完整实现
- 所有范式模块的全面迁移
- 所有 Spring AI 适配类的目录迁移

## 12. 结论

本设计的核心原则是：

- 上层无感
- 接入层有感
- 公共能力统一
- 厂商差异显式收口

该方案能够在不改动 Agent 主运行时的前提下，逐步建立统一的多 provider 接入边界，并为后续引入更多模型服务保留清晰的演进路径。
