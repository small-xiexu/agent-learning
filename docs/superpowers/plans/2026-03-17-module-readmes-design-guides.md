# Module README Design Guides Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 7 个智能体范式模块补齐高质量 `README.md`，沉淀理论背景、运行机制与 Spring AI Alibaba 映射实现。

**Architecture:** 采用统一的模块级 README 模板，在各模块根目录沉淀 ADR 风格设计文档。每份文档共享一致的章节结构，但内容围绕各自范式的理论动机、执行闭环、状态流转与框架映射进行定制，避免模板化空话。

**Tech Stack:** Markdown、Maven 多模块工程、Spring AI Alibaba 1.1.2.0 官方文档术语

---

## Chunk 1: 文档模板与信息源收口

### Task 1: 确认 README 落点与统一章节结构

**Files:**
- Create: `module-react-paradigm/README.md`
- Create: `module-plan-replan-paradigm/README.md`
- Create: `module-reflection-paradigm/README.md`
- Create: `module-graph-flow-paradigm/README.md`
- Create: `module-multi-agent-conversation/README.md`
- Create: `module-multi-agent-engineering/README.md`
- Create: `module-multi-agent-roleplay/README.md`

- [ ] **Step 1: 确认 7 个模块当前均无模块级 README**

Run: `find module-react-paradigm module-plan-replan-paradigm module-reflection-paradigm module-graph-flow-paradigm module-multi-agent-conversation module-multi-agent-engineering module-multi-agent-roleplay -maxdepth 2 -name README.md | sort`
Expected: 无输出

- [ ] **Step 2: 确认统一章节模板**

模板章节：
1. 模块定位
2. 理论背景
3. 运行机制
4. Spring AI Alibaba 映射
5. 工程落地建议
6. 与 `framework-core` 的关系
7. 适用场景与边界

- [ ] **Step 3: 收口官方术语来源**

参考来源仅使用 Spring AI Alibaba 官方文档与用户明确给出的理论约束，避免二手解释漂移。

## Chunk 2: 单体智能体 3 个模块 README

### Task 2: 编写 `module-react-paradigm/README.md`

**Files:**
- Create: `module-react-paradigm/README.md`

- [ ] **Step 1: 写入 ReAct 的理论背景**
- [ ] **Step 2: 写入 Thought -> Action -> Observation 的运行闭环**
- [ ] **Step 3: 写入 `ReactAgent`、Model Node、Tool Node、Graph 运行时映射**
- [ ] **Step 4: 校对术语与模块职责一致性**

### Task 3: 编写 `module-plan-replan-paradigm/README.md`

**Files:**
- Create: `module-plan-replan-paradigm/README.md`

- [ ] **Step 1: 写入 Planning / Solving 分层思想**
- [ ] **Step 2: 写入 Plan -> Execute -> Replan 的状态机循环**
- [ ] **Step 3: 写入 `outputType` / `outputSchema` 结构化输出映射**
- [ ] **Step 4: 补充状态管理与子任务执行建议**

### Task 4: 编写 `module-reflection-paradigm/README.md`

**Files:**
- Create: `module-reflection-paradigm/README.md`

- [ ] **Step 1: 写入执行 -> 反思 -> 修正的理论背景**
- [ ] **Step 2: 写入生成者 / 评审者双角色协作机制**
- [ ] **Step 3: 写入最大迭代次数、成本控制与停止条件**
- [ ] **Step 4: 补充与 `Memory` / `Message` 协议的结合方式**

## Chunk 3: 图编排与多智能体 4 个模块 README

### Task 5: 编写 `module-graph-flow-paradigm/README.md`

**Files:**
- Create: `module-graph-flow-paradigm/README.md`

- [ ] **Step 1: 写入状态机与有向图理论**
- [ ] **Step 2: 写入节点、边、条件边与循环机制**
- [ ] **Step 3: 写入 `FlowAgent` 与 `buildSpecificGraph` 映射**
- [ ] **Step 4: 补充适合承载复杂编排的边界说明**

### Task 6: 编写 `module-multi-agent-conversation/README.md`

**Files:**
- Create: `module-multi-agent-conversation/README.md`

- [ ] **Step 1: 写入群聊式去中心化协作理论**
- [ ] **Step 2: 写入专家接管与控制权迁移机制**
- [ ] **Step 3: 写入 Handoffs 映射与状态传递语义**
- [ ] **Step 4: 补充适用场景与失控风险控制**

### Task 7: 编写 `module-multi-agent-engineering/README.md`

**Files:**
- Create: `module-multi-agent-engineering/README.md`

- [ ] **Step 1: 写入消息驱动与工程化理论**
- [ ] **Step 2: 写入状态隔离、消息路由与数据接力机制**
- [ ] **Step 3: 写入 Context Engineering、`includeContents`、`returnReasoningContent`、占位符映射**
- [ ] **Step 4: 补充高并发与生产治理建议**

### Task 8: 编写 `module-multi-agent-roleplay/README.md`

**Files:**
- Create: `module-multi-agent-roleplay/README.md`

- [ ] **Step 1: 写入 CAMEL 式角色扮演理论**
- [ ] **Step 2: 写入 Inception Prompting 与角色协议设计**
- [ ] **Step 3: 写入 `SequentialAgent`、`outputKey`、顺序接力映射**
- [ ] **Step 4: 补充角色漂移与回合约束建议**

## Chunk 4: 验证

### Task 9: 验证 README 落地结果

**Files:**
- Verify: `module-react-paradigm/README.md`
- Verify: `module-plan-replan-paradigm/README.md`
- Verify: `module-reflection-paradigm/README.md`
- Verify: `module-graph-flow-paradigm/README.md`
- Verify: `module-multi-agent-conversation/README.md`
- Verify: `module-multi-agent-engineering/README.md`
- Verify: `module-multi-agent-roleplay/README.md`

- [ ] **Step 1: 列出 7 个 README 文件**

Run: `find module-react-paradigm module-plan-replan-paradigm module-reflection-paradigm module-graph-flow-paradigm module-multi-agent-conversation module-multi-agent-engineering module-multi-agent-roleplay -maxdepth 2 -name README.md | sort`
Expected: 输出 7 条 README 路径

- [ ] **Step 2: 运行最小构建验证**

Run: `mvn -q -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -DskipTests verify`
Expected: 退出码 0
