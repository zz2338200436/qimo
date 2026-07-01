---
title: API网关与路由题库
topic: API网关与路由
roleScope: all
tags:
  - API网关与路由
---

## Question
difficulty: 中等
type: MULTIPLE_CHOICE
content: API网关常见职责包括哪些？
options:
- 统一路由
- 认证鉴权
- 限流熔断
- 直接管理JVM垃圾回收
answer: A,B,C
analysis: 网关通常处理统一入口层的路由、安全、流量控制等问题。

## Question
difficulty: 中等
type: TRUE_FALSE
content: 网关中的路径重写常用于将外部访问路径转换为下游服务可识别的路径。
options:
- 正确
- 错误
answer: 正确
analysis: 路径重写能让统一入口和内部服务路径解耦。

## Question
difficulty: 中等
type: SHORT_ANSWER
content: 限流在API网关中的核心目标是什么？
answer: 保护系统在高并发下保持稳定
analysis: 限流可以控制请求速率，避免下游被突发流量压垮。

## Question
difficulty: 简单
type: SINGLE_CHOICE
content: API网关在微服务架构中的主要作用是？
options:
- 存储业务数据
- 统一入口、路由转发和认证
- 替代数据库
- 编译Java代码
answer: B
analysis: API网关作为系统唯一入口，处理路由、认证、限流、日志等横切关注点。

## Question
difficulty: 中等
type: SINGLE_CHOICE
content: Spring Cloud Gateway底层默认使用的Web框架是？
options:
- Netty
- Tomcat
- Jetty
- Undertow
answer: A
analysis: Spring Cloud Gateway基于Spring WebFlux，底层使用Netty实现非阻塞I/O。

## Question
difficulty: 中等
type: MULTIPLE_CHOICE
content: 以下哪些是API网关常见的功能？
options:
- 请求路由
- 数据库分库分表
- 限流与熔断
- 统一认证与鉴权
answer: A,C,D
analysis: 路由、限流熔断、认证鉴权均为网关核心职责。数据库分库分表属于数据层功能。

## Question
difficulty: 中等
type: TRUE_FALSE
content: 网关层可以进行请求/响应的统一格式转换和协议适配。
options:
- 正确
- 错误
answer: 正确
analysis: 网关可将外部HTTP请求转化为内部RPC调用，实现协议适配；也可统一包装响应格式。

## Question
difficulty: 中等
type: SHORT_ANSWER
content: 请简述API网关中"路由断言（Predicate）"和"过滤器（Filter）"的作用。
answer: 路由断言用于匹配请求条件决定转发目标，过滤器在请求前后进行预处理和后处理（如添加请求头、记录日志等）
analysis: 谓词决定"去哪"，过滤器决定"做什么"。Spring Cloud Gateway通过谓词匹配路由，通过过滤器链处 理请求。
