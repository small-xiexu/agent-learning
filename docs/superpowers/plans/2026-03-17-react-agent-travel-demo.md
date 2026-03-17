# React Agent Travel Demo Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `module-react-paradigm` 中实现首个纯手写 `ReActAgent`，并提供“智能旅行助手” Demo 与测试。

**Architecture:** 生产代码只新增一个 `ReActAgent`，通过 `HelloAgentsLLM` + `ToolRegistry` + `Message history` 组成最小闭环；测试侧新增 `ReActTravelDemo`，内部提供假 LLM 和本地工具，完整模拟天气查询与景点推荐流程。循环由 `while (step < maxSteps)` 控制，确保不会无限执行。

**Tech Stack:** Java 21、JUnit 5、framework-core 契约、Maven 多模块

---

## Chunk 1: 测试先行

### Task 1: 编写旅行助手闭环测试

**Files:**
- Create: `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/ReActTravelDemo.java`

- [ ] **Step 1: 写测试，验证 Agent 能完成天气查询 -> 景点推荐 -> 最终答案闭环**
- [ ] **Step 2: 运行 `ReActTravelDemo` 对应测试，确认先失败**

### Task 2: 编写 maxSteps 安全阀测试

**Files:**
- Modify: `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/ReActTravelDemo.java`

- [ ] **Step 1: 写测试，验证假 LLM 一直返回工具调用时，Agent 会在 `maxSteps` 后停止**
- [ ] **Step 2: 运行测试，确认按预期失败**

## Chunk 2: 最小实现

### Task 3: 实现 `ReActAgent`

**Files:**
- Create: `module-react-paradigm/src/main/java/com/xbk/agent/framework/react/application/executor/ReActAgent.java`

- [ ] **Step 1: 实现构造器注入与默认 `maxSteps=5`**
- [ ] **Step 2: 实现 `run(String userQuery)` 主循环**
- [ ] **Step 3: 实现消息追加、工具执行、最终答案解析**
- [ ] **Step 4: 提供最近一次历史快照读取能力，供 Demo 打印流转过程**

### Task 4: 完善 Demo

**Files:**
- Modify: `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/ReActTravelDemo.java`

- [ ] **Step 1: 实现本地假 `HelloAgentsLLM`**
- [ ] **Step 2: 实现 `WeatherTool` 与 `SearchAttractionTool`**
- [ ] **Step 3: 添加 `main` 方法，打印 Thought / Action / Observation / Final Answer**

## Chunk 3: 验证

### Task 5: 运行相关测试

**Files:**
- Verify: `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/ReActTravelDemo.java`

- [ ] **Step 1: 运行模块测试**

Run: `mvn -q -pl module-react-paradigm -am -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -Dtest=ReActTravelDemo test`
Expected: PASS

- [ ] **Step 2: 运行模块级验证**

Run: `mvn -q -pl module-react-paradigm -am -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository test`
Expected: PASS
