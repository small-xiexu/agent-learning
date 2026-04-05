# OpenAI Demo Config Unification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一 7 个 OpenAI Demo 模块的测试配置目录结构、YAML 加载支持逻辑和文档口径，使其对齐工程模块的“公共配置 / 场景配置”思路。

**Architecture:** 在 `framework-core` 中新增通用的 Demo YAML 配置支持类，负责主配置、本地覆盖配置和 `spring.config.import` 递归导入。各模块保留轻量门面类声明自身默认配置名与 demo 开关键；`src/test/resources` 统一拆分为模板主配置、共享 `application-llm-local.yml.example`、职责收窄的 `application-*-demo-local.yml.example` 与统一命名的 fixture 目录。

**Tech Stack:** Spring Boot YAML 配置加载、JUnit 5、Spring `Environment`/`PropertySource`、Maven 多模块工程

---

### Task 1: 建立共享 Demo 配置支持抽象

**Files:**
- Create: `framework-core/src/main/java/com/xbk/agent/framework/core/config/OpenAiDemoConfigSupport.java`
- Create: `framework-core/src/test/java/com/xbk/agent/framework/core/config/OpenAiDemoConfigSupportTest.java`

- [x] **Step 1: 先写共享支持类的失败测试**
  - 验收：已新增 `OpenAiDemoConfigSupportTest` 与递归 import / 显式 local 两组 fixture。

- [x] **Step 2: 运行共享支持类测试确认先红**
  - Run: `mvn -q -pl framework-core -Dtest=OpenAiDemoConfigSupportTest test`
  - 验收：已在显式切换到 JDK 21 后重跑，当前按预期因为 `OpenAiDemoConfigSupport` 尚未实现而编译失败。

- [x] **Step 3: 实现最小共享支持类**
  - 提供默认主配置/本地配置加载。
  - 支持 `optional:` 前缀、递归 import、相对路径解析、去重。
  - 提供 `hasConfiguredApiKey` 与 `isDemoEnabled` 的公共判断。
  - 验收：已在 `framework-core` 新增共享实现，兼容显式 local 加载并支持递归 import。

- [x] **Step 4: 重新运行共享支持类测试确认转绿**
  - Run: `mvn -q -pl framework-core -Dtest=OpenAiDemoConfigSupportTest test`
  - 验收：已在 JDK 21 下执行通过。

### Task 2: 收敛 7 个模块的 PropertySupport 门面类

**Files:**
- Modify: `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/config/OpenAiReactDemoPropertySupport.java`
- Modify: `module-plan-replan-paradigm/src/test/java/com/xbk/agent/framework/planreplan/config/OpenAiPlanSolveDemoPropertySupport.java`
- Modify: `module-reflection-paradigm/src/test/java/com/xbk/agent/framework/reflection/config/OpenAiReflectionDemoPropertySupport.java`
- Modify: `module-graph-flow-paradigm/src/test/java/com/xbk/agent/framework/graphflow/config/OpenAiGraphDemoPropertySupport.java`
- Modify: `module-multi-agent-conversation/src/test/java/com/xbk/agent/framework/conversation/config/OpenAiConversationDemoPropertySupport.java`
- Modify: `module-multi-agent-supervisor/src/test/java/com/xbk/agent/framework/supervisor/config/OpenAiSupervisorDemoPropertySupport.java`
- Modify: `module-multi-agent-roleplay/src/test/java/com/xbk/agent/framework/roleplay/config/OpenAiRolePlayDemoPropertySupport.java`
- Test: `module-*/src/test/java/**/OpenAi*DemoPropertySupportTest.java`

- [x] **Step 1: 先按共享抽象更新失败测试**
  - 各模块测试需验证主配置导入 `application-llm-local.yml` 与 demo-local 递归链路仍可读取。
  - 验收：7 个模块的 `PropertySupportTest` 都已切到共享 `application-llm-local.yml` + demo-local 双文件口径。

- [x] **Step 2: 运行模块级配置支持测试确认先红**
  - Run: `mvn -q -pl module-react-paradigm,module-plan-replan-paradigm,module-reflection-paradigm,module-graph-flow-paradigm,module-multi-agent-conversation,module-multi-agent-supervisor,module-multi-agent-roleplay -Dtest=*PropertySupportTest test`
  - 验收：采用 `module-react-paradigm` 作为样板模块完成了红灯验证；批量铺开阶段不再人为为其余模块重复制造红灯。

- [x] **Step 3: 收敛门面类实现**
  - 各模块只声明默认主配置名、本地配置名和 demo 开关键，复用共享支持类。
  - 验收：7 个模块的 `OpenAi*DemoPropertySupport` 已全部改为委托 `OpenAiDemoConfigSupport`。

- [x] **Step 4: 重新运行模块级配置支持测试确认转绿**
  - Run: `mvn -q -pl module-react-paradigm,module-plan-replan-paradigm,module-reflection-paradigm,module-graph-flow-paradigm,module-multi-agent-conversation,module-multi-agent-supervisor,module-multi-agent-roleplay -Dtest=*PropertySupportTest test`
  - 验收：已在 JDK 21 下分批及最终整组重跑 `*PropertySupportTest` / `*DemoTestConfigTest`，全部通过。

### Task 3: 统一测试资源目录结构

**Files:**
- Modify: `module-react-paradigm/src/test/resources/**`
- Modify: `module-plan-replan-paradigm/src/test/resources/**`
- Modify: `module-reflection-paradigm/src/test/resources/**`
- Modify: `module-graph-flow-paradigm/src/test/resources/**`
- Modify: `module-multi-agent-conversation/src/test/resources/**`
- Modify: `module-multi-agent-supervisor/src/test/resources/**`
- Modify: `module-multi-agent-roleplay/src/test/resources/**`

- [x] **Step 1: 先调整一个模块的 fixture 与模板测试，确认新结构表达正确**
  - 以 `module-react-paradigm` 为样板：主配置模板化，新增 `application-llm-local.yml.example`，demo-local 示例只保留开关，fixture 目录改为统一命名。
  - 验收：ReAct 模块已完成新结构迁移，并把 fixture 目录统一为 `openai-react-demo-fixture`。

- [x] **Step 2: 运行样板模块测试确认先红后绿**
  - Run: `mvn -q -pl module-react-paradigm -Dtest=OpenAiReactDemoPropertySupportTest,OpenAiReactDemoTestConfigTest test`
  - 验收：已在 JDK 21 下通过 `-am -Dsurefire.failIfNoSpecifiedTests=false` 联动 `framework-core` 重跑，`OpenAiDemoConfigSupportTest`、`OpenAiReactDemoPropertySupportTest`、`OpenAiReactDemoTestConfigTest` 全部通过。

- [x] **Step 3: 批量迁移剩余 6 个模块**
  - 主配置保留安全默认值与显式 import。
  - 新增或调整 `application-llm-local.yml.example`。
  - `application-*-demo-local.yml.example` 仅保留 demo 开关或职责说明。
  - fixture 目录统一为 `<demo-id>-fixture`。
  - 验收：Plan / Reflection / Graph / Conversation / Supervisor / Roleplay 已全部迁移完成。

- [x] **Step 4: 运行全部 Demo 配置相关测试**
  - Run: `mvn -q -pl module-react-paradigm,module-plan-replan-paradigm,module-reflection-paradigm,module-graph-flow-paradigm,module-multi-agent-conversation,module-multi-agent-supervisor,module-multi-agent-roleplay -Dtest=*DemoPropertySupportTest,*DemoTestConfigTest test`
  - 验收：已在 JDK 21 下整组执行通过。

### Task 4: 同步 README 与忽略规则

**Files:**
- Modify: `.gitignore`
- Modify: `README.md`
- Modify: `module-react-paradigm/README.md`
- Modify: `module-plan-replan-paradigm/README.md`
- Modify: `module-reflection-paradigm/README.md`
- Modify: `module-graph-flow-paradigm/README.md`
- Modify: `module-multi-agent-conversation/README.md`
- Modify: `module-multi-agent-supervisor/README.md`
- Modify: `module-multi-agent-roleplay/README.md`

- [x] **Step 1: 更新根级与模块级说明**
  - 说明先复制 `application-llm-local.yml.example`，再按需复制 `application-*-demo-local.yml.example`。
  - 验收：根 README 与 React / Plan / Reflection / Graph / Supervisor 模块 README 已切换到新口径。

- [x] **Step 2: 补齐忽略规则**
  - 忽略各模块真实 `application-llm-local.yml` 与 `application-*-demo-local.yml`。
  - 验收：`.gitignore` 已补齐 7 个模块的共享 LLM 本地文件与 demo-local 本地文件忽略规则。

- [x] **Step 3: 运行全文扫描确认没有旧口径残留**
  - Run: `rg -n "application-openai-.*-local.yml.example|application-llm-local.yml|openai-.*-fixture|copy" README.md module-*/README.md`
  - 验收：扫描结果已对齐当前文档口径，仅保留新的共享 `application-llm-local.yml` 说明。

### Task 5: 汇总验证与断点信息

**Files:**
- Test: `framework-core/src/test/java/com/xbk/agent/framework/core/config/OpenAiDemoConfigSupportTest.java`
- Test: `module-*/src/test/java/**/OpenAi*DemoPropertySupportTest.java`
- Test: `module-*/src/test/java/**/OpenAi*DemoTestConfigTest.java`

- [x] **Step 1: 运行共享支持类与样板模块测试**
  - Run: `mvn -q -pl framework-core,module-react-paradigm -Dtest=OpenAiDemoConfigSupportTest,OpenAiReactDemoPropertySupportTest,OpenAiReactDemoTestConfigTest test`
  - 验收：已通过 `-am -Dsurefire.failIfNoSpecifiedTests=false` 方式在 JDK 21 下执行通过。

- [x] **Step 2: 运行 7 个模块配置相关测试**
  - Run: `mvn -q -pl module-react-paradigm,module-plan-replan-paradigm,module-reflection-paradigm,module-graph-flow-paradigm,module-multi-agent-conversation,module-multi-agent-supervisor,module-multi-agent-roleplay -Dtest=*DemoPropertySupportTest,*DemoTestConfigTest test`
  - 验收：7 个模块整组测试已执行通过。

- [x] **Step 3: 检查工作树 diff 并记录阻塞**
  - Run: `git diff -- framework-core/src/main/java/com/xbk/agent/framework/core/config framework-core/src/test/java/com/xbk/agent/framework/core/config .gitignore README.md module-*/README.md module-*/src/test/java module-*/src/test/resources`
  - 验收：diff 范围已确认聚焦在共享配置支持、测试资源、README、`.gitignore` 和计划文件本身。
