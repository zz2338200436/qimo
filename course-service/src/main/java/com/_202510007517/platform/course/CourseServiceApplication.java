package com._202510007517.platform.course;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com._202510007517.platform")
@EnableFeignClients(basePackages = "com._202510007517.platform")
@EnableCaching
public class CourseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourseServiceApplication.class, args);
    }
}
