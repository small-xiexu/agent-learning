# Roleplay Rename And AutoGen Conversation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 CAMEL 模块重命名回 roleplay 范式模块，并在 conversation 模块中补齐 AutoGen RoundRobin 多智能体双实现。

**Architecture:** 先把 `module-multi-agent-roleplay` 规范化迁移为 `module-multi-agent-roleplay`，保持“模块按范式、类按方法”的命名层级；再在 `module-multi-agent-conversation` 中同时实现基于 `AgentLlmGateway` 的手写 RoundRobin 群聊与基于 Spring AI Alibaba `FlowAgent` 的循环图编排版本，两套实现共享同一份群聊上下文抽象。

**Tech Stack:** Java 21, Spring Boot 3.5, Spring AI Alibaba Agent Framework, Maven, JUnit 5

---

### Task 1: Rename CAMEL Module To Roleplay

**Files:**
- Modify: `pom.xml`
- Modify: `module-multi-agent-roleplay/pom.xml`
- Move: `module-multi-agent-roleplay` -> `module-multi-agent-roleplay`
- Move: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel` -> `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/roleplay`
- Move: `module-multi-agent-roleplay/src/test/java/com/xbk/agent/framework/camel` -> `module-multi-agent-roleplay/src/test/java/com/xbk/agent/framework/roleplay`
- Move: `module-multi-agent-roleplay/src/test/resources/application-openai-roleplay-demo*.yml` -> `application-openai-roleplay-demo*.yml`
- Move: `module-multi-agent-roleplay/src/test/resources/openai-roleplay-demo-fixture/*` -> `openai-roleplay-demo-fixture/*`

- [ ] 更新父工程模块路径和 dependencyManagement 坐标
- [ ] 更新子模块 artifactId、name、description
- [ ] 执行目录和包路径迁移，保留类名中的 `Camel`
- [ ] 同步测试配置类、资源 profile、README 和 docs 中的模块路径引用

### Task 2: Build Conversation Domain Skeleton

**Files:**
- Modify: `module-multi-agent-conversation/pom.xml`
- Modify/Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/**`

- [ ] 补齐 conversation 模块依赖，统一接入 `framework-core`、`framework-llm-autoconfigure`、`framework-llm-springai`、`spring-ai-alibaba-agent-framework`
- [ ] 新建 API、domain、support、application、infrastructure 分层骨架
- [ ] 定义群聊角色、共享上下文、运行结果和终止策略

### Task 3: Implement Handwritten AutoGen RoundRobin

**Files:**
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/api/ConversationRunResult.java`
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/domain/role/ConversationRoleType.java`
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/domain/role/ConversationRoleContract.java`
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/domain/memory/ConversationTurn.java`
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/domain/memory/ConversationMemory.java`
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/support/ConversationPromptTemplates.java`
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/application/executor/ProductManagerAgent.java`
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/application/executor/EngineerAgent.java`
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/application/executor/CodeReviewerAgent.java`
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/application/coordinator/RoundRobinGroupChat.java`

- [ ] 先写共享历史与结果模型的测试桩
- [ ] 让三个角色都直接依赖 `AgentLlmGateway`
- [ ] 用 `while` + 固定顺序 ProductManager -> Engineer -> CodeReviewer 推进群聊
- [ ] 把所有发言广播回同一份 `List<Message>` 上下文
- [ ] 仅允许 Reviewer 审批结束或达到最大轮次终止

### Task 4: Implement Spring AI Alibaba Conversation Flow

**Files:**
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/support/ConversationGatewayBackedChatModel.java`
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/infrastructure/agentframework/AlibabaConversationFlowAgent.java`
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/infrastructure/agentframework/node/ProductManagerNode.java`
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/infrastructure/agentframework/node/EngineerNode.java`
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/infrastructure/agentframework/node/CodeReviewerNode.java`
- Create: `module-multi-agent-conversation/src/main/java/com/xbk/agent/framework/conversation/infrastructure/agentframework/support/ConversationTranscriptStateSupport.java`

- [ ] 用 `FlowAgent + StateGraph + OverAllState` 表达 RoundRobin 回环
- [ ] 在状态中保存 `shared_messages`、`speaker_index`、`current_python_script`、`done`、`stop_reason`
- [ ] ProductManager 与 Engineer 固定顺序流转，Reviewer 决定继续还是结束
- [ ] 保证框架版底层仍然统一走 `AgentLlmGateway`

### Task 5: Add Tests, Demos, And Docs

**Files:**
- Create: `module-multi-agent-conversation/src/test/java/com/xbk/agent/framework/conversation/ConversationRoundRobinComparisonTest.java`
- Create: `module-multi-agent-conversation/src/test/java/com/xbk/agent/framework/conversation/HandwrittenConversationOpenAiDemo.java`
- Create: `module-multi-agent-conversation/src/test/java/com/xbk/agent/framework/conversation/AlibabaConversationFlowOpenAiDemo.java`
- Create: `module-multi-agent-conversation/src/test/java/com/xbk/agent/framework/conversation/config/OpenAiConversationDemoPropertySupport.java`
- Create: `module-multi-agent-conversation/src/test/java/com/xbk/agent/framework/conversation/config/OpenAiConversationDemoPropertySupportTest.java`
- Create: `module-multi-agent-conversation/src/test/java/com/xbk/agent/framework/conversation/config/OpenAiConversationDemoTestConfig.java`
- Create: `module-multi-agent-conversation/src/test/java/com/xbk/agent/framework/conversation/config/OpenAiConversationDemoTestConfigTest.java`
- Create: `module-multi-agent-conversation/src/test/resources/application-openai-conversation-demo.yml`
- Create: `module-multi-agent-conversation/src/test/resources/application-openai-conversation-demo-local.yml.example`
- Create: `module-multi-agent-conversation/src/test/resources/openai-conversation-demo-fixture/**`
- Modify: `module-multi-agent-conversation/README.md`
- Modify: `docs/项目整体架构导读.md`
- Modify: `docs/CAMEL范式从0到1掌握指南.md`

- [ ] 先用脚本化网关钉住手写版和框架版的轮询行为
- [ ] 再补 OpenAI 真实 Demo 与最小自动装配测试
- [ ] 更新 README 和总导读中的模块说明与学习路径

### Task 6: Verify Rename And New Module

**Files:**
- Verify only

- [ ] Run: `mvn -q -pl module-multi-agent-roleplay -am test`
- [ ] Run: `mvn -q -pl module-multi-agent-conversation -am test`
- [ ] Run: `mvn -q -pl module-multi-agent-roleplay,module-multi-agent-conversation -am -DskipTests compile`
- [ ] 复查重命名后的文档路径、profile 名称和断言文本是否一致
