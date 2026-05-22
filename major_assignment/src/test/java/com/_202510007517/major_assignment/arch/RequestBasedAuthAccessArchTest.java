package com._202510007517.major_assignment.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

class RequestBasedAuthAccessArchTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com._202510007517.major_assignment.controller");
    }

    @Test
    void non_auth_controllers_should_not_accept_http_session_parameters() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .and().areDeclaredInClassesThat().doNotHaveSimpleName("AuthController")
                .and().areDeclaredInClassesThat().doNotHaveSimpleName("CaptchaController")
                .and().areDeclaredInClassesThat().doNotHaveSimpleName("BaseController")
                .should().notHaveRawParameterTypes(HttpSession.class)
                .because("Controller 登录态入口已经统一到 HttpServletRequest request attribute，"
                        + "除认证与验证码等特例外不应继续暴露 HttpSession 参数。");

        rule.check(importedClasses);
    }
}
