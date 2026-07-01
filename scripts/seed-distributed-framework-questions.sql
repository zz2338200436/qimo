-- ============================================================
-- 分布式框架技术题库 —— 基于已有 knowledge_points 补充真实题目
-- 涵盖：分布式框架技术(9021) / 服务注册与发现(9022) / API网关(9023) / 配置中心(9024)
-- 题型：单选 SINGLE_CHOICE / 多选 MULTIPLE_CHOICE / 判断 TRUE_FALSE / 填空 FILL_BLANK / 简答 SHORT_ANSWER
-- ============================================================

USE sc_ai;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ==================== 9021 分布式框架技术 ====================

INSERT IGNORE INTO questions (id, content, correct_answer, difficulty, options, score, type, knowledge_point_id, analysis)
VALUES
-- 单选
(91210, '微服务架构与单体架构相比，以下哪项是微服务的核心优势？',
 'B', '中等',
 JSON_ARRAY('所有模块共享同一个数据库', '各服务可独立部署与扩展', '开发复杂度更低', '不需要API网关'),
 2, 'SINGLE_CHOICE', 9021,
 '微服务的核心价值在于独立部署和按需扩展，各服务拥有独立数据库。'),

(91211, '在分布式系统中，"CAP定理"中的C代表什么？',
 'A', '中等',
 JSON_ARRAY('一致性(Consistency)', '可用性(Availability)', '分区容错性(Partition Tolerance)', '并发性(Concurrency)'),
 2, 'SINGLE_CHOICE', 9021,
 'CAP定理由Eric Brewer提出，C=Consistency(一致性)，A=Availability(可用性)，P=Partition Tolerance(分区容错性)。'),

(91212, '以下哪种调用方式不属于微服务间常见的远程调用方式？',
 'D', '中等',
 JSON_ARRAY('REST API', 'RPC(如gRPC)', '消息队列异步通信', '函数直接调用'),
 2, 'SINGLE_CHOICE', 9021,
 '微服务独立部署在不同进程/主机上，不能直接函数调用，必须通过网络通信。'),

(91213, 'Spring Cloud中用于实现声明式HTTP客户端调用的组件是？',
 'B', '简单',
 JSON_ARRAY('Ribbon', 'Feign/OpenFeign', 'Zuul', 'Hystrix'),
 2, 'SINGLE_CHOICE', 9021,
 'OpenFeign提供声明式HTTP客户端，通过注解定义接口即可完成远程调用。'),

-- 多选
(91214, '分布式系统设计中常用的容错策略包括哪些？',
 'A,B,D', '中等',
 JSON_ARRAY('熔断(Circuit Breaker)', '重试(Retry)', '全量同步(Synchronous Full Copy)', '降级(Fallback)'),
 3, 'MULTIPLE_CHOICE', 9021,
 '熔断、重试、降级均为常见容错策略。"全量同步"不是容错策略而是数据同步方式。'),

(91215, '以下哪些属于微服务拆分应遵循的原则？',
 'A,B,C', '中等',
 JSON_ARRAY('按业务领域拆分', '高内聚、低耦合', '每个服务拥有独立数据库', '所有服务使用相同的技术栈'),
 3, 'MULTIPLE_CHOICE', 9021,
 '微服务拆分应按业务、追求高内聚低耦合、数据独立，不要求统一技术栈。'),

-- 判断
(91216, '在分布式系统中，网络分区（Network Partition）是不可避免的。',
 '正确', '中等',
 JSON_ARRAY('正确', '错误'),
 2, 'TRUE_FALSE', 9021,
 '根据CAP定理，网络分区在实际分布式系统中总会发生，P（分区容错性）几乎是必选项。'),

(91217, '微服务架构中所有服务必须使用同一编程语言开发。',
 '错误', '中等',
 JSON_ARRAY('正确', '错误'),
 2, 'TRUE_FALSE', 9021,
 '微服务之间通过标准协议通信，各服务可使用不同语言和技术栈，这是微服务的一大优势。'),

-- 填空
(91218, '分布式链路追踪中，用于唯一标识一次完整请求调用链的ID通常称为______。',
 'TraceId', '中等',
 NULL, 2, 'FILL_BLANK', 9021,
 'TraceId贯穿整个调用链路，配合SpanId可精确追踪每个服务节点的耗时和状态。'),

(91219, '微服务架构中实现服务间通信的两种主要模式是同步调用和______。',
 '异步消息', '中等',
 NULL, 2, 'FILL_BLANK', 9021,
 '同步调用如REST/RPC，异步消息如Kafka/RabbitMQ，两者可组合使用。'),

-- 简答
(91220, '请简述分布式系统中"雪崩效应"及其常见应对策略。',
 '熔断降级防止级联故障，限流保护服务，快速失败避免资源耗尽',
 '困难', NULL, 10, 'SHORT_ANSWER', 9021,
 '雪崩效应：一个服务不可用导致依赖方连锁崩溃。应对：熔断器(Circuit Breaker)、服务降级、限流、超时控制、线程隔离。');

-- ==================== 9022 服务注册与发现 ====================

INSERT IGNORE INTO questions (id, content, correct_answer, difficulty, options, score, type, knowledge_point_id, analysis)
VALUES
-- 单选
(91230, '以下哪项属于常见的服务注册中心？',
 'C', '简单',
 JSON_ARRAY('MySQL', 'Redis', 'Nacos', 'Nginx'),
 2, 'SINGLE_CHOICE', 9022,
 'Nacos是阿里巴巴开源的服务注册与配置中心。Consul和Eureka也是常见注册中心。'),

(91231, 'Eureka服务注册中心中，默认的心跳检测间隔通常是？',
 'B', '中等',
 JSON_ARRAY('15秒', '30秒', '60秒', '90秒'),
 2, 'SINGLE_CHOICE', 9022,
 'Eureka客户端默认每30秒发送一次心跳，若90秒未收到心跳则剔除实例。'),

-- 多选
(91232, '以下哪些属于服务注册中心的常见职责？',
 'A,B,C', '中等',
 JSON_ARRAY('服务实例注册', '健康检查', '服务发现', '直接处理业务请求'),
 3, 'MULTIPLE_CHOICE', 9022,
 '注册中心负责注册、发现和健康监测，不处理业务请求。流量由调用方直连服务实例。'),

-- 判断
(91233, '客户端负载均衡是指由服务调用方根据注册中心返回的实例列表自行选择实例。',
 '正确', '简单',
 JSON_ARRAY('正确', '错误'),
 2, 'TRUE_FALSE', 9022,
 '客户端负载均衡（如Ribbon/LoadBalancer）在调用方本地选择实例，无需经过代理。'),

-- 简答
(91234, '请解释服务注册中心中"服务级别协议（SLA）"的含义及常见指标。',
 'SLA是服务提供方与消费方间的质量约定，常见指标包括可用性(如99.9%)、响应时间(如P99<200ms)、错误率等',
 '困难', NULL, 10, 'SHORT_ANSWER', 9022,
 'SLA（Service Level Agreement）用于量化服务质量，驱动服务治理策略（如限流、熔断阈值设定）。');

-- ==================== 9023 API网关与路由 ====================

INSERT IGNORE INTO questions (id, content, correct_answer, difficulty, options, score, type, knowledge_point_id, analysis)
VALUES
-- 单选
(91240, 'API网关在微服务架构中的主要作用是？',
 'B', '简单',
 JSON_ARRAY('存储业务数据', '统一入口、路由转发和认证', '替代数据库', '编译Java代码'),
 2, 'SINGLE_CHOICE', 9023,
 'API网关作为系统唯一入口，处理路由、认证、限流、日志等横切关注点。'),

(91241, 'Spring Cloud Gateway底层默认使用的Web框架是？',
 'A', '中等',
 JSON_ARRAY('Netty', 'Tomcat', 'Jetty', 'Undertow'),
 2, 'SINGLE_CHOICE', 9023,
 'Spring Cloud Gateway基于Spring WebFlux，底层使用Netty实现非阻塞I/O。'),

-- 多选
(91242, '以下哪些是API网关常见的功能？',
 'A,C,D', '中等',
 JSON_ARRAY('请求路由', '数据库分库分表', '限流与熔断', '统一认证与鉴权'),
 3, 'MULTIPLE_CHOICE', 9023,
 '路由、限流熔断、认证鉴权均为网关核心职责。数据库分库分表属于数据层功能。'),

-- 判断
(91243, '网关层可以进行请求/响应的统一格式转换和协议适配。',
 '正确', '中等',
 JSON_ARRAY('正确', '错误'),
 2, 'TRUE_FALSE', 9023,
 '网关可将外部HTTP请求转化为内部RPC调用，实现协议适配；也可统一包装响应格式。'),

-- 简答
(91244, '请简述API网关中"路由断言（Predicate）"和"过滤器（Filter）"的作用。',
 '路由断言用于匹配请求条件决定转发目标，过滤器在请求前后进行预处理和后处理（如添加请求头、记录日志等）',
 '中等', NULL, 10, 'SHORT_ANSWER', 9023,
 '谓词决定"去哪"，过滤器决定"做什么"。Spring Cloud Gateway通过谓词匹配路由，通过过滤器链处 理请求。');

-- ==================== 9024 配置中心与动态刷新 ====================

INSERT IGNORE INTO questions (id, content, correct_answer, difficulty, options, score, type, knowledge_point_id, analysis)
VALUES
-- 单选
(91250, '下列哪项不属于配置中心的核心功能？',
 'D', '中等',
 JSON_ARRAY('集中管理多环境配置', '配置变更实时推送', '配置版本管理与回滚', '替代消息队列'),
 2, 'SINGLE_CHOICE', 9024,
 '配置中心处理配置管理，不替代消息队列（MQ）的功能。'),

(91251, 'Spring Cloud Config支持使用以下哪种存储后端存放配置文件？',
 'B', '中等',
 JSON_ARRAY('Redis', 'Git仓库', 'MongoDB', 'Elasticsearch'),
 2, 'SINGLE_CHOICE', 9024,
 'Spring Cloud Config天然支持Git作为配置存储后端，也支持本地文件系统和JDBC。'),

-- 判断
(91252, '使用配置中心可以实现不重启服务即更新运行中的配置。',
 '正确', '简单',
 JSON_ARRAY('正确', '错误'),
 2, 'TRUE_FALSE', 9024,
 '通过Spring Cloud Bus + @RefreshScope或Nacos的配置监听机制可实现配置热更新。'),

-- 简答
(91253, '请简述配置管理中"环境隔离"的重要性和常见实现方式。',
 '重要性：开发/测试/生产环境配置独立，防止错误配置影响生产。实现：按Profile区分(dev/test/prod)、按命名空间(Namespace)隔离、配置加密存储。',
 '中等', NULL, 10, 'SHORT_ANSWER', 9024,
 '环境隔离是配置管理的基本原则。常见方式：Spring Profile、Nacos命名空间、Apollo集群管理。');
