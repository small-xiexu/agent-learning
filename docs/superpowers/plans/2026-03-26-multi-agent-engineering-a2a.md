# Multi-Agent Engineering A2A And MQ Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `module-multi-agent-engineering` 同时落地“纯手写消息驱动版”“MQ 增强的手写版”和“Spring AI Alibaba A2A 框架版”多智能体工程协作实现，并统一复用 `AgentLlmGateway`。

**Architecture:** 先建立统一的领域协议与结果模型，再实现手写版 `MessageHub` 运行时与 `MqBackedMessageHub` 扩展，之后实现 A2A Provider / Consumer / Registry 集成边界，最后补 MQ 审计与异步增强层，并用对照测试验证两套实现的路由语义与最终结果一致。框架版保持“本地多应用 + Nacos 注册发现 + A2A 点对点调用”路径，MQ 只作为增强层，不把 A2A 弄成伪消息总线。

**Tech Stack:** Java 21, Maven, Spring Boot 3.5, Spring AI Alibaba Agent Framework, Spring AI Alibaba A2A, Nacos, RocketMQ, JUnit 5, Markdown

---

### Task 1: Establish Module Structure And Shared Domain Contracts

**Files:**
- Modify: `module-multi-agent-engineering/pom.xml`
- Modify: `module-multi-agent-engineering/README.md`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/package-info.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/api/EngineeringRunResult.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/domain/message/EngineeringMessage.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/domain/message/MessageHeaders.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/domain/message/MessageTopic.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/domain/message/MessageType.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/domain/routing/CustomerIntentType.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/domain/routing/RoutingDecision.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/domain/routing/SpecialistType.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/domain/ticket/CustomerServiceRequest.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/domain/ticket/SpecialistRequestPayload.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/domain/ticket/SpecialistResponsePayload.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/domain/trace/DeliveryRecord.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/domain/trace/EngineeringTrace.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/support/EngineeringPromptTemplates.java`

- [ ] 补齐模块依赖，至少覆盖 `framework-core`、测试依赖、框架版 A2A / Nacos 所需 starter，以及手写版 RocketMQ 增强实现所需依赖。
- [ ] 在 README 中把模块定位更新为“手写消息驱动版 + A2A 框架版”的双实现结构。
- [ ] 建立统一领域模型，确保手写版和框架版使用相同的消息、路由、工单和轨迹语义。
- [ ] 为所有核心协议类补类级 Javadoc，明确谁生产、谁消费、它在运行时承担什么角色。

### Task 2: Implement Gateway Adapter And Shared Prompt Layer

**Files:**
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/support/EngineeringGatewayBackedChatModel.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/application/routing/CustomerIntentClassifier.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/support/EngineeringGatewayBackedChatModelTest.java`

- [x] 参照现有 `ConversationGatewayBackedChatModel` / `CamelGatewayBackedChatModel`，实现本模块专用 `ChatModel` 适配器。
- [x] 保证 Spring AI Prompt 最终统一转成 `LlmRequest` 并经 `AgentLlmGateway` 发出。
- [x] 抽取三类角色提示词模板：Receptionist、TechSupport、Sales。
- [x] 为意图分类器单独建类，不把“识别技术 / 销售诉求”的逻辑散落到多个实现里。
- [x] 用单元测试钉住消息角色映射、Prompt 转换与 `AgentLlmGateway` 唯一路径约束。

### Task 3: Build Handwritten MessageHub Runtime

**Files:**
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/hub/MessageHub.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/hub/InMemoryMessageHub.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/hub/MqBackedMessageHub.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/hub/TopicSubscriptionRegistry.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/hub/AsyncMessageDispatcher.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/mq/RocketMqMessageProducer.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/mq/RocketMqMessageListener.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/mq/RocketMqTopicBindingSupport.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/runtime/PendingResponseRegistry.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/runtime/ConversationContextStore.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/runtime/DeliveryAuditLog.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/handwritten/InMemoryMessageHubTest.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/handwritten/MqBackedMessageHubTest.java`

- [x] 先写测试，钉住“同一主题可订阅多个消费者”“按主题投递”“异步执行”“轨迹记录”这几类行为。
- [x] 实现 `MessageHub` 抽象，明确 `subscribe`、`publish`、`close` 等最小接口。
- [x] 实现 `InMemoryMessageHub`，默认基于线程池异步分发。
- [x] 在 `MessageHub` 抽象稳定后，实现 `MqBackedMessageHub`，保证对外语义与内存实现保持同构。
- [x] 引入 `DeliveryAuditLog`，把每次 publish、deliver、consume、reply 变成可回放的审计记录。
- [x] 用 `PendingResponseRegistry` 绑定 `correlationId -> 等待结果的上下文`，为 Receptionist 的异步回包做准备。

### Task 4: Implement Handwritten Agents And Coordinator

**Files:**
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/agent/AbstractHandwrittenAgent.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/agent/HandwrittenReceptionistAgent.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/agent/HandwrittenTechSupportAgent.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/agent/HandwrittenSalesAgent.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/support/HandwrittenAgentPromptTemplates.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/handwritten/coordinator/HandwrittenEngineeringCoordinator.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/handwritten/HandwrittenEngineeringRoutingTest.java`

- [x] 先写对照测试，覆盖“技术诉求 -> 技术专家”“销售诉求 -> 销售专家”“结果回到 Receptionist”三条主线。
- [x] 为三类 Agent 统一抽象 `receive(EngineeringMessage)` 与 `send(EngineeringMessage)` 协议。
- [x] `HandwrittenReceptionistAgent` 只负责意图识别、路由决策、消息包装与结果回收，不直接处理专家问题。
- [x] `HandwrittenTechSupportAgent` / `HandwrittenSalesAgent` 只消费自己的主题，并按 `replyTo + correlationId` 回包。
- [x] `HandwrittenEngineeringCoordinator` 作为外部入口，对外暴露同步 `run()`，对内通过异步消息链驱动整个过程。
- [x] 测试中使用脚本化 `AgentLlmGateway`，把三类角色的模型返回固定下来，确保行为可断言。

### Task 5: Add Handwritten Demo And Trace Support

**Files:**
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/HandwrittenEngineeringOpenAiDemo.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/EngineeringDemoLogSupport.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/config/OpenAiEngineeringDemoPropertySupport.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/config/OpenAiEngineeringDemoTestConfig.java`
- Create: `module-multi-agent-engineering/src/test/resources/application-openai-engineering-demo.yml`
- Create: `module-multi-agent-engineering/src/test/resources/application-engineering-mq-local.yml`

- [x] 复用仓库现有 OpenAI Demo 启用风格，默认跳过外网调用。
- [x] 在 Demo 日志里打印消息主题、发送方、接收方、`correlationId` 和最终专家结果。
- [x] 让手写版 Demo 能直观看出“消息发布 -> 专家订阅 -> 回包 -> 最终汇总”的全过程。
- [x] 补一套 MQ 本地 profile，让 `MqBackedMessageHub` 的演示入口可单独启用。

### Task 6: Introduce Framework A2A Common Config And Runtime Contracts

**Files:**
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/config/A2aNacosCommonConfig.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/config/EngineeringMqEnhancementConfig.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/support/EngineeringAgentCardSupport.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/support/A2aResponseExtractor.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/support/A2aInvocationTraceSupport.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/framework/A2aSupportContractTest.java`

- [x] 根据当前 Spring AI Alibaba A2A / Nacos 官方能力，封装本模块所需的共用配置。
- [x] 同时抽出 MQ 增强层的公共配置，但保持它与 A2A 主通信链路解耦。
- [x] 抽一层 `EngineeringAgentCardSupport`，统一专家 Agent 名、描述、标签与 endpoint 约定。
- [x] 抽一层 `A2aResponseExtractor`，避免 A2A 响应解析逻辑散落在 Consumer 业务代码里。
- [x] 用契约测试钉住 Agent 命名规范、AgentCard 元信息和响应提取规则。

### Task 7: Implement Framework Provider Side

**Files:**
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/app/TechSupportProviderApplication.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/app/SalesProviderApplication.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/agent/TechSupportAgentFactory.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/agent/SalesAgentFactory.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/config/TechSupportA2aServerConfig.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/config/SalesA2aServerConfig.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/framework/ProviderSideAssemblyTest.java`

- [x] 先写装配测试，验证 Provider 侧确实暴露了技术专家和销售专家两类 Agent。
- [x] 每个 Provider 应用只承载一个主要专家 Agent，保持与官方推荐的单应用单 Agent 注册习惯一致。
- [x] 专家 Agent 的模型底层必须接到 `EngineeringGatewayBackedChatModel`，不能绕开 `AgentLlmGateway`。
- [x] 通过配置类显式声明 A2A Server、AgentCardProvider 与 Nacos registry 相关 Bean。
- [x] 测试要证明 Provider 装配完成后，专家 Agent 名称与 AgentCard 元信息正确可读。

### Task 8: Implement Framework Consumer Side

**Files:**
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/app/ReceptionistConsumerApplication.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/agent/FrameworkReceptionistService.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/client/SpecialistRemoteAgentLocator.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/client/TechSupportRemoteAgentFacade.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/client/SalesRemoteAgentFacade.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/config/ReceptionistA2aClientConfig.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/framework/FrameworkReceptionistRoutingTest.java`

- [x] 先写 Consumer 行为测试，钉住”Receptionist 根据意图选择正确远端 Agent 名”。
- [x] `FrameworkReceptionistService` 负责本地意图分类和远端调用编排，不直接承担专家回答生成。
- [x] `SpecialistRemoteAgentLocator` 负责从 Nacos 发现 AgentCard，并为后续 facade 提供统一查找入口。
- [x] 技术与销售 facade 分开，是为了让”找谁”和”怎么调”在结构上保持明确边界。
- [x] 测试需覆盖技术问题走远端技术 Agent、销售问题走远端销售 Agent 两条主路径。

### Task 9: Add Local Integration Setup For A2A + Nacos

**Files:**
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/framework/FrameworkA2aRoutingTest.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/framework/A2aNacosDiscoverySmokeTest.java`
- Create: `module-multi-agent-engineering/src/test/resources/application-a2a-tech-provider.yml`
- Create: `module-multi-agent-engineering/src/test/resources/application-a2a-sales-provider.yml`
- Create: `module-multi-agent-engineering/src/test/resources/application-a2a-receptionist-consumer.yml`
- Create: `module-multi-agent-engineering/src/test/resources/application-a2a-nacos-local.yml`

- [x] 明确本地集成测试依赖的 Nacos 启动方式，并在测试说明里写清外部前置条件。
- [x] 做一组最小 smoke test，验证 Consumer 至少能发现两个远端专家 Agent。
- [x] 做一组路由集成测试，验证用户请求经 Receptionist 路由后能拿到远端专家结果。
- [x] 对测试的 profile、资源文件、跳过条件做统一约定，避免 CI 中误触发外部依赖。

### Task 10: Add MQ Enhancement For Handwritten And Framework Versions

**Files:**
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/messaging/RoutingAuditEventPublisher.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/messaging/SpecialistEscalationPublisher.java`
- Create: `module-multi-agent-engineering/src/main/java/com/xbk/agent/framework/engineering/framework/messaging/AsyncResultCallbackListener.java`
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/MqEnhancementIntegrationTest.java`
- Create: `module-multi-agent-engineering/src/test/resources/application-engineering-mq-rocketmq.yml`

- [x] 为手写版补上基于 MQ 的 publish / subscribe 集成测试，验证它与 `InMemoryMessageHub` 的外部语义一致。
- [x] 为框架版补上路由审计事件、升级任务消息和异步回调监听，体现 MQ 作为增强层的真实角色。
- [x] 明确 MQ 只承接异步治理、审计和补偿，不接管 Receptionist 到专家 Agent 的 A2A 主调用链。
- [x] 为失败重试、死信主题和回调幂等预留清晰扩展点，但不在首版过度实现。

### Task 11: Add Comparison Tests Between Handwritten And Framework Versions

**Files:**
- Create: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/EngineeringComparisonTest.java`

- [x] 写对照测试，确保同一输入下两套实现得到相同的 `CustomerIntentType`。
- [x] 写对照测试，确保两套实现最终都返回结构一致的 `EngineeringRunResult`。
- [x] 写对照测试，明确两套实现的差异只体现在通信机制，不体现在业务语义。

### Task 12: Documentation And Teaching Comments

**Files:**
- Modify: `module-multi-agent-engineering/README.md`
- Create: `docs/AgentScope范式从0到1掌握指南.md`
- Modify: `README.md`

- [x] 把模块 README 调整成”模块定位 + 双实现总览 + 推荐阅读顺序 + Demo 入口”的结构。
- [ ] 新增 AgentScope / A2A 专题导读，解释为什么这个模块代表企业工程化方向。
- [x] 在仓库顶层 README 中补上 `module-multi-agent-engineering` 的学习入口。
- [x] 为核心类补教学型注释，重点解释消息协议、A2A 注册发现、远端调用而不是重复语法。

### Task 13: Verification

**Files:**
- Verify only

- [x] Run: `mvn -pl module-multi-agent-engineering -am test` → 44 tests, 0 failures, 3 skipped（外部服务依赖）
- [x] Run: `mvn -pl module-multi-agent-engineering -am -DskipTests compile` → BUILD SUCCESS
- [x] 复查两套实现都只经 `AgentLlmGateway` 发起模型调用
- [x] 复查 README、推荐阅读顺序、测试入口和配置入口已更新
- [x] 复查本地多应用 A2A 集成说明已在 README 中补充（三进程部署说明）
- [x] 复查 MQ 相关测试和配置始终位于增强层，未替代 A2A 主链路职责
