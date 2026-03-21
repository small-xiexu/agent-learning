# Reflection Prime Comparison Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `module-reflection-paradigm` 中落地同一道“素数生成代码优化”任务的两套 Java 实现，分别演示纯手写 Reflection 运行时与 Spring AI Alibaba 图编排版 Reflection 流程。

**Architecture:** 手写版由 JavaCoder、JavaReviewer、Memory 和 ReflectionAgent 组成，显式维护“初稿生成 -> 反思 -> 优化 -> 再反思”的 while 循环。框架版通过自定义 `FlowAgent`、`JavaCoderNode`、`JavaReviewerNode` 与 `StateGraph` 条件边，把代码版本和评审意见写入状态，在图中形成受控回环。

**Tech Stack:** Java 21, JUnit 5, Spring Boot Test, framework-core `AgentLlmGateway`, Spring AI Alibaba Agent Framework 1.1.2.0, Spring AI compatible ChatModel

---

## File Structure

### Create

- `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/domain/memory/ReflectionTurnRecord.java`
- `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/domain/memory/ReflectionMemory.java`
- `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/application/executor/HandwrittenJavaCoder.java`
- `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/application/executor/HandwrittenJavaReviewer.java`
- `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/application/coordinator/HandwrittenReflectionAgent.java`
- `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/infrastructure/agentframework/node/JavaCoderNode.java`
- `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/infrastructure/agentframework/node/JavaReviewerNode.java`
- `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/infrastructure/agentframework/AlibabaReflectionFlowAgent.java`
- `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/ReflectionPrimeGenerationDemoTest.java`
- `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/HandwrittenReflectionOpenAiDemo.java`
- `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/AlibabaReflectionFlowOpenAiDemo.java`
- `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/config/OpenAiReflectionDemoPropertySupport.java`
- `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/config/OpenAiReflectionDemoPropertySupportTest.java`
- `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/config/OpenAiReflectionDemoTestConfig.java`
- `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/config/OpenAiReflectionDemoTestConfigTest.java`
- `module-reflection-paradigm/src/test/resources/application-openai-reflection-demo.yml`
- `module-reflection-paradigm/src/test/resources/application-openai-reflection-demo-local.yml.example`
- `module-reflection-paradigm/src/test/resources/openai-reflection-demo-fixture/application-openai-reflection-demo.yml`
- `module-reflection-paradigm/src/test/resources/openai-reflection-demo-fixture/application-openai-reflection-demo-local.yml`
- `docs/Reflection范式新手导读.md`

### Modify

- `module-reflection-paradigm/pom.xml`
- `module-reflection-paradigm/README.md`
- `.gitignore`

## Chunk 1: 手写版 Reflection 闭环

### Task 1: 先写手写版失败测试

**Files:**
- Create: `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/ReflectionPrimeGenerationDemoTest.java`

- [ ] **Step 1: 写手写版失败测试**
- [ ] **Step 2: 运行测试确认因为类缺失而失败**

Run:
```bash
export JAVA_HOME=/Users/sxie/Library/Java/JavaVirtualMachines/azul-21.0.10/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
mvn -q -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -pl module-reflection-paradigm -am -Dtest=ReflectionPrimeGenerationDemoTest#shouldImprovePrimeGeneratorWithHandwrittenReflectionAgent -Dsurefire.failIfNoSpecifiedTests=false test
```

### Task 2: 实现手写版最小代码

**Files:**
- Create: `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/domain/memory/ReflectionTurnRecord.java`
- Create: `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/domain/memory/ReflectionMemory.java`
- Create: `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/application/executor/HandwrittenJavaCoder.java`
- Create: `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/application/executor/HandwrittenJavaReviewer.java`
- Create: `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/application/coordinator/HandwrittenReflectionAgent.java`

- [ ] **Step 1: 创建 Memory 与 TurnRecord**
- [ ] **Step 2: 实现 JavaCoder 的 INITIAL_PROMPT 与 REFINE_PROMPT**
- [ ] **Step 3: 实现 JavaReviewer 的 REFLECT_PROMPT**
- [ ] **Step 4: 实现 ReflectionAgent 的 while 循环与终止条件**
- [ ] **Step 5: 回跑手写版测试确认通过**

## Chunk 2: Spring AI Alibaba 图编排版

### Task 3: 先写图编排版失败测试

**Files:**
- Modify: `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/ReflectionPrimeGenerationDemoTest.java`

- [ ] **Step 1: 增加 FlowAgent 版失败测试**
- [ ] **Step 2: 运行测试确认因为图编排实现缺失而失败**

Run:
```bash
export JAVA_HOME=/Users/sxie/Library/Java/JavaVirtualMachines/azul-21.0.10/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
mvn -q -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -pl module-reflection-paradigm -am -Dtest=ReflectionPrimeGenerationDemoTest#shouldImprovePrimeGeneratorWithAlibabaReflectionFlowAgent -Dsurefire.failIfNoSpecifiedTests=false test
```

### Task 4: 实现图编排版最小代码

**Files:**
- Create: `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/infrastructure/agentframework/node/JavaCoderNode.java`
- Create: `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/infrastructure/agentframework/node/JavaReviewerNode.java`
- Create: `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/infrastructure/agentframework/AlibabaReflectionFlowAgent.java`

- [ ] **Step 1: 创建 JavaCoderNode，负责初稿与优化版代码生成**
- [ ] **Step 2: 创建 JavaReviewerNode，负责复杂度评审与停止判断**
- [ ] **Step 3: 用 FlowAgent + StateGraph + Conditional Edge 连接循环**
- [ ] **Step 4: 回跑图编排版测试确认通过**

## Chunk 3: 真实 Demo 与学习文档

### Task 5: 真实 OpenAI Demo 与配置支持

**Files:**
- Modify: `module-reflection-paradigm/pom.xml`
- Modify: `.gitignore`
- Create: `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/config/OpenAiReflectionDemoPropertySupport.java`
- Create: `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/config/OpenAiReflectionDemoPropertySupportTest.java`
- Create: `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/config/OpenAiReflectionDemoTestConfig.java`
- Create: `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/config/OpenAiReflectionDemoTestConfigTest.java`
- Create: `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/HandwrittenReflectionOpenAiDemo.java`
- Create: `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/AlibabaReflectionFlowOpenAiDemo.java`
- Create: `module-reflection-paradigm/src/test/resources/application-openai-reflection-demo.yml`
- Create: `module-reflection-paradigm/src/test/resources/application-openai-reflection-demo-local.yml.example`
- Create: `module-reflection-paradigm/src/test/resources/openai-reflection-demo-fixture/application-openai-reflection-demo.yml`
- Create: `module-reflection-paradigm/src/test/resources/openai-reflection-demo-fixture/application-openai-reflection-demo-local.yml`

- [ ] **Step 1: 增加 Reflection 模块所需 llm 自动配置依赖**
- [ ] **Step 2: 写配置支持失败测试并确认失败**
- [ ] **Step 3: 实现配置支持类与最小上下文配置**
- [ ] **Step 4: 增加两个真实 OpenAI Demo**
- [ ] **Step 5: 跑配置测试与 test-compile 确认通过**

## Chunk 4: README 与新手导读

### Task 6: 补模块 README 和 docs 导读

**Files:**
- Modify: `module-reflection-paradigm/README.md`
- Create: `docs/Reflection范式新手导读.md`

- [ ] **Step 1: 在 README 中增加新手导航与双实现对照入口**
- [ ] **Step 2: 编写 Reflection 新手导读**
- [ ] **Step 3: 解释 while 循环与 StateGraph 条件边回环的工程差异**

## Chunk 5: 总体验证

### Task 7: 全量验证

**Files:**
- Verify: `module-reflection-paradigm`

- [ ] **Step 1: 运行 Reflection 模块测试**

Run:
```bash
export JAVA_HOME=/Users/sxie/Library/Java/JavaVirtualMachines/azul-21.0.10/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
mvn -q -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -pl module-reflection-paradigm -am -Dtest=ReflectionPrimeGenerationDemoTest,OpenAiReflectionDemoPropertySupportTest,OpenAiReflectionDemoTestConfigTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [ ] **Step 2: 运行 Reflection 模块 test-compile**

Run:
```bash
export JAVA_HOME=/Users/sxie/Library/Java/JavaVirtualMachines/azul-21.0.10/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
mvn -q -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -pl module-reflection-paradigm -am -DskipTests test-compile
```
