package com._202510007517.major_assignment.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class StudentMapperXmlContractTest {

    @Test
    void getTeacherStudentCoursePerformance_supportsSemesterTimeRangeFiltering() throws IOException {
        String xmlContent;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("mapper/StudentMapper.xml")) {
            assertThat(inputStream).as("StudentMapper.xml should be available on test classpath").isNotNull();
            xmlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        String startMarker = "<select id=\"getTeacherStudentCoursePerformance\"";
        int startIndex = xmlContent.indexOf(startMarker);
        assertThat(startIndex).isGreaterThanOrEqualTo(0);

        int endIndex = xmlContent.indexOf("</select>", startIndex);
        assertThat(endIndex).isGreaterThan(startIndex);

        String selectBlock = xmlContent.substring(startIndex, endIndex);

        assertThat(selectBlock)
                .contains("<when test=\"timeRange == 'semester'\">")
                .contains("AND asub.submission_date >= DATE_SUB(CURRENT_DATE(), INTERVAL 4 MONTH)")
                .contains("AND es.submission_date >= DATE_SUB(CURRENT_DATE(), INTERVAL 4 MONTH)");
    }
}
