# 编码规范

> 版本：v0.1
> 最后更新：2026-05-12
> 作者：架构组

## 1. 适用范围

<!-- TODO: 覆盖的语言、框架版本、仓库与服务范围。 -->

## 2. 代码风格

### 2.1 命名与格式

<!-- TODO: 包名、类名、方法名、常量命名规则与格式化工具约定。 -->

### 2.2 注释与文档

<!-- TODO: Javadoc、方法注释、TODO 规范。 -->

## 3. 分层与职责约束

<!-- TODO: Controller / Service / Repository / Domain 层的职责边界与调用方向。 -->

## 4. 异常处理与返回规范

<!-- TODO: 全局异常、业务异常、错误码、统一响应体。 -->

## 5. 数据访问规范

### 5.1 Mapper SQL 组织规范

> 对应需求：R5.1（Mapper SQL 组织方式）；对应设计：§7.4 Mapper SQL 组织规范。

为避免注解式 SQL 与 XML SQL 混用、动态 SQL 失控，所有 `@Mapper` 接口在组织 SQL 时必须按下列表格进行判定。在"判定条件"命中任意一行时即归为该行对应的组织方式。

#### 复杂 SQL 判定表

| # | 判定条件 | 使用方式 | 理由 |
| - | -------- | -------- | ---- |
| 1 | 多表 JOIN（2 张表及以上，含子查询中的 JOIN） | **XML** | 注解拼接可读性差；XML 对别名、ON 条件支持更友好 |
| 2 | 含动态 SQL 元素：`<where>` / `<if>` / `<choose>` / `<when>` / `<foreach>` / `<bind>` / `<trim>` / `<set>` | **XML** | `@Select({"<script>", ...})` 拼接数组不利于版本对比与行级审计 |
| 3 | SQL 实际渲染后超过 10 行（注释与空行不计，UNION / UNION ALL 每段独立计算） | **XML** | 长 SQL 在注解中易丢失换行、缩进信息 |
| 4 | 涉及存储过程（`CALL ...`）、触发器或 `CREATE TEMPORARY TABLE` 等 DDL | **XML** | 调用签名与参数映射需要 `<parameterMap>` 支持 |
| 5 | 含 `CASE WHEN ... THEN ... END`、`WITH ... AS (...)` (CTE)、窗口函数、递归 CTE | **XML** | 复杂表达式以 XML 承载更容易做缩进与局部重写 |
| 6 | 同一 Mapper 中既有复杂 SQL 也有简单 CRUD | **拆分到两个 Mapper 文件** | 保持 Mapper 职责单一；XML 文件与注解接口不得混合承载同一 namespace |
| 7 | 简单 CRUD：单表 `SELECT` / `INSERT` / `UPDATE` / `DELETE`，且不含任何动态 SQL、SQL ≤ 10 行 | **注解** | 零构建成本，代码即文档 |
| 8 | 固定列列表的单表点查（含按主键、唯一索引定位） | **注解** | 同上 |

#### 等价规则

- 规则 1~5 中任意一条命中 ⇒ 必须使用 XML；
- 仅当 7~8 规则命中且 1~5 全部不命中时，才允许使用注解；
- 对规则 6 的"同一 Mapper"，可通过将简单 CRUD 与复杂查询拆成两个 Mapper 接口（例如 `AssignmentMapper` 与 `AssignmentQueryMapper`）各自独立承载。

### 5.2 规范判定示例

> 下列示例取自本仓库现有 Mapper，逐项说明为什么归属注解或 XML。

#### 示例 A：简单 CRUD → 注解

文件：`major_assignment/src/main/java/com/_202510007517/major_assignment/mapper/UserMapper.java`

```java
// 单表点查 + 固定列列表 + 无动态 SQL + SQL ≤ 10 行 → 命中规则 8
@Select("SELECT * FROM users WHERE username = #{username}")
User findByUsername(String username);

// 单表更新 + 无动态 SQL → 命中规则 7
@Update("UPDATE users SET username = #{username}, password = #{password}, ... WHERE id = #{id}")
void update(User user);
```

文件：`major_assignment/src/main/java/com/_202510007517/major_assignment/mapper/NotificationMapper.java`

```java
// 单表查询 + 固定列列表 + 简单 WHERE → 命中规则 7
@Select("SELECT id, student_id AS studentId, ... FROM notifications WHERE student_id = #{studentId} ORDER BY created_at DESC")
List<Notification> getNotificationsByStudentId(Long studentId);
```

#### 示例 B：多表 JOIN + 动态 SQL → XML

文件：`major_assignment/src/main/java/com/_202510007517/major_assignment/mapper/EarlyWarningMapper.java`（方法原为 `@Select({"<script>", ...})`，按规则 1+2 迁移到 XML）

迁移后接口只保留方法签名：

```java
// 无 @Select 注解——SQL 定义在 EarlyWarningMapper.xml 中
Long countTotalWarningsWithFilter(@Param("teacherId") Long teacherId,
                                  @Param("classId") Long classId,
                                  @Param("courseId") Long courseId);
```

对应 XML：`major_assignment/src/main/resources/mapper/EarlyWarningMapper.xml`

```xml
<select id="countTotalWarningsWithFilter" resultType="java.lang.Long">
    SELECT COUNT(*) FROM early_warnings ew
    <if test="classId != null">JOIN class_students cs ON ew.student_id = cs.student_id</if>
    WHERE ew.teacher_id = #{teacherId}
    <if test="classId != null"> AND cs.class_id = #{classId}</if>
    <if test="courseId != null"> AND ew.course_id = #{courseId}</if>
</select>
```

#### 示例 C：SQL 超过 10 行 / CASE / UNION → XML

文件：`major_assignment/src/main/resources/mapper/StudentMapper.xml`（方法：`getLearningStats`）—— 命中规则 3+5（CASE 子查询 + 超过 10 行）。完整 SQL 位于 XML，不得改回注解。

文件：`major_assignment/src/main/java/com/_202510007517/major_assignment/mapper/AssignmentMapper.java` 中的 `countMissingSubmissionsByTeacher` —— 含多层子查询 + UNION + 动态 `<if>` —— 命中规则 1、2、3，按本规范迁移到 `AssignmentMapper.xml`。

#### 示例 D：`<foreach>` 批量查询 → XML

文件：`major_assignment/src/main/java/com/_202510007517/major_assignment/mapper/KnowledgePointMapper.java` 中的 `findByCourseIds` / `getStudentMasteryByKnowledgePoints` / `getStudentInfoByIds` —— 命中规则 2（`<foreach>`），迁移到 `KnowledgePointMapper.xml`。

### 5.3 迁移与维护约定

- 新增 Mapper 方法先按上表判定；在代码评审时将判定依据写进 PR 描述。
- 已有违规（在注解中使用 `<script>` 或多表 JOIN）应按本规范迁移到 `src/main/resources/mapper/<Mapper 名>.xml`，对应接口方法保留签名、移除 `@Select/@Insert/@Update/@Delete` 注解。
- 迁移过程中必须保留原 SQL 的语义（列顺序、别名、WHERE 条件、JOIN 类型、ORDER BY 都不可改变），以免引发业务行为漂移。
- 每个 XML 文件的 `namespace` 必须与 Mapper 接口全限定名一致；`id` 必须与方法名一致。
- 新增 XML 文件会被 `mybatis.mapper-locations=classpath:mapper/*.xml`（见 `application.properties`）自动加载，无需额外配置。

### 5.4 本仓库现有 Mapper 组织情况（参考）

经 §7.4 改造后，`src/main/resources/mapper/` 下 XML 映射文件清单如下（与 `src/main/java/.../mapper/` 下同名接口一一对应）：

| XML 文件 | 承载的主要方法类型 |
| -------- | ------------------ |
| `AssignmentMapper.xml` | 多表 JOIN、动态 `<if>`、UNION 子查询、`INSERT ... SELECT` |
| `AssignmentSubmissionMapper.xml` | 分页 `<if>`、多表 JOIN、成绩趋势聚合 |
| `CourseMapper.xml` | 动态搜索条件、`<foreach>`、UNION 子查询、多表 JOIN、班级管理 CRUD |
| `EarlyWarningMapper.xml` | 动态 JOIN、`<if>`/`<choose>`、多表 JOIN |
| `ExamMapper.xml` | 多表 JOIN、UNION 子查询、`INSERT ... SELECT` |
| `ExamSubmissionMapper.xml` | 分页 `<if>`、多表 JOIN |
| `KnowledgeMasteryMapper.xml` | 多表 LEFT JOIN（题目—知识点—得分） |
| `KnowledgePointMapper.xml` | `<foreach>`、CASE、多表 JOIN、UNION 子查询、掌握度统计 |
| `StudentMapper.xml` | 学生学习统计 / 进度 / 时间分布（大量 CASE + UNION + `<if>`） |

仍保留注解的典型简单方法（示例）：

- `UserMapper`（单表 CRUD）
- `NotificationMapper`（单表 CRUD + 固定列列表）
- `BrowserErrorMapper`（接口仅声明方法签名；SQL 实现留待后续引入，不在本次 R5.1 整改范围内）
- `AssignmentMapper.getAllAssignments` / `getAssignmentById` / `insert` / `update` / `delete` 等单表读写

若后续在上述"保留注解"的文件中新增复杂 SQL，必须按本节判定规则迁移到对应 XML 或新建一个对应 XML 文件。

## 6. 日志规范

<!-- TODO: 日志级别、结构化字段、敏感信息脱敏、TraceId 透传。 -->

## 7. 接口契约

<!-- TODO: REST 设计、版本化、Feign 契约、兼容性策略。 -->

## 8. 测试规范

<!-- TODO: 单元测试、集成测试、契约测试、覆盖率门槛。 -->

## 9. 依赖与配置

<!-- TODO: 依赖引入审批、版本管理、配置外置与加密。 -->

## 变更记录

| 日期       | 变更人 | 变更内容                                                             |
| ---------- | ------ | -------------------------------------------------------------------- |
| 2026-05-10 | 架构组 | 初版骨架                                                             |
| 2026-05-12 | 架构组 | 填充 §5.1 Mapper SQL 组织规范（复杂 SQL 判定表 + 示例）+ §5.3 迁移约定 + §5.4 仓库现状说明，对应 R5.1 |
