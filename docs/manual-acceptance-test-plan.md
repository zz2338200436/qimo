# 手工验收测试清单

这份清单用于自动化测试通过后的最终浏览器手工验收。

## 前置条件

- 运行 `powershell -ExecutionPolicy Bypass -File .\scripts\start-idea-dev-frontend.ps1` 启动基础设施和前端预览。
- 通过 IDEA 以 `SPRING_PROFILES_ACTIVE=dev` 和 `CONFIG_SERVER_URL=http://localhost:8888` 启动 Java 服务。
- 打开前端页面：`http://localhost:5500`。
- 使用教师账号，例如 `teacher7`。
- 使用学生账号，例如 `student42`。
- 确认网关地址可用：`http://localhost:8080`。

## 运行状态检查

- 打开 `http://localhost:8080/actuator/health`。
- 预期结果：网关返回健康状态。
- 打开 `http://localhost:8761`。
- 预期结果：Eureka 页面可以正常打开，并能看到已注册的服务。
- 确认以下端口处于监听状态：`5500`、`8080`、`8081`、`8082`、`8083`、`8084`、`8085`、`8086`、`8087`、`8088`、`8761`。

## 教师端流程

1. 使用 `teacher7` 登录。
2. 打开教师端仪表盘。
3. 预期结果：页面正常加载，不会跳回登录页。
4. 打开课程管理。
5. 创建一门测试课程。
6. 编辑这门测试课程。
7. 为这门课程创建一个班级。
8. 将课程分配给班级。
9. 预期结果：课程、班级、课程分配记录都能在页面中查询到。
10. 打开知识点管理。
11. 在测试课程下创建并编辑一个知识点。
12. 预期结果：知识点详情能显示编辑后的内容。
13. 打开作业管理。
14. 创建一个测试作业。
15. 在另一个浏览器窗口中使用学生账号登录并提交该作业。
16. 回到教师端作业管理，对学生提交进行批改。
17. 预期结果：批改成功，学生端能看到已批改的提交记录。
18. 创建一个测试考试。
19. 使用学生账号提交该考试。
20. 回到教师端，对考试提交进行批改。
21. 预期结果：学生端成绩页面能看到批改后的成绩。
22. 打开通知管理。
23. 向 `student42` 发送一条通知。
24. 预期结果：学生端通知页面能看到该通知，并可以标记为已读。
25. 打开教师端设置。
26. 保存个人资料和通知设置。
27. 预期结果：保存成功，页面保持可用。

## 学生端流程

1. 使用 `student42` 登录。
2. 打开学生端仪表盘。
3. 预期结果：页面正常加载，不会跳回登录页。
4. 打开课程列表。
5. 进入一门课程详情页。
6. 预期结果：课程详情正常加载。
7. 打开作业列表。
8. 提交一个可用作业。
9. 预期结果：提交成功，刷新后仍能看到提交内容。
10. 打开考试列表。
11. 提交一个可用考试。
12. 预期结果：提交成功，教师批改后能在成绩记录中看到分数。
13. 打开通知页面。
14. 将一条通知标记为已读。
15. 删除一条测试通知。
16. 预期结果：已读和删除操作能正常更新列表，页面无异常。
17. 打开学生端设置。
18. 保存个人资料设置。
19. 保存通知设置。
20. 保存隐私设置。
21. 预期结果：所有设置均保存成功。

## 异常输入检查

- 清空浏览器 `sessionStorage` 后访问教师端页面。
- 预期结果：跳转到登录页，或显示受控的认证错误状态。
- 使用空的当前密码和过短的新密码尝试修改密码。
- 预期结果：请求失败，并返回校验错误。
- 创建作业时故意缺少必填字段。
- 预期结果：请求被拒绝，页面不会静默创建错误数据。
- 使用非法分数进行批改，例如负数或超过满分的分数。
- 预期结果：请求被拒绝，或页面阻止非法提交。

## 测试数据清理

- 删除手工创建的测试通知。
- 删除手工创建的测试作业和测试考试。
- 删除手工创建的知识点、课程分配、班级和课程。

## 自动化回归证据

最近已验证的命令：

```powershell
mvn test
node scripts\verify-idea-dev-workflow-contract.js
node scripts\verify-idea-dev-runtime-smoke.js
node scripts\verify-gateway-api-smoke.js
node scripts\verify-teacher-jwt-pages.js .\.runtime-logs\teacher-session-full-smoke.json
node scripts\verify-student-jwt-pages.js .\.runtime-logs\student-session-full-smoke.json
node scripts\verify-teacher-browser-crud.js .\.runtime-logs\teacher-session-full-smoke.json .\.runtime-logs\student-session-full-smoke.json
node scripts\verify-student-browser-crud.js .\.runtime-logs\teacher-session-full-smoke.json .\.runtime-logs\student-session-full-smoke.json
```

通过标准：

- 所有自动化命令均成功退出。
- 教师端和学生端手工流程均能完成，且没有影响功能的浏览器控制台错误。
- 异常输入检查能得到受控的校验失败或认证失败结果。
- 手工测试数据在测试结束后已清理。
