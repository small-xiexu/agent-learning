# ReAct 学习路线：手写版与 Spring AI Alibaba 官方版

## 1. 学习目标

这份文档不是为了重复解释 ReAct 理论，而是为了回答三个更实际的问题：

1. 应该先看哪份代码，才能最快理解 ReAct 的运行机制
2. 手写版 ReAct 和 Spring AI Alibaba 官方 `ReactAgent` 分别该怎么看
3. 什么时候开始接真实模型，以及模型该怎么配置

建议把这份文档当成“源码阅读导航”，而不是“架构设计文档”。

## 2. 总体学习顺序

推荐严格按下面顺序学习：

1. 先看手写版主循环
2. 再看手写版离线 Demo
3. 再看 `framework-core` 的底座协议
4. 再看官方 Spring AI Alibaba Demo
5. 最后再跑真实 OpenAI 对照版

这个顺序的核心理由只有一个：

- 先搞清楚 ReAct 是怎么“手动跑起来的”
- 再理解官方框架到底帮你自动接管了哪些事情

## 3. 第一阶段：先学手写版 ReAct

### 3.1 第一眼必须看的文件

- `module-react-paradigm/src/main/java/com/xbk/agent/framework/react/application/executor/ReActAgent.java`

这是整个手写版 ReAct 的核心入口。  
学习时只盯住 `run(String userQuery)` 这一条主链。

你要重点理解的不是每一行语法，而是这 5 个动作：

1. 把当前 `history` 发给模型
2. 读取模型回复
3. 判断模型是要调工具还是已经给最终答案
4. 如果要调工具，就执行工具
5. 把工具结果写回上下文，继续下一轮

其中最关键的安全机制是：

- `while (step < maxSteps)`

它的意义不是语法细节，而是：

- ReAct 允许模型不断“继续思考并继续行动”
- 但工程上绝对不能允许无限循环

### 3.2 第二眼看的文件

- `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/ReActTravelDemo.java`

这个文件适合用来理解“数据是怎么流动的”。

里面有两个最值得看的假模型：

- `TravelDemoHelloAgentsLLM`
  - 模拟正常的三轮闭环
  - 先查天气，再推荐景点，最后输出答案
- `AlwaysToolCallingHelloAgentsLLM`
  - 故意永远不返回最终答案
  - 专门用来验证 `maxSteps` 安全阀

你在这里要建立的直觉是：

- 手写版 ReAct 不是“模型自己会跑”
- 而是我们把模型回复解释成“下一步动作”
- 然后由运行时继续推进循环

### 3.3 推荐先跑的命令

```bash
export JAVA_HOME=/Users/sxie/Library/Java/JavaVirtualMachines/azul-21.0.10/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

/Users/sxie/maven/apache-maven-3.6.3/bin/mvn -q \
  -pl module-react-paradigm -am \
  -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository \
  -Dtest=ReActTravelDemo \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

## 4. 第二阶段：补齐对 `framework-core` 的理解

当你把手写版 `ReActAgent` 的主循环看明白之后，再去看底座协议。

推荐顺序分成两层，不要一上来就把“接口”和“实现”混在一起看。

### 4.1 先看协议层

这一层回答的是：这个框架内部到底约定了哪些核心对象，它们分别代表什么。

1. `framework-core/src/main/java/com/xbk/agent/framework/core/llm/HelloAgentsLLM.java`
2. `framework-core/src/main/java/com/xbk/agent/framework/core/llm/model/LlmRequest.java`
3. `framework-core/src/main/java/com/xbk/agent/framework/core/llm/model/LlmResponse.java`
4. `framework-core/src/main/java/com/xbk/agent/framework/core/llm/model/ToolCall.java`
5. `framework-core/src/main/java/com/xbk/agent/framework/core/memory/Message.java`
6. `framework-core/src/main/java/com/xbk/agent/framework/core/tool/ToolRegistry.java`

注意：这一层很多文件本身是接口或协议对象，所以你会觉得“代码不多”。这是正常的，因为它们负责定义边界，不负责把流程真正跑起来。

### 4.2 再看实现层

这一层回答的是：前面那些协议在项目里到底是怎么落地的。

1. `framework-core/src/main/java/com/xbk/agent/framework/core/llm/DefaultHelloAgentsLLM.java`
2. `framework-core/src/main/java/com/xbk/agent/framework/core/tool/support/DefaultToolRegistry.java`
3. `module-react-paradigm/src/main/java/com/xbk/agent/framework/react/application/executor/ReActAgent.java`

这样看会顺很多：

- `HelloAgentsLLM`
  - 先看“统一模型入口长什么样”
- `DefaultHelloAgentsLLM`
  - 再看“这个入口在默认实现里怎么被调用”
- `ToolRegistry`
  - 先看“工具注册中心提供了什么能力”
- `DefaultToolRegistry`
  - 再看“工具最后是怎么被查找和执行的”
- `ReActAgent`
  - 最后回到手写 ReAct 主循环，把前面的协议和实现串起来

这一阶段的目标不是立刻记住所有字段，而是搞懂 5 个核心角色：

- `Message`
  - 对话里的每一条消息
- `LlmRequest`
  - 当前这一轮发给模型的输入
- `LlmResponse`
  - 模型这一轮返回的结果
- `ToolCall`
  - 模型请求调用工具的结构化指令
- `ToolRegistry`
  - 真正执行工具的地方

如果这一层看不明白，后面看官方版时就容易觉得一切都是“魔法”。

## 5. 第三阶段：学习 Spring AI Alibaba 官方版

### 5.1 先看的文件

- `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/SpringAIReActTravelDemo.java`

这份 Demo 的学习重点不是“工具怎么写”，而是：

- 为什么官方版不需要自己写 `while`
- 为什么官方版不需要自己维护 `history`
- 为什么官方版不需要自己解析 tool call

因为这些工作已经被：

- `ReactAgent`
- Graph Runtime
- `Model Node / Tool Node`

自动接管了。

### 5.2 对照阅读建议

建议你把下面两份文件来回切着看：

- `module-react-paradigm/src/main/java/com/xbk/agent/framework/react/application/executor/ReActAgent.java`
- `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/SpringAIReActTravelDemo.java`

阅读时只问自己两个问题：

1. 手写版这里是我自己做的，官方版是谁帮我做了？
2. 官方版虽然更短，但它到底把复杂度藏到哪一层了？

## 6. 第四阶段：看对照分析文档

当你已经看过两套代码后，再读下面这份文档最合适：

- `docs/react-agent-handwritten-vs-official.md`

这份文档适合你在以下场景使用：

- 读完代码后做整理
- 想回顾两套实现的职责边界
- 想确认官方 `ReactAgent` 到底替你省掉了哪些运行时逻辑

如果在还没看源码前先读它，通常会显得抽象；  
先看代码、再看这份文档，吸收效果最好。

## 7. 第五阶段：最后再接真实模型

离线版看明白后，再去跑真实模型对照版：

- 手写真实版：
  - `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/ReActTravelOpenAiDemo.java`
- 官方真实版：
  - `module-react-paradigm/src/test/java/com/xbk/agent/framework/react/SpringAIReActTravelOpenAiDemo.java`

这里的重点不是“能不能跑通”，而是：

- 同一个真实 `ChatModel`
- 同一组工具
- 同一个用户问题
- 手写版和官方版到底会表现出什么差异

### 7.1 当前项目的模型配置方式

真实模型基础配置在：

- `module-react-paradigm/src/test/resources/application-openai-react-demo.yml`

默认配置项包括：

- `spring.ai.openai.api-key`
- `spring.ai.openai.chat.options.model=gpt-4o`
- `demo.react.openai.enabled`

### 7.2 推荐的本地配置方式

推荐方式有两种。

#### 方式一：环境变量

```bash
export OPENAI_API_KEY='你的key'
```

#### 方式二：本地 yml

项目已经提供模板：

- `module-react-paradigm/src/test/resources/application-openai-react-demo-local.yml.example`

本地可使用的实际文件名是：

- `module-react-paradigm/src/test/resources/application-openai-react-demo-local.yml`

这个本地文件已经被 `.gitignore` 忽略，不会误提交到仓库。

### 7.3 真实 Demo 运行命令

```bash
export JAVA_HOME=/Users/sxie/Library/Java/JavaVirtualMachines/azul-21.0.10/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

/Users/sxie/maven/apache-maven-3.6.3/bin/mvn -q \
  -pl module-react-paradigm -am \
  -Dmaven.repo.local=/Users/sxie/xbk/agent-learning/.m2/repository \
  -Ddemo.react.openai.enabled=true \
  -Dtest=ReActTravelOpenAiDemo,SpringAIReActTravelOpenAiDemo \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

## 8. 最推荐的学习节奏

如果你是第一次系统学习 ReAct，我建议按下面节奏推进：

### 第 1 天

- 只看手写版 `ReActAgent`
- 跑 `ReActTravelDemo`
- 搞懂 `while (step < maxSteps)`

### 第 2 天

- 看 `framework-core` 的协议对象
- 搞懂 `Message / LlmRequest / LlmResponse / ToolCall / ToolRegistry`

### 第 3 天

- 看 `SpringAIReActTravelDemo`
- 对照 `docs/react-agent-handwritten-vs-official.md`

### 第 4 天

- 配真实 OpenAI Key
- 跑两套真实 Demo
- 观察真实模型在工具调用上的行为差异

## 9. 一句话总结

最推荐的学习策略不是“先学官方 API”，而是：

- 先学手写版，搞懂 ReAct 为什么能跑起来
- 再学官方版，搞懂框架替你自动做了什么
- 最后接真实模型，验证两者在同一个底层模型上的差异

这样学，ReAct 就不会只是一个会调用工具的黑盒，而会变成你真正能掌控的运行机制。
