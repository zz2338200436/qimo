---
title: 认证授权方案
version: v0.1
last_updated: 2026-05-10
author: 架构组
---

# 认证授权方案

## 1. 设计目标

阶段 2 起，认证能力从单体迁入 `auth-service`。统一入口由 Gateway 承担，业务服务只信任 Gateway 写入的身份头：

- `X-User-Id`
- `X-Roles`
- `X-Active-Role`
- `X-Trace-Id`

Auth_Service 拥有凭证数据与 token 生命周期；User_Service 拥有用户资料与角色；业务服务不得直接读取凭证表。

## 2. 认证流程

### 2.1 登录与 Token 签发

登录入口为 `POST /api/auth/login`：

1. `GET /api/auth/captcha` 生成验证码图片，响应头返回 `X-Captcha-Key`。
2. 登录请求提交 `username / password / captcha / captchaKey`。
3. Auth_Service 校验 Redis `CAPTCHA:IMG:{captchaKey}`，成功后删除验证码，防止重放。
4. Auth_Service 从 `sc_auth.auth_credentials` 校验 BCrypt 口令。
5. Auth_Service 调用 User_Service 查询用户资料和角色。
6. Auth_Service 签发 Access Token 和 Refresh Token。

### 2.2 Token 结构与校验

Access Token 使用 RSA `RS256` 签名，Header 带 `kid`。Claims 结构：

| Claim | 说明 |
| --- | --- |
| `sub` | 用户 ID 字符串 |
| `userId` | 用户 ID 数值 |
| `username` | 用户名 |
| `roles` | 角色列表，例如 `["TEACHER"]` |
| `activeRole` | 当前生效角色 |
| `iat` | 签发时间 |
| `exp` | 过期时间，默认 30 分钟 |
| `jti` | token 唯一 ID |
| `tokenType` | 固定为 `access` |

Refresh Token 是随机 UUID，存储在 Redis `AUTH:REFRESH:{refreshToken}`，默认有效期 7 天。刷新采用滚动策略：刷新成功后删除旧 Refresh Token，并签发新的 Access Token 和 Refresh Token。

## 3. 授权模型

### 3.1 角色与权限定义

角色统一使用大写：`ADMIN / TEACHER / STUDENT`。Gateway 只做身份可信化和粗粒度入口保护，资源归属校验仍由业务服务执行，例如“教师只能访问自己的班级和学生”。

### 3.2 网关层与服务层协作

Gateway 的 `JwtAuthenticationFilter` 验签、检查过期和 Redis 黑名单；`HeaderEnrichFilter` 会先删除客户端自带的身份头，再用 JWT claims 重写身份头。业务服务不得信任客户端直传的 `X-User-Id / X-Roles / X-Active-Role`。

## 4. 多角色会话策略

微服务阶段采用“单 Token + activeRole”策略。用户拥有多个角色时，JWT 的 `roles` 保存全集，`activeRole` 表示当前使用角色。`POST /api/auth/switch-role` 会校验 `targetRole ∈ roles`，然后吊销旧 Access Token 并签发新 Access Token。

这替代单体阶段的多 Cookie Session 模型。过渡期 Gateway 继续保留 `legacy-route`，单体旧 AuthController 标记为 `@Deprecated`，保留一个阶段用于回滚。

## 5. 安全控制

安全控制：

- 验证码：Redis key 使用 `CAPTCHA:IMG:{captchaKey}`，成功校验即删除。
- 口令：`auth_credentials.password_hash` 必须为 BCrypt。
- 黑名单：登出和切换角色写入 `AUTH:JTI_BLACKLIST:{jti}`，TTL 等于 Access Token 剩余有效期。
- CORS：由 Gateway `gateway.cors.*` 配置统一管理，当前通过 `application.yml` 与环境变量外置。
- 限流：Gateway 按 `(ip, userId, routeId)` 分桶。
- 审计：Gateway 与服务透传 `X-Trace-Id`，服务侧写入 MDC。

密钥轮换采用 `kid -> {publicKey, privateKey}` 配置。当前通过服务配置维护新旧密钥并存 48 小时，新签发 token 使用 `auth.active-kid`，验签按 JWT Header 的 `kid` 查找公钥。

## 6. 迁移与兼容

迁移顺序：

1. Gateway 上线 `auth-route / auth-secured / legacy-route`。
2. Auth_Service 接管 `/api/auth/login / refresh / logout / me / switch-role / captcha`。
3. User_Service 提供用户资料和角色内部接口。
4. 旧单体 AuthController 标记 `@Deprecated` 并保留一个阶段。
5. 阶段 2 检查点通过后，删除旧单体认证入口。

## 变更记录

| 日期       | 变更人 | 变更内容 |
| ---------- | ------ | -------- |
| 2026-05-10 | 架构组 | 初版骨架 |
| 2026-05-12 | Codex | 补充 Auth_Service JWT、验证码、多角色与密钥轮换设计 |
| 2026-05-12 | Codex | 去除 Nacos 依赖描述，改为 Eureka + 本地配置过渡口径 |
