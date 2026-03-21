# Plan-and-Solve 苹果题双实现对照 Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `module-plan-replan-paradigm` 中落地同一道“买苹果”逻辑题的两套 Java 实现，分别演示纯手写 Plan-and-Solve 运行时与 Spring AI Alibaba `SequentialAgent` 编排方式。

**Architecture:** 手写版由 `Planner`、`Executor` 与 `PlanAndSolveAgent` 组成，显式维护计划、历史执行记录与逐步求解循环。框架版使用两个子 Agent 和一个 `SequentialAgent`，通过 `outputKey` 与占位符把计划结果写入状态并传递给执行阶段。

**Tech Stack:** Java 21, JUnit 5, Spring Boot Test, framework-core `AgentLlmGateway`, Spring AI Alibaba Agent Framework 1.1.2.0

---

## File Structure

### Create

- `module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/domain/plan/PlanStep.java`
- `module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/domain/execution/StepExecutionRecord.java`
- `module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/application/executor/HandwrittenPlanner.java`
- `module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/application/executor/HandwrittenExecutor.java`
- `module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/application/coordinator/HandwrittenPlanAndSolveAgent.java`
- `module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/infrastructure/agentframework/AlibabaSequentialPlanAndSolveAgent.java`
- `module-plan-replan-paradigm/src/test/java/com/xbk/agent/framework/planreplan/PlanAndSolveAppleProblemDemoTest.java`

## Chunk 1: 手写版最小闭环

### Task 1: 先写手写版失败测试

**Files:**
- Create: `module-plan-replan-paradigm/src/test/java/com/xbk/agent/framework/planreplan/PlanAndSolveAppleProblemDemoTest.java`

- [ ] **Step 1: 写手写版失败测试**

测试目标：
- `HandwrittenPlanner` 能把问题拆成结构化步骤
- `HandwrittenPlanAndSolveAgent` 能按步骤执行并得到最终答案
- `history` 中保留每一步的执行结果

- [ ] **Step 2: 运行测试确认失败**

Run:
```bash
mvn -q -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -pl module-plan-replan-paradigm -am -Dtest=PlanAndSolveAppleProblemDemoTest#shouldSolveAppleProblemWithHandwrittenPlanAndSolveAgent -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 因为相关类尚未创建而失败。

### Task 2: 实现手写版最小代码

**Files:**
- Create: `module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/domain/plan/PlanStep.java`
- Create: `module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/domain/execution/StepExecutionRecord.java`
- Create: `module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/application/executor/HandwrittenPlanner.java`
- Create: `module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/application/executor/HandwrittenExecutor.java`
- Create: `module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/application/coordinator/HandwrittenPlanAndSolveAgent.java`

- [ ] **Step 1: 创建计划步骤与执行记录 DTO**
- [ ] **Step 2: 实现 `HandwrittenPlanner`**
- [ ] **Step 3: 实现 `HandwrittenExecutor`**
- [ ] **Step 4: 实现 `HandwrittenPlanAndSolveAgent` 手写循环**
- [ ] **Step 5: 回跑手写版测试确认通过**

## Chunk 2: Spring AI Alibaba 顺序编排版

### Task 3: 先写框架版失败测试

**Files:**
- Modify: `module-plan-replan-paradigm/src/test/java/com/xbk/agent/framework/planreplan/PlanAndSolveAppleProblemDemoTest.java`

- [ ] **Step 1: 增加框架版失败测试**

测试目标：
- 使用 `SequentialAgent` 串联 `PlannerAgent` 与 `ExecutorAgent`
- `planner` 输出通过 `outputKey=plan_result` 写入状态
- `executor` 能通过占位符读取 `{input}` 与 `{plan_result}`

- [ ] **Step 2: 运行该测试确认失败**

Run:
```bash
mvn -q -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -pl module-plan-replan-paradigm -am -Dtest=PlanAndSolveAppleProblemDemoTest#shouldSolveAppleProblemWithAlibabaSequentialAgent -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 因为框架版封装类尚未创建而失败。

### Task 4: 实现框架版顺序 Agent 封装

**Files:**
- Create: `module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/infrastructure/agentframework/AlibabaSequentialPlanAndSolveAgent.java`

- [ ] **Step 1: 创建 `PlannerAgent` 构造逻辑**
- [ ] **Step 2: 创建 `ExecutorAgent` 构造逻辑**
- [ ] **Step 3: 创建 `SequentialAgent` 封装与状态读取逻辑**
- [ ] **Step 4: 回跑框架版测试确认通过**

## Chunk 3: 总体验证

### Task 5: 全量验证

**Files:**
- Verify: `module-plan-replan-paradigm`

- [ ] **Step 1: 运行测试类**

Run:
```bash
mvn -q -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -pl module-plan-replan-paradigm -am -Dtest=PlanAndSolveAppleProblemDemoTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 两个对照测试全部通过。

- [ ] **Step 2: 运行模块编译**

Run:
```bash
mvn -q -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -pl module-plan-replan-paradigm -am -DskipTests compile
```

Expected: 编译通过。
