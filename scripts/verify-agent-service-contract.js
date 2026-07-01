const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');

function methodBlock(content, methodName) {
  const marker = `${methodName}(`;
  const markerIndex = content.indexOf(marker);
  if (markerIndex < 0) {
    return '';
  }
  const nextMethodIndex = content.indexOf(');', markerIndex);
  if (nextMethodIndex < 0) {
    return content.slice(markerIndex);
  }
  return content.slice(markerIndex, nextMethodIndex + 2);
}

const checks = [
  {
    name: 'root pom registers agent-service-api',
    file: 'pom.xml',
    contains: '<module>agent-service-api</module>',
  },
  {
    name: 'root pom registers agent-service',
    file: 'pom.xml',
    contains: '<module>agent-service</module>',
  },
  {
    name: 'agent-service declares service identity',
    file: 'agent-service/src/main/resources/application.yml',
    contains: 'name: agent-service',
  },
  {
    name: 'agent controller exposes chat endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/controller/AgentController.java',
    contains: '@PostMapping("/chat")',
  },
  {
    name: 'agent controller exposes confirmation endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/controller/AgentController.java',
    contains: '@PostMapping("/actions/{actionId}/confirm")',
  },
  {
    name: 'agent confirmation DTO carries optional second confirmation text',
    file: 'agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/AgentActionConfirmDTO.java',
    contains: 'secondConfirmationText',
  },
  {
    name: 'agent action preview exposes second confirmation flag',
    file: 'agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/AgentActionPreviewDTO.java',
    contains: 'requiresSecondConfirmation',
  },
  {
    name: 'agent action preview exposes second confirmation phrase',
    file: 'agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/AgentActionPreviewDTO.java',
    contains: 'secondConfirmationPhrase',
  },
  {
    name: 'agent controller exposes cancellation endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/controller/AgentController.java',
    contains: '@PostMapping("/actions/{actionId}/cancel")',
  },
  {
    name: 'agent feign exposes cancellation endpoint',
    file: 'agent-service-api/src/main/java/com/_202510007517/platform/agent/api/feign/AgentFeignClient.java',
    contains: '@PostMapping("/actions/{actionId}/cancel")',
  },
  {
    name: 'agent controller exposes action detail endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/controller/AgentController.java',
    contains: '@GetMapping("/actions/{actionId}")',
  },
  {
    name: 'agent controller exposes session list endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/controller/AgentController.java',
    contains: '@GetMapping("/sessions")',
  },
  {
    name: 'agent controller exposes session detail endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/controller/AgentController.java',
    contains: '@GetMapping("/sessions/{sessionId}")',
  },
  {
    name: 'gateway routes agent API',
    file: 'config-server/src/main/resources/config-repo/gateway.yml',
    contains: 'Path=/api/agent/**',
  },
  {
    name: 'gateway targets agent-service',
    file: 'config-server/src/main/resources/config-repo/gateway.yml',
    contains: 'uri: lb://agent-service',
  },
  {
    name: 'agent persistence migration exists',
    file: 'agent-service/src/main/resources/db/migration/V1__init_agent_schema.sql',
    contains: 'agent_actions',
  },
  {
    name: 'agent message DTO exists',
    file: 'agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/AgentMessageDTO.java',
    contains: 'class AgentMessageDTO',
  },
  {
    name: 'agent session DTO exposes messages',
    file: 'agent-service-api/src/main/java/com/_202510007517/platform/agent/api/dto/AgentSessionDTO.java',
    contains: 'List<AgentMessageDTO> messages',
  },
  {
    name: 'agent message persistence migration exists',
    file: 'agent-service/src/main/resources/db/migration/V3__add_agent_messages.sql',
    contains: 'agent_messages',
  },
  {
    name: 'agent message repository exists',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/repository/AgentMessageRepository.java',
    contains: 'findBySessionIdOrderByCreatedAtAscIdAsc',
  },
  {
    name: 'assignment feign exposes student assignment list endpoint',
    file: 'assignment-service-api/src/main/java/com/_202510007517/platform/assignment/api/feign/AssignmentFeignClient.java',
    contains: '@GetMapping("/student/{studentId}")',
  },
  {
    name: 'agent intent uses student stats naming',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java',
    contains: 'QUERY_STUDENT_STATS',
  },
  {
    name: 'agent intent exposes create course',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java',
    contains: 'CREATE_COURSE',
  },
  {
    name: 'agent intent exposes update course',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java',
    contains: 'UPDATE_COURSE',
  },
  {
    name: 'agent intent exposes delete course',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java',
    contains: 'DELETE_COURSE',
  },
  {
    name: 'agent intent exposes create class',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java',
    contains: 'CREATE_CLASS',
  },
  {
    name: 'course feign exposes create course endpoint',
    file: 'course-service-api/src/main/java/com/_202510007517/platform/course/api/feign/CourseFeignClient.java',
    contains: 'createCourse',
  },
  {
    name: 'course feign exposes update course endpoint',
    file: 'course-service-api/src/main/java/com/_202510007517/platform/course/api/feign/CourseFeignClient.java',
    contains: 'updateCourse',
  },
  {
    name: 'course feign exposes delete course endpoint',
    file: 'course-service-api/src/main/java/com/_202510007517/platform/course/api/feign/CourseFeignClient.java',
    contains: 'deleteCourse',
  },
  {
    name: 'course feign exposes create class endpoint',
    file: 'course-service-api/src/main/java/com/_202510007517/platform/course/api/feign/CourseFeignClient.java',
    contains: 'createClass',
  },
  {
    name: 'agent notification feign exposes unread count endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/NotificationEdgeClient.java',
    contains: '@GetMapping("/student/unread-count")',
  },
  {
    name: 'agent notification feign exposes mark all read endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/NotificationEdgeClient.java',
    contains: '@PutMapping("/read-all")',
  },
  {
    name: 'agent intent exposes mark single notification read',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java',
    contains: 'MARK_NOTIFICATION_READ',
  },
  {
    name: 'agent intent exposes teacher send notification',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java',
    contains: 'SEND_NOTIFICATION',
  },
  {
    name: 'agent intent exposes teacher batch send notification',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java',
    contains: 'SEND_BATCH_NOTIFICATION',
  },
  {
    name: 'agent intent exposes delete notification',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java',
    contains: 'DELETE_NOTIFICATION',
  },
  {
    name: 'agent intent exposes delete all read notifications',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java',
    contains: 'DELETE_ALL_READ_NOTIFICATIONS',
  },
  {
    name: 'agent notification feign exposes mark single read endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/NotificationEdgeClient.java',
    contains: '@PutMapping("/{notificationId}/read")',
  },
  {
    name: 'agent notification feign exposes delete endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/NotificationEdgeClient.java',
    contains: '@DeleteMapping("/{notificationId}")',
  },
  {
    name: 'agent notification feign exposes delete all read endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/NotificationEdgeClient.java',
    contains: '@DeleteMapping("/delete-all-read")',
  },
  {
    name: 'agent notification feign exposes teacher send endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/NotificationEdgeClient.java',
    contains: '@PostMapping("/teacher/send")',
  },
  {
    name: 'agent notification feign exposes teacher batch send endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/NotificationEdgeClient.java',
    contains: '@PostMapping("/teacher/send-batch")',
  },
  {
    name: 'agent analysis feign exposes study time distribution endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/AnalysisEdgeClient.java',
    contains: '@GetMapping("/study-time-distribution")',
  },
  {
    name: 'agent intent exposes teacher dashboard query',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java',
    contains: 'QUERY_TEACHER_DASHBOARD',
  },
  {
    name: 'agent intent exposes learning summary query',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java',
    contains: 'QUERY_LEARNING_SUMMARY',
  },
  {
    name: 'agent intent exposes score trend query',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java',
    contains: 'QUERY_SCORE_TREND',
  },
  {
    name: 'agent intent exposes knowledge mastery query',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/model/AgentIntent.java',
    contains: 'QUERY_KNOWLEDGE_MASTERY',
  },
  {
    name: 'agent teacher analysis feign exposes dashboard endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    contains: '@GetMapping("/dashboard")',
  },
  {
    name: 'agent teacher analysis feign exposes learning summary endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    contains: '@GetMapping("/learning-summary")',
  },
  {
    name: 'agent teacher analysis feign exposes score trend endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    contains: '@GetMapping("/score-trend")',
  },
  {
    name: 'agent teacher analysis feign exposes knowledge mastery endpoint',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    contains: '@GetMapping("/knowledge-points/mastery/student/{studentId}/course/{courseId}")',
  },
  {
    name: 'agent teacher analysis feign accepts class filter',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    method: 'getLearningSummary',
    contains: 'value = "classId"',
  },
  {
    name: 'agent teacher analysis feign accepts course filter',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    method: 'getLearningSummary',
    contains: 'value = "courseId"',
  },
  {
    name: 'agent teacher analysis feign accepts time range filter',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    method: 'getLearningSummary',
    contains: 'value = "timeRange"',
  },
  {
    name: 'agent teacher analysis feign sends user id for learning summary',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    method: 'getLearningSummary',
    contains: 'CommonTraceConstants.USER_ID_HEADER',
  },
  {
    name: 'agent teacher analysis feign accepts class filter for score trend',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    method: 'listScoreTrend',
    contains: 'value = "classId"',
  },
  {
    name: 'agent teacher analysis feign accepts course filter for score trend',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    method: 'listScoreTrend',
    contains: 'value = "courseId"',
  },
  {
    name: 'agent teacher analysis feign accepts time range filter for score trend',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    method: 'listScoreTrend',
    contains: 'value = "timeRange"',
  },
  {
    name: 'agent teacher analysis feign sends user id for score trend',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    method: 'listScoreTrend',
    contains: 'CommonTraceConstants.USER_ID_HEADER',
  },
  {
    name: 'agent teacher analysis feign sends user id for knowledge mastery',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    method: 'listStudentKnowledgeMastery',
    contains: 'CommonTraceConstants.USER_ID_HEADER',
  },
  {
    name: 'agent teacher analysis feign accepts student path for knowledge mastery',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    method: 'listStudentKnowledgeMastery',
    contains: '@PathVariable("studentId")',
  },
  {
    name: 'agent teacher analysis feign accepts course path for knowledge mastery',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/client/TeacherAnalysisEdgeClient.java',
    method: 'listStudentKnowledgeMastery',
    contains: '@PathVariable("courseId")',
  },
  {
    name: 'agent teacher dashboard tool maps teacher dashboard intent',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/TeacherDashboardQueryTool.java',
    contains: 'QUERY_TEACHER_DASHBOARD',
  },
  {
    name: 'agent teacher dashboard tool returns teacher dashboard key',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/TeacherDashboardQueryTool.java',
    contains: '"teacherDashboard"',
  },
  {
    name: 'agent learning summary tool maps learning summary intent',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/LearningSummaryQueryTool.java',
    contains: 'QUERY_LEARNING_SUMMARY',
  },
  {
    name: 'agent learning summary tool returns learning summary key',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/LearningSummaryQueryTool.java',
    contains: '"learningSummary"',
  },
  {
    name: 'agent score trend tool maps score trend intent',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/ScoreTrendQueryTool.java',
    contains: 'QUERY_SCORE_TREND',
  },
  {
    name: 'agent score trend tool returns score trend key',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/ScoreTrendQueryTool.java',
    contains: '"scoreTrend"',
  },
  {
    name: 'agent knowledge mastery tool maps knowledge mastery intent',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/KnowledgeMasteryQueryTool.java',
    contains: 'QUERY_KNOWLEDGE_MASTERY',
  },
  {
    name: 'agent knowledge mastery tool returns knowledge mastery key',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/KnowledgeMasteryQueryTool.java',
    contains: '"knowledgeMastery"',
  },
  {
    name: 'agent course create tool maps create course intent',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/CourseCreateTool.java',
    contains: 'CREATE_COURSE',
  },
  {
    name: 'agent course create tool calls course service create',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/CourseCreateTool.java',
    contains: 'courseClient.createCourse',
  },
  {
    name: 'agent course create tool requires teacher role',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/CourseCreateTool.java',
    contains: '只有教师可以创建课程。',
  },
  {
    name: 'agent course create test covers feign request binding',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/tool/CourseCreateToolTest.java',
    contains: 'createsCourseThroughCourseServiceForTeacher',
  },
  {
    name: 'agent course update tool maps update course intent',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/CourseUpdateTool.java',
    contains: 'UPDATE_COURSE',
  },
  {
    name: 'agent course update tool calls course service update',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/CourseUpdateTool.java',
    contains: 'courseClient.updateCourse',
  },
  {
    name: 'agent course update tool requires teacher role',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/CourseUpdateTool.java',
    contains: '只有教师可以更新课程。',
  },
  {
    name: 'agent course update test covers feign request binding',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/tool/CourseUpdateToolTest.java',
    contains: 'updatesCourseThroughCourseServiceForTeacher',
  },
  {
    name: 'agent course delete tool maps delete course intent',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/CourseDeleteTool.java',
    contains: 'DELETE_COURSE',
  },
  {
    name: 'agent course delete tool calls course service delete',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/CourseDeleteTool.java',
    contains: 'courseClient.deleteCourse',
  },
  {
    name: 'agent course delete tool requires teacher role',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/CourseDeleteTool.java',
    contains: '只有教师可以删除课程。',
  },
  {
    name: 'agent course delete test covers feign request binding',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/tool/CourseDeleteToolTest.java',
    contains: 'deletesCourseThroughCourseServiceForTeacher',
  },
  {
    name: 'agent class create tool maps create class intent',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/ClassCreateTool.java',
    contains: 'CREATE_CLASS',
  },
  {
    name: 'agent class create tool calls course service create class',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/ClassCreateTool.java',
    contains: 'courseClient.createClass',
  },
  {
    name: 'agent class create tool requires teacher role',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/ClassCreateTool.java',
    contains: '只有教师可以创建班级。',
  },
  {
    name: 'agent class create test covers feign request binding',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/tool/ClassCreateToolTest.java',
    contains: 'createsClassThroughCourseServiceForTeacher',
  },
  {
    name: 'agent rule intent test covers course creation',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/service/RuleBasedIntentRecognitionServiceTest.java',
    contains: 'recognizesCourseCreation',
  },
  {
    name: 'agent rule intent test covers course updating',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/service/RuleBasedIntentRecognitionServiceTest.java',
    contains: 'recognizesCourseUpdating',
  },
  {
    name: 'agent rule intent test covers course deleting',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/service/RuleBasedIntentRecognitionServiceTest.java',
    contains: 'recognizesCourseDeleting',
  },
  {
    name: 'agent rule intent test covers class creation',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/service/RuleBasedIntentRecognitionServiceTest.java',
    contains: 'recognizesClassCreation',
  },
  {
    name: 'agent slot requirement test covers course creation',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentSlotRequirementServiceTest.java',
    contains: 'requiresCourseNameCodeCreditAndTotalHoursForCreateCourse',
  },
  {
    name: 'agent slot requirement test covers course updating',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentSlotRequirementServiceTest.java',
    contains: 'requiresCourseIdNameCodeCreditAndTotalHoursForUpdateCourse',
  },
  {
    name: 'agent slot requirement test covers course deleting',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentSlotRequirementServiceTest.java',
    contains: 'requiresCourseIdForDeleteCourse',
  },
  {
    name: 'agent slot requirement test covers class creation',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentSlotRequirementServiceTest.java',
    contains: 'requiresClassNameYearAndCapacityForCreateClass',
  },
  {
    name: 'agent registry test covers course create tool',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/tool/ToolRegistryTest.java',
    contains: 'resolvesCourseCreateToolByIntent',
  },
  {
    name: 'agent registry test covers course update tool',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/tool/ToolRegistryTest.java',
    contains: 'resolvesCourseUpdateToolByIntent',
  },
  {
    name: 'agent mark notification read tool maps notification intent',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/NotificationMarkReadTool.java',
    contains: 'MARK_NOTIFICATION_READ',
  },
  {
    name: 'agent teacher send notification tool maps send intent',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/TeacherSendNotificationTool.java',
    contains: 'SEND_NOTIFICATION',
  },
  {
    name: 'agent teacher batch send notification tool maps batch send intent',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/TeacherSendBatchNotificationTool.java',
    contains: 'SEND_BATCH_NOTIFICATION',
  },
  {
    name: 'agent delete notification tool maps delete intent',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/NotificationDeleteTool.java',
    contains: 'DELETE_NOTIFICATION',
  },
  {
    name: 'agent delete all read notifications tool maps delete all read intent',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/tool/NotificationDeleteAllReadTool.java',
    contains: 'DELETE_ALL_READ_NOTIFICATIONS',
  },
  {
    name: 'agent audit maps delete notification to notification service',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentAuditService.java',
    contains: 'DELETE_NOTIFICATION',
  },
  {
    name: 'agent audit maps batch send notification to notification service',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentAuditService.java',
    contains: 'SEND_BATCH_NOTIFICATION',
  },
  {
    name: 'agent orchestrator validates second confirmation',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java',
    contains: 'validateSecondConfirmation',
  },
  {
    name: 'agent action planning service exists',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionPlanningService.java',
    contains: 'class ActionPlanningService',
  },
  {
    name: 'agent action planning service creates preview responses',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionPlanningService.java',
    contains: 'createPreviewResponse',
  },
  {
    name: 'agent action confirmation service exists',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionConfirmationService.java',
    contains: 'class ActionConfirmationService',
  },
  {
    name: 'agent action confirmation service validates confirmation preflight',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionConfirmationService.java',
    contains: 'requireActionForConfirmation',
  },
  {
    name: 'agent action confirmation service validates cancellation preflight',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionConfirmationService.java',
    contains: 'requireActionForCancellation',
  },
  {
    name: 'agent idempotency service exists',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentIdempotencyService.java',
    contains: 'class AgentIdempotencyService',
  },
  {
    name: 'agent idempotency service generates action keys',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentIdempotencyService.java',
    contains: 'newActionKey',
  },
  {
    name: 'agent risk policy exists',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentRiskPolicy.java',
    contains: 'class AgentRiskPolicy',
  },
  {
    name: 'agent risk policy classifies intent risk',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentRiskPolicy.java',
    contains: 'riskLevel',
  },
  {
    name: 'agent risk policy drives second confirmation',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentRiskPolicy.java',
    contains: 'requiresSecondConfirmation',
  },
  {
    name: 'agent confirmation policy exists',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentConfirmationPolicy.java',
    contains: 'class AgentConfirmationPolicy',
  },
  {
    name: 'agent confirmation policy classifies confirmation requirements',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentConfirmationPolicy.java',
    contains: 'requiresConfirmation',
  },
  {
    name: 'agent confirmation policy delegates second confirmation to risk policy',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentConfirmationPolicy.java',
    contains: 'riskPolicy.requiresSecondConfirmation',
  },
  {
    name: 'agent data masking policy exists',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentDataMaskingPolicy.java',
    contains: 'class AgentDataMaskingPolicy',
  },
  {
    name: 'agent data masking policy masks text',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentDataMaskingPolicy.java',
    contains: 'maskText',
  },
  {
    name: 'agent data masking policy masks metadata',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentDataMaskingPolicy.java',
    contains: 'maskMetadata',
  },
  {
    name: 'agent data masking policy handles sensitive metadata keys',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentDataMaskingPolicy.java',
    contains: 'isSensitiveMetadataKey',
  },
  {
    name: 'agent data masking tests cover structured credential metadata',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentDataMaskingPolicyTest.java',
    contains: 'masksStructuredCredentialMetadataKeys',
  },
  {
    name: 'agent data masking tests cover json-like credential text',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/service/AgentDataMaskingPolicyTest.java',
    contains: 'masksJsonLikeCredentialTextFragments',
  },
  {
    name: 'agent orchestrator delegates confirmation requirement checks',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java',
    contains: 'confirmationPolicy.requiresConfirmation',
  },
  {
    name: 'agent action mapper delegates risk classification',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentActionMapper.java',
    contains: 'riskPolicy.riskLevel',
  },
  {
    name: 'agent action planning service delegates idempotency key generation',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionPlanningService.java',
    contains: 'idempotencyService.newActionKey',
  },
  {
    name: 'agent action planning service masks persisted action preview payload',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionPlanningService.java',
    contains: 'dataMaskingPolicy.maskMetadata(preview.getPreview())',
  },
  {
    name: 'agent action planning service masks persisted action request slots',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionPlanningService.java',
    contains: 'dataMaskingPolicy.maskMetadata(recognizedIntent.slots())',
  },
  {
    name: 'agent action confirmation service delegates idempotency key validation',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionConfirmationService.java',
    contains: 'idempotencyService.assertMatches',
  },
  {
    name: 'agent action execution service exists',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionExecutionService.java',
    contains: 'class ActionExecutionService',
  },
  {
    name: 'agent action execution service executes tools',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionExecutionService.java',
    contains: 'toolRegistry.resolve',
  },
  {
    name: 'agent action execution service masks persisted tool result payload',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionExecutionService.java',
    contains: 'dataMaskingPolicy.maskMetadata(tool.execute',
  },
  {
    name: 'agent action execution service masks thrown error messages',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionExecutionService.java',
    contains: 'dataMaskingPolicy.maskText',
  },
  {
    name: 'agent action execution service handles null thrown error messages',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionExecutionService.java',
    contains: 'DEFAULT_FAILURE_MESSAGE',
  },
  {
    name: 'agent audit service exists',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentAuditService.java',
    contains: 'class AgentAuditService',
  },
  {
    name: 'agent audit service records action execution',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentAuditService.java',
    contains: 'recordActionExecution',
  },
  {
    name: 'agent audit service maps intent target services',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentAuditService.java',
    contains: 'targetService',
  },
  {
    name: 'agent audit service masks persisted audit errors',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentAuditService.java',
    contains: 'dataMaskingPolicy.maskText(errorMessage)',
  },
  {
    name: 'agent action execution service delegates audit logging',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/ActionExecutionService.java',
    contains: 'auditService.recordActionExecution',
  },
  {
    name: 'agent session service exists',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentSessionService.java',
    contains: 'class AgentSessionService',
  },
  {
    name: 'agent session service resolves chat sessions',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentSessionService.java',
    contains: 'resolveSession',
  },
  {
    name: 'agent session service persists chat messages',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentSessionService.java',
    contains: 'saveAssistantMessage',
  },
  {
    name: 'agent session service masks persisted message content',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentSessionService.java',
    contains: 'dataMaskingPolicy.maskText',
  },
  {
    name: 'agent session service masks persisted message metadata',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentSessionService.java',
    contains: 'dataMaskingPolicy.maskMetadata',
  },
  {
    name: 'agent session service serves session history',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentSessionService.java',
    contains: 'findBySessionIdOrderByCreatedAtAscIdAsc',
  },
  {
    name: 'agent orchestrator delegates preview creation to action planning service',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java',
    contains: 'actionPlanningService.createPreviewResponse',
  },
  {
    name: 'agent orchestrator delegates confirmation preflight',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java',
    contains: 'actionConfirmationService.requireActionForConfirmation',
  },
  {
    name: 'agent orchestrator delegates cancellation preflight',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java',
    contains: 'actionConfirmationService.requireActionForCancellation',
  },
  {
    name: 'agent orchestrator delegates confirmed action execution',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java',
    contains: 'actionExecutionService.execute',
  },
  {
    name: 'agent orchestrator delegates session resolution',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java',
    contains: 'sessionService.resolveSession',
  },
  {
    name: 'agent orchestrator delegates assistant message persistence',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java',
    contains: 'sessionService.saveAssistantMessage',
  },
  {
    name: 'agent validates numeric slots before preview creation',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentSlotRequirementService.java',
    contains: 'requireNumberIfPresent',
  },
  {
    name: 'agent validates numeric list slots before preview creation',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentSlotRequirementService.java',
    contains: 'requireNumberListIfPresent',
  },
  {
    name: 'agent validates map slots before preview creation',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentSlotRequirementService.java',
    contains: 'requireMapIfPresent',
  },
  {
    name: 'agent llm recognition declares slot allowlist',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: 'ALLOWED_SLOT_NAMES',
  },
  {
    name: 'agent llm recognition sanitizes model slots',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: 'sanitizeSlots(response.slots())',
  },
  {
    name: 'agent llm recognition sanitizes model missing slots',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: 'sanitizeMissingSlots(response.missingSlots())',
  },
  {
    name: 'agent llm recognition logs masked prompt',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: 'agent llm intent prompt',
  },
  {
    name: 'agent llm recognition logs masked model output',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: 'agent llm intent output',
  },
  {
    name: 'agent llm recognition logs recognized result',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: 'agent llm intent recognized',
  },
  {
    name: 'agent llm recognition uses masking policy for text logs',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: 'dataMaskingPolicy.maskText',
  },
  {
    name: 'agent llm recognition uses masking policy for slot logs',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: 'dataMaskingPolicy.maskMetadata(enrichedIntent.slots())',
  },
  {
    name: 'agent llm recognition tests reject unknown slot keys',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionServiceTest.java',
    contains: 'dropsUnknownLlmSlotKeysAndMissingSlotNames',
  },
  {
    name: 'agent llm recognition tests masked logging',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionServiceTest.java',
    contains: 'logsLlmPromptOutputAndRecognizedIntentWithSensitiveDataMasked',
  },
  {
    name: 'agent llm recognition allows assignment and exam lifecycle slots',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: '"description"',
  },
  {
    name: 'agent llm recognition allows publish date slot',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: '"publishDate"',
  },
  {
    name: 'agent llm recognition allows active status slot',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: '"isActive"',
  },
  {
    name: 'agent llm recognition allows online status slot',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: '"isOnline"',
  },
  {
    name: 'agent llm recognition allows offline location slot',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: '"location"',
  },
  {
    name: 'agent llm recognition allows grading submission id slot',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: '"submissionId"',
  },
  {
    name: 'agent llm recognition allows grading score slot',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: '"score"',
  },
  {
    name: 'agent llm recognition allows teacher comment slot',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: '"teacherComment"',
  },
  {
    name: 'agent llm recognition allows analysis class slot',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: '"classId"',
  },
  {
    name: 'agent llm recognition allows analysis time range slot',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionService.java',
    contains: '"timeRange"',
  },
  {
    name: 'agent llm recognition tests preserve publish exam business slots',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionServiceTest.java',
    contains: 'preservesImplementedPublishExamBusinessSlots',
  },
  {
    name: 'agent llm recognition tests preserve grade and analysis business slots',
    file: 'agent-service/src/test/java/com/_202510007517/platform/agent/service/LlmIntentRecognitionServiceTest.java',
    contains: 'preservesImplementedGradeAndAnalysisBusinessSlots',
  },
  {
    name: 'agent orchestrator supports second confirmation pending status',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java',
    contains: 'PENDING_SECOND_CONFIRMATION',
  },
  {
    name: 'agent orchestrator reports batch operation count during second confirmation',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentOrchestrator.java',
    contains: 'operationCount',
  },
  {
    name: 'agent action mapper marks critical actions for second confirmation',
    file: 'agent-service/src/main/java/com/_202510007517/platform/agent/service/AgentActionMapper.java',
    contains: 'requiresSecondConfirmation',
  },
  {
    name: 'agent frontend sends second confirmation text',
    file: 'frontend/dist/agent-chat-panel.js',
    contains: 'secondConfirmationText',
  },
  {
    name: 'agent frontend handles second confirmation pending status',
    file: 'frontend/dist/agent-chat-panel.js',
    contains: "result.status === 'PENDING_SECOND_CONFIRMATION'",
  },
  {
    name: 'agent frontend calls cancellation endpoint',
    file: 'frontend/dist/agent-chat-panel.js',
    contains: '`/api/agent/actions/${preview.actionId}/cancel`',
  },
  {
    name: 'agent frontend resolves course detail command from multiple course id field names',
    file: 'frontend/dist/agent-chat-panel.js',
    contains: 'return course?.id ?? course?.courseId ?? course?.course_id ?? null;',
  },
  {
    name: 'agent frontend renders course detail payload with a dedicated detail card',
    file: 'frontend/dist/agent-chat-panel.js',
    contains: 'return renderCourseDetailCard(value.course);',
  },
  {
    name: 'agent frontend defines a dedicated course detail renderer',
    file: 'frontend/dist/agent-chat-panel.js',
    contains: 'function renderCourseDetailCard(course) {',
  },
  {
    name: 'agent frontend can switch current chat session from history selection',
    file: 'frontend/dist/agent-chat-panel.js',
    contains: 'switchSession(sessionId, options = {}) {',
  },
  {
    name: 'agent frontend persists current chat session id',
    file: 'frontend/dist/agent-chat-panel.js',
    contains: 'persistCurrentSessionId(sessionId) {',
  },
  {
    name: 'agent frontend listens for session selection events',
    file: 'frontend/dist/agent-chat-panel.js',
    contains: "window.addEventListener('agent-session-selected'",
  },
  {
    name: 'legacy static agent frontend handles second confirmation pending status',
    file: 'major_assignment/src/main/resources/static/agent-chat-panel.js',
    contains: "result.status === 'PENDING_SECOND_CONFIRMATION'",
  },
  {
    name: 'legacy static agent frontend calls cancellation endpoint',
    file: 'major_assignment/src/main/resources/static/agent-chat-panel.js',
    contains: '`/api/agent/actions/${preview.actionId}/cancel`',
  },
  {
    name: 'legacy static agent frontend resolves course detail command from multiple course id field names',
    file: 'major_assignment/src/main/resources/static/agent-chat-panel.js',
    contains: 'return course?.id ?? course?.courseId ?? course?.course_id ?? null;',
  },
  {
    name: 'legacy static agent frontend renders course detail payload with a dedicated detail card',
    file: 'major_assignment/src/main/resources/static/agent-chat-panel.js',
    contains: 'return renderCourseDetailCard(value.course);',
  },
  {
    name: 'legacy static agent frontend defines a dedicated course detail renderer',
    file: 'major_assignment/src/main/resources/static/agent-chat-panel.js',
    contains: 'function renderCourseDetailCard(course) {',
  },
  {
    name: 'legacy static agent frontend can switch current chat session from history selection',
    file: 'major_assignment/src/main/resources/static/agent-chat-panel.js',
    contains: 'switchSession(sessionId, options = {}) {',
  },
  {
    name: 'legacy static agent frontend persists current chat session id',
    file: 'major_assignment/src/main/resources/static/agent-chat-panel.js',
    contains: 'persistCurrentSessionId(sessionId) {',
  },
  {
    name: 'legacy static agent frontend listens for session selection events',
    file: 'major_assignment/src/main/resources/static/agent-chat-panel.js',
    contains: "window.addEventListener('agent-session-selected'",
  },
  {
    name: 'agent history detail exposes continue session action in frontend dist',
    file: 'frontend/dist/agent-history-panel.js',
    contains: 'data-agent-history-continue',
  },
  {
    name: 'agent history detail dispatches session selection event in frontend dist',
    file: 'frontend/dist/agent-history-panel.js',
    contains: "window.dispatchEvent(new CustomEvent('agent-session-selected'",
  },
  {
    name: 'agent history frontend dist tracks current session highlight events',
    file: 'frontend/dist/agent-history-panel.js',
    contains: "window.addEventListener('agent-session-changed'",
  },
  {
    name: 'agent history frontend dist restores current session from storage',
    file: 'frontend/dist/agent-history-panel.js',
    contains: "window.sessionStorage.getItem(getCurrentSessionStorageKey())",
  },
  {
    name: 'agent history frontend dist exposes current session highlight class',
    file: 'frontend/dist/agent-history-panel.js',
    contains: 'agent-history-item-current',
  },
  {
    name: 'agent history detail exposes continue session action in legacy static bundle',
    file: 'major_assignment/src/main/resources/static/agent-history-panel.js',
    contains: 'data-agent-history-continue',
  },
  {
    name: 'agent history detail dispatches session selection event in legacy static bundle',
    file: 'major_assignment/src/main/resources/static/agent-history-panel.js',
    contains: "window.dispatchEvent(new CustomEvent('agent-session-selected'",
  },
  {
    name: 'agent history legacy static bundle tracks current session highlight events',
    file: 'major_assignment/src/main/resources/static/agent-history-panel.js',
    contains: "window.addEventListener('agent-session-changed'",
  },
  {
    name: 'agent history legacy static bundle restores current session from storage',
    file: 'major_assignment/src/main/resources/static/agent-history-panel.js',
    contains: "window.sessionStorage.getItem(getCurrentSessionStorageKey())",
  },
  {
    name: 'agent history legacy static bundle exposes current session highlight class',
    file: 'major_assignment/src/main/resources/static/agent-history-panel.js',
    contains: 'agent-history-item-current',
  },
];

const failures = [];

for (const check of checks) {
  const fullPath = path.join(root, check.file);
  if (!fs.existsSync(fullPath)) {
    failures.push(`${check.name}: missing ${check.file}`);
    continue;
  }
  const content = fs.readFileSync(fullPath, 'utf8');
  const searchableContent = check.method ? methodBlock(content, check.method) : content;
  if (!searchableContent.includes(check.contains)) {
    failures.push(`${check.name}: ${check.file} does not contain ${check.contains}`);
  }
}

if (failures.length > 0) {
  console.error('Agent service contract verification failed:');
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log(`Agent service contract verification passed (${checks.length} checks).`);
