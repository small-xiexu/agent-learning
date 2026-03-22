# module-multi-agent-camel :: CAMEL

## 模块定位

`module-multi-agent-camel` 现在承载的是 **CAMEL 多智能体协作范式**，而不是泛化的“角色扮演”示例集合。  
模块核心关注点是：

- 用强约束 Inception Prompt 固定角色边界
- 用统一 `AgentLlmGateway` 驱动两个互补角色
- 用手写路由和 `FlowAgent` 两种方式对照展示 CAMEL 协作闭环

## 当前实现

这个模块提供两套正式实现：

- `HandwrittenCamelAgent`
  用显式 `while` 循环维护交易员与程序员之间的消息接力、共享 transcript 和 `<CAMEL_TASK_DONE>` 终止协议。
- `AlibabaCamelFlowAgent`
  用 `FlowAgent + StateGraph + OverAllState` 维护交易员与程序员之间的受控 handoff 回环。

两套实现都统一站在 `AgentLlmGateway` 上，差异只体现在流程控制方式：

- 手写版把路由逻辑写死在 Java 代码里
- 框架版把路由逻辑抽到状态图和条件边里

## 关键概念

- `CamelRoleContract`
  定义角色名称、系统提示、输出键和稳定职责边界。
- `CamelConversationMemory`
  保存共享 transcript，并按当前角色视角重映射成统一消息列表。
- `CamelTraderAgent / CamelProgrammerAgent`
  分别扮演业务提出者和实现者。
- `CamelTraderHandoffNode / CamelProgrammerHandoffNode`
  在 FlowAgent 版中承担最小 baton 的读取、生成和状态写回。

当前示例任务已经统一成 Java 语境：

- 交易员负责提出“获取实时股价并计算移动平均线”的业务目标
- 程序员负责交付一段完整的 Java 程序

## 与其他范式的区别

- 相比 `ReAct`：CAMEL 不是单智能体 + 工具，而是多角色对话协作。
- 相比 `Plan-and-Solve`：CAMEL 不是固定前后工序，而是控制权在角色之间动态流转。
- 相比 `Reflection`：CAMEL 不是同一目标对象的自我反思，而是不同职责角色围绕任务持续协商推进。

## 工程建议

- 角色职责必须互补，不能让两个角色都试图做总体决策。
- Prompt 必须同时约束“你是谁”“你不能做什么”“下一棒交给谁”。
- 手写版适合理解 CAMEL 原始思想，框架版更适合企业落地和后续扩展。
