# Multi-Agent Engineering Nacos & RocketMQ 增强技术方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 针对 `module-multi-agent-engineering` 现有 Nacos + RocketMQ 集成方案的 6 项改进，提升运维弹性、可观测性和生产就绪度。

**Architecture:** 在保持现有"handwritten 消息驱动版 + framework A2A 版"双实现结构不变的前提下，逐项增强基础设施集成能力。每项改进独立可交付、可验证，互不阻塞。

**Tech Stack:** Java 21, Maven, Spring Boot 3.5, Nacos 2.x (Config + Discovery), RocketMQ 5.x, Resilience4j / Sentinel, OpenTelemetry, JUnit 5

---

## 现状总结

| 维度 | handwritten 版 | framework 版 |
|---|---|---|
| **Nacos** | 未使用 | 服务发现（`SpecialistRemoteAgentLocator` 查询 Provider URL） |
| **RocketMQ** | 主通信通道（`MqBackedMessageHub` 映射 5 个逻辑 topic 到 `ENGINEERING_MSG:TAG`） | 可选增强层（审计 / 升级 / 回调，默认 disabled） |
| **容错** | 无 | 仅 30s timeout，无断路器 |
| **可观测** | `DeliveryAuditLog`（publish/deliver/consume 三阶段） | `A2aInvocationTraceSupport`（START/SUCCESS/FAILURE 日志） |
| **配置管理** | topic 映射硬编码在 `RocketMqTopicBindingSupport` 静态块 | YAML + `@ConfigurationProperties` |

---

## 改进一：Nacos 承担配置中心职责

### 1.1 问题

当前 Nacos **仅用于服务发现**（`A2aNacosCommonConfig` 只配了 `spring.cloud.nacos.discovery`）。Prompt 模板、路由规则、MQ topic 映射、超时阈值等运行时参数写死在代码或 YAML 里，修改必须重启进程。

### 1.2 方案

引入 Nacos Config 作为运行时配置中心，支持热更新。

**配置分层策略：**

| 层级 | 存储位置 | 内容 | 变更频率 |
|---|---|---|---|
| 基础设施层 | 本地 YAML | Nacos 地址、RocketMQ nameserver、端口 | 极低（部署时确定） |
| 业务参数层 | Nacos Config | 路由规则、超时阈值、MQ topic 名、开关 | 中（运维调整） |
| Prompt 模板层 | Nacos Config | 意图分类 prompt、专家系统 prompt | 高（Prompt 工程迭代） |

**Nacos Config DataId 规划：**

```
Group: engineering-agent

DataId: engineering-routing-rules.yml
────────────────────────────────────
engineering:
  routing:
    intent-keywords:
      tech-support: ["报错", "异常", "NullPointerException", "错误", "日志", "堆栈"]
      sales: ["报价", "购买", "方案", "部署", "license", "定价"]
    default-fallback: tech-support

DataId: engineering-prompt-templates.yml
────────────────────────────────────────
engineering:
  prompts:
    receptionist-system: |
      你是一位客服接待员，负责判断用户意图...
    tech-support-system: |
      你是一位技术支持专家...
    sales-system: |
      你是一位售前顾问...

DataId: engineering-runtime-params.yml
──────────────────────────────────────
engineering:
  a2a:
    timeout: 30000
  mq:
    enabled: true
    topic:
      audit: engineering.audit
      escalation: engineering.escalation
      callback: engineering.callback
```

**核心改动点：**

**Files:**
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/config/NacosConfigIntegrationConfig.java`
- Modify: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/config/A2aNacosCommonConfig.java`
- Modify: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/support/EngineeringPromptTemplates.java`
- Modify: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/application/routing/CustomerIntentClassifier.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/framework/NacosConfigRefreshContractTest.java`

- [ ] **Step 1:** 引入 `spring-cloud-starter-alibaba-nacos-config` 依赖，配置 bootstrap.yml 指向 Nacos Config。
- [ ] **Step 2:** 新增 `NacosConfigIntegrationConfig`，用 `@RefreshScope` 标注需要热更新的 Bean。
- [ ] **Step 3:** 将 `EngineeringPromptTemplates` 从硬编码常量改为从 Nacos Config 读取，保留本地默认值作为 fallback。
- [ ] **Step 4:** 将 `CustomerIntentClassifier` 的关键词匹配规则改为可配置，支持运行时追加新意图类型。
- [ ] **Step 5:** 将 `EngineeringMqEnhancementConfig` 的 topic 名和 enabled 开关纳入 Nacos Config，实现不重启切换 MQ 增强层。
- [ ] **Step 6:** 写契约测试验证：修改 Nacos Config 后，`@RefreshScope` Bean 确实拿到新值。

### 1.3 收益

- Prompt 模板迭代不需要发版重启
- 路由规则可运维调整（如新增意图类型、调整关键词）
- MQ 增强层可运行时开关，支持灰度发布
- 超时阈值可根据真实负载动态调优

---

## 改进二：handwritten 版接入 Nacos 实现 topic 动态绑定

### 2.1 问题

handwritten 版的 `RocketMqTopicBindingSupport` 将 5 个逻辑 topic 到 RocketMQ destination 的映射**硬编码在 static 初始化块**中：

```java
// 当前硬编码方式
BINDINGS.put(MessageTopic.CUSTOMER_REQUEST, "ENGINEERING_MSG:CUSTOMER_REQUEST");
BINDINGS.put(MessageTopic.SUPPORT_TECH_REQUEST, "ENGINEERING_MSG:SUPPORT_TECH_REQUEST");
// ...
```

如果要新增一个专家类型（如 `LEGAL_SUPPORT`），需要改代码、编译、部署。两套实现在"可运维性"上不对齐。

### 2.2 方案

让 `RocketMqTopicBindingSupport` 支持从 Nacos Config 动态拉取 topic 映射，保留本地硬编码作为默认值。

**Nacos Config DataId：**

```
Group: engineering-agent

DataId: engineering-mq-topic-bindings.yml
─────────────────────────────────────────
engineering:
  mq:
    unified-topic: ENGINEERING_MSG
    bindings:
      customer.request: CUSTOMER_REQUEST
      support.tech.request: SUPPORT_TECH_REQUEST
      support.sales.request: SUPPORT_SALES_REQUEST
      support.reply.receptionist: SUPPORT_REPLY_RECEPTIONIST
      dead.letter: DEAD_LETTER
```

**核心改动点：**

**Files:**
- Modify: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/mq/RocketMqTopicBindingSupport.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/mq/NacosTopicBindingRefresher.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/handwritten/NacosTopicBindingRefreshTest.java`

- [ ] **Step 1:** 将 `RocketMqTopicBindingSupport` 从纯静态工具类改为可注入 Bean，保留静态默认值作为 fallback。
- [ ] **Step 2:** 新增 `NacosTopicBindingRefresher`，监听 Nacos Config 变更事件，刷新 topic 映射表。
- [ ] **Step 3:** 刷新时使用 copy-on-write 策略——构建新的不可变 Map 后原子替换引用，避免并发读写问题。
- [ ] **Step 4:** 写测试验证：新增一个 topic 绑定后，`toRocketMqDestination()` 和 `buildSubscribeExpression()` 都能感知到。

### 2.3 收益

- handwritten 版与 framework 版在"可运维性"上对齐
- 新增专家类型时，只需在 Nacos 控制台添加 topic 映射，无需改代码
- 为后续动态扩展 agent 类型铺路

---

## 改进三：Dead Letter 与 Escalation 消费端补齐

### 3.1 问题

**framework 版**：`SpecialistEscalationPublisher` 会向 `engineering.escalation` topic 发送升级事件，但**没有消费端**——事件发出后石沉大海。

**handwritten 版**：`MessageTopic.DEAD_LETTER` 定义了死信 topic，`RocketMqTopicBindingSupport` 也做了映射（`ENGINEERING_MSG:DEAD_LETTER`），但**没有任何 agent 订阅**。

### 3.2 方案

为两套实现分别补齐消费端，形成闭环。

**3.2.1 framework 版：Escalation Consumer**

```
A2A 调用超时/失败
  → SpecialistEscalationPublisher 发到 engineering.escalation
    → EscalationConsumer 消费
      → 策略 1：重试另一个 specialist（如 tech 失败则尝试 sales 的通用回答能力）
      → 策略 2：生成人工工单（发到外部工单系统或打印到审计日志）
      → 策略 3：触发备用 agent（如离线知识库检索 agent）
```

**3.2.2 handwritten 版：Dead Letter Consumer**

```
消息路由失败 / 无订阅者 / 处理异常
  → 消息投入 dead.letter topic
    → DeadLetterConsumer 消费
      → 记录到 DeliveryAuditLog（补全审计链）
      → 可选：触发告警（日志级别 ERROR + 指标上报）
```

**核心改动点：**

**Files:**
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/messaging/EscalationConsumer.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/messaging/EscalationStrategy.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/mq/DeadLetterConsumer.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/framework/EscalationConsumerTest.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/handwritten/DeadLetterConsumerTest.java`

- [ ] **Step 1:** 定义 `EscalationStrategy` 接口（`handle(EscalationEvent)`），支持多种处理策略组合。
- [ ] **Step 2:** 实现 `EscalationConsumer`，消费 `engineering.escalation` topic，按策略链执行。首版实现两个策略：日志记录 + 重试。
- [ ] **Step 3:** 实现 `DeadLetterConsumer`，消费 `ENGINEERING_MSG:DEAD_LETTER` tag，补全 `DeliveryAuditLog` 审计链并以 ERROR 级别打印。
- [ ] **Step 4:** 两个 Consumer 都遵循现有优雅降级模式——MQ 不可用时 no-op，消费失败不阻塞主链路。
- [ ] **Step 5:** 写测试验证：escalation 事件发出后确实被消费并执行了策略；dead letter 消息确实进入了审计日志。

### 3.3 收益

- 升级事件和死信消息不再"有去无回"
- 形成完整的异常处理闭环，具备生产运维价值
- 为后续对接外部工单系统（如 Jira、飞书工单）预留清晰扩展点

---

## 改进四：A2A 调用引入断路器保护

### 4.1 问题

当前 `SpecialistRemoteAgentLocator` + `A2aHttpTransportSupport` 的 A2A 调用链路**只有 30s timeout 保护**。当 Provider 频繁超时或宕机时：
- 每次请求都要等 30s 才失败——用户体验极差
- 大量请求堆积在 Receptionist 线程池——雪崩风险
- 无法自动降级到备选方案

### 4.2 方案

在 A2A 调用外层包装 **Resilience4j CircuitBreaker**，实现快速失败 + 自动降级。

> 选择 Resilience4j 而非 Sentinel 的理由：本模块已是 Spring Boot 3.5 生态，Resilience4j 与 Spring Boot Actuator 集成更轻量；Sentinel 更适合网关级流控场景。如果项目已有 Sentinel 基础设施，可替换。

**断路器配置参数设计：**

```yaml
# Nacos Config DataId: engineering-runtime-params.yml
engineering:
  a2a:
    circuit-breaker:
      enabled: true
      # 滑动窗口内失败率超过 50% 则打开断路器
      failure-rate-threshold: 50
      # 滑动窗口大小（最近 10 次调用）
      sliding-window-size: 10
      # 断路器打开后，等待 60s 进入半开状态
      wait-duration-in-open-state: 60s
      # 半开状态允许 3 次试探调用
      permitted-calls-in-half-open-state: 3
      # 慢调用阈值（超过 15s 视为慢调用）
      slow-call-duration-threshold: 15s
      # 慢调用占比超过 80% 也触发断路器
      slow-call-rate-threshold: 80
```

**降级策略设计：**

```
A2A 调用（正常路径）
  │
  ├─ 成功 → 返回结果
  │
  └─ 断路器打开（快速失败）
       │
       ├─ 策略 1：发送 Escalation 到 MQ（已有 SpecialistEscalationPublisher）
       ├─ 策略 2：返回友好提示（"专家暂时不可用，已记录您的问题"）
       └─ 策略 3：尝试切换到另一个 Specialist（tech 不可用则尝试 sales 的通用能力）
```

**每个 Specialist Facade 独立断路器：**

```
TechSupportRemoteAgentFacade  →  CircuitBreaker("tech-support")
SalesRemoteAgentFacade        →  CircuitBreaker("sales")
```

技术专家宕机不影响销售专家的调用。

**核心改动点：**

**Files:**
- Modify: `module-multi-agent-engineering/pom.xml` (添加 resilience4j 依赖)
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/config/A2aCircuitBreakerConfig.java`
- Modify: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/client/TechSupportRemoteAgentFacade.java`
- Modify: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/client/SalesRemoteAgentFacade.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/client/A2aFallbackHandler.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/framework/A2aCircuitBreakerTest.java`

- [ ] **Step 1:** 添加 `resilience4j-spring-boot3` 和 `resilience4j-circuitbreaker` 依赖。
- [ ] **Step 2:** 新增 `A2aCircuitBreakerConfig`，为每个 Specialist 注册独立的 `CircuitBreaker` 实例。
- [ ] **Step 3:** 在两个 Facade 的调用方法上用 `CircuitBreaker.decorateSupplier()` 包装，失败时走 `A2aFallbackHandler`。
- [ ] **Step 4:** `A2aFallbackHandler` 实现三级降级：发 Escalation → 返回友好提示 → 尝试备选 Specialist。
- [ ] **Step 5:** 断路器状态变更事件接入 `A2aInvocationTraceSupport`，形成可观测闭环。
- [ ] **Step 6:** 写测试验证：连续失败 N 次后断路器打开，后续调用快速失败并触发降级；等待恢复后半开状态允许试探。

### 4.3 收益

- Provider 故障时快速失败（毫秒级），不再每次等 30s
- 独立断路器隔离不同 Specialist 的故障域
- 自动降级 + 自动恢复，减少人工干预
- 断路器状态可通过 Actuator 端点监控

---

## 改进五：统一 Trace 链路接入 OpenTelemetry

### 5.1 问题

两套实现各有独立的 trace 机制，格式和存储方式不统一：

| 实现 | Trace 机制 | 存储 | 格式 |
|---|---|---|---|
| handwritten | `DeliveryAuditLog` | 内存 `CopyOnWriteArrayList` | `DeliveryRecord`(eventType, messageId, topic, fromAgent, toAgent) |
| framework | `A2aInvocationTraceSupport` | 日志输出 | `[A2A-INVOKE][START\|SUCCESS\|FAILURE]` 文本 |

问题：
- 两套 trace 无法关联（没有统一的 traceId/spanId）
- Nacos 调用、MQ 消息投递、LLM 调用三类操作不在同一条链路上
- 日志散落在各进程，排查跨进程问题需要人工拼接

### 5.2 方案

引入 OpenTelemetry（OTel）作为统一可观测层，保留现有 trace 类作为领域审计，新增分布式链路追踪。

**Trace Span 设计：**

```
[receptionist] handle-request (root span)
  ├── [receptionist] classify-intent
  │     └── [llm] chat-completion (LLM 调用)
  ├── [receptionist] locate-specialist (Nacos 查询)
  ├── [receptionist] invoke-specialist (A2A HTTP 调用)
  │     └── [tech-support] handle-a2a-request (跨进程 span)
  │           └── [llm] chat-completion (LLM 调用)
  ├── [receptionist] synthesize-response
  │     └── [llm] chat-completion (LLM 调用)
  └── [mq] publish-audit-event (可选，MQ 增强)
```

**Span Attributes 规范：**

| Attribute | 说明 | 示例 |
|---|---|---|
| `agent.name` | 当前 agent 名 | `receptionist-agent` |
| `agent.type` | agent 类型 | `receptionist` / `tech-support` / `sales` |
| `conversation.id` | 会话 ID | `conv-abc-123` |
| `routing.intent` | 路由意图 | `TECH_SUPPORT` |
| `routing.target` | 路由目标 | `tech-support-agent` |
| `mq.topic` | MQ topic | `engineering.audit` |
| `llm.model` | 模型名 | `gpt-4o` |
| `llm.token.total` | token 消耗 | `1024` |

**核心改动点：**

**Files:**
- Modify: `module-multi-agent-engineering/pom.xml` (添加 opentelemetry 依赖)
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/trace/OtelTracingInterceptor.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/trace/OtelSpanAttributes.java`
- Modify: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/support/A2aInvocationTraceSupport.java`
- Modify: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/runtime/DeliveryAuditLog.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/framework/OtelTracingContractTest.java`

- [ ] **Step 1:** 添加 `opentelemetry-api` 和 `opentelemetry-sdk` 依赖，配置 `OtelTracingInterceptor` 自动为关键操作创建 Span。
- [ ] **Step 2:** 在 `A2aHttpTransportSupport` 的 HTTP 调用中注入 W3C TraceContext（`traceparent` header），实现跨进程 trace 传播。
- [ ] **Step 3:** 在 `A2aInvocationTraceSupport` 中同时写入 OTel Span Event，保持现有日志输出不变，新增结构化 trace。
- [ ] **Step 4:** 在 `DeliveryAuditLog` 中为每个 DeliveryRecord 附加 spanId，使 handwritten 版的审计记录也能关联到分布式 trace。
- [ ] **Step 5:** 为 MQ 消息投递补 Span（publish 时创建、consume 时链接），使异步消息也可追溯。
- [ ] **Step 6:** 写契约测试验证：一次完整请求产生的 Span 树结构符合预期设计。

### 5.3 收益

- 跨进程 trace 关联：Receptionist → Specialist 的调用链路一目了然
- Nacos 查询、A2A 调用、LLM 调用、MQ 投递统一在一条 trace 上
- 可对接 Jaeger / Zipkin / SkyWalking 等后端，形成可视化调用链路图
- 保留现有领域审计类作为业务层日志，OTel 负责基础设施层观测

---

## 改进六：MQ 异步调用替代同步 A2A 的混合模式

### 6.1 问题

当前 framework 版是**纯同步 HTTP A2A** 调用。Receptionist 发起 HTTP POST → 等待 Specialist 返回 → 阻塞线程直到超时或成功。在高并发场景下：
- Receptionist 线程池被大量等待中的请求占满
- Specialist 承受瞬时流量冲击，容易被打垮
- 无法利用 MQ 天然的削峰填谷能力

### 6.2 方案

在现有同步 A2A 之外，新增**"请求走 MQ 投递、结果走 MQ 回调"的异步混合模式**，作为第三种可选通信方式。

> 注意：这不是替代 A2A，而是新增一种**高并发场景下的降级/备选通道**。三种模式应可通过配置切换。

**异步混合模式消息流：**

```
用户请求
  → Receptionist 分类意图
    → 发送 SpecialistRequest 到 MQ（engineering.specialist.request topic）
      → Specialist 消费请求
        → 处理完毕，发送结果到 MQ（engineering.callback topic）
          → Receptionist 的 CallbackListener 消费回调
            → 匹配 correlationId，完成 CompletableFuture
              → 返回结果
```

**通信模式配置：**

```yaml
engineering:
  communication:
    # 可选值：sync-a2a | async-mq | auto
    #   sync-a2a: 纯同步 HTTP A2A（现有默认）
    #   async-mq: 纯异步 MQ 投递 + 回调
    #   auto: 正常走 sync-a2a，断路器打开时自动切到 async-mq
    mode: auto
    async-mq:
      request-topic: engineering.specialist.request
      callback-topic: engineering.callback
      # 异步模式下的最大等待时间
      callback-timeout: 60s
```

**auto 模式决策逻辑：**

```
收到用户请求
  → 检查目标 Specialist 的断路器状态
    ├─ CLOSED（健康）→ 走 sync-a2a
    ├─ OPEN（故障）→ 走 async-mq（削峰 + 等恢复）
    └─ HALF_OPEN（试探）→ 走 sync-a2a（让断路器试探）
```

**与改进四（断路器）的协同：**

断路器打开时，不再只是快速失败返回友好提示，而是自动切到 MQ 异步通道——请求不丢失，只是响应时间变长。

**核心改动点：**

**Files:**
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/config/CommunicationModeConfig.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/client/AsyncMqSpecialistFacade.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/messaging/SpecialistRequestPublisher.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/messaging/SpecialistCallbackListener.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/client/AdaptiveSpecialistInvoker.java`
- Modify: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/agent/FrameworkReceptionistService.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/framework/AsyncMqCommunicationTest.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/framework/AdaptiveInvokerTest.java`

- [ ] **Step 1:** 新增 `CommunicationModeConfig`，定义 sync-a2a / async-mq / auto 三种模式枚举和对应参数。
- [ ] **Step 2:** 实现 `SpecialistRequestPublisher`，将 `SpecialistRequestPayload` 序列化后发到 MQ。
- [ ] **Step 3:** 实现 `SpecialistCallbackListener`，消费回调消息后通过 `PendingResponseRegistry`（复用 handwritten 版的 correlationId → Future 机制）完成异步等待。
- [ ] **Step 4:** 实现 `AsyncMqSpecialistFacade`，封装 MQ 发送 + Future 等待 + 超时处理。
- [ ] **Step 5:** 实现 `AdaptiveSpecialistInvoker`，根据配置模式和断路器状态动态选择 sync-a2a 或 async-mq 通道。
- [ ] **Step 6:** 修改 `FrameworkReceptionistService`，将直接调用 Facade 改为通过 `AdaptiveSpecialistInvoker` 间接调用。
- [ ] **Step 7:** Specialist Provider 侧新增 MQ Consumer，消费 `engineering.specialist.request` 后处理并发送回调到 `engineering.callback`。
- [ ] **Step 8:** 写测试验证：sync-a2a 模式行为不变；async-mq 模式请求经 MQ 流转后结果正确返回；auto 模式在断路器打开时自动切换。

### 6.3 收益

- 高并发场景下 MQ 天然削峰，Specialist 不会被瞬时流量打垮
- 与断路器联动，故障时请求不丢失，只是延迟交付
- 三种模式可配置切换，渐进式采用
- 复用 handwritten 版的 `PendingResponseRegistry` + `correlationId` 机制，两套实现进一步趋同

---

## 实施优先级与依赖关系

```
改进一（Nacos 配置中心）─── 基础，其他改进的配置参数都可纳入
  │
  ├── 改进二（topic 动态绑定）── 依赖改进一的 Nacos Config 基础设施
  │
  ├── 改进三（Dead Letter / Escalation 消费端）── 独立，但受益于改进一的开关配置
  │
  ├── 改进四（断路器）── 独立，但配置参数可纳入改进一
  │     │
  │     └── 改进六（MQ 异步混合模式）── 依赖改进四的断路器状态做 auto 切换
  │
  └── 改进五（OTel 统一 Trace）── 独立，但应在其他改进落地后统一接入
```

**推荐实施顺序：**

| 批次 | 改进项 | 理由 |
|---|---|---|
| **第一批** | 改进一（Nacos Config） + 改进三（消费端补齐） | 基础设施 + 快速补齐短板，风险最低 |
| **第二批** | 改进四（断路器） + 改进二（topic 动态绑定） | 提升容错能力，利用第一批的 Nacos Config 基础 |
| **第三批** | 改进六（MQ 混合模式） | 依赖断路器，复杂度最高，需充分测试 |
| **第四批** | 改进五（OTel Trace） | 横切面改进，在所有功能稳定后统一接入效果最好 |

---

## 风险与注意事项

| 风险 | 影响 | 缓解措施 |
|---|---|---|
| Nacos Config 不可用 | Prompt / 路由规则回退到本地默认值 | 所有 Nacos Config 读取都保留本地 fallback |
| 断路器误开 | 健康的 Specialist 被短暂熔断 | 调高 `slidingWindowSize`，结合慢调用率综合判断 |
| MQ 异步模式回调丢失 | 请求永久挂起 | `callbackTimeout` 兜底 + 超时后发 Escalation |
| OTel SDK 引入性能开销 | 每次调用多几微秒 | 使用采样策略（如 10% 采样率），生产环境不全量采集 |
| 三种通信模式共存增加复杂度 | 排查问题时需确认当时走的哪条路径 | 每次调用的 trace 中记录 `communication.mode` 属性 |
