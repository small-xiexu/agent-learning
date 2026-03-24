# Supervisor Docs And Comments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `module-multi-agent-supervisor` 补齐专题导读、模块 README 和主测试代码注释，使读者能从 0 到 1 理解 Supervisor 范式及其 Spring AI Alibaba 落地过程。

**Architecture:** 先新增一篇面向新手但覆盖完整运行链路的 Supervisor 专题导读，再把模块 README 调整为“总览 + 导读入口”结构，最后对 `src/main/java` 与 `src/test/java` 中的核心类补充职责导向注释，突出手写版与框架版的一一映射关系。所有代码行为保持不变，只提升文档完备度和源码可读性。

**Tech Stack:** Java 21, Maven, Spring Boot 3.5, Spring AI Alibaba Agent Framework, JUnit 5, Markdown

---

### Task 1: Add Supervisor Guide

**Files:**
- Create: `docs/Supervisor范式从0到1掌握指南.md`
- Modify: `docs/项目整体架构导读.md`

- [ ] 梳理现有 `AutoGen`、`CAMEL`、`Plan-and-Solve` 导读的写作风格和章节粒度
- [ ] 新增 Supervisor 专题导读，覆盖范式本体、与相邻模块差异、技术栈分层、手写版与框架版对照、Spring AI Alibaba 实现过程、测试边界、真实 Demo 启动方式
- [ ] 在顶层架构导读中补充 Supervisor 专题入口

### Task 2: Restructure Supervisor README

**Files:**
- Modify: `module-multi-agent-supervisor/README.md`

- [ ] 保留已有理论说明中的有效内容
- [ ] 增加 `当前实现`、`模块里的最小对照样例`、`双实现总览`、`与测试的对应关系`、`真实 OpenAI Demo`、`推荐阅读顺序`、`延伸阅读`
- [ ] 删除或改写偏未来态的规划描述，使 README 与当前代码现状一致

### Task 3: Improve Main Source Comments

**Files:**
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/api/SupervisorRunResult.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/domain/memory/SupervisorStepRecord.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/domain/routing/CompletionPolicy.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/domain/routing/RoutingDecision.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/domain/routing/SupervisorWorkerType.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/domain/state/SupervisorWorkflowState.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/framework/agent/AlibabaSupervisorFlowAgent.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/framework/prompt/FrameworkSupervisorPromptTemplates.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/framework/support/SupervisorGatewayBackedChatModel.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/framework/support/SupervisorStateExtractor.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/framework/support/SupervisorStateKeys.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/handwritten/coordinator/HandwrittenSupervisorCoordinator.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/handwritten/executor/ReviewerAgent.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/handwritten/executor/TranslatorAgent.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/handwritten/executor/WriterAgent.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/handwritten/memory/SupervisorScratchpad.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/handwritten/parser/SupervisorDecisionJsonParser.java`
- Modify: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/handwritten/prompt/HandwrittenSupervisorPromptTemplates.java`

- [ ] 补充类级职责注释，解释该类在 Supervisor 体系里的位置
- [ ] 补充方法级 Javadoc，突出输入、输出、状态变化与关键流程
- [ ] 只在复杂控制流、状态恢复和框架适配点增加局部注释，避免噪音

### Task 4: Improve Test And Demo Comments

**Files:**
- Modify: `module-multi-agent-supervisor/src/test/java/com/xbk/agent/framework/supervisor/AlibabaSupervisorFlowOpenAiDemo.java`
- Modify: `module-multi-agent-supervisor/src/test/java/com/xbk/agent/framework/supervisor/HandwrittenSupervisorOpenAiDemo.java`
- Modify: `module-multi-agent-supervisor/src/test/java/com/xbk/agent/framework/supervisor/SupervisorDemoLogSupport.java`
- Modify: `module-multi-agent-supervisor/src/test/java/com/xbk/agent/framework/supervisor/SupervisorPatternComparisonTest.java`
- Modify: `module-multi-agent-supervisor/src/test/java/com/xbk/agent/framework/supervisor/config/OpenAiSupervisorDemoPropertySupport.java`
- Modify: `module-multi-agent-supervisor/src/test/java/com/xbk/agent/framework/supervisor/config/OpenAiSupervisorDemoPropertySupportTest.java`
- Modify: `module-multi-agent-supervisor/src/test/java/com/xbk/agent/framework/supervisor/config/OpenAiSupervisorDemoTestConfig.java`
- Modify: `module-multi-agent-supervisor/src/test/java/com/xbk/agent/framework/supervisor/config/OpenAiSupervisorDemoTestConfigTest.java`

- [ ] 让测试注释明确“这组测试在钉什么行为”
- [ ] 让 Demo 注释明确“为什么默认跳过真实外网调用”
- [ ] 让配置支撑类注释说明 profile、资源文件和启用条件

### Task 5: Verify Consistency

**Files:**
- Verify only

- [ ] Run: `JAVA_HOME=/Users/xiexu/Library/Java/JavaVirtualMachines/corretto-21.0.10/Contents/Home PATH=/Users/xiexu/Library/Java/JavaVirtualMachines/corretto-21.0.10/Contents/Home/bin:$PATH mvn -q -pl module-multi-agent-supervisor -am test`
- [ ] 复查新专题导读、README、顶层导读之间的链接
- [ ] 复查 README 里的代码入口、测试入口、配置入口是否都存在
