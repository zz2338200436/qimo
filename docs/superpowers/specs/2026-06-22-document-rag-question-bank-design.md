# 文档型 RAG 题库替代数据库出题设计

## 1. 背景

当前项目中，“生成题目”能力走的是数据库题库链路：

- `agent-service` 识别 `GENERATE_QUESTIONS`
- `GenerateQuestionsTool` 调用 `ai-service`
- `ai-service` 在 `LocalMockAiModelClient` 中查询 `questions` / `knowledge_points` 表

这条链路的问题是：

- 题库维护成本高，必须写入数据库
- 教师无法直接通过增删文档维护题库
- RAG 与题库能力割裂，当前 RAG 只用于知识问答
- 题目来源不可直接映射到具体文档资产

本次改造目标是把“题库来源”从数据库迁移到文档，并使用 RAG 检索文档中的结构化题目块完成出题。

## 2. 目标

- 支持教师通过新增或修改 `md` 文档维护题库
- “生成题目”仅从文档题库读取，不再依赖数据库题库
- 保持当前“严格题库模式 + 明确不足提示”
- 支持按主题、难度、题型、标签等维度筛选和检索
- 保持题目来源可追溯到具体文档
- 保留当前知识问答 RAG，不与题库 RAG 混用

## 3. 非目标

- 不做“纯生成式出题”，即不允许模型脱离文档自由编题
- 不在本次设计中保留数据库题库作为正式主链路
- 不要求首版支持 PDF、Word 直接入库
- 不要求首版做后台可视化上传管理页

## 4. 推荐方案

推荐采用 `文档即题库 + 结构化解析 + 向量检索 + 严格返回真实题目`。

核心原则：

- 文档是题库源，不是数据库
- RAG 负责召回，不负责凭空出题
- 题目必须先被结构化解析成标准对象，才能参与检索
- 当匹配题目不足时，返回部分结果并明确提示，不进行补编

这是三种备选路径中最稳的一条：

- 方案 A：严格文档题库模式，推荐
- 方案 B：文档题库优先，不足时 LLM 补题，不推荐首版
- 方案 C：知识文档检索后让模型现编题，不推荐

## 5. 用户体验

教师侧体验应变成：

1. 教师把题库 markdown 放入指定目录
2. 系统启动时加载文档题库，或通过刷新动作重新建索引
3. 教师发起“生成十道 Java基础中等题”
4. 系统返回命中的真实题目列表
5. 如果只命中 6 道，则返回 6 道并提示 `题库仅匹配到 6/10 道题`

“查询题库有什么题”也应返回文档题库汇总，而不是数据库统计。

## 6. 目录与资产设计

新增题库目录：

```text
docs/
  question-bank/
    java/
    distributed-framework/
    database/
    cloud/
```

建议允许两种组织方式，但系统内部统一解析为同一种题目对象：

- 按模块分目录
- 按主题分文件

首版建议约束为：每个文件只放同一主题题目，便于维护。

## 7. 题库文档格式

首版不建议把题目散写在普通课程讲义里。应使用专门的题库 markdown，格式固定。

推荐格式：一题一个二级块。

示例：

```md
---
title: Java基础中等题库 01
topic: Java基础
roleScope: teacher
tags:
  - java
  - basic
  - oop
---

## Question
difficulty: 中等
type: SINGLE_CHOICE

content: 下列关于方法重载的说法，正确的是哪一项？

options:
- A. 仅返回值不同也算重载
- B. 参数列表不同才构成重载
- C. 访问修饰符不同才构成重载
- D. 方法名不同也算重载

answer: B
analysis: 方法重载要求同名方法参数列表不同，返回值差异不能单独构成重载。

## Question
difficulty: 中等
type: TRUE_FALSE

content: char类型在Java中可以直接表示Unicode字符。

answer: true
analysis: Java 的 char 本质上是 UTF-16 代码单元。
```

首版约束：

- 必填字段：`topic`、`difficulty`、`type`、`content`、`answer`
- 可选字段：`analysis`、`options`、`tags`、`source`、`courseId`
- `type` 允许值：
  - `SINGLE_CHOICE`
  - `MULTIPLE_CHOICE`
  - `TRUE_FALSE`
  - `FILL_BLANK`
  - `SHORT_ANSWER`
  - `ESSAY`

## 8. 核心架构

保留现有知识问答 RAG，同时新增一条独立的“题库 RAG”链路。

### 8.1 保留的现有能力

- `RagMarkdownDocumentLoader`
- `RagMarkdownChunker`
- `InMemoryRagIndex`
- `RagKnowledgeService`

这些仍服务于知识问答，不直接承担结构化题库出题。

### 8.2 新增能力

新增以下组件：

- `QuestionBankProperties`
- `QuestionDocumentLoader`
- `QuestionMarkdownParser`
- `QuestionChunk`
- `QuestionRagIndex`
- `QuestionRagService`
- `QuestionBankSummaryService`

职责划分：

- `QuestionBankProperties`
  - 维护题库文档路径、是否启用、最大返回数、最小相似度、是否允许热刷新
- `QuestionDocumentLoader`
  - 扫描题库目录并读取 markdown 原文
- `QuestionMarkdownParser`
  - 把 markdown 解析成题目对象列表
- `QuestionChunk`
  - 单道题的标准结构
- `QuestionRagIndex`
  - 存放题目 embedding 与可过滤元数据
- `QuestionRagService`
  - 处理“按条件生成题目”的检索逻辑
- `QuestionBankSummaryService`
  - 汇总当前文档题库信息，供“查询题库”使用

## 9. 数据模型

建议新增如下内部模型：

```java
record QuestionChunk(
    String questionId,
    String sourcePath,
    String documentTitle,
    String topic,
    String difficulty,
    String type,
    List<String> tags,
    String content,
    List<String> options,
    String answer,
    String analysis,
    String roleScope,
    Integer order
) {}
```

检索索引项：

```java
record IndexedQuestion(
    QuestionChunk question,
    List<Double> embedding
) {}
```

其中 embedding 文本建议由这些字段拼成：

- `topic`
- `difficulty`
- `type`
- `tags`
- `content`
- `analysis`

不要只对 `content` 做 embedding，否则按主题和标签召回会变弱。

## 10. 检索流程

“生成题目”链路改造后应为：

1. Agent 识别 `GENERATE_QUESTIONS`
2. `GenerateQuestionsTool` 不再调用 `ai-service`
3. 直接调用本地 `QuestionRagService`
4. `QuestionRagService` 执行：
   - 请求标准化
   - 元数据过滤
   - embedding 检索
   - 去重
   - 截取目标数量
   - 结构化返回
5. 返回结果维持当前前端兼容字段：
   - `topic`
   - `count`
   - `actualCount`
   - `partial`
   - `difficulty`
   - `questions`
   - `message`

建议先过滤再向量检索：

- 先按 `difficulty` 精确过滤
- 再按 `topic`、`tags`、`type` 做条件过滤
- 最后对候选集合做 embedding 排序

这样能避免“主题相近但难度错位”的误召回。

## 11. 题库不足策略

沿用当前严格题库模式：

- 命中 0 道：返回空列表与明确提示
- 命中不足：返回部分列表，`partial=true`
- 不允许自动补编

消息文案保持一致：

- 无匹配：`题库暂无匹配题目，请先维护题库或调整主题/难度`
- 部分匹配：`题库仅匹配到 X/Y 道题，请补充题库或放宽主题/难度条件`

## 12. 配置设计

新增配置建议：

```yml
agent:
  question-bank:
    enabled: true
    document-paths:
      - docs/question-bank
    embedding-base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
    embedding-model: ${OLLAMA_EMBEDDING_MODEL:qwen3-embedding:0.6b}
    max-candidates: 50
    min-score: 0.15
    allow-reload: true
```

说明：

- 题库 RAG 与知识问答 RAG 可以复用同一 embedding 服务
- 但配置项应独立，避免将知识问答文档和题库文档混在同一索引

## 13. 代码改造点

### 13.1 agent-service

需要改：

- `GenerateQuestionsTool`
  - 从调用 `AiEdgeClient.generateQuestions(...)`
  - 改为调用本地 `QuestionRagService.generateQuestions(...)`

- `QuestionBankSummaryTool`
  - 从调用 `ai-service` 汇总
  - 改为调用本地 `QuestionBankSummaryService`

- `AgentOrchestrator`
  - 逻辑基本不变
  - 仍把 `GENERATE_QUESTIONS` 当作 DATA 响应

- 新增配置类与服务类

### 13.2 ai-service

首版建议：

- 保留原接口，但不再作为主链路使用
- 不再继续扩数据库题库
- 后续可以将数据库题库逻辑标记为 legacy

这样能降低切换风险。

## 14. 与现有 RAG 的边界

必须明确拆分两种 RAG：

- `知识问答 RAG`
  - 面向“什么是”“怎么理解”“怎么使用”
  - 输出自然语言答案
  - 使用现有 `RagKnowledgeService`

- `题库检索 RAG`
  - 面向“生成十道……题”
  - 输出结构化题目对象
  - 使用新建 `QuestionRagService`

不能直接让 `RagKnowledgeService` 兼做题库服务，否则输出语义和结构会混乱。

## 15. 热加载与刷新

首版建议提供两种刷新方式：

- 启动时全量加载
- 管理员显式触发刷新

不建议首版直接做文件系统自动监听。原因：

- 容器环境下文件监听不稳定
- 调试复杂度高
- 显式刷新更容易验证

后续可扩展一个管理接口：

- `POST /api/agent/question-bank/reload`

用于重建题库索引。

## 16. 错误处理

需要覆盖以下场景：

- 文档路径不存在
- markdown 格式不合法
- 缺少必填字段
- options 与 type 不匹配
- embedding 服务不可用
- 索引为空

策略：

- 单文件解析失败时跳过该文件并记录日志
- 单题解析失败时跳过该题并记录来源
- 所有题都失败时，返回“题库暂不可用”
- embedding 不可用时不应返回随机题

## 17. 测试设计

至少新增以下测试：

### 单元测试

- `QuestionMarkdownParserTest`
  - 正常解析单题、多题、带 options、不带 options
  - 缺字段时拒绝

- `QuestionRagServiceTest`
  - 命中足够题目时返回满额
  - 命中不足时返回 partial
  - 难度不匹配时不混题
  - 多主题下优先召回相关主题

- `QuestionBankSummaryServiceTest`
  - 正确汇总题量、主题、难度分布

### 集成测试

- `GenerateQuestionsTool` 走本地文档题库而非 `ai-service`
- `QUERY_QUESTION_BANK` 返回文档汇总
- 题库 reload 后新增题可见

## 18. 迁移策略

建议分三步：

### 阶段一：并存

- 新增文档题库目录与解析链路
- 保留数据库题库实现
- 增加开关切换新旧实现

### 阶段二：切主

- `GenerateQuestionsTool` 默认切到文档题库
- `QUERY_QUESTION_BANK` 切到文档汇总

### 阶段三：去遗留

- 停止维护数据库题库 seed
- 删除 `ai-service` 中 question-bank 主逻辑
- 保留兼容期后彻底移除

如果你希望快速落地，也可以直接跳过阶段一，前提是首批题库 markdown 已准备好。

## 19. 风险与权衡

主要风险：

- 文档格式不统一会导致解析脆弱
- 首批题库 markdown 编写成本高
- 纯 embedding 召回可能受题目文本简短影响

对应策略：

- 先锁死模板格式
- 加 parser 校验与错误报告
- 采用“元数据过滤 + embedding 排序”的组合，而不是纯向量召回

## 20. 推荐落地顺序

1. 确定题库 markdown 模板
2. 新建 `docs/question-bank/` 并迁移一批代表性题目
3. 实现 `QuestionMarkdownParser`
4. 实现 `QuestionRagService`
5. 改造 `GenerateQuestionsTool`
6. 改造 `QuestionBankSummaryTool`
7. 增加 reload 接口
8. 运行教师端联调与回归

## 21. 最终设计结论

本次改造采用：

- 文档作为题库源
- RAG 仅负责题目检索与排序
- 结果必须来自真实文档题目
- 出题链路不再依赖数据库

这条路线最符合你的诉求：

- 可以持续添加文档
- 可以从文档里读取题库
- 可以保持严格题库模式
- 可以保留 RAG 的可扩展性
- 可以让题目来源具备文档级可追踪性
