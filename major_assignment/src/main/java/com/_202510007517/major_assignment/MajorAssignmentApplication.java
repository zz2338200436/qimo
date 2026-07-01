package com._202510007517.major_assignment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com._202510007517.major_assignment.mapper")
@EnableCaching
@EnableScheduling
public class MajorAssignmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MajorAssignmentApplication.class, args);
    }
}
