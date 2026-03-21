# LLM Provider Abstraction Phase 2 Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `framework-core` 中残留的 Spring AI 适配实现迁移到 `framework-llm-springai`，让 `framework-core` 只保留厂商无关的统一协议与运行时抽象。

**Architecture:** 保持 `framework-core` 提供 `AgentLlmGateway`、`LlmClient`、`LlmRequest`、`LlmResponse` 等公共协议；把 `adapter.springai` 下的具体实现和测试整体迁入 `framework-llm-springai`。对外仍通过 `framework-llm-springai` 暴露 Spring AI 能力，上层模块不感知迁移细节。

**Tech Stack:** Java 21, Maven, Spring Boot 3.5.9, Spring AI 1.1.2, JUnit 5

---

## Chunk 1: 迁移边界与模块依赖

### Task 1: 收口 Spring AI 适配类的归属

**Files:**
- Modify: `framework-core/pom.xml`
- Modify: `framework-llm-springai/pom.xml`

- [ ] **Step 1: 确认 `framework-core` 中需要迁出的 Spring AI 类**

迁移范围：
- `SpringAiLlmClient`
- `SpringAiMessageMapper`
- `SpringAiModelResolver`
- `SpringAiOptionsMapper`
- `SpringAiResponseMapper`
- `SpringAiStreamEventMapper`
- `SpringAiStreamingLlmClient`
- `SpringAiStructuredOutputLlmClient`
- `SpringAiToolMapper`

- [ ] **Step 2: 调整模块依赖**

要求：
- `framework-core` 不再依赖 `spring-ai-client-chat`
- `framework-llm-springai` 补足运行这些适配类所需的 Spring AI 依赖

- [ ] **Step 3: 运行受影响模块的编译/测试基线**

Run: `/Users/sxie/maven/apache-maven-3.6.3/bin/mvn -q -pl framework-core,framework-llm-springai,module-react-paradigm -am -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository test`

Expected: 迁移前基线通过。

## Chunk 2: 代码与测试迁移

### Task 2: 迁移 Spring AI 适配实现和测试

**Files:**
- Move: `framework-core/src/main/java/com/xbk/agent/framework/core/llm/adapter/springai/*`
- Move: `framework-core/src/test/java/com/xbk/agent/framework/core/llm/adapter/springai/*`

- [ ] **Step 1: 将实现类迁移到 `framework-llm-springai`**

目标包名建议统一为：
`com.xbk.agent.framework.llm.springai.adapter`

- [ ] **Step 2: 将测试类迁移到 `framework-llm-springai`**

保留原有断言语义，只修正包名和引用。

- [ ] **Step 3: 修正所有直接引用**

至少覆盖：
- `framework-llm-springai` 中的 `OpenAiCompatibleProviderAdapter`
- 受影响测试

- [ ] **Step 4: 删除 `framework-core` 中已迁出的 Spring AI 源码和测试目录**

要求：
- `framework-core` 不再出现 `adapter.springai` 包
- `framework-core` 测试中不再依赖 Spring AI 具体类型

## Chunk 3: 回归验证与文档同步

### Task 3: 运行回归并同步设计文档

**Files:**
- Modify: `docs/superpowers/specs/2026-03-20-llm-provider-abstraction-design.md`
- Modify: `docs/ReAct学习路径.md`

- [ ] **Step 1: 运行迁移后的模块测试**

Run: `/Users/sxie/maven/apache-maven-3.6.3/bin/mvn -q -pl framework-core,framework-llm-springai,module-react-paradigm -am -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository test`

Expected: 所有相关模块测试通过。

- [ ] **Step 2: 更新文档对实际包归属的描述**

要求：
- 设计文档不再把 Spring AI 适配实现列为 `framework-core` 的长期归属
- 学习文档中的接入链路仍然成立，但类所在模块更新为 `framework-llm-springai`

## Done Criteria

- `framework-core` 不再包含 `adapter.springai` 具体实现和测试。
- `framework-llm-springai` 承担全部 Spring AI 适配代码与测试。
- `framework-llm-springai` 对外行为不变，Phase 1 接入链路继续可用。
- `framework-core, framework-llm-springai, module-react-paradigm` 组合测试通过。
