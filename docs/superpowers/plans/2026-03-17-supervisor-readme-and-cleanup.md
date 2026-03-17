# Supervisor README And Cleanup Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 提交现有 7 份模块 README，清理已合并的旧 worktree，并补齐 `module-multi-agent-supervisor` 的模块级 README。

**Architecture:** 先在当前文档分支提交已完成的 7 份 README 与本轮计划文档，再安全移除已合并且无未提交改动的旧 worktree，最后基于 Spring AI Alibaba 官方 `SupervisorAgent` / Handoffs / 多步循环路由语义新增 `module-multi-agent-supervisor/README.md`，并单独验证构建通过。

**Tech Stack:** Git worktree、Markdown、Maven 多模块、Spring AI Alibaba 1.1.2.0 官方文档

---

## Chunk 1: 提交 7 份 README

### Task 1: 提交当前模块 README 批次

**Files:**
- Create: `docs/superpowers/plans/2026-03-17-module-readmes-design-guides.md`
- Create: `module-react-paradigm/README.md`
- Create: `module-plan-replan-paradigm/README.md`
- Create: `module-reflection-paradigm/README.md`
- Create: `module-graph-flow-paradigm/README.md`
- Create: `module-multi-agent-conversation/README.md`
- Create: `module-multi-agent-engineering/README.md`
- Create: `module-multi-agent-roleplay/README.md`
- Create: `docs/superpowers/plans/2026-03-17-supervisor-readme-and-cleanup.md`

- [ ] **Step 1: 检查当前工作区变更**

Run: `git status --short`
Expected: 仅包含上述 README 与 plan 文档

- [ ] **Step 2: 暂存并创建提交**

Run: `git add docs/superpowers/plans/2026-03-17-module-readmes-design-guides.md docs/superpowers/plans/2026-03-17-supervisor-readme-and-cleanup.md module-react-paradigm/README.md module-plan-replan-paradigm/README.md module-reflection-paradigm/README.md module-graph-flow-paradigm/README.md module-multi-agent-conversation/README.md module-multi-agent-engineering/README.md module-multi-agent-roleplay/README.md`
Expected: `git status --short` 中这些文件转为已暂存

- [ ] **Step 3: 提交文档批次**

Run: `git commit -m "docs(modules): 补充七大智能体范式设计指南"`
Expected: 生成 1 个新提交

## Chunk 2: 清理旧 worktree

### Task 2: 清理已合并的旧 worktree

**Files:**
- Verify: `.worktrees/framework-core-acl-expansion`

- [ ] **Step 1: 确认旧分支已合并且工作区干净**

Run: `git branch --merged main`
Expected: 包含 `feat-framework-core-acl-expansion-0316-xiexu`

Run: `git -C /Users/sxie/xbk/agent-learning/.worktrees/framework-core-acl-expansion status --short`
Expected: 无输出

- [ ] **Step 2: 移除 worktree**

Run: `git worktree remove .worktrees/framework-core-acl-expansion`
Expected: worktree 路径从 `git worktree list` 中消失

- [ ] **Step 3: 删除本地已合并分支**

Run: `git branch -d feat-framework-core-acl-expansion-0316-xiexu`
Expected: 本地分支删除成功

## Chunk 3: 新增 supervisor README

### Task 3: 编写 `module-multi-agent-supervisor/README.md`

**Files:**
- Create: `module-multi-agent-supervisor/README.md`

- [ ] **Step 1: 写入监督者范式的模块定位与理论背景**
- [ ] **Step 2: 写入集中路由、多步骤循环与 worker 返回监督者的运行机制**
- [ ] **Step 3: 写入 `SupervisorAgent`、多步循环路由与 Handoffs/Tool Calling 边界的 Spring AI Alibaba 映射**
- [ ] **Step 4: 写入与 `framework-core` 的关系、适用场景与工程边界**

### Task 4: 验证并提交 supervisor README

**Files:**
- Verify: `module-multi-agent-supervisor/README.md`

- [ ] **Step 1: 运行最小构建验证**

Run: `mvn -q -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository -DskipTests verify`
Expected: 退出码 0

- [ ] **Step 2: 暂存并创建第二个提交**

Run: `git add module-multi-agent-supervisor/README.md`
Run: `git commit -m "docs(supervisor): 补充监督者范式设计指南"`
Expected: 生成第二个新提交
