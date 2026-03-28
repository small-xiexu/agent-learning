# 多智能体工程模块、A2A 协议与 MQ 增强层落地设计

## 1. 背景与目标

`agent-learning` 目前已经完成了 ReAct、Plan-and-Solve、Reflection、Graph Flow、Conversation、CAMEL、Supervisor 等多种 Agent 范式的教学落地。

`module-multi-agent-engineering` 的定位不再是“再增加一种协作写法”，而是把多智能体系统推到更接近生产工程的层面：

- 不再依赖单进程里直接传对象或共享消息历史。
- 不再把多 Agent 协作理解为“轮流说话”。
- 开始显式建模消息协议、路由主题、跨进程发现、点对点调用与工程隔离。
- 在合适的位置引入真实基础设施，让学习者看到 A2A、注册中心、消息队列在企业系统里的职责边界。

本模块的核心目标如下：

- 在一个模块中同时提供 **纯手写底层逻辑版** 和 **Spring AI Alibaba A2A 协议框架版**。
- 两个版本都统一复用仓库现有的 `AgentLlmGateway` 作为唯一大模型调用底座。
- 让学习者能够同时理解：
  - AgentScope 风格消息驱动多智能体的“本质”是什么。
  - Spring AI Alibaba A2A 在企业里如何解决 Agent 注册、发现与通信。
  - MQ 在多智能体工程里应该放在哪一层，以及为什么它不应替代 A2A 主链路。

测试业务场景统一为“分布式智能客服路由”：

- 用户提交复杂诉求。
- `ReceptionistAgent` 判断意图并路由。
- 技术问题发给 `TechSupportAgent`。
- 报价与购买问题发给 `SalesAgent`。
- 最终返回专家处理结果。

## 2. 设计目标

本次设计希望达成以下结果：

- 形成一套可教学、可测试、可对照的工程模块，而不是一次性 Demo。
- 手写版完整体现消息中间件式协作，而不是退回到直接 Java 调用。
- 框架版完整体现 A2A Server、Nacos Registry/Discovery、Remote Agent 调用链，而不是伪装成单 JVM 多 Bean。
- 把 MQ 作为增强层正式纳入设计，使手写版能够从内存 Hub 平滑演进到真实消息中间件实现。
- 两个版本的角色、输入、路由语义、输出结果保持同构，方便横向对照。
- 框架版所有本地 Agent 的模型推理仍统一通过 `AgentLlmGateway` 发起，避免引入第二套模型出口。

## 3. 非目标

以下内容不在第一版设计范围内：

- 不在手写版中实现持久化消息重试、消费位点恢复、水平扩缩容分片。
- 不在框架版中实现完整生产部署脚本、容器编排或 K8s 清单。
- 不覆盖多租户权限、鉴权网关、灰度发布等高级企业治理能力。
- 不把框架版强行实现成事件总线风格广播；第一版聚焦 A2A 点对点调用。
- 不追求把 MQ 治理能力一次性做满到生产级运维深度；首版聚焦职责边界与最小可运行集成。

## 4. 总体架构

模块内保留两条并行实现主线。

### 4.1 手写底层逻辑版

手写版要回答的问题是：

> 如果完全不依赖现成 Agent 框架，仅从消息驱动和工程解耦角度出发，多智能体协作的最小本质应该长什么样？

这里的核心不是 LLM，而是：

- 一个中心 `MessageHub`
- 一套明确的消息协议
- 基于主题订阅的消息路由
- 基于 `correlationId` 的请求-响应关联
- 基于 `replyTo` 的回包机制

### 4.2 Spring AI Alibaba A2A 协议框架版

框架版要回答的问题是：

> 当 Agent 真正分布在不同服务实例里时，如何让一个 Agent 找到另一个 Agent，并以标准协议发起调用？

这里的核心不是“自己再造一个消息总线”，而是：

- A2A Server 暴露本地 Agent 能力
- Agent Card 作为远端能力描述
- Nacos 负责 Agent 注册与发现
- Consumer 通过 `A2aRemoteAgent` 一类远端代理完成点对点调用

### 4.3 MQ 增强层

MQ 不作为本模块的“第三条并行主线”，而是作为前两条主线的增强能力层。

它主要承担以下职责：

- 为手写版提供 `MessageHub` 的真实消息中间件实现
- 为框架版提供异步审计、长耗时升级、回调通知等补充能力
- 体现 A2A 与 MQ 在企业架构里的分工，而不是让二者互相替代

## 5. 统一模型底座设计

两个版本都必须遵守同一个底层约束：

- **唯一模型出口：`AgentLlmGateway`**

具体体现如下：

- 手写版各个 Agent 直接依赖 `AgentLlmGateway`。
- 框架版 Provider 侧如果需要 `ChatModel`，必须通过类似 `*GatewayBackedChatModel` 的适配器把 Spring AI Prompt 转回 `AgentLlmGateway`。
- 不允许在 `module-multi-agent-engineering` 里直接新建一套独立于 `AgentLlmGateway` 的 OpenAI / DashScope / ChatClient 调用路径。

这样做的价值是：

- 本仓库所有范式模块始终共用一套模型抽象。
- A2A 模块体现的是“通信机制升级”，而不是“模型调用链分叉”。

## 6. 包结构设计

建议采用与 `conversation`、`roleplay`、`supervisor` 相近的分层方式：

```text
module-multi-agent-engineering
└── src/main/java/com/xbk/agent/framework/engineering
    ├── api
    │   └── EngineeringRunResult.java
    ├── application
    │   ├── coordinator
    │   │   └── EngineeringScenarioCoordinator.java
    │   └── routing
    │       └── CustomerIntentClassifier.java
    ├── config
    │   ├── EngineeringModuleProperties.java
    │   └── package-info.java
    ├── domain
    │   ├── message
    │   │   ├── EngineeringMessage.java
    │   │   ├── MessageHeaders.java
    │   │   ├── MessageTopic.java
    │   │   └── MessageType.java
    │   ├── routing
    │   │   ├── CustomerIntentType.java
    │   │   ├── RoutingDecision.java
    │   │   └── SpecialistType.java
    │   ├── ticket
    │   │   ├── CustomerServiceRequest.java
    │   │   ├── SpecialistRequestPayload.java
    │   │   └── SpecialistResponsePayload.java
    │   └── trace
    │       ├── DeliveryRecord.java
    │       └── EngineeringTrace.java
    ├── handwritten
    │   ├── agent
    │   │   ├── AbstractHandwrittenAgent.java
    │   │   ├── HandwrittenReceptionistAgent.java
    │   │   ├── HandwrittenSalesAgent.java
    │   │   └── HandwrittenTechSupportAgent.java
    │   ├── coordinator
    │   │   └── HandwrittenEngineeringCoordinator.java
    │   ├── hub
    │   │   ├── MessageHub.java
    │   │   ├── InMemoryMessageHub.java
    │   │   ├── MqBackedMessageHub.java
    │   │   ├── AsyncMessageDispatcher.java
    │   │   └── TopicSubscriptionRegistry.java
    │   ├── mq
    │   │   ├── RocketMqMessageProducer.java
    │   │   ├── RocketMqMessageListener.java
    │   │   └── RocketMqTopicBindingSupport.java
    │   ├── runtime
    │   │   ├── PendingResponseRegistry.java
    │   │   ├── ConversationContextStore.java
    │   │   └── DeliveryAuditLog.java
    │   └── support
    │       └── HandwrittenAgentPromptTemplates.java
    ├── framework
    │   ├── app
    │   │   ├── ReceptionistConsumerApplication.java
    │   │   ├── SalesProviderApplication.java
    │   │   └── TechSupportProviderApplication.java
    │   ├── agent
    │   │   ├── FrameworkReceptionistService.java
    │   │   ├── SalesAgentFactory.java
    │   │   └── TechSupportAgentFactory.java
    │   ├── client
    │   │   ├── SpecialistRemoteAgentLocator.java
    │   │   ├── SalesRemoteAgentFacade.java
    │   │   └── TechSupportRemoteAgentFacade.java
    │   ├── config
    │   │   ├── A2aNacosCommonConfig.java
    │   │   ├── EngineeringMqEnhancementConfig.java
    │   │   ├── ReceptionistA2aClientConfig.java
    │   │   ├── SalesA2aServerConfig.java
    │   │   └── TechSupportA2aServerConfig.java
    │   ├── messaging
    │   │   ├── RoutingAuditEventPublisher.java
    │   │   ├── SpecialistEscalationPublisher.java
    │   │   └── AsyncResultCallbackListener.java
    │   └── support
    │       ├── EngineeringAgentCardSupport.java
    │       ├── A2aResponseExtractor.java
    │       └── A2aInvocationTraceSupport.java
    └── support
        ├── EngineeringPromptTemplates.java
        └── EngineeringGatewayBackedChatModel.java
```

## 7. 统一领域协议设计

为避免两套实现出现“业务语义漂移”，消息和路由语义必须在 `domain` 层统一。

### 7.1 核心消息对象

`EngineeringMessage` 建议包含以下字段：

- `messageId`
- `conversationId`
- `correlationId`
- `fromAgent`
- `toAgent`
- `topic`
- `messageType`
- `replyTo`
- `timestamp`
- `payload`

### 7.2 为什么需要这些字段

`conversationId`

- 标识整次客服处理链路。
- 用于把 Receptionist 与专家之间的多跳消息绑定到同一条业务会话。

`correlationId`

- 标识某一次请求-响应对。
- 用于 Receptionist 在异步回包时找到原始请求上下文。

`replyTo`

- 指定专家处理完成后应该把结果发回哪个主题。
- 它让专家 Agent 不需要知道 Receptionist 的 Java 类型或对象引用。

`topic`

- 表示消息投递的逻辑地址。
- 在手写版中，它就是 `MessageHub` 的订阅主题。
- 在框架版中，它更多保留为业务语义字段，而真实寻址由 A2A Registry / AgentCard 完成。

### 7.3 路由语义

统一定义：

- `CustomerIntentType`
  - `TECH_SUPPORT`
  - `SALES_CONSULTING`
  - `UNKNOWN`
- `SpecialistType`
  - `TECH_SUPPORT`
  - `SALES`
- `RoutingDecision`
  - `specialistType`
  - `reason`
  - `targetTopic`
  - `targetAgentName`

这样做的价值是：

- 手写版依靠 `targetTopic` 路由。
- 框架版依靠 `targetAgentName` 发现远端 Agent。
- 两者业务决策对象保持一致。

## 8. 手写版设计：MessageHub 如何彻底解耦 Agent

### 8.1 核心思想

手写版不能再走“一个协调器直接调用下一个 Agent 方法”的老路。

它必须体现出以下事实：

- Agent 之间通过消息而不是调用栈协作。
- 接收者并不知道发送者的类实现。
- 发送者也不需要持有接收者引用。
- 路由依据是消息主题与协议，而不是 Java 对象关系。

### 8.2 MessageHub 的职责

`MessageHub` 不是普通工具类，而是这个版本的中心运行时底座。

它至少承担四种职责：

- `subscribe(topic, agent)`：登记某个主题由哪些 Agent 消费。
- `publish(message)`：把消息投递到目标主题。
- 异步分发：不要求发布者阻塞等待消费者执行完成。
- 轨迹记录：保留每次投递和消费的审计日志。

### 8.3 MessageHub 的双实现策略

为了同时兼顾“教学可解释性”和“工程完整性”，手写版的 `MessageHub` 应设计成接口，并提供两种实现：

- `InMemoryMessageHub`
  - 用最少依赖解释主题订阅、异步分发、`replyTo + correlationId` 回包机制
  - 适合作为第一阅读入口和单元测试默认实现
- `MqBackedMessageHub`
  - 基于真实 MQ 承接 publish / subscribe
  - 让学习者看到消息驱动范式如何从内存模型平滑演进到企业基础设施

这层抽象的关键价值是：

- Agent 只依赖消息协议，不依赖具体传输介质
- 手写版不会因为后续接入 RocketMQ 就推翻已有 Agent 设计
- 读者能清楚看到“AgentScope 本质”和“工程化增强”之间的连续性

### 8.4 三类 Agent 的协作方式

#### ReceptionistAgent

输入：

- 外部用户请求消息，主题为 `customer.request`

职责：

- 调用 `AgentLlmGateway` 分析意图。
- 形成 `RoutingDecision`。
- 包装成专家请求消息并发布到：
  - `support.tech.request`
  - 或 `support.sales.request`

它不直接调用 `TechSupportAgent` / `SalesAgent` 的任何方法。

#### TechSupportAgent

输入：

- 只订阅 `support.tech.request`

职责：

- 读取专家请求 payload
- 调用 `AgentLlmGateway` 生成技术处理结果
- 将结果发布到 `replyTo` 指定的主题

#### SalesAgent

输入：

- 只订阅 `support.sales.request`

职责与 `TechSupportAgent` 类似，只是提示词与处理目标不同。

### 8.5 Receptionist 如何拿回最终结果

手写版中，Receptionist 不应通过回调直接拿返回值，而应继续遵守消息驱动约束。

建议这样处理：

- Receptionist 在发专家请求时，写入：
  - `correlationId`
  - `replyTo=support.reply.receptionist`
- Receptionist 同时订阅 `support.reply.receptionist`
- 当回包到达时：
  - 用 `correlationId` 到 `PendingResponseRegistry` 查找原始请求
  - 组装最终 `EngineeringRunResult`

这套机制与真实异步消息系统的请求-响应模式保持一致。

### 8.6 为什么这能“彻底解耦”

与传统方法调用相比，`MessageHub` 机制的解耦点在于：

- **结构解耦**：Agent 不再直接依赖彼此的类。
- **时序解耦**：发送与处理不是同一个调用栈。
- **路由解耦**：主题可以映射到不同消费者实现。
- **部署解耦**：后续可把 `InMemoryMessageHub` 替换成真实 MQ，而 Agent 协议不变。

这正是 AgentScope 风格消息驱动的教学核心。

## 9. 框架版设计：Spring AI Alibaba A2A 如何落地

### 9.1 总体思路

框架版不再模拟消息代理，而是把“跨服务 Agent 通信”交给 A2A 标准协议层。

第一版采用三进程本地集成模式：

- `ReceptionistConsumerApplication`
- `TechSupportProviderApplication`
- `SalesProviderApplication`

原因是：

- 这最接近真实企业环境。
- 能真实体现注册、发现、寻址与远程调用。
- 避免把 A2A 简化成单 JVM 内部 Bean 调用。

### 9.2 Provider 侧

`TechSupportProviderApplication` 和 `SalesProviderApplication` 各自承担：

- 声明本地专家 Agent
- 通过 A2A Server 暴露该 Agent
- 为远端 Consumer 提供 Agent Card
- 在启动时把 Agent Card / endpoint 注册到 Nacos

本地专家 Agent 的推理底座仍然通过 `EngineeringGatewayBackedChatModel -> AgentLlmGateway` 走统一调用链。

### 9.3 Consumer 侧

`ReceptionistConsumerApplication` 承担：

- 接收用户诉求
- 本地判断意图
- 基于目标专家名查找远端 AgentCard
- 通过 `A2aRemoteAgent` 一类远端代理发起点对点调用
- 提取远端响应并返回最终结果

### 9.4 A2A 在底层解决了什么问题

框架版的真正价值不在于“把 HTTP 请求换个名字”，而在于它解决了多 Agent 工程里的三个关键问题。

#### 1. 发现问题

Consumer 不需要硬编码某个专家的固定 IP 与端口。

它只需要知道一个逻辑 Agent 名称，例如：

- `tech_support_agent`
- `sales_agent`

Nacos Registry 负责维护“这个名字当前对应哪个可用 endpoint”。

#### 2. 能力描述问题

远端 Agent 并不只是一个地址，还需要让调用方知道：

- 它是谁
- 它擅长什么
- 它支持什么输入输出能力
- 它暴露什么端点

这正是 Agent Card 的作用。

Agent Card 相当于远端 Agent 的“服务名片 + 能力说明书”。

#### 3. 协议统一问题

Consumer 与 Provider 之间的交互不再是每个团队各写各的 REST 接口，而是统一走 A2A 协议的请求/响应模型。

这使得：

- 不同 Agent 服务之间的接入方式统一
- 注册与发现机制统一
- 后续接入更多远端专家 Agent 的成本明显下降

### 9.5 为什么主链路仍然不让 MQ 替代 A2A

从当前官方文档和主仓路线看，A2A 第一优先能力是 **Agent-to-Agent 的标准化点对点通信**，不是替代 MQ 的广播系统。

因此首版框架版应明确定位为：

- Receptionist 做本地编排
- 专家服务做远端被调 Agent
- 通过同步请求/响应完成协作

这条路径既贴合官方能力，也能让教学目标聚焦。

## 10. MQ 在整体架构中的角色

MQ 在本模块中不是“是否采用 A2A 的替代答案”，而是围绕主链路的工程增强层。

### 10.1 在手写版中的角色

在手写版里，MQ 的价值是让 `MessageHub` 从“概念运行时”走向“真实传输通道”：

- `InMemoryMessageHub` 负责把机制讲清楚
- `MqBackedMessageHub` 负责把机制接到真实中间件

这样可以完整回答一个关键问题：

> 如果 AgentScope 的本质是消息驱动，那么它在企业里最终如何落到真实基础设施上？

### 10.2 在框架版中的角色

在框架版里，MQ 不接管 Receptionist 到专家 Agent 的主调用链。

它更适合承担：

- 路由审计事件发布
- 长耗时问题升级任务投递
- 专家处理完成后的异步通知
- 失败补偿、重试和死信观测

### 10.3 设计边界

本模块对 A2A、Nacos 与 MQ 的职责划分应保持明确：

- `A2A` 负责 Agent 间标准化点对点通信
- `Nacos` 负责 Agent 注册、发现与 AgentCard 元数据寻址
- `MQ` 负责异步消息治理与工程增强

只有把这三层职责拆开，学习者才能真正理解企业多智能体系统为什么需要多种基础设施协作，而不是把所有问题都压给同一种通信机制。

## 11. 框架版与统一网关的衔接方式

由于 Spring AI Alibaba A2A 生态多数能力以 `ChatModel` / 框架 Agent 为入口，本模块需要提供一个网关适配层：

- `EngineeringGatewayBackedChatModel`

它的职责与现有 `ConversationGatewayBackedChatModel`、`CamelGatewayBackedChatModel` 一致：

- 接收 Spring AI Prompt
- 转成仓库统一的 `LlmRequest`
- 通过 `AgentLlmGateway` 调用模型
- 再转回 Spring AI `ChatResponse`

这样 A2A 模块既能接入框架标准能力，又不会破坏仓库统一的模型边界。

## 12. 配置与运行模式设计

建议配置拆分为三组 profile：

- `engineering-handwritten-demo`
- `engineering-mq-local`
- `engineering-a2a-tech-provider`
- `engineering-a2a-sales-provider`
- `engineering-a2a-receptionist-consumer`

框架版推荐补充的公共配置项：

- `engineering.a2a.nacos.server-addr`
- `engineering.a2a.nacos.namespace`
- `engineering.a2a.agent.tech.name`
- `engineering.a2a.agent.sales.name`
- `engineering.a2a.agent.receptionist.name`
- `engineering.a2a.timeout`

MQ 增强层建议补充：

- `engineering.mq.enabled`
- `engineering.mq.name-server`
- `engineering.mq.topic.audit`
- `engineering.mq.topic.escalation`
- `engineering.mq.topic.callback`

## 13. 测试策略

### 13.1 手写版测试

必须覆盖：

- Receptionist 正确把技术问题投递给 TechSupport
- Receptionist 正确把销售问题投递给 Sales
- 专家结果能够通过 `replyTo + correlationId` 回到 Receptionist
- MessageHub 审计日志能完整记录投递轨迹
- `MqBackedMessageHub` 与 `InMemoryMessageHub` 对外行为保持同构

### 13.2 框架版测试

必须覆盖：

- 远端专家 AgentCard 能被 Consumer 发现
- Receptionist 能根据意图选择正确远端 Agent
- 技术问题调用 TechSupport Provider
- 销售问题调用 Sales Provider
- A2A 返回结果能被统一提取为 `EngineeringRunResult`
- MQ 增强层投递的审计或回调事件不影响 A2A 主链路语义

### 13.3 对照测试

应增加一组对照测试，验证：

- 相同输入下，手写版与框架版的路由决策一致
- 两者返回的最终业务结果结构一致

## 14. 风险与关键设计判断

### 14.1 风险一：过度追求“像 MQ 一样”

如果把框架版硬做成远程事件总线，会偏离当前 A2A 官方能力重心，也会显著增加实现复杂度。

设计判断：

- 手写版强调“消息总线本质”
- 框架版强调“跨服务 A2A 通信”

这两者可以同构，但不必机械等形。

### 14.2 风险二：模型调用链分叉

如果图方便直接在框架版里用独立 `ChatModel` 直连 provider，就会破坏整个仓库的统一网关抽象。

设计判断：

- 框架版必须通过 `EngineeringGatewayBackedChatModel` 接回 `AgentLlmGateway`

### 14.3 风险三：单应用伪分布式掩盖 A2A 价值

如果把三个角色都塞进一个应用，读者会很难真正理解：

- 为什么要注册
- 为什么要发现
- Agent Card 解决了什么问题

设计判断：

- 首版采用本地多应用模式

### 14.4 风险四：手写版过早绑定单一 MQ SDK

如果 `MessageHub` 抽象没有先稳定下来，就直接让 Agent 面向某个 MQ SDK 编程，会导致：

- Agent 协议与中间件 API 缠死
- `InMemoryMessageHub` 无法继续作为教学对照实现
- 后续替换或裁剪中间件成本过高

设计判断：

- Agent 只面向 `MessageHub`
- MQ 适配逻辑收敛在 `handwritten.mq` 与 `MqBackedMessageHub`

## 15. 结论

`module-multi-agent-engineering` 不应只是“再做一个多 Agent Demo”，而应成为整个仓库里最接近企业工程的模块。

它的教学价值在于同时讲清三件事：

- 手写版说明：多智能体消息驱动的底层本质，是显式消息协议、主题订阅、异步解耦与关联回包。
- 框架版说明：Spring AI Alibaba A2A 把这些问题进一步提升到跨服务工程层，通过 Agent Card、Nacos Registry/Discovery 和远端 Agent 调用，解决真实分布式 Agent 的发现与通信问题。
- MQ 增强层说明：企业系统还需要异步治理、审计追踪和补偿机制，但这些能力应作为增强层接入，而不应破坏 A2A 主链路的职责边界。

从范式定位上看，A2A 确实更接近“企业级多智能体工程化”的王牌方向，但它的价值不在“更高级”，而在“更接近真实系统边界”。
