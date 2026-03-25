# agent-learning

> 基于 Spring Boot + Spring AI Alibaba 的 Agent 范式学习仓库。
> 目标读者：有 Java / Spring Boot 开发经验、正在转型学习 Agent 应用开发的工程师。

---

## 写在前面：Java 工程师如何理解 Agent

作为 Java 工程师，你可能已经很熟悉"Service 调用 DAO、DAO 访问数据库"这套分层结构。**Agent 应用的核心变化在于：执行流不再是你写死的 `if/else` 或固定调用链，而是由大模型（LLM）在运行时动态决策的。**

一个最简单的 Agent 的工作方式是这样的：

```
用户输入
  → LLM 推理（我该做什么？调哪个工具？）
  → 执行工具（查数据库、调接口、搜索...）
  → 把结果喂回 LLM 继续推理
  → 直到 LLM 判断任务完成，输出最终结果
```

这个"推理 → 行动 → 观察 → 再推理"的循环，就是 Agent 的基本运行模型。

当系统里出现多个 Agent 互相协作时，就变成了**多智能体（Multi-Agent）**系统——多个 Agent 分工合作，有的负责规划，有的负责执行，有的负责审查，共同完成复杂任务。

**本项目要解决的问题**：如何系统地学习并工程化落地这些 Agent 协作范式（即已被验证有效的设计模式），同时保证代码结构清晰、可对照、可扩展。

---

## 环境要求

| 依赖 | 版本要求 |
| --- | --- |
| JDK | 21+ |
| Maven | 3.9+ |
| Spring Boot | 3.5.9 |
| Spring AI | 1.1.2 |
| Spring AI Alibaba | 1.1.2.0 |

---

## 快速开始

每个范式模块的 Demo 均以 **JUnit 测试**形式运行，无需启动 Spring Boot 服务。

**第一步：配置本地模型参数**

每个模块的 `src/test/resources/` 下都有一个 `application-*-local.yml.example` 文件，复制并去掉 `.example` 后缀：

```bash
# 以 ReAct 模块为例
cp module-react-paradigm/src/test/resources/application-openai-react-demo-local.yml.example \
   module-react-paradigm/src/test/resources/application-openai-react-demo-local.yml
```

然后填入你自己的模型参数：

```yaml
llm:
  provider: openai-compatible   # 固定值，使用 OpenAI 兼容协议
  base-url: https://api.openai.com   # 替换为你的模型服务地址
  api-key: your-api-key              # 替换为你的 API Key
  model: gpt-4o                      # 替换为你要使用的模型名
  chat-completions-path: /v1/chat/completions
```

> 项目使用 `openai-compatible` 协议，兼容 OpenAI、通义千问、DeepSeek 等主流服务，只需修改 `base-url`、`api-key`、`model` 三个字段即可切换。

**第二步：运行第一个 Demo**

建议从 `module-react-paradigm` 开始，直接在 IDE 中运行对应测试类即可。

---

## 项目结构

```
agent-learning/
├── framework-core/                     # 核心协议层：统一消息、LLM 请求/响应、工具、记忆接口定义
├── framework-llm-autoconfigure/        # 自动装配层：读配置、选 Provider、装配 AgentLlmGateway Bean
├── framework-llm-springai/             # 模型适配层：对接 Spring AI / OpenAI Compatible SDK
│
├── module-react-paradigm/              # ReAct 范式（推理 → 行动 → 观察闭环）
├── module-plan-replan-paradigm/        # Plan-and-Solve 范式（先规划再执行）
├── module-reflection-paradigm/         # Reflection 范式（生成 → 反思 → 改进）
├── module-graph-flow-paradigm/         # LangGraph 范式（状态图驱动复杂工作流）
├── module-multi-agent-roleplay/        # CAMEL 范式（双 Agent 角色扮演接力）
├── module-multi-agent-conversation/    # AutoGen 范式（多 Agent 群聊协作）
├── module-multi-agent-supervisor/      # Supervisor 范式（中心化监督者调度）
└── module-multi-agent-engineering/     # AgentScope 范式（生产级工程化协同，演进中）
```

---

## 🎯 项目愿景与核心方法论

`agent-learning` 的目标，不是堆砌零散 Demo，而是系统性复刻主流 Agent 协作范式，并把"范式本体"与"企业级工程化落地"放进同一套仓库中长期对照。

> **范式**：可以理解为"已被学术界或工业界验证有效的 Agent 协作设计模式"。比如 ReAct（推理+行动）、Reflection（生成+反思）、Supervisor（集中调度）等，都是有正式论文或框架背书的经典模式。

项目的核心方法论是 **"左右互搏、对照学习"**：

- **左手（手写版）**：用纯 Java 代码拆开底层状态流转与执行流，真正看清 Agent Runtime 的本质。
- **右手（框架版）**：用 Spring AI Alibaba 的高阶抽象，把同一范式重构为可扩展、可治理、可观测的企业级实现。
- **中轴（统一约束）**：两条线都必须统一依赖 `AgentLlmGateway` 防腐层，保证范式演进与模型接入解耦。

| 维度 | 探究本质（纯手写版） | 企业级落地（框架版） |
| --- | --- | --- |
| 核心目标 | 还原范式原貌，理解运行时到底怎么跑 | 验证范式如何稳定落地到生产工程 |
| 关注重点 | `while` 循环、状态机、消息接力、终止条件、显式路由 | `SupervisorAgent`、`Handoffs`、`FlowAgent`、`StateGraph` 等高阶能力 |
| 学习价值 | 看到"底牌"，避免只会调用框架 API | 看到"工程面"，避免停留在玩具 Demo |
| 约束边界 | 必须显式暴露状态、路由、终止逻辑 | 必须使用框架原生抽象，不能退化回普通手写循环 |
| 统一原则 | 两个版本解决同一道题 | 两个版本都必须走统一 `AgentLlmGateway` |

一句话概括本项目：**用手写版理解范式本体，用框架版理解企业落地，用统一 Gateway 守住工程边界。**

---

## 🏗️ 四层架构：为什么这样分层

**先说痛点，再看分层。** 假设没有任何抽象，你的 Agent 代码直接依赖阿里云模型的 SDK 发起对话请求。某天你想换成 OpenAI，或者同时支持两家——你需要改遍所有 Agent 代码。这与 Java 里"直接在 Service 里写 JDBC 代码"是同一个问题。

解决方案也是同一个思路：**分层隔离**。

```
范式实验层   ≈  你的业务 Service 层（只管业务逻辑，不关心底层实现）
自动装配层   ≈  Spring Boot AutoConfiguration（读配置、按条件选实现、装配 Bean）
模型适配层   ≈  DAO 具体实现层（对接不同数据库驱动 / 这里是对接不同模型 SDK）
核心协议层   ≈  接口 / 协议定义层（相当于 JPA 的 Repository 接口 + 统一模型）
```

| 层次 | 模块 | 核心职责 | 稳定边界 |
| --- | --- | --- | --- |
| 核心协议层 | `framework-core` | 定义统一的消息、记忆、工具、LLM 请求与响应协议 | `AgentLlmGateway`、`LlmRequest`、`LlmResponse`、`Message`、`Memory`、`ToolRegistry` |
| 自动装配层 | `framework-llm-autoconfigure` | 基于统一 `llm.*` 配置自动选择 Provider 并装配网关 | 只向上暴露统一 `AgentLlmGateway` Bean |
| 模型适配层 | `framework-llm-springai` | 基于 Spring AI 完成具体 Provider 适配与能力映射 | 把底层模型差异收敛到统一协议之下 |
| 范式实验层 | `module-*` | 落地不同 Agent / Workflow / Multi-Agent 范式 | 只消费统一协议，不关心底层 Provider 细节 |

### `AgentLlmGateway`：整个架构的核心边界

`AgentLlmGateway` 是整个项目的**统一模型门面**，也就是所有范式模块与底层模型之间的防腐层（Anti-Corruption Layer）。

> **防腐层**：借鉴 DDD 中的概念，指在两个系统边界之间建立一层翻译/隔离，防止外部系统的细节"污染"内部模型。这里的作用是：防止某家模型 SDK 的私有 API、数据结构渗透进业务模块。

它对外暴露三种能力：

| 方法 | 说明 |
| --- | --- |
| `chat(request)` | 同步对话，最常用 |
| `stream(request, handler)` | 流式输出（token 逐步返回），底层需支持 |
| `structuredChat(request, spec)` | 结构化输出（直接返回强类型 Java 对象），底层需支持 |

**项目铁律**：所有范式模块只能通过 `AgentLlmGateway` 与模型通信，禁止直接依赖 `ChatModel`、厂商 SDK 或私有接口。任何模块绕过网关直连模型，都视为破坏架构边界。

这条铁律的意义在于：**范式模块研究的是"怎么组织智能体协作"，而不是"怎么调某家模型的 API"。** 两件事必须解耦。

---

## 🧩 各范式模块一览

> **名词说明**
> - **ReAct**：Reasoning + Acting，推理与行动交替的单 Agent 范式，来自 Google 2022 年论文。
> - **Reflection**：Agent 对自己的输出进行反思和修正的范式。
> - **Plan-and-Solve**：先规划步骤、再逐步执行的范式。
> - **CAMEL**：两个互补角色扮演、持续接力完成任务的双 Agent 范式。
> - **AutoGen**：多个 Agent 围绕共享对话历史进行群聊协作的范式，由微软提出。
> - **Supervisor**：一个中心化的监督者统一调度多个 Worker Agent 的范式。
> - **LangGraph**：用显式状态图（节点 + 边 + 条件路由）描述复杂工作流的范式，由 LangChain 提出。
> - **AgentScope**：面向生产级工程的消息驱动、状态隔离、跨进程协作范式，由阿里提出。

| 模块 | 对应范式 | 一句话定位 |
| --- | --- | --- |
| `module-react-paradigm` | ReAct | 最基础的 Agent 闭环：推理 → 调工具 → 观察 → 继续推理 |
| `module-plan-replan-paradigm` | Plan-and-Solve | 增强型单 Agent：先让 LLM 制定计划，再按计划逐步执行 |
| `module-reflection-paradigm` | Reflection | 增强型单 Agent：先生成结果，再让另一个角色反思和批评，循环改进 |
| `module-graph-flow-paradigm` | LangGraph | 复杂工作流底座：用状态图替代 `if/else`，让流程可审计、可观测 |
| `module-multi-agent-roleplay` | CAMEL | 双 Agent 接力：两个互补角色（如程序员 + 交易员）受控交棒完成任务 |
| `module-multi-agent-conversation` | AutoGen | 多 Agent 群聊：多个专家围绕共享对话历史轮番发言、协同推进 |
| `module-multi-agent-supervisor` | Supervisor | 中心化调度：一个监督者拆解任务、分发给 Worker、收集结果、决策下一步 |
| `module-multi-agent-engineering` | AgentScope | 生产级工程：消息驱动、状态隔离、跨服务 Agent 协同（演进中） |

每个模块都提供**手写版**和**框架版（Spring AI Alibaba）**两套实现，解决同一道业务题，方便直接对比。详细说明参见各模块自己的 README。

### 配套导读入口

- `module-graph-flow-paradigm`：详见《[Graph Flow 范式从 0 到 1 掌握指南](./docs/GraphFlow范式从0到1掌握指南.md)》
- `module-multi-agent-conversation`：详见《[AutoGen 范式从 0 到 1 掌握指南](./docs/AutoGen范式从0到1掌握指南.md)》
- `module-multi-agent-supervisor`：详见《[Supervisor 范式从 0 到 1 掌握指南](./docs/Supervisor范式从0到1掌握指南.md)》
- 框架版通用组件（ReactAgent / SupervisorAgent / OverAllState / StateGraph 等）：详见《[Spring AI Alibaba 框架核心组件指南](./docs/SpringAIAlibaba框架核心组件指南.md)》

---

## 📚 推荐学习路径

建议按"先单 Agent、再复杂工作流、后多智能体协作"的顺序推进：

| 阶段 | 推荐模块 | 学习目标 | 完成后能做到 |
| --- | --- | --- | --- |
| 第一阶段 | `module-react-paradigm` | 建立最基本的 Agent Runtime 直觉，理解推理 → 行动 → 观察闭环 | 手写一个能调用工具的 ReAct Agent，理解 LLM 如何驱动执行流 |
| 第二阶段 | `module-plan-replan-paradigm`、`module-reflection-paradigm` | 理解"先规划再执行"与"先生成再反思"的增强型单 Agent 范式 | 让 Agent 能制定多步计划并执行；让 Agent 能对自己的输出进行多轮反思和修正 |
| 第三阶段 | `module-graph-flow-paradigm` | 理解为什么复杂流程最终要上升到显式状态机和图编排 | 用状态图描述带有分支、循环的复杂工作流，而不是用嵌套 `if/else` |
| 第四阶段 | `module-multi-agent-conversation`、`module-multi-agent-supervisor`、`module-multi-agent-roleplay`、`module-multi-agent-engineering` | 系统掌握去中心化群聊、中心化调度、角色扮演交棒、分布式工程化协作 | 设计并实现多个 Agent 分工协作、互相交接任务的完整系统 |

这个顺序的核心逻辑是：**先理解单个 Agent 如何形成闭环，再理解多个 Agent 如何共享状态、交接控制权、被统一调度，最后再进入工程化与分布式协同。**

---

## 结语

`agent-learning` 的顶层设计，本质上是一套 **"统一模型边界之上的范式实验场"**：底层用 `AgentLlmGateway` 收敛模型接入，上层用多模块承载不同智能体范式，中间用"手写版 vs 框架版"的持续对照，把抽象理论转化为可验证、可复用、可工程化的知识体系。

项目未来不是简单增加更多 Demo，而是沿着这条主线持续演进：**每新增一个范式，都要同时回答它的理论归属、运行时本质、企业级映射以及统一 Gateway 边界。**
