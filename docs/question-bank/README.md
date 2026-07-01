# 题库文档格式

题库目录用于给 `agent-service` 的文档型题库 RAG 提供真实题目来源。

## 目录规范

- 每个主题建议单独成文件
- 仅支持 `.md`
- 推荐放在 `docs/question-bank/<module>/`

## 必填字段

- `topic`
- `difficulty`
- `type`
- `content`
- `answer`

## 示例

```md
---
title: Java基础中等题库
topic: Java基础
roleScope: all
tags:
  - java
---

## Question
difficulty: 中等
type: SINGLE_CHOICE
content: Java中用于表示继承的关键字是什么？
options:
- A. import
- B. extends
- C. implements
answer: B
analysis: extends 用于类继承。
```
