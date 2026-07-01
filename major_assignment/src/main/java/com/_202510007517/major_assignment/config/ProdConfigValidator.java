package com._202510007517.major_assignment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Prod Profile 启动阶段快速失败校验器。
 *
 * <p>对应需求：R4.4 —— IF 环境变量缺失必需的敏感字段并且当前 Profile 为 {@code prod}，
 * THEN THE System SHALL 在启动阶段快速失败并输出明确的启动失败原因。</p>
 *
 * <p>校验范围与 {@code application-prod.properties} 中以 {@code ${ENV_NAME}} 方式外置的字段保持一致：
 * <ul>
 *   <li>数据库：{@code spring.datasource.username} / {@code spring.datasource.password}</li>
 *   <li>Redis：{@code spring.data.redis.password}</li>
 * </ul>
 * 后续接入邮件 / AI API Key 等新敏感字段时，追加到 {@link #REQUIRED_FIELDS} 即可。</p>
 *
 * <p>触发条件：仅在 {@link Environment#getActiveProfiles()} 含 {@code prod} 时执行校验；
 * 任意必需字段为空白（null / 空串 / 纯空白）即判定失败，日志输出所有缺失项后调用
 * {@code System.exit(1)} 以保证容器编排立即感知启动失败。</p>
 */
@Component
public class ProdConfigValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProdConfigValidator.class);

    private static final String PROD_PROFILE = "prod";

    /**
     * 必需的敏感配置项列表。
     * 元素格式为 {@code [属性键, 对应环境变量名]}，日志输出时两者都会展示，便于运维定位。
     */
    private static final String[][] REQUIRED_FIELDS = new String[][]{
            {"spring.datasource.username", "DB_USERNAME"},
            {"spring.datasource.password", "DB_PASSWORD"},
            {"spring.data.redis.password", "REDIS_PASSWORD"}
    };

    private final Environment environment;
    private final IntConsumer exitHandler;

    @Autowired
    public ProdConfigValidator(Environment environment) {
        this(environment, System::exit);
    }

    ProdConfigValidator(Environment environment, IntConsumer exitHandler) {
        this.environment = environment;
        this.exitHandler = exitHandler;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProdProfileActive()) {
            return;
        }

        List<String> missing = collectMissingFields();
        if (missing.isEmpty()) {
            log.info("ProdConfigValidator: 所有必需的敏感配置已正确注入。");
            return;
        }

        log.error("=========================================================");
        log.error("Prod 启动校验失败：以下敏感配置缺失或为空，无法启动。");
        for (String item : missing) {
            log.error("  - {}", item);
        }
        log.error("请检查对应环境变量是否已在运行环境中注入。");
        log.error("=========================================================");
        // 显式快速失败：保证容器编排立即感知启动失败（R4.4）
        exitHandler.accept(1);
    }

    private boolean isProdProfileActive() {
        String[] active = environment.getActiveProfiles();
        return active != null && Arrays.asList(active).contains(PROD_PROFILE);
    }

    private List<String> collectMissingFields() {
        List<String> missing = new ArrayList<>();
        for (String[] field : REQUIRED_FIELDS) {
            String propertyKey = field[0];
            String envName = field[1];
            String value = resolvePropertyOrNull(propertyKey);
            if (isBlank(value)) {
                missing.add(propertyKey + " (env: " + envName + ")");
            }
        }
        return missing;
    }

    private String resolvePropertyOrNull(String propertyKey) {
        try {
            return environment.getProperty(propertyKey);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
