---
title: SQL基础题库
topic: SQL基础
roleScope: all
tags:
  - SQL基础
---

## Question
difficulty: 中等
type: SINGLE_CHOICE
content: SQL语句中用于对分组结果进行条件过滤的关键字是哪个？
options:
- WHERE
- ORDER BY
- GROUP BY
- HAVING
answer: D
analysis: WHERE过滤分组前行数据，HAVING过滤分组后的聚合结果。

## Question
difficulty: 中等
type: TRUE_FALSE
content: INNER JOIN只返回两个表中满足连接条件的匹配记录。
options:
- 正确
- 错误
answer: 正确
analysis: 内连接只返回两侧都能匹配的记录。

## Question
difficulty: 中等
type: SHORT_ANSWER
content: 请写出SQL中COUNT(*)的常见用途。
answer: 统计查询结果中的行数
analysis: COUNT(*)常用于统计满足条件的记录数量。

## Question
difficulty: 中等
type: FILL_BLANK
content: 如果要筛选聚合后的分组结果，应优先使用哪个关键字？
answer: HAVING
analysis: HAVING用于过滤聚合后的分组结果。

## Question
difficulty: 中等
type: TRUE_FALSE
content: LEFT JOIN的特点是保留左表全部记录，即使右表没有匹配项。
options:
- 正确
- 错误
answer: 正确
analysis: 左连接会完整保留左侧记录，右侧匹配不到时返回空值。

## Question
difficulty: 中等
type: SINGLE_CHOICE
content: 下列哪项最适合描述GROUP BY的作用？
options:
- 按主键删除重复行
- 按照指定列对记录分组以便聚合统计
- 把多张表合并成一张表结构
- 自动创建索引
answer: B
analysis: GROUP BY通常配合聚合函数进行统计分析。

## Question
difficulty: 困难
type: SHORT_ANSWER
content: 请简述COUNT(*)与COUNT(列名)的一个常见区别。
answer: COUNT(*)统计所有行，COUNT(列名)通常忽略该列为NULL的行
analysis: COUNT(*)关注行数，COUNT(列名)通常只统计该列非空值。
