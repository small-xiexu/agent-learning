# CAMEL 股票分析双实现对照 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `module-multi-agent-roleplay` 中落地同一道“股票分析 Python 脚本开发”任务的两套 Java 实现，分别演示纯手写 CAMEL 协作运行时与基于 Spring AI Alibaba `FlowAgent + StateGraph + OverAllState` 的企业工程化交接回环，并且两套实现都统一使用现有 `AgentLlmGateway`。

**Architecture:** 手写版围绕 `交易员 -> 程序员 -> 交易员` 的显式 while 循环展开，手动维护共享对话 memory、路由和终止标记。框架版保留 `ReactAgent` 作为角色定义对象，用 `FlowAgent`、节点和条件边驱动活动角色切换，并通过最小共享状态而不是手工拼接大段历史来完成 handoff。

**Tech Stack:** Java 21, JUnit 5, Spring Boot Test, framework-core `AgentLlmGateway`, framework-core `Message/Memory`, Spring AI Alibaba Agent Framework 1.1.2.0

---

## File Structure

### Modify

- `module-multi-agent-roleplay/pom.xml`

### Create

- `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/api/CamelRunResult.java`
- `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/domain/role/CamelRoleType.java`
- `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/domain/role/CamelRoleContract.java`
- `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/domain/memory/CamelDialogueTurn.java`
- `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/domain/memory/CamelConversationMemory.java`
- `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/support/CamelPromptTemplates.java`
- `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/support/CamelGatewayBackedChatModel.java`
- `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/application/executor/CamelTraderAgent.java`
- `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/application/executor/CamelProgrammerAgent.java`
- `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/application/coordinator/HandwrittenCamelAgent.java`
- `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/infrastructure/agentframework/AlibabaCamelFlowAgent.java`
- `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/infrastructure/agentframework/node/CamelTraderHandoffNode.java`
- `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/infrastructure/agentframework/node/CamelProgrammerHandoffNode.java`
- `module-multi-agent-roleplay/src/test/java/com/xbk/agent/framework/camel/CamelStockAnalysisComparisonTest.java`
- `module-multi-agent-roleplay/src/test/java/com/xbk/agent/framework/camel/config/OpenAiRolePlayDemoTestConfigTest.java`

## Design Constraints To Preserve During Implementation

- 两个角色必须使用强约束 Inception Prompt，明确一问一答、程序员每次只输出一段代码、交易员负责提出下一步需求。
- 任意一方输出 `<CAMEL_TASK_DONE>` 必须立即触发停止。
- 手写版必须显式保留 while 循环和共享 memory。
- 框架版必须显式保留 `FlowAgent + StateGraph + OverAllState`，不能退化成普通 while。
- 两套实现都必须依赖 `AgentLlmGateway`，不能直接绕到外部模型 SDK。
- 框架版需要把“活动角色切换”建模成状态流转，而不是固定顺序的顺序链。

## Chunk 1: 建立模块依赖与最小测试入口

### Task 1: 先补齐 roleplay 模块依赖

**Files:**
- Modify: `module-multi-agent-roleplay/pom.xml`

- [ ] **Step 1: 为模块加入与相邻范式一致的运行时依赖**
  目标：
  - 补充 `framework-llm-autoconfigure`
  - 补充 `framework-llm-springai`
  - 补充 `spring-boot-starter`
  - 补充 `spring-ai-alibaba-agent-framework`
  - 补充 `spring-boot-starter-test`

- [ ] **Step 2: 确认模块具备编译手写版与框架版所需的最小依赖面**

### Task 2: 先写总测试骨架并让它失败

**Files:**
- Create: `module-multi-agent-roleplay/src/test/java/com/xbk/agent/framework/camel/CamelStockAnalysisComparisonTest.java`

- [ ] **Step 1: 增加手写版失败测试**
  测试目标：
  - `HandwrittenCamelAgent` 能驱动交易员和程序员多轮对话
  - 共享 memory 中能看到至少两轮角色接力
  - 最终结果包含 Python 脚本核心要素：股票实时价格、移动平均线、终止标记

- [ ] **Step 2: 增加框架版失败测试**
  测试目标：
  - `AlibabaCamelFlowAgent` 能通过 `FlowAgent` 完成交接回环
  - 状态中能观察到 `active_role` 切换、最新 trader/programmer 输出和 `done` 标记
  - 最终结果与手写版一样，能产出完整脚本文本

- [ ] **Step 3: 运行测试确认因为类尚未创建而失败**
  建议命令：
  `mvn -q -pl module-multi-agent-roleplay -am -Dtest=CamelStockAnalysisComparisonTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Chunk 2: 先实现手写版 CAMEL 最小闭环

### Task 3: 建立角色契约与对话内存模型

**Files:**
- Create: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/domain/role/CamelRoleType.java`
- Create: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/domain/role/CamelRoleContract.java`
- Create: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/domain/memory/CamelDialogueTurn.java`
- Create: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/domain/memory/CamelConversationMemory.java`
- Create: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/support/CamelPromptTemplates.java`

- [ ] **Step 1: 定义角色枚举和角色契约对象**
  要点：
  - 交易员与程序员角色名固定
  - 系统提示、交付对象、终止标记、禁止项集中建模

- [ ] **Step 2: 定义对话轮次对象**
  要点：
  - 记录轮次、发言角色、消息文本
  - 保证后续既能给测试断言，也能给运行结果回放

- [ ] **Step 3: 定义共享 conversation memory**
  要点：
  - 支持追加轮次
  - 支持快照
  - 支持按“当前角色视角”映射为 `framework-core Message` 列表

- [ ] **Step 4: 集中编写 CAMEL 强约束 Prompt 模板**
  要点：
  - 交易员 prompt 明确“不写代码，只提需求和验收”
  - 程序员 prompt 明确“每次只写一段代码，不改变业务目标”
  - 双方都必须遵守一问一答和 `<CAMEL_TASK_DONE>` 协议

### Task 4: 建立统一网关文本交换支撑类

**Files:**
- Create: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/support/CamelGatewayBackedChatModel.java`

- [ ] **Step 1: 封装 `AgentLlmGateway` 调用入口**
  要点：
  - 统一创建 `LlmRequest`
  - 统一生成 `conversationId` 与 `requestId`
  - 统一提取响应文本

- [ ] **Step 2: 封装统一消息构造**
  要点：
  - 避免手写版角色执行器和框架版节点重复拼装 `Message`
  - 保持所有调用都可追踪、可测试

### Task 5: 实现手写版两个角色执行器

**Files:**
- Create: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/application/executor/CamelTraderAgent.java`
- Create: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/application/executor/CamelProgrammerAgent.java`

- [ ] **Step 1: 实现交易员单轮发言逻辑**
  要点：
  - 输入为原始任务或程序员上一轮输出
  - 使用交易员专属 system prompt
  - 输出为下一步业务要求或终止标记

- [ ] **Step 2: 实现程序员单轮发言逻辑**
  要点：
  - 输入为交易员上一轮需求
  - 使用程序员专属 system prompt
  - 输出为一段完整 Java 代码或终止标记

### Task 6: 实现手写版协调器

**Files:**
- Create: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/api/CamelRunResult.java`
- Create: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/application/coordinator/HandwrittenCamelAgent.java`
- Modify: `module-multi-agent-roleplay/src/test/java/com/xbk/agent/framework/camel/CamelStockAnalysisComparisonTest.java`

- [ ] **Step 1: 设计统一运行结果对象**
  要点：
  - 包含原始任务
  - 包含最终脚本文本
  - 包含停止角色、停止原因、完整 transcript
  - 为框架版预留状态快照字段

- [ ] **Step 2: 在 `HandwrittenCamelAgent` 中实现显式 while 循环**
  要点：
  - 交易员先开场
  - 程序员接收交易员输出
  - 再把程序员输出回传交易员
  - 每轮都写入共享 memory

- [ ] **Step 3: 实现停止条件**
  要点：
  - 任意一方输出 `<CAMEL_TASK_DONE>` 时立即停止
  - 达到最大轮次时兜底停止并返回当前 transcript

- [ ] **Step 4: 回跑手写版测试并补齐断言**
  断言重点：
  - transcript 顺序正确
  - 终止机制生效
  - 最终代码文本包含实时价格获取与移动平均线逻辑

## Chunk 3: 再实现 Spring AI Alibaba 图编排版

### Task 7: 为框架版定义状态协议与角色元数据

**Files:**
- Create: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/infrastructure/agentframework/AlibabaCamelFlowAgent.java`
- Modify: `module-multi-agent-roleplay/src/test/java/com/xbk/agent/framework/camel/CamelStockAnalysisComparisonTest.java`

- [ ] **Step 1: 设计图运行时状态键**
  必需状态：
  - `input`
  - `conversation_id`
  - `active_role`
  - `turn_count`
  - `message_for_trader`
  - `message_for_programmer`
  - `last_trader_output`
  - `last_programmer_output`
  - `current_java_code`
  - `done`
  - `stop_reason`
  - `transcript`

- [ ] **Step 2: 在框架版主类中声明交易员与程序员 `ReactAgent` 元数据**
  要点：
  - `ReactAgent` 主要承载角色名称、职责说明、system prompt、输出键
  - 真实执行链仍由 `FlowAgent` 图节点承担

- [ ] **Step 3: 在测试中先断言这些角色元数据和状态键约定**

### Task 8: 实现 handoff 节点

**Files:**
- Create: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/infrastructure/agentframework/node/CamelTraderHandoffNode.java`
- Create: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/infrastructure/agentframework/node/CamelProgrammerHandoffNode.java`

- [ ] **Step 1: 实现交易员 handoff 节点**
  要点：
  - 从状态读取程序员最新输出或初始任务
  - 经 `AgentLlmGateway` 生成交易员回复
  - 写回 `last_trader_output`、`message_for_programmer`、`active_role`、`transcript`

- [ ] **Step 2: 实现程序员 handoff 节点**
  要点：
  - 从状态读取交易员最新要求
  - 经 `AgentLlmGateway` 生成程序员代码输出
  - 写回 `last_programmer_output`、`current_java_code`、`message_for_trader`、`active_role`、`transcript`

- [ ] **Step 3: 在节点内统一复用 `CamelGatewayBackedChatModel`**
  要点：
  - 保证框架版底层仍然完全走统一网关
  - 避免节点内部再次复制消息拼装逻辑

### Task 9: 实现 `FlowAgent` 主体与条件边

**Files:**
- Modify: `module-multi-agent-roleplay/src/main/java/com/xbk/agent/framework/camel/infrastructure/agentframework/AlibabaCamelFlowAgent.java`
- Modify: `module-multi-agent-roleplay/src/test/java/com/xbk/agent/framework/camel/CamelStockAnalysisComparisonTest.java`

- [ ] **Step 1: 在 `buildSpecificGraph(...)` 中创建 `TRADER_NODE` 和 `PROGRAMMER_NODE`**

- [ ] **Step 2: 建立基础路径**
  路径要求：
  - `START -> TRADER_NODE`
  - `TRADER_NODE -> PROGRAMMER_NODE` 或 `END`
  - `PROGRAMMER_NODE -> TRADER_NODE` 或 `END`

- [ ] **Step 3: 实现条件边逻辑**
  停止条件：
  - 任意输出含 `<CAMEL_TASK_DONE>`
  - 最大轮次到达
  - 状态显式标记 `done=true`

- [ ] **Step 4: 在 `run(String task)` 中完成初始状态构建与结果回填**
  要点：
  - 把图最终状态还原为统一 `CamelRunResult`
  - 让调用方可以同时看到最终脚本和状态快照

- [ ] **Step 5: 回跑框架版测试并补齐断言**
  断言重点：
  - `active_role` 发生切换
  - `transcript` 被状态持续累积
  - `done` 与 `stop_reason` 合理

## Chunk 4: 增加配置烟雾测试与总体验证

### Task 10: 增加自动装配烟雾测试

**Files:**
- Create: `module-multi-agent-roleplay/src/test/java/com/xbk/agent/framework/camel/config/OpenAiRolePlayDemoTestConfigTest.java`

- [ ] **Step 1: 参照相邻模块的配置测试，验证上下文内可获得 `AgentLlmGateway`**

- [ ] **Step 2: 如果框架版需要 Spring 容器中的其他运行时依赖，一并做最小装配校验**

### Task 11: 运行完整验证

**Files:**
- Verify: `module-multi-agent-roleplay`

- [ ] **Step 1: 运行对照测试类**
  建议命令：
  `mvn -q -pl module-multi-agent-roleplay -am -Dtest=CamelStockAnalysisComparisonTest -Dsurefire.failIfNoSpecifiedTests=false test`

- [ ] **Step 2: 运行配置烟雾测试**
  建议命令：
  `mvn -q -pl module-multi-agent-roleplay -am -Dtest=OpenAiRolePlayDemoTestConfigTest -Dsurefire.failIfNoSpecifiedTests=false test`

- [ ] **Step 3: 运行模块编译验证**
  建议命令：
  `mvn -q -pl module-multi-agent-roleplay -am -DskipTests compile`

- [ ] **Step 4: 复核最终注释**
  要点：
  - 在两套主类末尾注释中解释
  - “手写 while 做消息路由”与“Spring AI Alibaba handoff 图编排”在工程思维、边界清晰度、状态治理和可维护性上的差异

## Risks And Guardrails

- 不要把框架版退化成普通 while；回环必须交给图和条件边。
- 不要在框架版里直接依赖外部模型 SDK；统一走 `AgentLlmGateway`。
- 不要把完整 transcript 反复拼成超长 prompt；框架版优先传递最小必要 baton。
- 不要让交易员角色写代码，也不要让程序员角色重写业务目标。
- 测试桩必须脚本化地模拟多轮对话，保证两套实现都能稳定复现同一个学习任务。
