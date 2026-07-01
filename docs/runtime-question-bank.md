# 运行时题库文档

更新时间：2026-06-23  
数据来源：`docs/question-bank/seeded/` 运行时文档题库，内容由运行中 MySQL 容器 `qimo-mysql` 的 `sc_ai` 库导出  
用途：核对当前“生成题目”实际可用题库，避免把旧 sample 文档误当作运行时数据

## 0. 路由约定

- 题目相关请求默认直接从题库检索，不再回退到 `ai-service` 生成
- 普通聊天只走通用助手，题库问答只走题库检索助手
- 如果题目相关请求缺少题库数据，返回的是“题库不足/部分命中”，不是改走通用大模型兜底

## 1. 结论

- 当前“生成题目”运行时使用的是 `docs/question-bank/seeded/` 文档题库，不再直接从 `sc_ai.questions` 在线取题。
- `docs/question-bank/seeded/` 的内容来自 `sc_ai.questions` + `sc_ai.knowledge_points` 的导出结果。
- 当前运行时题库共有 `95` 道题、`16` 个知识点模块。
- Flyway 已应用到 `V23__expand_ai_question_bank_data.sql`。
- 当前还没有 `V24` 迁移文件，也没有 `V24` 落库记录。
- `docs/question-bank/java/java-basic-sample.md` 只保留给测试和示例，不参与默认运行时加载。

## 2. 当前已应用迁移

| installed_rank | version | description | success |
| --- | --- | --- | --- |
| 1 | 1 | init ai schema | 1 |
| 2 | 20.2 | create event infrastructure | 1 |
| 3 | 21 | create ai question bank | 1 |
| 4 | 22 | seed ai question bank data | 1 |
| 5 | 23 | expand ai question bank data | 1 |

## 3. 题量分布

| 模块 | 难度 | 数量 |
| --- | --- | ---: |
| API网关与路由 | 简单 | 1 |
| API网关与路由 | 中等 | 7 |
| Java基础 | 简单 | 1 |
| Java基础 | 中等 | 10 |
| Java集合框架 | 中等 | 7 |
| SQL基础 | 中等 | 6 |
| SQL基础 | 困难 | 1 |
| 事务管理 | 困难 | 5 |
| 云计算基础 | 简单 | 2 |
| 云计算基础 | 中等 | 2 |
| 云计算架构 | 中等 | 3 |
| 分布式框架技术 | 中等 | 9 |
| 分布式框架技术 | 困难 | 1 |
| 排序算法 | 中等 | 3 |
| 数据库基础 | 简单 | 2 |
| 数据库基础 | 中等 | 2 |
| 服务注册与发现 | 简单 | 2 |
| 服务注册与发现 | 中等 | 6 |
| 服务注册与发现 | 困难 | 1 |
| 栈和队列 | 中等 | 4 |
| 线性表 | 中等 | 3 |
| 虚拟化技术 | 中等 | 3 |
| 配置中心与动态刷新 | 简单 | 1 |
| 配置中心与动态刷新 | 中等 | 6 |
| 面向对象编程 | 中等 | 7 |

## 4. 当前稀缺模块

下列主题在“严格题库模式”下，如果请求数量过大，仍然会返回 `partial=true`：

- 事务管理 / 困难：5
- 服务注册与发现 / 中等：6
- 服务注册与发现 / 困难：1
- SQL基础 / 困难：1
- 分布式框架技术 / 困难：1
- 配置中心与动态刷新 / 简单：1

## 5. 运行时题干清单

### API网关与路由

- 简单 / SINGLE_CHOICE：API网关在微服务架构中的主要作用是？
- 中等 / MULTIPLE_CHOICE：API网关常见职责包括哪些？
- 中等 / TRUE_FALSE：网关中的路径重写常用于将外部访问路径转换为下游服务可识别的路径。
- 中等 / SHORT_ANSWER：限流在API网关中的核心目标是什么？
- 中等 / SINGLE_CHOICE：Spring Cloud Gateway底层默认使用的Web框架是？
- 中等 / MULTIPLE_CHOICE：以下哪些是API网关常见的功能？
- 中等 / TRUE_FALSE：网关层可以进行请求/响应的统一格式转换和协议适配。
- 中等 / SHORT_ANSWER：请简述API网关中"路由断言（Predicate）"和"过滤器（Filter）"的作用。

### Java基础

- 简单 / SINGLE_CHOICE：Java中用于表示一个类继承另一个类的关键字是哪个？
- 中等 / SINGLE_CHOICE：下列关于Java基本数据类型的说法，正确的是哪一项？
- 中等 / SHORT_ANSWER：Java异常处理中，finally代码块的主要作用是什么？
- 中等 / FILL_BLANK：Java中`this`关键字通常用于表示什么？
- 中等 / SINGLE_CHOICE：下列关于方法重载的说法，正确的是哪一项？
- 中等 / SHORT_ANSWER：简述Java中访问修饰符private的作用。
- 中等 / SINGLE_CHOICE：下列关于boolean类型的说法，正确的是哪一项？
- 中等 / SHORT_ANSWER：局部变量在Java中使用前必须满足什么条件？
- 中等 / TRUE_FALSE：char类型在Java中可以直接表示Unicode字符。
- 中等 / SINGLE_CHOICE：下列哪项能够构成方法重载？
- 中等 / SHORT_ANSWER：请说明this关键字在实例方法中的常见用途。

### Java集合框架

- 中等 / TRUE_FALSE：HashMap在通常情况下允许使用null作为key。
- 中等 / SINGLE_CHOICE：如果需要保持元素插入顺序并允许重复元素，通常应优先选择哪种集合接口？
- 中等 / FILL_BLANK：Set接口的一个典型特征是什么？
- 中等 / TRUE_FALSE：ArrayList底层通常基于动态数组实现。
- 中等 / SINGLE_CHOICE：在需要通过键快速查找值的场景中，通常优先考虑哪种接口？
- 中等 / SHORT_ANSWER：LinkedList相较ArrayList更适合哪类操作？
- 中等 / TRUE_FALSE：Hashtable允许使用null作为key。

### SQL基础

- 中等 / SINGLE_CHOICE：SQL语句中用于对分组结果进行条件过滤的关键字是哪个？
- 中等 / TRUE_FALSE：INNER JOIN只返回两个表中满足连接条件的匹配记录。
- 中等 / SHORT_ANSWER：请写出SQL中COUNT(*)的常见用途。
- 中等 / FILL_BLANK：如果要筛选聚合后的分组结果，应优先使用哪个关键字？
- 中等 / TRUE_FALSE：LEFT JOIN的特点是保留左表全部记录，即使右表没有匹配项。
- 中等 / SINGLE_CHOICE：下列哪项最适合描述GROUP BY的作用？
- 困难 / SHORT_ANSWER：请简述COUNT(*)与COUNT(列名)的一个常见区别。

### 事务管理

- 困难 / FILL_BLANK：事务ACID特性中的I代表什么？
- 困难 / TRUE_FALSE：脏读是指一个事务读取到了另一个事务尚未提交的数据。
- 困难 / TRUE_FALSE：事务的一致性强调事务执行前后数据应满足既定约束。
- 困难 / FILL_BLANK：可重复读主要用来减少哪类并发现象？
- 困难 / SINGLE_CHOICE：下列哪项最能体现事务的原子性？

### 云计算基础

- 简单 / SINGLE_CHOICE：云计算的典型特征不包括哪一项？
- 简单 / TRUE_FALSE：IaaS主要向用户提供计算、存储、网络等基础设施资源。
- 中等 / TRUE_FALSE：按需自助服务是云计算的重要特征之一。
- 中等 / SHORT_ANSWER：资源池化在云计算中意味着什么？

### 云计算架构

- 中等 / SHORT_ANSWER：请简述PaaS与SaaS的区别。
- 中等 / TRUE_FALSE：SaaS更偏向于向最终用户直接提供可使用的软件能力。
- 中等 / FILL_BLANK：如果企业需要直接管理操作系统与中间件，通常更接近哪类服务模式？

### 分布式框架技术

- 中等 / SINGLE_CHOICE：微服务架构中，将系统拆分为多个自治服务的主要目的不包括哪一项？
- 中等 / SHORT_ANSWER：分布式系统中，服务调用链路追踪主要解决什么问题？
- 中等 / SHORT_ANSWER：微服务拆分时，常见的边界划分依据是什么？
- 中等 / TRUE_FALSE：分布式链路追踪中的traceId通常用于标识一次完整请求链路。
- 中等 / MULTIPLE_CHOICE：以下哪些属于微服务拆分应遵循的原则？
- 中等 / TRUE_FALSE：在分布式系统中，网络分区（Network Partition）是不可避免的。
- 中等 / TRUE_FALSE：微服务架构中所有服务必须使用同一编程语言开发。
- 中等 / FILL_BLANK：分布式链路追踪中，用于唯一标识一次完整请求调用链的ID通常称为______。
- 中等 / FILL_BLANK：微服务架构中实现服务间通信的两种主要模式是同步调用和______。
- 困难 / SHORT_ANSWER：请简述分布式系统中"雪崩效应"及其常见应对策略。

### 排序算法

- 中等 / SINGLE_CHOICE：在平均情况下，快速排序的时间复杂度通常是多少？
- 中等 / TRUE_FALSE：冒泡排序在最坏情况下的时间复杂度是O(n^2)。
- 中等 / SINGLE_CHOICE：下列哪种排序算法平均情况下通常具有O(n log n)复杂度？

### 数据库基础

- 简单 / FILL_BLANK：关系数据库中用于唯一标识一条记录的字段或字段组合称为什么？
- 简单 / SINGLE_CHOICE：数据库中的外键主要用于表达哪种关系？
- 中等 / SHORT_ANSWER：主键字段的一个关键要求是什么？
- 中等 / TRUE_FALSE：关系模型中，外键的核心作用是维护表之间的参照完整性。

### 服务注册与发现

- 简单 / SINGLE_CHOICE：以下哪项属于常见的服务注册中心？
- 简单 / TRUE_FALSE：客户端负载均衡是指由服务调用方根据注册中心返回的实例列表自行选择实例。
- 中等 / TRUE_FALSE：服务注册中心通常保存服务实例的地址、端口和健康状态等信息。
- 中等 / SINGLE_CHOICE：下列哪项更符合服务发现的作用？
- 中等 / SHORT_ANSWER：服务实例注册到注册中心后，消费者通常通过什么信息完成调用定位？
- 中等 / SINGLE_CHOICE：注册中心中健康检查信息的主要价值是什么？
- 中等 / SINGLE_CHOICE：Eureka服务注册中心中，默认的心跳检测间隔通常是？
- 中等 / MULTIPLE_CHOICE：以下哪些属于服务注册中心的常见职责？
- 困难 / SHORT_ANSWER：请解释服务注册中心中"服务级别协议（SLA）"的含义及常见指标。

### 栈和队列

- 中等 / TRUE_FALSE：栈的基本操作特点是先进先出。
- 中等 / FILL_BLANK：队列的典型操作特点是什么？
- 中等 / FILL_BLANK：栈最典型的操作原则是什么？
- 中等 / SHORT_ANSWER：循环队列的一个常见目的是什么？

### 线性表

- 中等 / FILL_BLANK：线性表的顺序存储结构通常使用什么来存放元素？
- 中等 / SHORT_ANSWER：顺序表随机访问效率通常较高的主要原因是什么？
- 中等 / TRUE_FALSE：链表插入新节点时通常不需要整体移动大量元素。

### 虚拟化技术

- 中等 / TRUE_FALSE：容器相比传统虚拟机通常具有启动更快、镜像更轻量的特点。
- 中等 / SINGLE_CHOICE：容器与传统虚拟机相比，哪项说法更合理？
- 中等 / TRUE_FALSE：镜像是容器运行环境及应用内容的静态封装。

### 配置中心与动态刷新

- 简单 / TRUE_FALSE：使用配置中心可以实现不重启服务即更新运行中的配置。
- 中等 / SHORT_ANSWER：配置中心相比写死在本地配置文件中的主要优势是什么？
- 中等 / SHORT_ANSWER：如果配置中心支持动态刷新，通常意味着什么？
- 中等 / TRUE_FALSE：配置中心的环境隔离有助于避免开发、测试、生产配置相互混用。
- 中等 / SINGLE_CHOICE：下列哪项不属于配置中心的核心功能？
- 中等 / SINGLE_CHOICE：Spring Cloud Config支持使用以下哪种存储后端存放配置文件？
- 中等 / SHORT_ANSWER：请简述配置管理中"环境隔离"的重要性和常见实现方式。

### 面向对象编程

- 中等 / SHORT_ANSWER：请简述面向对象程序设计中封装的含义。
- 中等 / MULTIPLE_CHOICE：下列哪些属于面向对象的基本特征？
- 中等 / TRUE_FALSE：抽象类可以包含已经实现的普通方法。
- 中等 / FILL_BLANK：接口与抽象类相比，更适合描述什么？
- 中等 / SINGLE_CHOICE：下列哪项最能体现多态？
- 中等 / TRUE_FALSE：封装的直接价值之一是降低内部实现对外部调用者的暴露程度。
- 中等 / SHORT_ANSWER：请简述继承在面向对象设计中的主要作用。
