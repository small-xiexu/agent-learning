# module-multi-agent-engineering

## 模块定位

`module-multi-agent-engineering` 是仓库中最偏向**企业工程化**的多智能体实践模块。

它不关注"哪个 Agent 更聪明"，而是聚焦于：在高并发、强治理、长链路场景下，**多个 Agent 如何可控地协作**。

本模块以一个真实场景落地：智能客服路由——用户问题被自动识别意图（技术/销售），并路由给对应的专家 Agent 处理。

---

## 双实现总览

本模块提供两套完整实现，覆盖从消息驱动到 A2A 协议的完整知识路径：

### 手写消息驱动版（`handwritten/`）

- 手动实现 `MessageHub`，管理消息的发布、订阅与异步分发
- `InMemoryMessageHub`：线程池异步投递，基础版本
- `MqBackedMessageHub`：RocketMQ 驱动，扩展到跨进程投递
- `HandwrittenEngineeringCoordinator`：同步入口，内部通过消息链异步驱动
- 所有 Agent 通过 `MessageHub.subscribe/publish` 通信，没有直接调用链

适合理解：**消息驱动范式的本质、异步回包机制、DeliveryAuditLog 轨迹记录**。

### Spring AI Alibaba A2A 框架版（`framework/`）

- 使用 A2A（Agent-to-Agent）协议进行跨进程通信
- Provider 侧：每个专家 Agent 独立成进程，暴露 `/.well-known/agent.json` + JSON-RPC endpoint
- Consumer 侧：Receptionist 通过 Nacos 发现专家 Agent，同步调用 A2A HTTP 接口
- MQ 仅作为**增强层**（审计/升级/回调），不替代 A2A 主通信链路

适合理解：**A2A 协议注册发现、生产级多进程部署、MQ 作为异步补偿的正确角色**。

---

## 核心共享组件

两套实现共用以下组件，保证业务语义一致：

| 组件 | 路径 | 职责 |
|---|---|---|
| `EngineeringRunResult` | `api/` | 统一结果模型，手写版与框架版都返回此结构 |
| `CustomerIntentClassifier` | `application/routing/` | 意图分类，两版共用，调用 `AgentLlmGateway` |
| `RoutingDecision` | `domain/routing/` | 路由决策模型，含意图类型、专家类型、目标 topic/agent |
| `EngineeringPromptTemplates` | `support/` | 三类角色提示词模板（接待员/技术/销售） |
| `AgentLlmGateway` | `framework-core` | 所有模型调用的唯一出口，不允许绕过 |

---

## 推荐阅读顺序

### 阶段一：理解消息驱动基础

1. `domain/message/MessageTopic.java` — 消息主题枚举，先理解有哪些消息类型
2. `handwritten/hub/MessageHub.java` — 最小接口：subscribe/publish/close
3. `handwritten/hub/InMemoryMessageHub.java` — 内存实现，看线程池异步分发
4. `handwritten/hub/AsyncMessageDispatcher.java` — 分发机制细节
5. `handwritten/runtime/DeliveryAuditLog.java` — 审计记录，理解消息可观测性

### 阶段二：理解 Agent 协作机制

6. `handwritten/agent/AbstractHandwrittenAgent.java` — Agent 基类，receive/send 协议
7. `handwritten/agent/HandwrittenReceptionistAgent.java` — 接待员：意图识别 + 路由 + 等待回包
8. `handwritten/agent/HandwrittenTechSupportAgent.java` — 技术专家：消费主题并回包
9. `handwritten/coordinator/HandwrittenEngineeringCoordinator.java` — 同步入口，异步驱动

### 阶段三：理解 A2A 框架版

10. `framework/config/A2aNacosCommonConfig.java` — A2A 公共配置，Nacos 连接与 Agent 命名
11. `framework/support/EngineeringAgentCardSupport.java` — AgentCard 构建，专家名/描述/技能
12. `framework/config/TechSupportA2aServerConfig.java` — Provider 侧装配，暴露 A2A endpoint
13. `framework/client/SpecialistRemoteAgentLocator.java` — 从 Nacos 发现 Agent URL
14. `framework/client/TechSupportRemoteAgentFacade.java` — A2A 调用封装
15. `framework/agent/FrameworkReceptionistService.java` — 框架版接待员：分类 + 调用 Facade

### 阶段四：理解 MQ 增强层

16. `framework/messaging/RoutingAuditEventPublisher.java` — 路由审计事件
17. `framework/messaging/SpecialistEscalationPublisher.java` — 专家升级任务投递
18. `framework/messaging/AsyncResultCallbackListener.java` — 异步回调监听基类
19. `framework/config/EngineeringMqEnhancementConfig.java` — MQ 开关配置（enabled=false 即 no-op）

### 阶段五：理解 MqBackedMessageHub

20. `handwritten/mq/RocketMqTopicBindingSupport.java` — 逻辑主题 → RocketMQ 目的地映射
21. `handwritten/mq/RocketMqMessageProducer.java` — 消息序列化 + convertAndSend
22. `handwritten/hub/MqBackedMessageHub.java` — MQ 版本，语义与 InMemoryMessageHub 同构

---

## Demo 入口

### 手写版本地运行

```
# 直接运行测试（不依赖外部服务）
mvn -pl module-multi-agent-engineering test -Dtest=HandwrittenEngineeringRoutingTest

# 开启 OpenAI 真实模型 Demo（需配置 API Key）
mvn -pl module-multi-agent-engineering test \
  -Dtest=HandwrittenEngineeringOpenAiDemo \
  -Dopenai.engineering.demo=true
```

### 框架版 A2A 本地集成运行

需要先启动本地 Nacos（默认 8848 端口）：

```
# 1. 启动技术专家 Provider（8081 端口）
# profile: a2a-tech-provider,a2a-nacos-local

# 2. 启动销售顾问 Provider（8082 端口）
# profile: a2a-sales-provider,a2a-nacos-local

# 3. 启动 Receptionist Consumer（8080 端口）
# profile: a2a-receptionist-consumer,a2a-nacos-local

# 4. 运行完整 A2A 集成测试
mvn -pl module-multi-agent-engineering test \
  -Dtest=FrameworkA2aRoutingTest \
  -Da2a.integration=true
```

### 对照测试（无外部依赖）

```
# 验证两套实现路由语义一致
mvn -pl module-multi-agent-engineering test -Dtest=EngineeringComparisonTest
```

---

## 三进程本地部署说明

框架版设计为**三个独立 Spring Boot 应用**：

| 应用 | Profile | 端口 | 职责 |
|---|---|---|---|
| `TechSupportProviderApplication` | `a2a-tech-provider` | 8081 | 技术专家 Agent，暴露 A2A endpoint |
| `SalesProviderApplication` | `a2a-sales-provider` | 8082 | 销售顾问 Agent，暴露 A2A endpoint |
| `ReceptionistConsumerApplication` | `a2a-receptionist-consumer` | 8080 | 接待员，Nacos 发现 + A2A 调用 |

前置条件：
- Nacos Server 已启动（`127.0.0.1:8848`）
- Provider 应用已向 Nacos 注册（serviceName 分别为 `tech-support-agent`、`sales-agent`）
- 应用 properties 中已填写真实 LLM API Key

---

## MQ 增强层说明

MQ（RocketMQ）在本模块中**只承担增强层职责**，不替代 A2A 主通信链路：

- `engineering.mq.enabled=false`（默认）：MQ 增强层完全 no-op，A2A 链路不受影响
- `engineering.mq.enabled=true`：启用三条 MQ 主题：
  - `engineering.audit`：每次路由决策的审计事件
  - `engineering.escalation`：A2A 超时后的专家升级任务
  - `engineering.callback`：长耗时专家处理完成后的异步回调

本地 RocketMQ 测试 profile：`engineering-mq-rocketmq`（需本地 Broker 已启动，端口 9876）。

---

## 与 framework-core 的关系

- `AgentLlmGateway`：所有模型调用的唯一出口，两套实现都不绕过
- `Message` / `Memory`：提供统一消息语义
- `EngineeringGatewayBackedChatModel`：把 Spring AI `ChatModel` 调用适配到 `AgentLlmGateway`
