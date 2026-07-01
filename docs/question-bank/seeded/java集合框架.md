---
title: Java集合框架题库
topic: Java集合框架
roleScope: all
tags:
  - Java集合框架
---

## Question
difficulty: 中等
type: TRUE_FALSE
content: HashMap在通常情况下允许使用null作为key。
options:
- 正确
- 错误
answer: 正确
analysis: HashMap允许一个null key，而Hashtable不允许null key或null value。

## Question
difficulty: 中等
type: SINGLE_CHOICE
content: 如果需要保持元素插入顺序并允许重复元素，通常应优先选择哪种集合接口？
options:
- List
- Set
- Map
- Queue
answer: A
analysis: List有序且允许重复元素。

## Question
difficulty: 中等
type: FILL_BLANK
content: Set接口的一个典型特征是什么？
answer: 元素不允许重复
analysis: Set强调唯一性，不保证像List那样允许重复元素。

## Question
difficulty: 中等
type: TRUE_FALSE
content: ArrayList底层通常基于动态数组实现。
options:
- 正确
- 错误
answer: 正确
analysis: ArrayList以数组为基础，擅长随机访问。

## Question
difficulty: 中等
type: SINGLE_CHOICE
content: 在需要通过键快速查找值的场景中，通常优先考虑哪种接口？
options:
- List
- Set
- Map
- Deque
answer: C
analysis: Map以键值对形式组织数据，适合按键检索。

## Question
difficulty: 中等
type: SHORT_ANSWER
content: LinkedList相较ArrayList更适合哪类操作？
answer: 频繁插入和删除中间元素
analysis: 链表结构更适合中间节点插删，但随机访问通常不如数组结构。

## Question
difficulty: 中等
type: TRUE_FALSE
content: Hashtable允许使用null作为key。
options:
- 正确
- 错误
answer: 错误
analysis: Hashtable不允许null key和null value。
