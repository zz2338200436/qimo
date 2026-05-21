package com._202510007517.platform.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRouteConfigTest {

    @Test
    void analysisRouteCarriesTeacherAndStudentAnalysisCompatibilityEndpoints() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int analysisIndex = routeIds.indexOf("analysis-route");

        assertThat(analysisIndex).isNotNegative();
        assertThat(properties.getProperty(routeKey(analysisIndex, "uri")))
                .isEqualTo("lb://analysis-service");
        assertThat(properties.getProperty(routePredicateKey(analysisIndex, 0)))
                .isEqualTo("Path=/api/teacher/score-trend,/api/teacher/dashboard,/api/teacher/learning-summary,/api/teacher/analysis/**,/api/teacher/knowledge-points/mastery/student/*/course/*,/api/teacher/knowledge-points/stats/course/*,/api/knowledge-points/analysis/teacher/**,/api/early-warnings/**,/api/teacher/early-warnings/**,/api/student/early-warnings,/api/student/study-time-distribution,/api/student/stats,/api/student/knowledge-points,/api/student/knowledge-points/*");
        assertThat(properties.getProperty(routePredicateKey(analysisIndex, 1)))
                .isEqualTo("Method=GET,POST,PUT,DELETE");
    }

    @Test
    void notificationRouteIsConfiguredBeforeLegacyFallback() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int notificationIndex = routeIds.indexOf("notification-route");

        assertThat(notificationIndex).isNotNegative();
        assertThat(properties.getProperty(routeKey(notificationIndex, "uri")))
                .isEqualTo("lb://notification-service");
        assertThat(properties.getProperty(routePredicateKey(notificationIndex, 0)))
                .isEqualTo("Path=/api/notifications/**");
        assertThat(properties.getProperty(routePredicateKey(notificationIndex, 1)))
                .isEqualTo("Method=GET,POST,PUT,DELETE");
        assertThat(properties.getProperty(routeFilterKey(notificationIndex, 0, "name")))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty(routeFilterArgKey(notificationIndex, 0, "name")))
                .isEqualTo("notification-service");
        assertThat(properties.getProperty(routeFilterArgKey(notificationIndex, 0, "fallbackUri")))
                .isEqualTo("forward:/_fallback/notification-service");
    }

    @Test
    void aiRouteIsConfiguredBeforeLegacyFallbackWithUserRateLimit() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int aiIndex = routeIds.indexOf("ai-route");

        assertThat(aiIndex).isNotNegative();
        assertThat(properties.getProperty(routeKey(aiIndex, "uri")))
                .isEqualTo("lb://ai-service");
        assertThat(properties.getProperty(routePredicateKey(aiIndex, 0)))
                .isEqualTo("Path=/api/ai/**");
        assertThat(properties.getProperty(routePredicateKey(aiIndex, 1)))
                .isEqualTo("Method=POST");
        assertThat(properties.getProperty("gateway.rate-limit.routes.ai-route.replenish-rate"))
                .isEqualTo("2");
        assertThat(properties.getProperty("gateway.rate-limit.routes.ai-route.burst-capacity"))
                .isEqualTo("4");
        assertThat(properties.getProperty("gateway.rate-limit.routes.ai-route.requested-tokens"))
                .isEqualTo("1");
    }

    @Test
    void studentExamSubmitRouteIsCoreWithDedicatedLimitsAndCircuitBreaker() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int examSubmitIndex = routeIds.indexOf("student-exam-submit-route");

        assertThat(examSubmitIndex).isNotNegative();
        assertThat(properties.getProperty(routeKey(examSubmitIndex, "uri")))
                .isEqualTo("lb://exam-service");
        assertThat(properties.getProperty(routePredicateKey(examSubmitIndex, 0)))
                .isEqualTo("Path=/api/student/exams/*/submit");
        assertThat(properties.getProperty(routePredicateKey(examSubmitIndex, 1)))
                .isEqualTo("Method=POST");
        assertThat(properties.getProperty(routeFilterKey(examSubmitIndex, 0, "name")))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty(routeFilterArgKey(examSubmitIndex, 0, "name")))
                .isEqualTo("exam-service-student-submit");
        assertThat(properties.getProperty(routeFilterArgKey(examSubmitIndex, 0, "fallbackUri")))
                .isEqualTo("forward:/_fallback/exam-service");
        assertThat(properties.getProperty("gateway.rate-limit.routes.student-exam-submit-route.replenish-rate"))
                .isEqualTo("40");
        assertThat(properties.getProperty("gateway.rate-limit.routes.student-exam-submit-route.burst-capacity"))
                .isEqualTo("80");
        assertThat(properties.getProperty("gateway.rate-limit.routes.student-exam-submit-route.requested-tokens"))
                .isEqualTo("1");
        assertThat(properties.getProperty("resilience4j.circuitbreaker.instances.exam-service-student-submit.failure-rate-threshold"))
                .isEqualTo("30");
        assertThat(properties.getProperty("resilience4j.circuitbreaker.instances.exam-service-student-submit.minimum-number-of-calls"))
                .isEqualTo("20");
    }

    @Test
    void teacherExamCrudRouteCarriesCircuitBreakerFallback() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int teacherExamIndex = routeIds.indexOf("teacher-exam-crud-route");

        assertThat(teacherExamIndex).isNotNegative();
        assertThat(properties.getProperty(routeKey(teacherExamIndex, "uri")))
                .isEqualTo("lb://exam-service");
        assertThat(properties.getProperty(routePredicateKey(teacherExamIndex, 0)))
                .isEqualTo("Path=/api/teacher/exams,/api/teacher/exams/**");
        assertThat(properties.getProperty(routePredicateKey(teacherExamIndex, 1)))
                .isEqualTo("Method=GET,POST,PUT,DELETE");
        assertThat(properties.getProperty(routeFilterKey(teacherExamIndex, 0, "name")))
                .isEqualTo("CircuitBreaker");
        assertThat(properties.getProperty(routeFilterArgKey(teacherExamIndex, 0, "name")))
                .isEqualTo("exam-service-teacher");
        assertThat(properties.getProperty(routeFilterArgKey(teacherExamIndex, 0, "fallbackUri")))
                .isEqualTo("forward:/_fallback/exam-service");
    }

    @Test
    void explicitLegacyRoutesReplaceCatchAllLegacyRoute() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        assertThat(routeIds).doesNotContain("legacy-route");
        int authRouteIndex = routeIds.indexOf("auth-route");
        int authSecuredIndex = routeIds.indexOf("auth-secured");
        assertThat(routeIds).doesNotContain(
                "legacy-public-route",
                "legacy-auth-settings-route",
                "legacy-system-route",
                "legacy-browser-error-route",
                "legacy-dashboard-route",
                "legacy-knowledge-route",
                "legacy-early-warning-route",
                "legacy-student-route",
                "legacy-teacher-dashboard-route",
                "legacy-analysis-trigger-route");
        assertThat(firstLegacyRouteIndex(routeIds)).isEqualTo(-1);
        assertThat(properties.getProperty(routePredicateKey(authRouteIndex, 0)))
                .contains("/api/public/captcha");
        assertThat(properties.getProperty(routePredicateKey(authSecuredIndex, 0)))
                .contains("/api/auth/notification-settings");
        int courseRouteIndex = routeIds.indexOf("course-route");
        assertThat(properties.getProperty(routePredicateKey(courseRouteIndex, 0)))
                .contains("/api/system/semesters")
                .contains("/api/system/student/courses")
                .contains("/api/system/teacher/courses")
                .contains("/api/system/time-ranges");
        assertThat(properties.getProperty("gateway.security.whitelist-paths[3]"))
                .isEqualTo("/api/public/captcha");
        assertThat(properties.getProperty("gateway.security.whitelist-paths[4]"))
                .isEqualTo("/api/errors/browser");
        assertThat(properties.getProperty("gateway.security.whitelist-paths[5]"))
                .isEqualTo("/api/errors/browser/batch");
    }

    @Test
    void courseRouteAlsoCarriesStudentDashboardCompatibilityEndpoints() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int courseRouteIndex = routeIds.indexOf("course-route");
        assertThat(courseRouteIndex).isNotNegative();
        assertThat(properties.getProperty(routePredicateKey(courseRouteIndex, 0)))
                .contains("/api/dashboard/student-performance")
                .contains("/api/teacher/knowledge-points")
                .contains("/api/teacher/knowledge-points/*")
                .contains("/api/teacher/knowledge-points/course/*");
    }

    @Test
    void studentKnowledgePointCompatibilityRouteLivesInAnalysisService() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int analysisIndex = routeIds.indexOf("analysis-route");
        assertThat(analysisIndex).isNotNegative();
        assertThat(properties.getProperty(routeKey(analysisIndex, "uri")))
                .isEqualTo("lb://analysis-service");
        assertThat(properties.getProperty(routePredicateKey(analysisIndex, 0)))
                .contains("/api/student/knowledge-points")
                .contains("/api/student/knowledge-points/*");
        assertThat(routeIds).doesNotContain("legacy-student-route");
    }

    @Test
    void teacherDashboardCompatibilityRouteLivesInAnalysisService() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int analysisIndex = routeIds.indexOf("analysis-route");
        assertThat(analysisIndex).isNotNegative();
        assertThat(properties.getProperty(routeKey(analysisIndex, "uri")))
                .isEqualTo("lb://analysis-service");
        assertThat(properties.getProperty(routePredicateKey(analysisIndex, 0)))
                .contains("/api/teacher/dashboard")
                .contains("/api/teacher/learning-summary")
                .contains("/api/teacher/analysis/**");
        assertThat(routeIds).doesNotContain("legacy-teacher-dashboard-route");
    }

    @Test
    void teacherStudentCompatibilityRouteLivesInCourseService() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int courseRouteIndex = routeIds.indexOf("course-route");
        assertThat(courseRouteIndex).isNotNegative();
        assertThat(properties.getProperty(routeKey(courseRouteIndex, "uri")))
                .isEqualTo("lb://course-service");
        assertThat(properties.getProperty(routePredicateKey(courseRouteIndex, 0)))
                .contains("/api/teacher/check-class-name")
                .contains("/api/teacher/students/**");
        assertThat(routeIds).doesNotContain("legacy-teacher-dashboard-route");
    }

    @Test
    void studentProfileCompatibilityRouteLivesInUserService() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int studentProfileRouteIndex = routeIds.indexOf("student-profile-compatibility-route");
        assertThat(studentProfileRouteIndex).isNotNegative();
        assertThat(properties.getProperty(routeKey(studentProfileRouteIndex, "uri")))
                .isEqualTo("lb://user-service");
        assertThat(properties.getProperty(routePredicateKey(studentProfileRouteIndex, 0)))
                .isEqualTo("Path=/api/student/profile,/api/student/notification-settings,/api/student/privacy-settings,/api/student/upload-avatar,/api/student/export-data,/api/students/*/class");
        assertThat(properties.getProperty(routePredicateKey(studentProfileRouteIndex, 1)))
                .isEqualTo("Method=GET,POST,PUT");
        assertThat(routeIds).doesNotContain("legacy-student-route");
    }

    @Test
    void studentChangePasswordCompatibilityRouteLivesInAuthService() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int authSecuredIndex = routeIds.indexOf("auth-secured");
        assertThat(authSecuredIndex).isNotNegative();
        assertThat(properties.getProperty(routeKey(authSecuredIndex, "uri")))
                .isEqualTo("lb://auth-service");
        assertThat(properties.getProperty(routePredicateKey(authSecuredIndex, 0)))
                .contains("/api/student/change-password");
        assertThat(routeIds).doesNotContain("legacy-student-route");
    }

    @Test
    void studentEarlyWarningsCompatibilityRouteLivesInAnalysisService() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int analysisIndex = routeIds.indexOf("analysis-route");
        assertThat(analysisIndex).isNotNegative();
        assertThat(properties.getProperty(routeKey(analysisIndex, "uri")))
                .isEqualTo("lb://analysis-service");
        assertThat(properties.getProperty(routePredicateKey(analysisIndex, 0)))
                .contains("/api/student/early-warnings");
        assertThat(routeIds).doesNotContain("legacy-student-route");
    }

    @Test
    void studentStudyTimeDistributionCompatibilityRouteLivesInAnalysisService() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int analysisIndex = routeIds.indexOf("analysis-route");
        assertThat(analysisIndex).isNotNegative();
        assertThat(properties.getProperty(routeKey(analysisIndex, "uri")))
                .isEqualTo("lb://analysis-service");
        assertThat(properties.getProperty(routePredicateKey(analysisIndex, 0)))
                .contains("/api/student/study-time-distribution");
        assertThat(routeIds).doesNotContain("legacy-student-route");
    }

    @Test
    void studentStatsCompatibilityRouteLivesInAnalysisService() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int analysisIndex = routeIds.indexOf("analysis-route");
        assertThat(analysisIndex).isNotNegative();
        assertThat(properties.getProperty(routeKey(analysisIndex, "uri")))
                .isEqualTo("lb://analysis-service");
        assertThat(properties.getProperty(routePredicateKey(analysisIndex, 0)))
                .contains("/api/student/stats");
        assertThat(routeIds).doesNotContain("legacy-student-route");
    }

    @Test
    void knowledgeAssociationCompatibilityRoutesLiveInDomainServices() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int assignmentKnowledgeRouteIndex = routeIds.indexOf("assignment-knowledge-route");
        int examKnowledgeRouteIndex = routeIds.indexOf("exam-knowledge-route");
        assertThat(assignmentKnowledgeRouteIndex).isNotNegative();
        assertThat(examKnowledgeRouteIndex).isNotNegative();
        assertThat(routeIds).doesNotContain("legacy-knowledge-route");
        assertThat(properties.getProperty(routeKey(assignmentKnowledgeRouteIndex, "uri")))
                .isEqualTo("lb://assignment-service");
        assertThat(properties.getProperty(routePredicateKey(assignmentKnowledgeRouteIndex, 0)))
                .isEqualTo("Path=/api/teacher/knowledge-points/assignment/**");
        assertThat(properties.getProperty(routePredicateKey(assignmentKnowledgeRouteIndex, 1)))
                .isEqualTo("Method=GET,POST");
        assertThat(properties.getProperty(routeKey(examKnowledgeRouteIndex, "uri")))
                .isEqualTo("lb://exam-service");
        assertThat(properties.getProperty(routePredicateKey(examKnowledgeRouteIndex, 0)))
                .isEqualTo("Path=/api/teacher/knowledge-points/exam/**");
        assertThat(properties.getProperty(routePredicateKey(examKnowledgeRouteIndex, 1)))
                .isEqualTo("Method=GET,POST");
    }

    @Test
    void knowledgeAnalysisTriggerRouteLivesInAnalysisService() {
        Properties properties = loadGatewayProperties();
        List<String> routeIds = readRouteIds(properties);

        int analysisTriggerRouteIndex = routeIds.indexOf("knowledge-analysis-trigger-route");
        assertThat(analysisTriggerRouteIndex).isNotNegative();
        assertThat(properties.getProperty(routeKey(analysisTriggerRouteIndex, "uri")))
                .isEqualTo("lb://analysis-service");
        assertThat(properties.getProperty(routePredicateKey(analysisTriggerRouteIndex, 0)))
                .isEqualTo("Path=/api/teacher/knowledge-points/analyze/student/*/course/*");
        assertThat(properties.getProperty(routePredicateKey(analysisTriggerRouteIndex, 1)))
                .isEqualTo("Method=POST");
    }

    private static Properties loadGatewayProperties() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));
        Properties properties = factory.getObject();
        assertThat(properties).isNotNull();
        return properties;
    }

    private static List<String> readRouteIds(Properties properties) {
        List<String> routeIds = new ArrayList<>();
        for (int index = 0; ; index++) {
            String routeId = properties.getProperty(routeKey(index, "id"));
            if (routeId == null) {
                return routeIds;
            }
            routeIds.add(routeId);
        }
    }

    private static String routeKey(int routeIndex, String property) {
        return "spring.cloud.gateway.routes[" + routeIndex + "]." + property;
    }

    private static String routePredicateKey(int routeIndex, int predicateIndex) {
        return routeKey(routeIndex, "predicates[" + predicateIndex + "]");
    }

    private static String routeFilterKey(int routeIndex, int filterIndex, String property) {
        return routeKey(routeIndex, "filters[" + filterIndex + "]." + property);
    }

    private static String routeFilterArgKey(int routeIndex, int filterIndex, String property) {
        return routeKey(routeIndex, "filters[" + filterIndex + "].args." + property);
    }

    private static int firstLegacyRouteIndex(List<String> routeIds) {
        for (int index = 0; index < routeIds.size(); index++) {
            if (routeIds.get(index).startsWith("legacy-")) {
                return index;
            }
        }
        return -1;
    }
}
