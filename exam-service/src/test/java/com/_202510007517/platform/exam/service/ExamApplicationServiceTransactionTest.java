package com._202510007517.platform.exam.service;

import com._202510007517.platform.exam.api.dto.TeacherExamUpsertRequestDTO;
import com._202510007517.platform.exam.domain.ExamSubmissionRecord;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ExamApplicationServiceTransactionTest {

    @ParameterizedTest
    @MethodSource("teacherWriteMethods")
    void teacherWriteMethodsRollbackAsSingleUnit(String methodName, Class<?>[] parameterTypes) throws Exception {
        Method method = ExamApplicationService.class.getMethod(methodName, parameterTypes);

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.rollbackFor()).contains(Exception.class);
    }

    static Stream<Arguments> teacherWriteMethods() {
        return Stream.of(
                Arguments.of("createTeacherExam", new Class<?>[]{Long.class, TeacherExamUpsertRequestDTO.class}),
                Arguments.of("updateTeacherExam", new Class<?>[]{Long.class, Long.class, TeacherExamUpsertRequestDTO.class}),
                Arguments.of("deleteTeacherExam", new Class<?>[]{Long.class, Long.class}),
                Arguments.of("updateTeacherExamSubmission", new Class<?>[]{Long.class, Long.class, ExamSubmissionRecord.class}),
                Arguments.of("deleteTeacherExamSubmission", new Class<?>[]{Long.class, Long.class})
        );
    }
}
