# Shared LLM Local Config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把工程模块的本地 `llm.*` 配置抽到独立共享文件，并让 OpenAI Demo 与 A2A 三进程两条链路都通过显式导入使用同一份本地 LLM 配置。

**Architecture:** 在 `src/test/resources` 下新增中性的 `application-llm-local.yml`，只承载本地 LLM 接入配置。`application-openai-engineering-demo.yml` 与 `application-a2a-nacos-local.yml` 都显式导入该文件；Demo 专用开关保留在 `application-openai-engineering-demo-local.yml`。同时补齐针对手动配置加载支持类的测试，确保递归导入也能被识别。

**Tech Stack:** Spring Boot profile/config import、JUnit 5、ApplicationContextRunner、YAML 配置加载

---

### Task 1: 补测试覆盖共享配置文件导入链

**Files:**
- Modify: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/config/OpenAiEngineeringDemoPropertySupportTest.java`
- Test: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/config/OpenAiEngineeringDemoPropertySupportTest.java`

- [x] **Step 1: 先写失败用例**
  - 增加一个 fixture 场景：主配置导入共享 `application-llm-local.yml`，本地 Demo 文件只保留 `enabled=true`。
  - 断言 `OpenAiEngineeringDemoPropertySupport` 仍能读到 `llm.api-key` 和 Demo 开关。
  - 验收：已补 `OpenAiEngineeringDemoPropertySupportTest` 和对应 fixture，共享 LLM 与 Demo 开关已拆开建模。

- [ ] **Step 2: 运行单测确认先红**
  - Run: `mvn -q -pl module-multi-agent-engineering -Dtest=OpenAiEngineeringDemoPropertySupportTest test`
  - 阻塞：本机仅有 JDK 17，`module-multi-agent-engineering` 要求 release 21，当前无法拿到真正的红灯结果。
  - 已完成：已确认 Maven 依赖链和 surefire 参数问题，当前唯一剩余阻塞是 JDK 版本。
  - 下一步：在 JDK 21 环境下重跑该命令，确认测试先红。

- [ ] **Step 3: 实现最小配置加载修复**
  - 让手动 YAML 加载支持 `spring.config.import` 的递归导入。
  - 进行中：`OpenAiEngineeringDemoPropertySupport` 已补递归 import、去重和相对路径解析，待 JDK 21 下编译验证。

- [ ] **Step 4: 重新运行单测确认转绿**
  - Run: `mvn -q -pl module-multi-agent-engineering -Dtest=OpenAiEngineeringDemoPropertySupportTest test`
  - 待验证：实现已落地，但受 JDK 21 缺失影响尚未执行。

### Task 2: 抽离共享 LLM 本地配置并接入 Demo/A2A

**Files:**
- Create: `module-multi-agent-engineering/src/test/resources/application-llm-local.yml.example`
- Modify: `module-multi-agent-engineering/src/test/resources/application-openai-engineering-demo.yml`
- Modify: `module-multi-agent-engineering/src/test/resources/application-a2a-nacos-local.yml`
- Modify: `module-multi-agent-engineering/src/test/resources/application-openai-engineering-demo-local.yml`
- Modify: `.gitignore`

- [x] **Step 1: 新增共享配置模板**
  - 创建 `application-llm-local.yml.example`，只放 `llm.provider/base-url/api-key/model/chat-completions-path`。
  - 验收：已新增 `application-llm-local.yml.example`。

- [x] **Step 2: 显式接入 Demo 与 A2A**
  - `application-openai-engineering-demo.yml` 导入共享 `application-llm-local.yml`。
  - `application-a2a-nacos-local.yml` 也导入共享 `application-llm-local.yml`。
  - 验收：两条导入链都已改成显式 import，静态检查已确认配置文件内容正确。

- [ ] **Step 3: 收窄 Demo 本地文件职责**
  - `application-openai-engineering-demo-local.yml` 只保留 Demo 开关说明，不再承载共享 `llm.*`。
  - 进行中：已新增 `.example` 文件表达新职责；实际本地文件保留给用户现有工作区，避免覆盖其私有配置。

- [x] **Step 4: 把真实本地共享文件加入忽略**
  - 在 `.gitignore` 中加入 `module-multi-agent-engineering/src/test/resources/application-llm-local.yml`。
  - 验收：`.gitignore` 已加入 `application-llm-local.yml` 和 `application-openai-engineering-demo-local.yml` 的忽略规则。

### Task 3: 更新说明文档

**Files:**
- Modify: `docs/多智能体工程范式从0到1掌握指南.md`

- [x] **Step 1: 改写 OpenAI Demo 的本地配置说明**
  - 说明先创建共享的 `application-llm-local.yml`，再按需创建 `application-openai-engineering-demo-local.yml`。
  - 验收：12.2、故障排查和运行前检查清单都已切到共享 `application-llm-local.yml` 口径。

- [x] **Step 2: 改写 A2A 配置说明**
  - 说明 A2A 三进程复用共享 `application-llm-local.yml`，`application-a2a-nacos-local.yml` 只负责 Nacos/A2A 公共配置。
  - 验收：13.3 与 A2A 启动命令已改到共享配置方案，全文扫描未再发现要求 A2A 额外激活 `openai-engineering-demo` 的残留命令。

### Task 4: 跑验证命令

**Files:**
- Test: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/config/OpenAiEngineeringDemoPropertySupportTest.java`
- Test: `module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/config/OpenAiEngineeringDemoTestConfigTest.java`

- [ ] **Step 1: 运行配置支持类测试**
  - Run: `mvn -q -pl module-multi-agent-engineering -Dtest=OpenAiEngineeringDemoPropertySupportTest test`
  - 阻塞：本机缺少 JDK 21。

- [ ] **Step 2: 运行最小自动配置测试**
  - Run: `mvn -q -pl module-multi-agent-engineering -Dtest=OpenAiEngineeringDemoTestConfigTest test`
  - 阻塞：本机缺少 JDK 21。

- [ ] **Step 3: 检查工作树 diff**
  - Run: `git diff -- .gitignore module-multi-agent-engineering/src/test/resources module-multi-agent-engineering/src/test/java/com/xbk/agent/framework/engineering/config docs/多智能体工程范式从0到1掌握指南.md`
  - 待验证：待文档残留清理完成后执行。
