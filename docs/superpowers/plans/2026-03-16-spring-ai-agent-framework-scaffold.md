# Spring AI Agent Framework Scaffold Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前单模块 Maven 工程改造成 `spring-ai-agent-framework` 多模块脚手架，并提供统一 BOM 管理与标准包目录。

**Architecture:** 根工程重写为聚合父 POM，通过 BOM 统一管理 Spring 生态版本；`framework-core` 作为抽象基座，其余模块按范式分治并只依赖 `framework-core`。当前阶段仅构建配置与目录骨架，不写业务实现。

**Tech Stack:** Java 21, Maven, Spring Boot 3.5.9, Spring Cloud 2025.0.0, Spring Cloud Alibaba 2025.0.0.0, Spring AI 1.1.2, Spring AI Alibaba 1.1.2.0.

---

## Chunk 1: Parent And Module POMs

### Task 1: Rewrite root parent POM

**Files:**
- Modify: `pom.xml`
- Test: `pom.xml`

- [ ] **Step 1: Replace the existing single-module root POM**

将根工程改为 `framework-parent`，声明 `packaging` 为 `pom`，添加 6 个子模块，并设置 Java 21 和统一编码属性。

- [ ] **Step 2: Add BOM-based dependency management**

在 `dependencyManagement` 中导入：
- `org.springframework.boot:spring-boot-dependencies:3.5.9`
- `org.springframework.cloud:spring-cloud-dependencies:2025.0.0`
- `com.alibaba.cloud:spring-cloud-alibaba-dependencies:2025.0.0.0`
- `org.springframework.ai:spring-ai-bom:1.1.2`
- `com.alibaba.cloud.ai:spring-ai-alibaba-bom:1.1.2.0`
- `com.alibaba.cloud.ai:spring-ai-alibaba-extensions-bom:1.1.2.1`

- [ ] **Step 3: Add minimal build governance**

补充 Maven Compiler Plugin 与 Enforcer 的最小配置，固定 Java 21。

### Task 2: Create child module POMs

**Files:**
- Create: `framework-core/pom.xml`
- Create: `module-react-paradigm/pom.xml`
- Create: `module-plan-replan-paradigm/pom.xml`
- Create: `module-reflection-paradigm/pom.xml`
- Create: `module-graph-flow-paradigm/pom.xml`
- Create: `module-multi-agent-supervisor/pom.xml`

- [ ] **Step 1: Add `framework-core/pom.xml`**

继承根父 POM，并引入 `spring-context`、`spring-boot-autoconfigure`、`spring-ai-client-chat` 和测试依赖。

- [ ] **Step 2: Add the paradigm module POMs**

所有范式模块均继承根父 POM，依赖 `framework-core`。根据职责增加：
- ReAct / Plan-Replan / Reflection / Supervisor：`spring-ai-alibaba-agent-framework`
- Graph Flow：`spring-ai-alibaba-agent-framework` + `spring-ai-alibaba-graph-core`

## Chunk 2: Directory Skeleton

### Task 3: Remove obsolete single-module entrypoint

**Files:**
- Delete: `src/main/java/com/xbk/Main.java`

- [ ] **Step 1: Delete the old sample main class**

根工程改为父 POM 后，不再保留旧的单模块 `Main.java`。

### Task 4: Create package directories

**Files:**
- Create: `framework-core/src/main/java/com/xbk/agent/framework/core/...`
- Create: `module-react-paradigm/src/main/java/com/xbk/agent/framework/react/...`
- Create: `module-plan-replan-paradigm/src/main/java/com/xbk/agent/framework/planreplan/...`
- Create: `module-reflection-paradigm/src/main/java/com/xbk/agent/framework/reflection/...`
- Create: `module-graph-flow-paradigm/src/main/java/com/xbk/agent/framework/graphflow/...`
- Create: `module-multi-agent-supervisor/src/main/java/com/xbk/agent/framework/supervisor/...`

- [ ] **Step 1: Create the standard package skeleton**

为每个模块创建 `api / application / domain / infrastructure / config / support` 及其关键子目录。

- [ ] **Step 2: Add test source roots**

为每个模块创建 `src/test/java`，保留后续 TDD 入口。

## Chunk 3: Verification

### Task 5: Run Maven verification

**Files:**
- Test: `pom.xml`
- Test: `framework-core/pom.xml`
- Test: `module-react-paradigm/pom.xml`
- Test: `module-plan-replan-paradigm/pom.xml`
- Test: `module-reflection-paradigm/pom.xml`
- Test: `module-graph-flow-paradigm/pom.xml`
- Test: `module-multi-agent-supervisor/pom.xml`

- [ ] **Step 1: Run Maven verification**

Run: `mvn -q -DskipTests verify`

Expected:
- 所有模块被正确识别
- BOM 导入成功
- 基础依赖可以解析
- 构建退出码为 0

- [ ] **Step 2: Fix dependency or plugin issues if verification fails**

如果出现错误，优先修正：
- 错误的 BOM artifactId
- 错误的模块依赖关系
- 不存在或不兼容的基础依赖 artifactId

- [ ] **Step 3: Record verification status**

在交付说明中明确给出验证命令和实际结果，不做未验证成功声明。

## Notes

- 本计划不包含自动提交和自动推送步骤，提交动作应由显式指令触发。
- 本计划针对配置型脚手架，不引入测试先行的业务代码实现。
