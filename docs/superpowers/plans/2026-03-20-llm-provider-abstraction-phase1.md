# LLM Provider Abstraction Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立第一阶段统一 `llm.*` 接入链路，以 Spring AI 作为底座，先跑通 OpenAI compatible provider，并让手写版 ReAct 真实模型 demo 通过统一 `AgentLlmGateway` 接入。

**Architecture:** 保持 `framework-core` 作为纯协议层，新增 `framework-llm-autoconfigure` 负责统一配置和 adapter 注册，新增 `framework-llm-springai` 负责基于 Spring AI 手动构造 OpenAI compatible `ChatModel` 并适配到统一网关。第一阶段允许旧官方 demo 和部分现有 Spring AI 类暂时并存，不做一次性大搬迁。

**Tech Stack:** Java 21, Maven, Spring Boot 3.5.9, Spring AI 1.1.2, Spring AI OpenAI, JUnit 5

---

## File Structure

### Create

- `framework-llm-autoconfigure/pom.xml`
- `framework-llm-autoconfigure/src/main/java/com/xbk/agent/framework/llm/autoconfigure/LlmProperties.java`
- `framework-llm-autoconfigure/src/main/java/com/xbk/agent/framework/llm/autoconfigure/LlmCapabilitiesProperties.java`
- `framework-llm-autoconfigure/src/main/java/com/xbk/agent/framework/llm/autoconfigure/ProviderAdapter.java`
- `framework-llm-autoconfigure/src/main/java/com/xbk/agent/framework/llm/autoconfigure/ProviderAdapterResolver.java`
- `framework-llm-autoconfigure/src/main/java/com/xbk/agent/framework/llm/autoconfigure/AgentLlmGatewayAutoConfiguration.java`
- `framework-llm-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `framework-llm-autoconfigure/src/test/java/com/xbk/agent/framework/llm/autoconfigure/ProviderAdapterResolverTest.java`
- `framework-llm-autoconfigure/src/test/java/com/xbk/agent/framework/llm/autoconfigure/AgentLlmGatewayAutoConfigurationTest.java`
- `framework-llm-springai/pom.xml`
- `framework-llm-springai/src/main/java/com/xbk/agent/framework/llm/springai/openai/OpenAiCompatibleProviderAdapter.java`
- `framework-llm-springai/src/main/java/com/xbk/agent/framework/llm/springai/openai/OpenAiCompatibleChatModelFactory.java`
- `framework-llm-springai/src/main/java/com/xbk/agent/framework/llm/springai/autoconfigure/SpringAiProviderAutoConfiguration.java`
- `framework-llm-springai/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `framework-llm-springai/src/test/java/com/xbk/agent/framework/llm/springai/openai/OpenAiCompatibleProviderAdapterTest.java`
- `framework-llm-springai/src/test/java/com/xbk/agent/framework/llm/springai/autoconfigure/SpringAiProviderAutoConfigurationTest.java`

### Modify

- `pom.xml`
- `module-react-paradigm/pom.xml`
- `module-react-paradigm/src/test/resources/application-openai-react-demo.yml`
- `module-react-paradigm/src/test/resources/application-openai-react-demo-local.yml.example`
- `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/ReActTravelOpenAiDemo.java`
- `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/config/OpenAiReactDemoTestConfigTest.java`
- `docs/react-learning-path.md`

### Reuse Without Moving in Phase 1

- `framework-core/src/main/java/com/xbk/agent/framework/core/llm/AgentLlmGateway.java`
- `framework-core/src/main/java/com/xbk/agent/framework/core/llm/DefaultAgentLlmGateway.java`
- `framework-core/src/main/java/com/xbk/agent/framework/core/llm/adapter/springai/SpringAiLlmClient.java`
- `framework-core/src/main/java/com/xbk/agent/framework/core/llm/adapter/springai/SpringAiMessageMapper.java`
- `framework-core/src/main/java/com/xbk/agent/framework/core/llm/adapter/springai/SpringAiOptionsMapper.java`
- `framework-core/src/main/java/com/xbk/agent/framework/core/llm/adapter/springai/SpringAiResponseMapper.java`

### Notes

- Phase 1 不迁移现有 `framework-core` 中的 Spring AI 适配类，只在新模块中复用它们。
- Phase 1 不要求移除 `module-react-paradigm` 对 Spring AI starter 的全部直接依赖，先以功能跑通和边界建立为准。
- 本计划不包含自动提交步骤；如需提交，仅在用户明确要求 `/code-commit` 后执行。

## Chunk 1: 模块骨架与统一配置入口

### Task 1: 建立新模块并接入根构建

**Files:**
- Modify: `pom.xml`
- Create: `framework-llm-autoconfigure/pom.xml`
- Create: `framework-llm-springai/pom.xml`

- [ ] **Step 1: 更新根 `pom.xml` 模块列表**

添加 `framework-llm-autoconfigure` 和 `framework-llm-springai` 到 `<modules>`，并补齐依赖管理条目。

- [ ] **Step 2: 创建 `framework-llm-autoconfigure/pom.xml`**

依赖至少包含 `framework-core`、`spring-boot-autoconfigure`、`spring-boot-configuration-processor`、`spring-boot-starter-test`。

- [ ] **Step 3: 创建 `framework-llm-springai/pom.xml`**

依赖至少包含 `framework-core`、`framework-llm-autoconfigure`、`spring-ai-openai`、`spring-context`、`spring-retry`、`micrometer-observation`、`spring-boot-starter-test`。

- [ ] **Step 4: 验证新模块被 Maven 识别**

Run: `/Users/sxie/maven/apache-maven-3.6.3/bin/mvn -q -pl framework-llm-autoconfigure,framework-llm-springai -am -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository test`

Expected: 两个新模块被识别，若尚无源码则仅完成空模块测试阶段。

### Task 2: 引入统一配置对象与 adapter 注册表

**Files:**
- Create: `framework-llm-autoconfigure/src/main/java/com/xbk/agent/framework/llm/autoconfigure/LlmProperties.java`
- Create: `framework-llm-autoconfigure/src/main/java/com/xbk/agent/framework/llm/autoconfigure/LlmCapabilitiesProperties.java`
- Create: `framework-llm-autoconfigure/src/main/java/com/xbk/agent/framework/llm/autoconfigure/ProviderAdapter.java`
- Create: `framework-llm-autoconfigure/src/main/java/com/xbk/agent/framework/llm/autoconfigure/ProviderAdapterResolver.java`
- Create: `framework-llm-autoconfigure/src/test/java/com/xbk/agent/framework/llm/autoconfigure/ProviderAdapterResolverTest.java`

- [ ] **Step 1: 先写 `ProviderAdapterResolverTest`**

覆盖三类行为：
- 只有一个匹配 adapter 时返回该 adapter
- 没有匹配 adapter 时抛清晰异常
- 多个 adapter 同时匹配时抛清晰异常

- [ ] **Step 2: 创建 `ProviderAdapter` 接口**

接口至少表达：
- 是否支持某个 `provider-id`
- 如何基于 `LlmProperties` 构建统一 `AgentLlmGateway`

- [ ] **Step 3: 创建 `LlmProperties` 与 `LlmCapabilitiesProperties`**

最小字段覆盖：
- `provider`
- `baseUrl`
- `apiKey`
- `model`
- `chatCompletionsPath`
- `timeout`
- `capabilities`
- `providerOptions`

- [ ] **Step 4: 实现 `ProviderAdapterResolver`**

以 Spring Bean 列表作为输入，不写死枚举和 `switch` 路由。

- [ ] **Step 5: 运行 `framework-llm-autoconfigure` 单测**

Run: `/Users/sxie/maven/apache-maven-3.6.3/bin/mvn -q -pl framework-llm-autoconfigure -am -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -Dtest=ProviderAdapterResolverTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: `ProviderAdapterResolverTest` PASS。

### Task 3: 新增统一网关自动装配

**Files:**
- Create: `framework-llm-autoconfigure/src/main/java/com/xbk/agent/framework/llm/autoconfigure/AgentLlmGatewayAutoConfiguration.java`
- Create: `framework-llm-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `framework-llm-autoconfigure/src/test/java/com/xbk/agent/framework/llm/autoconfigure/AgentLlmGatewayAutoConfigurationTest.java`

- [ ] **Step 1: 先写 `AgentLlmGatewayAutoConfigurationTest`**

覆盖两类行为：
- 存在匹配 adapter 时能够装配 `AgentLlmGateway`
- 缺少匹配 adapter 时启动失败且错误信息可读

- [ ] **Step 2: 实现 `AgentLlmGatewayAutoConfiguration`**

要求：
- 使用 `@AutoConfiguration`
- 通过 `@EnableConfigurationProperties(LlmProperties.class)` 启用统一配置
- 仅在上下文中不存在用户自定义 `AgentLlmGateway` 时自动装配

- [ ] **Step 3: 补 `AutoConfiguration.imports`**

确保 Spring Boot 3 可以自动发现该自动配置类。

- [ ] **Step 4: 运行 `framework-llm-autoconfigure` 全量测试**

Run: `/Users/sxie/maven/apache-maven-3.6.3/bin/mvn -q -pl framework-llm-autoconfigure -am -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository test`

Expected: `framework-llm-autoconfigure` 模块测试全部通过。

## Chunk 2: Spring AI OpenAI Compatible 接入

### Task 4: 实现手动构建 `ChatModel` 的 Spring AI OpenAI compatible 工厂

**Files:**
- Create: `framework-llm-springai/src/main/java/com/xbk/agent/framework/llm/springai/openai/OpenAiCompatibleChatModelFactory.java`
- Create: `framework-llm-springai/src/test/java/com/xbk/agent/framework/llm/springai/openai/OpenAiCompatibleProviderAdapterTest.java`

- [ ] **Step 1: 先写工厂行为测试**

测试至少验证：
- `baseUrl` 被映射到 `OpenAiApi.Builder.baseUrl(...)`
- `apiKey` 被映射到 `OpenAiApi.Builder.apiKey(...)`
- `chatCompletionsPath` 被映射到 `OpenAiApi.Builder.completionsPath(...)`
- `model` 被映射到 `OpenAiChatOptions.model(...)`

- [ ] **Step 2: 实现 `OpenAiCompatibleChatModelFactory`**

使用以下已确认可用的 Spring AI API：
- `OpenAiApi.builder()`
- `OpenAiChatOptions.builder()`
- `OpenAiChatModel.builder()`

要求：
- 支持从 `LlmProperties` 手动构建 `ChatModel`
- 允许 `chatCompletionsPath` 为空时回退默认 `/v1/chat/completions`
- 预留 `providerOptions` 映射到 `extraBody` 或 HTTP headers 的扩展位

- [ ] **Step 3: 运行工厂相关测试**

Run: `/Users/sxie/maven/apache-maven-3.6.3/bin/mvn -q -pl framework-llm-springai -am -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -Dtest=OpenAiCompatibleProviderAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: 工厂与映射测试通过。

### Task 5: 实现 `OpenAiCompatibleProviderAdapter` 与 Spring AI provider 自动装配

**Files:**
- Create: `framework-llm-springai/src/main/java/com/xbk/agent/framework/llm/springai/openai/OpenAiCompatibleProviderAdapter.java`
- Create: `framework-llm-springai/src/main/java/com/xbk/agent/framework/llm/springai/autoconfigure/SpringAiProviderAutoConfiguration.java`
- Create: `framework-llm-springai/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `framework-llm-springai/src/test/java/com/xbk/agent/framework/llm/springai/autoconfigure/SpringAiProviderAutoConfigurationTest.java`

- [ ] **Step 1: 先写 `SpringAiProviderAutoConfigurationTest`**

覆盖三类行为：
- `llm.provider=openai-compatible` 时能注册 `ProviderAdapter`
- 存在统一配置时能注册 `ChatModel` Bean
- 已有用户自定义 `ChatModel` 或 adapter 时不重复注册

- [ ] **Step 2: 实现 `OpenAiCompatibleProviderAdapter`**

要求：
- `supports("openai-compatible")` 返回 `true`
- 使用 `OpenAiCompatibleChatModelFactory` 构建 `ChatModel`
- 通过 `SpringAiLlmClient` + `DefaultAgentLlmGateway` 产出统一网关

- [ ] **Step 3: 实现 `SpringAiProviderAutoConfiguration`**

要求：
- 暴露 `ProviderAdapter` Bean
- 为需要直接使用 Spring AI 官方 `ReactAgent` 的场景暴露 `ChatModel` Bean
- 不在 Phase 1 强制迁移 `framework-core` 中既有 Spring AI 类

- [ ] **Step 4: 补 `AutoConfiguration.imports` 并跑模块测试**

Run: `/Users/sxie/maven/apache-maven-3.6.3/bin/mvn -q -pl framework-llm-springai -am -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository test`

Expected: `framework-llm-springai` 模块测试全部通过。

## Chunk 3: demo 接入与文档对齐

### Task 6: 改造 `module-react-paradigm` 使用统一配置和统一网关

**Files:**
- Modify: `module-react-paradigm/pom.xml`
- Modify: `module-react-paradigm/src/test/resources/application-openai-react-demo.yml`
- Modify: `module-react-paradigm/src/test/resources/application-openai-react-demo-local.yml.example`
- Modify: `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/ReActTravelOpenAiDemo.java`
- Modify: `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/config/OpenAiReactDemoTestConfigTest.java`

- [ ] **Step 1: 先调整测试目标**

把 `OpenAiReactDemoTestConfigTest` 从“只校验 `ChatModel` Bean”升级为：
- 校验统一 `AgentLlmGateway` Bean 存在
- 校验在 `openai-compatible` 配置下仍可提供 `ChatModel` Bean

- [ ] **Step 2: 更新 `module-react-paradigm/pom.xml`**

新增对 `framework-llm-autoconfigure`、`framework-llm-springai` 的依赖。保留或移除现有 OpenAI starter 依赖，以最终测试通过且官方 demo 不受影响为准。

- [ ] **Step 3: 把测试配置从 `spring.ai.openai.*` 改到 `llm.*`**

要求：
- 保留 `demo.react.openai.enabled`
- 将 `base-url`、`api-key`、`model`、`chat-completions-path` 按 spec 中的映射关系写入统一配置
- 本地示例文件继续保持可复制、可覆写、可忽略提交

- [ ] **Step 4: 改造 `ReActTravelOpenAiDemo`**

将 `context.getBean(ChatModel.class) -> new SpringAiLlmClient(...) -> new DefaultAgentLlmGateway(...)` 替换为直接从 Spring 容器获取统一 `AgentLlmGateway` Bean。

- [ ] **Step 5: 运行无外部依赖测试**

Run: `/Users/sxie/maven/apache-maven-3.6.3/bin/mvn -q -pl module-react-paradigm -am -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -Dtest=OpenAiReactDemoTestConfigTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: 配置装载测试通过，说明统一配置与自动装配链路已闭合。

- [ ] **Step 6: 在有真实 key 的前提下运行手写版真实 demo**

Run: `/Users/sxie/maven/apache-maven-3.6.3/bin/mvn -q -pl module-react-paradigm -am -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -Ddemo.react.openai.enabled=true -Dtest=ReActTravelOpenAiDemo -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: `ReActTravelOpenAiDemo` 可以通过统一 `AgentLlmGateway` 跑通，且历史消息中包含工具调用结果。

### Task 7: 更新学习文档，解释统一接入路径

**Files:**
- Modify: `docs/react-learning-path.md`

- [ ] **Step 1: 增加“统一 `llm.*` 配置”说明**

补充内容至少包括：
- 为什么不再让上层直接写 `spring.ai.openai.*`
- OpenAI compatible 场景中 `base-url` 与 `chat-completions-path` 的填写规则
- `AgentLlmGateway` 与 `ChatModel` 在新结构中的职责边界

- [ ] **Step 2: 运行文档相关回归检查**

Run: `rg -n "spring.ai.openai|llm\\." docs/react-learning-path.md module-react-paradigm/src/test/resources/application-openai-react-demo.yml`

Expected: 文档和配置用语一致，不再互相矛盾。

## Done Criteria

- `framework-llm-autoconfigure` 与 `framework-llm-springai` 模块建立完成并能通过测试。
- 统一 `llm.*` 配置可驱动 OpenAI compatible provider 创建 `AgentLlmGateway`。
- `ReActTravelOpenAiDemo` 通过统一网关运行。
- `OpenAiReactDemoTestConfigTest` 能验证统一网关和 `ChatModel` Bean 的存在。
- 文档已解释 `url`、`api-key`、`model`、`chat-completions-path` 的填写方式。

## Execution Notes

- 先只做 Phase 1，不在同一轮里迁移 `framework-core` 中全部 Spring AI 类。
- 如 `SpringAIReActTravelOpenAiDemo` 因配置切换受到影响，可在 Phase 1 中通过保留兼容 Bean 或保留旧配置回退处理，但不要扩大到全面重构。
- 如需进入实际编码，优先按 Chunk 顺序执行，并在每个 Chunk 结束后做一次本地回归。
