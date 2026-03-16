# Spring AI Agent Framework Scaffold Design

**Date:** 2026-03-16

## Goal

在当前仓库根目录直接改造成 `spring-ai-agent-framework` 的 Maven 多模块父工程，建立统一版本治理、模块边界和包结构骨架，为后续按范式逐步实现 Agent 能力提供稳定基座。

## Baseline

- Java 21
- GroupId: `com.xbk`
- Base package: `com.xbk.agent.framework`
- Spring Boot: `3.5.9`
- Spring Cloud: `2025.0.0`
- Spring Cloud Alibaba: `2025.0.0.0`
- Spring AI: `1.1.2`
- Spring AI Alibaba: `1.1.2.0`
- Spring AI Alibaba Extensions: `1.1.2.1`

## Module Topology

```text
framework-parent
├── framework-core
├── module-react-paradigm
├── module-plan-replan-paradigm
├── module-reflection-paradigm
├── module-graph-flow-paradigm
└── module-multi-agent-supervisor
```

依赖方向统一为：

- `framework-parent` 只负责聚合和版本治理
- `framework-core` 只负责底层抽象
- 所有范式模块仅依赖 `framework-core`
- 范式模块之间不直接依赖，避免范式耦合

## Design Principles

- 父 POM 使用 `dependencyManagement` 集中导入官方 BOM
- 子模块不得声明核心依赖版本号
- 不在框架骨架阶段绑定具体模型供应商 Starter
- 目录结构按 `api / application / domain / infrastructure / config / support` 分层
- 当前阶段仅生成脚手架，不生成业务实现

## Package Layout

### framework-core

根包：`com.xbk.agent.framework.core`

- `api`
- `application`
- `domain.agent`
- `domain.memory`
- `domain.tool`
- `spi.chat`
- `spi.memory`
- `spi.tool`
- `infrastructure.springai`
- `infrastructure.registry`
- `config`
- `support`

### module-react-paradigm

根包：`com.xbk.agent.framework.react`

- `api`
- `application.executor`
- `application.factory`
- `domain.policy`
- `domain.prompt`
- `domain.runtime`
- `infrastructure.agentframework`
- `infrastructure.hook`
- `infrastructure.tool`
- `config`
- `support`

### module-plan-replan-paradigm

根包：`com.xbk.agent.framework.planreplan`

- `api`
- `application.coordinator`
- `application.executor`
- `domain.plan`
- `domain.execution`
- `domain.replan`
- `domain.policy`
- `infrastructure.agentframework`
- `infrastructure.interceptor`
- `infrastructure.hook`
- `infrastructure.parser`
- `config`
- `support`

### module-reflection-paradigm

根包：`com.xbk.agent.framework.reflection`

- `api`
- `application.loop`
- `application.evaluator`
- `domain.critic`
- `domain.revision`
- `domain.iteration`
- `domain.policy`
- `infrastructure.agentframework`
- `infrastructure.scoring`
- `infrastructure.memory`
- `config`
- `support`

### module-graph-flow-paradigm

根包：`com.xbk.agent.framework.graphflow`

- `api`
- `application.compiler`
- `application.router`
- `domain.state`
- `domain.node`
- `domain.edge`
- `domain.routing`
- `infrastructure.graph`
- `infrastructure.persistence`
- `infrastructure.checkpoint`
- `config`
- `support`

### module-multi-agent-supervisor

根包：`com.xbk.agent.framework.supervisor`

- `api`
- `application.orchestrator`
- `application.handoff`
- `domain.role`
- `domain.team`
- `domain.supervision`
- `domain.handoff`
- `domain.policy`
- `infrastructure.supervisor`
- `infrastructure.subagent`
- `infrastructure.routing`
- `config`
- `support`

## Verification Strategy

- 通过 `mvn -q -DskipTests verify` 验证父子模块结构、BOM 和基础依赖解析
- 本次为配置和目录脚手架，按配置型改动处理，不引入业务级单元测试

## Constraints

- 本设计不包含自动提交和分支治理流程，提交策略由后续显式指令驱动
