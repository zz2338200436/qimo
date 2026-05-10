package com._202510007517.major_assignment.arch;

import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.CompletableFuture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * 结构测试：
 * <ul>
 *   <li>所有 {@link RestController @RestController} 方法必须返回 {@link ResponseResult}
 *       或其合法包装（{@link ResponseEntity}、{@link CompletableFuture}、{@link Resource}、
 *       {@code byte[]}、{@code void}）。这是 P4 响应契约一致性的结构前提。</li>
 *   <li>{@link RestControllerAdvice @RestControllerAdvice} /
 *       {@link ControllerAdvice @ControllerAdvice} 只能存在一处，且必须命名为
 *       {@code GlobalExceptionHandler}。对齐 Design §Error Handling §1
 *       "GlobalExceptionHandler 是唯一异常出口"。</li>
 * </ul>
 * 对齐 tasks.md 4.4 Part A / Requirements 3.1, 3.2。
 */
class ControllerReturnTypeArchTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com._202510007517.major_assignment");
    }

    /**
     * Allow-list：业务 Controller 合法的返回类型。
     * <ul>
     *   <li>{@link ResponseResult}：默认契约。</li>
     *   <li>{@link ResponseEntity}：需要显式设置 HTTP 状态码时的包装。</li>
     *   <li>{@link CompletableFuture}：异步端点（当前未使用，预留）。</li>
     *   <li>{@link Resource}：文件下载流。</li>
     *   <li>{@code byte[]}：二进制输出。</li>
     *   <li>{@code void}：直接写入 {@code HttpServletResponse}（当前 CaptchaController 图片、
     *       EarlyWarningController Excel 导出）。</li>
     * </ul>
     */
    private static final DescribedPredicate<JavaClass> ALLOWED_RETURN_TYPES =
            new DescribedPredicate<JavaClass>(
                    "ResponseResult, ResponseEntity, CompletableFuture, Resource, byte[], or void") {
                @Override
                public boolean test(JavaClass javaClass) {
                    if (javaClass.isEquivalentTo(void.class)) {
                        return true;
                    }
                    if (javaClass.isArray()) {
                        // 仅允许 byte[]
                        return javaClass.getComponentType().isEquivalentTo(byte.class);
                    }
                    return javaClass.isAssignableTo(ResponseResult.class)
                            || javaClass.isAssignableTo(ResponseEntity.class)
                            || javaClass.isAssignableTo(CompletableFuture.class)
                            || javaClass.isAssignableTo(Resource.class);
                }
            };

    @Test
    void rest_controller_methods_must_return_response_result() {
        ArchRule rule = methods()
                .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                .and().arePublic()
                // 排除 Object 工具方法（若有人重写）
                .and().doNotHaveName("equals")
                .and().doNotHaveName("hashCode")
                .and().doNotHaveName("toString")
                .should().haveRawReturnType(ALLOWED_RETURN_TYPES)
                .because("P4 响应契约一致性要求 @RestController 方法必须返回 ResponseResult "
                        + "（或其合法包装：ResponseEntity / Resource / byte[] / void / CompletableFuture）。"
                        + "详见 Design §Error Handling §1、tasks.md 4.4。");

        rule.check(importedClasses);
    }

    @Test
    void only_one_controller_advice_named_global_exception_handler() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestControllerAdvice.class)
                .or().areAnnotatedWith(ControllerAdvice.class)
                .should().haveSimpleName("GlobalExceptionHandler")
                .because("Design §Error Handling §1：GlobalExceptionHandler 必须是唯一异常出口。");

        rule.check(importedClasses);
    }
}
