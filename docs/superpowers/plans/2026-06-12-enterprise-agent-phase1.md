# Enterprise Agent Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first runnable enterprise Agent loop: natural-language command input, action preview, explicit confirmation, audit persistence, and tool execution for assignment publishing/submission plus read-only query and AI-generation forwarding.

**Architecture:** Add `agent-service-api` and `agent-service` as independent Spring Cloud modules. `agent-service` owns Agent sessions/actions/audits and calls existing service API modules or narrow edge clients; it never writes another service database directly. The first release supports rule-based intent parsing with a clean `IntentRecognitionService` boundary so a model-backed parser can replace it later.

**Tech Stack:** Java 17, Spring Boot 3.5.3, Spring Cloud 2025.0.0, Spring Web, OpenFeign, Spring Data JPA, Flyway, H2/MySQL-mode tests, existing `common` response/trace conventions.

---

## File Structure

- Create: `agent-service-api/pom.xml`  
  API module containing DTOs and Feign client for `/api/agent/**`.
- Create: `agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/*.java`  
  Request/response DTOs for chat, preview, confirmation, execution result, sessions, and actions.
- Create: `agent-service-api/src/main/java/com/_202510007517/platform/agent/api/feign/AgentFeignClient.java`  
  Optional internal client for frontend-compatible Agent endpoints.
- Create: `agent-service/pom.xml`  
  Runtime module depending on `common`, `agent-service-api`, `assignment-service-api`, `course-service-api`, `exam-service-api`, and `ai-service-api`.
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/AgentServiceApplication.java`  
  Spring Boot entrypoint with OpenFeign enabled.
- Create: `agent-service/src/main/resources/application.yml`  
  Service identity, test profile, config-center profile handling.
- Create: `agent-service/src/main/resources/db/migration/V1__init_agent_schema.sql`  
  Agent-owned tables: sessions, messages, actions, audit logs.
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/domain/*.java`  
  JPA entities for Agent-owned records.
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/repository/*.java`  
  Spring Data repositories.
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/model/*.java`  
  Internal enums and command planning models.
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/service/*.java`  
  Orchestration, intent recognition, slot extraction, confirmation, audit, and idempotency services.
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/tool/*.java`  
  Tool registry plus first-wave tools.
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/client/AiEdgeClient.java`  
  Narrow Feign client for current `ai-service` endpoints because `ai-service-api` currently has DTOs but no Feign client.
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAssignmentEdgeClient.java`  
  Narrow Feign client for teacher assignment creation because existing assignment API Feign exposes internal submit/query but not teacher create.
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/controller/AgentController.java`  
  Public `/api/agent/**` endpoints.
- Modify: `pom.xml`  
  Add `agent-service-api` and `agent-service` modules after `ai-service`.
- Modify: `docker-compose.yml` and deployment docs only after the service compiles; keep runtime wiring for a later task if baseline needs to stay small.

---

### Task 1: Add Agent API Module

**Files:**
- Create: `agent-service-api/pom.xml`
- Create: `agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/AgentChatRequestDTO.java`
- Create: `agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/AgentChatResponseDTO.java`
- Create: `agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/AgentActionPreviewDTO.java`
- Create: `agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/AgentActionConfirmDTO.java`
- Create: `agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/AgentExecutionResultDTO.java`
- Create: `agent-service-api/src/main/java/com/_202510007517/platform/agent/api/feign/AgentFeignClient.java`
- Modify: `pom.xml`
- Test: `mvn -pl agent-service-api -am test`

- [ ] **Step 1: Write API module smoke test by compiling the module**

Run before creating the module:

```powershell
mvn -pl agent-service-api -am test
```

Expected: FAIL because Maven cannot find the selected project `agent-service-api`.

- [ ] **Step 2: Create `agent-service-api/pom.xml`**

Use this exact POM shape:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com._202510007517</groupId>
        <artifactId>parent-pom</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <relativePath>../parent-pom/pom.xml</relativePath>
    </parent>

    <artifactId>agent-service-api</artifactId>
    <name>agent-service-api</name>
    <description>Feign interfaces and DTOs exposed by agent-service.</description>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Add API DTOs**

Create DTOs with JavaBean getters/setters, no Lombok:

```java
package com._202510007517.platform.agent.api.dto;

import jakarta.validation.constraints.NotBlank;

public class AgentChatRequestDTO {
    private String sessionId;
    @NotBlank
    private String message;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
```

```java
package com._202510007517.platform.agent.api.dto;

public class AgentChatResponseDTO {
    private String sessionId;
    private String responseType;
    private String message;
    private AgentActionPreviewDTO actionPreview;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getResponseType() { return responseType; }
    public void setResponseType(String responseType) { this.responseType = responseType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public AgentActionPreviewDTO getActionPreview() { return actionPreview; }
    public void setActionPreview(AgentActionPreviewDTO actionPreview) { this.actionPreview = actionPreview; }
}
```

```java
package com._202510007517.platform.agent.api.dto;

import java.util.Map;

public class AgentActionPreviewDTO {
    private Long actionId;
    private String intent;
    private String riskLevel;
    private String title;
    private String summary;
    private Map<String, Object> preview;
    private String idempotencyKey;

    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Map<String, Object> getPreview() { return preview; }
    public void setPreview(Map<String, Object> preview) { this.preview = preview; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
```

```java
package com._202510007517.platform.agent.api.dto;

import jakarta.validation.constraints.NotBlank;

public class AgentActionConfirmDTO {
    @NotBlank
    private String idempotencyKey;

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
```

```java
package com._202510007517.platform.agent.api.dto;

import java.util.Map;

public class AgentExecutionResultDTO {
    private Long actionId;
    private String intent;
    private String status;
    private String message;
    private Map<String, Object> result;

    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, Object> getResult() { return result; }
    public void setResult(Map<String, Object> result) { this.result = result; }
}
```

- [ ] **Step 4: Add `AgentFeignClient`**

```java
package com._202510007517.platform.agent.api.feign;

import com._202510007517.platform.agent.api.dto.AgentActionConfirmDTO;
import com._202510007517.platform.agent.api.dto.AgentChatRequestDTO;
import com._202510007517.platform.agent.api.dto.AgentChatResponseDTO;
import com._202510007517.platform.agent.api.dto.AgentExecutionResultDTO;
import com._202510007517.platform.common.web.ResponseResult;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "agent-service", path = "/api/agent")
public interface AgentFeignClient {
    @PostMapping("/chat")
    ResponseResult<AgentChatResponseDTO> chat(@RequestBody @Valid AgentChatRequestDTO request);

    @PostMapping("/actions/{actionId}/confirm")
    ResponseResult<AgentExecutionResultDTO> confirm(@PathVariable("actionId") Long actionId,
                                                    @RequestBody @Valid AgentActionConfirmDTO request);
}
```

If this fails because `common` is not a dependency, add `common` to `agent-service-api` or remove `ResponseResult` from the Feign contract. Prefer adding `common` to match existing API style if the build allows it.

- [ ] **Step 5: Register module in root `pom.xml`**

Add:

```xml
<module>agent-service-api</module>
```

before `agent-service`.

- [ ] **Step 6: Run API module test**

Run:

```powershell
mvn -pl agent-service-api -am test
```

Expected: PASS.

---

### Task 2: Add Agent Runtime Module Skeleton

**Files:**
- Create: `agent-service/pom.xml`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/AgentServiceApplication.java`
- Create: `agent-service/src/main/resources/application.yml`
- Create: `agent-service/src/test/java/com/_202510007517/platform/agent/AgentServiceApplicationTest.java`
- Modify: `pom.xml`
- Test: `mvn -pl agent-service -am test -DskipTests=false`

- [ ] **Step 1: Write failing application context test**

Create:

```java
package com._202510007517.platform.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(
        classes = AgentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:agent-app;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver"
        })
class AgentServiceApplicationTest {

    @Autowired
    private Environment environment;

    @Test
    void startsWithAgentServiceIdentity() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("agent-service");
        assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8092);
    }
}
```

Run:

```powershell
mvn -pl agent-service -am -Dtest=AgentServiceApplicationTest test
```

Expected: FAIL because `agent-service` and `AgentServiceApplication` do not exist.

- [ ] **Step 2: Add `agent-service/pom.xml`**

Use dependencies mirroring `ai-service`, plus API modules:

```xml
<dependencies>
    <dependency><groupId>com._202510007517</groupId><artifactId>common</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>com._202510007517</groupId><artifactId>agent-service-api</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>com._202510007517</groupId><artifactId>assignment-service-api</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>com._202510007517</groupId><artifactId>course-service-api</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>com._202510007517</groupId><artifactId>exam-service-api</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>com._202510007517</groupId><artifactId>ai-service-api</artifactId><version>${project.version}</version></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-openfeign</artifactId></dependency>
    <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-netflix-eureka-client</artifactId></dependency>
    <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-config</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-mysql</artifactId></dependency>
    <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
</dependencies>
```

- [ ] **Step 3: Add application entrypoint**

```java
package com._202510007517.platform.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com._202510007517.platform")
@EnableFeignClients(basePackages = "com._202510007517.platform")
public class AgentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentServiceApplication.class, args);
    }
}
```

- [ ] **Step 4: Add application config**

```yaml
server:
  port: 8092

spring:
  application:
    name: agent-service
  profiles:
    default: dev
  config:
    import:
      - configserver:${CONFIG_SERVER_URL:http://localhost:8888}

---
spring:
  config:
    activate:
      on-profile: test
    import:
      - optional:classpath:application-common.yml
  cloud:
    config:
      enabled: false

---
spring:
  config:
    activate:
      on-profile: docker | prod
  cloud:
    config:
      fail-fast: true
      retry:
        initial-interval: 1000
        max-attempts: 6
        max-interval: 5000
        multiplier: 1.5
```

- [ ] **Step 5: Register runtime module**

Add to root `pom.xml`:

```xml
<module>agent-service</module>
```

after `agent-service-api`.

- [ ] **Step 6: Run skeleton test**

Run:

```powershell
mvn -pl agent-service -am -Dtest=AgentServiceApplicationTest test
```

Expected: PASS.

---

### Task 3: Add Agent Persistence And Repository Tests

**Files:**
- Create: `agent-service/src/main/resources/db/migration/V1__init_agent_schema.sql`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/domain/AgentSessionEntity.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/domain/AgentActionEntity.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/domain/AgentAuditLogEntity.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/repository/AgentSessionRepository.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/repository/AgentActionRepository.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/repository/AgentAuditLogRepository.java`
- Create: `agent-service/src/test/java/com/_202510007517/platform/agent/repository/AgentRepositoryTest.java`

- [ ] **Step 1: Write failing repository test**

Test behavior:

```java
@Test
void savesPendingActionWithIdempotencyKey() {
    AgentSessionEntity session = new AgentSessionEntity();
    session.setUserId(7L);
    session.setUserRole("TEACHER");
    session.setStatus("ACTIVE");
    AgentSessionEntity savedSession = sessionRepository.save(session);

    AgentActionEntity action = new AgentActionEntity();
    action.setSessionId(savedSession.getId());
    action.setIntent("PUBLISH_ASSIGNMENT");
    action.setStatus("PENDING_CONFIRMATION");
    action.setRiskLevel("MEDIUM");
    action.setPreviewJson("{\"title\":\"作业\"}");
    action.setRequestJson("{\"title\":\"作业\"}");
    action.setIdempotencyKey("idem-1");
    AgentActionEntity savedAction = actionRepository.save(action);

    assertThat(actionRepository.findById(savedAction.getId())).isPresent();
    assertThat(savedAction.getIdempotencyKey()).isEqualTo("idem-1");
}
```

Run:

```powershell
mvn -pl agent-service -am -Dtest=AgentRepositoryTest test
```

Expected: FAIL because entities/repositories/schema do not exist.

- [ ] **Step 2: Add Flyway schema**

Create tables:

```sql
CREATE TABLE IF NOT EXISTS agent_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    user_role VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    KEY idx_agent_sessions_user_time (user_id, created_at)
);

CREATE TABLE IF NOT EXISTS agent_actions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    intent VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    risk_level VARCHAR(64) NOT NULL,
    preview_json TEXT,
    request_json TEXT,
    result_json TEXT,
    idempotency_key VARCHAR(128) NOT NULL,
    error_message VARCHAR(512),
    expires_at TIMESTAMP(6) NULL,
    confirmed_at TIMESTAMP(6) NULL,
    executed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_agent_actions_idempotency (idempotency_key),
    KEY idx_agent_actions_session_time (session_id, created_at),
    KEY idx_agent_actions_status_time (status, created_at)
);

CREATE TABLE IF NOT EXISTS agent_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_role VARCHAR(64) NOT NULL,
    operation VARCHAR(128) NOT NULL,
    target_service VARCHAR(128) NOT NULL,
    target_resource VARCHAR(256),
    trace_id VARCHAR(128),
    success BOOLEAN NOT NULL,
    error_message VARCHAR(512),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_agent_audit_action (action_id),
    KEY idx_agent_audit_user_time (user_id, created_at)
);
```

- [ ] **Step 3: Add entities and repositories**

Implement JavaBean JPA entities using `@Table` names above, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`, and fields matching the schema. Repositories extend `JpaRepository<Entity, Long>`.

- [ ] **Step 4: Run repository test**

Run:

```powershell
mvn -pl agent-service -am -Dtest=AgentRepositoryTest test
```

Expected: PASS.

---

### Task 4: Implement Intent Recognition And Action Planning

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentRiskLevel.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/model/RecognizedIntent.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/service/IntentRecognitionService.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/service/RuleBasedIntentRecognitionService.java`
- Create: `agent-service/src/test/java/com/_202510007517/platform/agent/service/RuleBasedIntentRecognitionServiceTest.java`

- [ ] **Step 1: Write failing intent tests**

Cover at least:

```java
@Test
void recognizesAssignmentPublishing() {
    RecognizedIntent result = service.recognize("给Java课程发布作业，标题是Spring Cloud实验，截止明晚十点，满分100");
    assertThat(result.intent()).isEqualTo(AgentIntent.PUBLISH_ASSIGNMENT);
    assertThat(result.slots()).containsEntry("title", "Spring Cloud实验");
    assertThat(result.slots()).containsEntry("maxScore", 100);
}

@Test
void recognizesAssignmentSubmission() {
    RecognizedIntent result = service.recognize("帮我提交数据库作业，内容是实验报告已完成");
    assertThat(result.intent()).isEqualTo(AgentIntent.SUBMIT_ASSIGNMENT);
    assertThat(result.slots()).containsEntry("content", "实验报告已完成");
}

@Test
void recognizesReadOnlyQueries() {
    assertThat(service.recognize("我有哪些待提交作业").intent()).isEqualTo(AgentIntent.QUERY_PENDING_ASSIGNMENTS);
    assertThat(service.recognize("查看我的课程").intent()).isEqualTo(AgentIntent.QUERY_COURSES);
    assertThat(service.recognize("生成五道Java选择题").intent()).isEqualTo(AgentIntent.GENERATE_QUESTIONS);
}
```

Run:

```powershell
mvn -pl agent-service -am -Dtest=RuleBasedIntentRecognitionServiceTest test
```

Expected: FAIL because service does not exist.

- [ ] **Step 2: Implement minimal parser**

Implement:

```java
public enum AgentIntent {
    PUBLISH_ASSIGNMENT,
    SUBMIT_ASSIGNMENT,
    QUERY_PENDING_ASSIGNMENTS,
    QUERY_COURSES,
    QUERY_ASSIGNMENTS,
    QUERY_EXAMS,
    QUERY_NOTIFICATIONS,
    QUERY_STUDENT_STATS,
    GENERATE_QUESTIONS,
    GENERATE_EXAM,
    GENERATE_LEARNING_SUGGESTIONS,
    UNKNOWN
}
```

Use simple keyword matching and regex extraction for:

- `title`: text after `标题是` until comma-like separator.
- `content`: text after `内容是`.
- `maxScore`: number before or after `满分`.
- `courseName`: text before `课程` where present.

- [ ] **Step 3: Run intent tests**

Run:

```powershell
mvn -pl agent-service -am -Dtest=RuleBasedIntentRecognitionServiceTest test
```

Expected: PASS.

---

### Task 5: Implement Orchestrator Preview Flow

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentActionMapper.java`
- Create: `agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentOrchestratorTest.java`

- [ ] **Step 1: Write failing orchestrator test**

Expected behavior:

```java
@Test
void createsPreviewForAssignmentPublishingInsteadOfExecuting() {
    AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null,
            "给Java课程发布作业，标题是Spring Cloud实验，截止明晚十点，满分100");

    assertThat(response.getResponseType()).isEqualTo("ACTION_PREVIEW");
    assertThat(response.getActionPreview().getIntent()).isEqualTo("PUBLISH_ASSIGNMENT");
    assertThat(response.getActionPreview().getIdempotencyKey()).isNotBlank();
    assertThat(response.getActionPreview().getPreview()).containsEntry("title", "Spring Cloud实验");
}
```

Run:

```powershell
mvn -pl agent-service -am -Dtest=AgentOrchestratorTest test
```

Expected: FAIL because orchestrator does not exist.

- [ ] **Step 2: Implement preview flow**

Flow:

```text
chat(userId, role, sessionId, message)
  -> create session if sessionId absent
  -> recognize intent
  -> if UNKNOWN return TEXT
  -> if read-only return TEXT for now
  -> if write intent create agent_actions row with PENDING_CONFIRMATION
  -> return ACTION_PREVIEW
```

Persist `previewJson` and `requestJson` with `ObjectMapper`.

- [ ] **Step 3: Run orchestrator test**

Run:

```powershell
mvn -pl agent-service -am -Dtest=AgentOrchestratorTest test
```

Expected: PASS.

---

### Task 6: Implement Action Execution Tools

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/tool/AgentTool.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/tool/ToolRegistry.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/tool/AssignmentSubmitTool.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/tool/AssignmentPublishTool.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAssignmentEdgeClient.java`
- Create: `agent-service/src/test/java/com/_202510007517/platform/agent/tool/ToolRegistryTest.java`

- [ ] **Step 1: Write failing registry test**

```java
@Test
void resolvesAssignmentPublishToolByIntent() {
    AgentTool tool = registry.resolve(AgentIntent.PUBLISH_ASSIGNMENT);
    assertThat(tool.intent()).isEqualTo(AgentIntent.PUBLISH_ASSIGNMENT);
}
```

Expected: FAIL because registry/tools do not exist.

- [ ] **Step 2: Implement tool interface**

```java
public interface AgentTool {
    AgentIntent intent();
    Map<String, Object> execute(Long userId, String userRole, Map<String, Object> request);
}
```

- [ ] **Step 3: Implement assignment tools**

`AssignmentSubmitTool` uses existing `AssignmentFeignClient.submit`.  
`AssignmentPublishTool` uses `TeacherAssignmentEdgeClient`:

```java
@FeignClient(name = "assignment-service", path = "/api/teacher/assignments")
public interface TeacherAssignmentEdgeClient {
    @PostMapping
    ResponseResult<AssignmentDTO> createAssignment(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestBody TeacherAssignmentUpsertRequestDTO request);
}
```

- [ ] **Step 4: Run registry test**

Run:

```powershell
mvn -pl agent-service -am -Dtest=ToolRegistryTest test
```

Expected: PASS.

---

### Task 7: Implement Confirmation Endpoint And Audit

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/controller/AgentController.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/config/AgentServiceExceptionHandler.java`
- Create: `agent-service/src/test/java/com/_202510007517/platform/agent/controller/AgentControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

Cover:

```java
@Test
void chatReturnsActionPreviewEnvelope() throws Exception {
    mockMvc.perform(post("/api/agent/chat")
            .header("X-User-Id", "7")
            .header("X-Active-Role", "TEACHER")
            .contentType("application/json")
            .content("{\"message\":\"发布作业，标题是Spring Cloud实验，满分100\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.responseType").value("ACTION_PREVIEW"));
}

@Test
void missingUserIdentityReturnsFailureEnvelope() throws Exception {
    mockMvc.perform(post("/api/agent/chat")
            .contentType("application/json")
            .content("{\"message\":\"查看我的课程\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value(400));
}
```

- [ ] **Step 2: Implement `AgentController`**

Endpoints:

```java
@PostMapping("/chat")
public ResponseResult<AgentChatResponseDTO> chat(...)

@PostMapping("/actions/{actionId}/confirm")
public ResponseResult<AgentExecutionResultDTO> confirm(...)
```

Resolve user identity from `CommonTraceConstants.USER_ID_HEADER`, role from `ACTIVE_ROLE_HEADER` then `ROLES_HEADER`.

- [ ] **Step 3: Implement confirmation**

Rules:

- action must exist
- action status must be `PENDING_CONFIRMATION`
- provided key must equal stored `idempotencyKey`
- execute with `ToolRegistry`
- update action status to `EXECUTED` or `FAILED`
- insert audit row

- [ ] **Step 4: Run controller tests**

Run:

```powershell
mvn -pl agent-service -am -Dtest=AgentControllerTest test
```

Expected: PASS.

---

### Task 8: Add Read-Only And AI Forwarding Tools

**Files:**
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/client/AiEdgeClient.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/tool/CourseQueryTool.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/tool/StudentAssignmentQueryTool.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/tool/ExamQueryTool.java`
- Create: `agent-service/src/main/java/com/_202510007517/platform/agent/tool/AiGenerationTool.java`
- Test: `agent-service/src/test/java/com/_202510007517/platform/agent/tool/ReadOnlyToolTest.java`

- [ ] **Step 1: Write failing read-only test**

Verify read-only intent returns `TEXT` or `DATA` response without creating a pending action:

```java
@Test
void readOnlyCourseQueryDoesNotCreatePendingAction() {
    AgentChatResponseDTO response = orchestrator.chat(7L, "TEACHER", null, "查看我的课程");
    assertThat(response.getResponseType()).isEqualTo("DATA");
    assertThat(actionRepository.findAll()).isEmpty();
}
```

- [ ] **Step 2: Implement read-only dispatch**

Read-only tools execute immediately after permission context resolution.

- [ ] **Step 3: Add `AiEdgeClient`**

```java
@FeignClient(name = "ai-service", path = "/api/ai")
public interface AiEdgeClient {
    @PostMapping("/generate-questions")
    ResponseResult<Map<String, Object>> generateQuestions(
            @RequestHeader(CommonTraceConstants.USER_ID_HEADER) String userId,
            @RequestHeader(CommonTraceConstants.ACTIVE_ROLE_HEADER) String role,
            @RequestBody GenerateQuestionsRequestDTO request);
}
```

Add `generateExam` and `learningSuggestions` in the same style.

- [ ] **Step 4: Run read-only tool tests**

Run:

```powershell
mvn -pl agent-service -am -Dtest=ReadOnlyToolTest test
```

Expected: PASS.

---

### Task 9: Gateway Route And Verification Scripts

**Files:**
- Modify: `gateway/src/main/resources/application-common.yml` or existing route config file if routes are centralized there.
- Create: `scripts/verify-agent-service-contract.js`
- Create: `scripts/verify-agent-runtime-smoke.js`

- [ ] **Step 1: Write failing contract script**

Script checks:

- root `pom.xml` includes `agent-service-api`
- root `pom.xml` includes `agent-service`
- `agent-service/src/main/resources/application.yml` contains `name: agent-service`
- gateway route config contains `/api/agent/**`

Expected before gateway route: FAIL.

- [ ] **Step 2: Add gateway route**

Add route:

```yaml
- id: agent-route
  uri: lb://agent-service
  predicates:
    - Path=/api/agent/**
```

Place beside existing AI route.

- [ ] **Step 3: Run contract script and Maven tests**

Run:

```powershell
node .\scripts\verify-agent-service-contract.js
mvn -pl agent-service -am test
```

Expected: both PASS.

---

### Task 10: Documentation Update

**Files:**
- Modify: `docs/enterprise-agent-design-checklist.md`
- Modify: `docs/architecture.md`
- Modify: `docs/data-ownership.md`

- [ ] **Step 1: Mark Phase 1 implemented items**

In `docs/enterprise-agent-design-checklist.md`, mark completed Phase 1 items with `[x]` only after tests pass.

- [ ] **Step 2: Add architecture note**

Add a concise section to `docs/architecture.md`:

```markdown
### Agent Service

`agent-service` provides the natural-language business operation entrypoint. It owns Agent sessions, action previews, confirmation state, and audit records. It does not own course, assignment, exam, analysis, notification, user, or AI business data; it calls owning services through API contracts.
```

- [ ] **Step 3: Add data ownership row**

Add to `docs/data-ownership.md`:

```markdown
| Agent_Service | `sc_agent` | Agent sessions, messages, action previews, confirmation state, audit logs | Agent does not directly write business-owned tables; execution is delegated to owning services. |
```

- [ ] **Step 4: Run docs/contract verification**

Run:

```powershell
node .\scripts\verify-agent-service-contract.js
```

Expected: PASS.

---

## Self-Review

- Spec coverage: Phase 1 covers independent `agent-service`, API DTOs, session/action/audit persistence, intent recognition, preview, confirmation, assignment publish/submit tools, read-only query tools, AI generation forwarding, gateway route, tests, and docs. Later all-domain tools remain in the enterprise checklist for Phase 2 and Phase 3.
- Placeholder scan: The plan avoids open-ended placeholders and specifies exact files, commands, and expected outcomes.
- Type consistency: DTO names, endpoint names, enum names, and service names are consistent across tasks.
