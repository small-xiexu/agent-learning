# GraphFlow范式从0到1掌握指南

## 1. 这篇文档到底要解决什么问题

很多 Java 工程师第一次看到 `module-graph-flow-paradigm` 时，通常会同时卡在两层：

- 第一层是范式层：为什么一个问答流程也要建“图”，普通 `if/else + while` 不行吗
- 第二层是框架层：Spring AI Alibaba 里的 `FlowAgent`、`StateGraph`、`OverAllState`、`CompileConfig` 到底怎么协同

最常见的困惑通常有这几类：

1. 这不就是“先理解问题，再搜索，再总结”吗，为什么还要叫 Graph Flow
2. 手写版的 `GraphRunner` 和框架版的 `StateGraph` 到底在对照什么
3. 条件分支到底写在节点里，还是写在边上
4. `GraphState` 和 `OverAllState` 有什么本质区别
5. `CompileConfig.recursionLimit` 在这种图里到底起什么作用

这篇文档不是单纯介绍一个模块目录，也不是脱离仓库讲图编排百科。

它真正要做的事情是：

**借 `module-graph-flow-paradigm` 这个最小落地案例，把 Graph Flow 范式的运行时本体和 Spring AI Alibaba 的原生图执行方式，从 0 到 1 讲明白。**

---

## 2. 先说人话：Graph Flow 到底是什么

Graph Flow 的核心，不是“把代码画成图”，而是：

**把原本散落在 `if/else`、`while`、异常处理里的执行路径，提升成一张显式状态图。**

在这张图里：

- 节点代表“这一步要做什么”
- 边代表“下一步要去哪里”
- 条件边代表“根据当前状态决定走哪条路”
- 状态代表“流程当前已经知道了什么事实”

这个模块里的案例非常克制，就是一个三步问答助手：

1. `UnderstandNode` 先把用户问题提炼成搜索关键词
2. `SearchNode` 去搜索
3. 搜索成功则 `AnswerNode` 做总结，搜索失败则 `FallbackNode` 给降级回答

你可以把它理解成：

**同一个业务题，手写版用显式状态机还原图运行时，框架版用 `StateGraph` 把同一套路由规则声明出来。**

---

## 3. 为什么复杂流程最终要上升到“图”

一开始，很多流程都能用普通代码写出来。

但当下面这些特征出现时，嵌套 `if/else` 会迅速失控：

- 有多个分支出口
- 某一步失败后要走降级路径
- 某些节点需要循环重试
- 希望把流程路径回放出来
- 希望把“节点职责”和“路由决策”拆开治理

这就是图编排存在的价值：

- 节点只负责改状态，不负责决定全局路由
- 路由逻辑集中声明，方便审计
- 状态是显式一等公民，便于回放
- 失败分支、终止分支、循环分支都能直接看见

所以 Graph Flow 不是“更酷的写法”，而是：

**当流程复杂到需要治理时，把隐式控制流升级成显式结构。**

---

## 4. 先建立一张技术栈地图

`module-graph-flow-paradigm` 可以拆成 4 层：

| 层次 | 代表对象 | 在本模块里的职责 |
| --- | --- | --- |
| 范式层 | `module-graph-flow-paradigm` | 表达图编排、条件分支、降级路径和状态驱动流程 |
| 统一协议层 | `framework-core`、`AgentLlmGateway` | 对模型调用提供统一抽象，避免模块直连厂商 SDK |
| Spring AI 适配层 | `ChatModel`、自动装配模块 | 把统一 `llm.*` 配置接到真实 OpenAI Compatible 模型 |
| Spring AI Alibaba 运行时层 | `FlowAgent`、`StateGraph`、`OverAllState`、`CompileConfig` | 把图结构真正跑起来 |

这张图最关键的启发是：

- `AgentLlmGateway` 解决的是“统一模型入口”
- Spring AI 解决的是“怎么接真实模型”
- Spring AI Alibaba 解决的是“怎么把流程做成原生图运行时”

它们不是同一层能力。

---

## 5. 手写版到底在对照什么

手写版的主入口是：

- `HandwrittenGraphFlow`
- `GraphRunner`
- `GraphState`
- `UnderstandNode / SearchNode / AnswerNode / FallbackNode`

它回答的是：

**如果完全不用图框架，一个最小 Graph Flow runtime 应该怎么写。**

### 5.1 `GraphState`：手写版的全局状态

`GraphState` 是整个流程共享的事实容器，里面放了：

- `userQuery`
- `searchQuery`
- `searchResults`
- `finalAnswer`
- `errorMessage`
- `stepStatus`

这里最重要的不是字段多少，而是：

**所有节点都通过它交接事实，`GraphRunner` 再根据 `stepStatus` 决定下一跳。**

### 5.2 `StepStatus`：手写版的“路由指针”

这个枚举不是业务结果，而是流程当前跑到了哪里。

大致可以理解成：

- `INIT`
- `UNDERSTOOD`
- `SEARCH_SUCCESS`
- `SEARCH_FAILED`
- `END`

只要这个指针没到 `END`，`GraphRunner` 就继续跑。

### 5.3 `GraphRunner`：手写版的最小图执行引擎

`GraphRunner` 的核心就是一段：

- `while (stepStatus != END)`
- `switch(stepStatus)`

这段代码是整个模块最关键的教学点。

因为它把两件事拆开了：

- 节点负责更新状态
- 引擎负责集中路由

这恰好就是 Graph Flow / LangGraph / StateGraph 的底层思想。

### 5.4 四个节点分别干什么

- `UnderstandNode`
  只负责把用户问题提炼成搜索关键词
- `SearchNode`
  只负责调搜索工具并写回结果或错误
- `AnswerNode`
  只负责基于搜索结果生成最终回答
- `FallbackNode`
  只负责在搜索失败时生成降级回答

你会发现，这里节点职责都很窄。

这不是为了“拆更多类”，而是为了让：

- 节点逻辑更容易调试
- 状态变化更容易回放
- 路由决策更容易从节点里抽离出来

---

## 6. 框架版到底在对照什么

框架版的主入口是：

- `AlibabaGraphFlowAgent`
- `SearchResultEdgeRouter`
- `UnderstandNodeAction / SearchNodeAction / AnswerNodeAction / FallbackNodeAction`

它回答的是：

**如果把同一件事交给 Spring AI Alibaba 的原生图运行时治理，应该怎么做。**

### 6.1 `FlowAgent`：图运行时的总外壳

`AlibabaGraphFlowAgent` 继承 `FlowAgent`。

它不是单个业务节点，而是整张图的调度外壳。

你可以把它理解成：

**一个“会跑 StateGraph”的 Agent 容器。**

### 6.2 `StateGraph`：不是展示结构，而是执行结构

在这个模块里，`StateGraph` 不是画图工具，而是运行时真正依赖的路由定义。

框架版通过它显式声明：

- 从 `START` 进入 `understand`
- 再进入 `search`
- 搜索后根据路由结果决定去 `answer` 还是 `fallback`
- 最后都流向 `END`

也就是说：

**手写版的 `while + switch`，在框架版里被 `addNode + addEdge + addConditionalEdges` 替代。**

### 6.3 `OverAllState`：框架版的全局状态

`OverAllState` 和手写版 `GraphState` 的角色是一样的，都是流程共享状态。

区别在于：

- 手写版是强类型 POJO
- 框架版是弱类型键值状态容器

所以框架版代码里你会看到大量这样的键：

- `user_query`
- `search_query`
- `search_results`
- `search_failed`
- `error_message`
- `final_answer`

它本质上还是那本“全流程共享草稿本”，只是换成了更适合框架合并与路由的表示方式。

### 6.4 `CompileConfig`：图运行时的保险丝

这个模块里，`CompileConfig` 目前最重要的配置就是 `recursionLimit`。

它控制的是：

**图最多允许执行多少步。**

即使当前案例是线性的四步图，也依然建议显式设置：

- 防止路由表误配导致无限循环
- 给后续扩展循环边、重试边时预留治理位

在这里它不是业务参数，而是运行时护栏。

---

## 7. 这个模块最关键的教学点：失败分支不是写在 prompt 里，而是写在图里

这个案例最值得学的地方，不是“三步问答”本身，而是：

**搜索失败后的降级路径，是通过显式条件边表达的。**

在手写版里：

- `SearchNode` 写入 `SEARCH_SUCCESS` 或 `SEARCH_FAILED`
- `GraphRunner` 根据 `stepStatus` 决定下一跳

在框架版里：

- `SearchNodeAction` 把 `search_failed` 写入状态
- `SearchResultEdgeRouter` 读取状态并返回 `answer` 或 `fallback`
- `StateGraph.addConditionalEdges(...)` 把标签映射到真实节点

这件事非常重要，因为它体现了 Graph Flow 的工程边界：

- 节点负责产出事实
- 路由器负责解释事实
- 图负责声明跳转关系

而不是把“失败了怎么办”悄悄埋进系统提示词里，让模型自己猜。

---

## 8. 与测试和真实 Demo 的对应关系

如果你想先看“这个模块到底保证了什么行为”，优先看：

- `src/test/java/com/xbk/agent/framework/graphflow/GraphFlowDemoTest.java`

这组测试钉住了四件事：

- 手写版搜索成功时必须走 `AnswerNode`
- 手写版搜索失败时必须走 `FallbackNode`
- 框架版搜索成功时必须走 `answer` 条件边
- 框架版搜索失败时必须走 `fallback` 条件边

除此之外，模块还补了两类配置支撑测试：

- `OpenAiGraphDemoPropertySupportTest`
  验证真实 Demo 的主配置模板职责、本地配置读取和启用开关判断
- `OpenAiGraphDemoTestConfigTest`
  验证最小自动装配下可以得到 `AgentLlmGateway` 和 `ChatModel`

真实 OpenAI Demo 也补成了两套：

- `src/test/java/com/xbk/agent/framework/graphflow/HandwrittenGraphFlowOpenAiDemo.java`
- `src/test/java/com/xbk/agent/framework/graphflow/AlibabaGraphFlowOpenAiDemo.java`

对应配置：

- `src/test/resources/application-openai-graph-demo.yml`
- `src/test/resources/application-openai-graph-demo-local.yml.example`
- `src/test/resources/application-openai-graph-demo-local.yml`

默认情况下，真实 Demo 会被安全跳过，避免在未配 Key 的环境下误打外网。

---

## 9. 推荐阅读顺序

如果你想顺着源码快速吃透，推荐顺序如下：

1. 先看 `GraphFlowDemoTest`
2. 再看手写版
   - `GraphState`
   - `StepStatus`
   - `GraphRunner`
   - `UnderstandNode / SearchNode / AnswerNode / FallbackNode`
3. 再看框架版
   - `AlibabaGraphFlowAgent`
   - `SearchResultEdgeRouter`
   - `UnderstandNodeAction / SearchNodeAction / AnswerNodeAction / FallbackNodeAction`
4. 最后看真实 Demo 与配置支撑
   - `OpenAiGraphDemoPropertySupport`
   - `OpenAiGraphDemoTestConfig`
   - `HandwrittenGraphFlowOpenAiDemo`
   - `AlibabaGraphFlowOpenAiDemo`

这样看，你会比较容易把：

- 图范式本体
- 统一模型网关
- Spring AI 模型接入
- Spring AI Alibaba 图运行时

这 4 层真正串起来。

---

## 10. 工程落地建议

### 10.1 先定状态，再写节点

图编排里最容易失控的不是边不够多，而是状态字段定义混乱。

如果状态没有先收敛，后面的节点和路由基本都会越写越乱。

### 10.2 节点职责越单一，图越容易治理

一个节点如果同时做：

- 模型理解
- 工具调用
- 分支判断
- 异常兜底

后面几乎一定很难调试。

### 10.3 条件边要基于显式状态，而不是提示词暗规则

真正可治理的 Graph Flow，不是靠模型“自己知道失败了该去哪”，而是靠图结构把路由规则写明白。

### 10.4 `CompileConfig` 不是可有可无

即使今天只是一个小图，也应该养成给 `recursionLimit` 设上限的习惯。

图一旦演进出回环，运行时护栏就是必须项。

---

## 11. 结论

`module-graph-flow-paradigm` 的价值，不是再写一个三步问答 Demo，而是把同一个问题同时落成了两种 runtime：

- 手写版：帮助你理解 Graph Flow 的最小本体
- 框架版：帮助你理解 Spring AI Alibaba 如何把图流程提升成原生运行时

把这两套代码对照着看，Graph Flow 这个词就不会再停留在“状态图看起来很高级”，而会真正落到：

**节点怎么拆、状态怎么流、路由怎么声明、失败分支怎么治理。**
