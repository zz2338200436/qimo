---
title: Java基础题库
topic: Java基础
roleScope: all
tags:
  - Java基础
---

## Question
difficulty: 简单
type: SINGLE_CHOICE
content: Java中用于表示一个类继承另一个类的关键字是哪个？
options:
- implements
- extends
- import
- package
answer: B
analysis: Java类继承使用extends，接口实现使用implements。

## Question
difficulty: 中等
type: SINGLE_CHOICE
content: 下列关于Java基本数据类型的说法，正确的是哪一项？
options:
- String是基本数据类型
- boolean可以与int自动转换
- int默认占用4字节
- char只能存储ASCII字符
answer: C
analysis: int为32位整数，通常占4字节；String是引用类型，boolean不能与int自动转换。

## Question
difficulty: 中等
type: SHORT_ANSWER
content: Java异常处理中，finally代码块的主要作用是什么？
answer: 用于执行释放资源等收尾逻辑
analysis: finally一般用于关闭连接、释放文件句柄等清理工作。

## Question
difficulty: 中等
type: FILL_BLANK
content: Java中`this`关键字通常用于表示什么？
answer: 当前对象的引用
analysis: this通常用于在实例方法或构造器中引用当前对象。

## Question
difficulty: 中等
type: SINGLE_CHOICE
content: 下列关于方法重载的说法，正确的是哪一项？
options:
- 返回值不同即可构成重载
- 参数列表不同即可构成重载
- 访问修饰符不同即可构成重载
- 抛出异常不同即可构成重载
answer: B
analysis: 方法重载依赖参数列表不同，和返回值或异常声明无关。

## Question
difficulty: 中等
type: SHORT_ANSWER
content: 简述Java中访问修饰符private的作用。
answer: 限制成员只能在当前类内部访问
analysis: private用于封装实现细节，避免外部直接访问成员。

## Question
difficulty: 中等
type: SINGLE_CHOICE
content: 下列关于boolean类型的说法，正确的是哪一项？
options:
- boolean默认值是null
- boolean可以与int自动互转
- boolean占用4字节是语法规定
- boolean只有true和false两个取值
answer: D
analysis: Java语言层面上boolean表示真值类型，只能取true或false。

## Question
difficulty: 中等
type: SHORT_ANSWER
content: 局部变量在Java中使用前必须满足什么条件？
answer: 先完成显式初始化
analysis: 局部变量不会像成员变量那样自动获得默认值，使用前必须显式赋值。

## Question
difficulty: 中等
type: TRUE_FALSE
content: char类型在Java中可以直接表示Unicode字符。
options:
- 正确
- 错误
answer: 正确
analysis: char是16位Unicode字符类型，可表示基本多文种平面中的字符。

## Question
difficulty: 中等
type: SINGLE_CHOICE
content: 下列哪项能够构成方法重载？
options:
- 方法名相同但参数列表不同
- 方法名不同但返回值相同
- 方法名相同但只有返回值不同
- 方法名相同且参数完全相同
answer: A
analysis: Java方法重载依赖参数列表差异，而不是仅依赖返回值。

## Question
difficulty: 中等
type: SHORT_ANSWER
content: 请说明this关键字在实例方法中的常见用途。
answer: 引用当前对象并区分成员变量与局部变量
analysis: this常用于访问当前对象成员，或在同名场景下区分成员变量与局部变量。
