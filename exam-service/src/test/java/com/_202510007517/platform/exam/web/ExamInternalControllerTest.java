package com._202510007517.platform.exam.web;

import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.service.ExamApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExamInternalController.class)
@Import(com._202510007517.platform.exam.config.ExamServiceExceptionHandler.class)
class ExamInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExamApplicationService examApplicationService;

    @Test
    void getTeacherExamReturnsInternalDto() throws Exception {
        ExamDTO exam = new ExamDTO();
        exam.setId(9001L);
        exam.setTitle("期中考试");
        exam.setCourseId(2L);
        when(examApplicationService.getTeacherExam(7L, 9001L)).thenReturn(exam);

        mockMvc.perform(get("/internal/exams/9001/teacher")
                        .param("teacherId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9001))
                .andExpect(jsonPath("$.title").value("期中考试"))
                .andExpect(jsonPath("$.courseId").value(2));

        verify(examApplicationService).getTeacherExam(7L, 9001L);
    }

    @Test
    void listKnowledgePointIdsReturnsInternalArray() throws Exception {
        when(examApplicationService.listKnowledgePointIds(9001L)).thenReturn(List.of(701L, 702L));

        mockMvc.perform(get("/internal/exams/9001/knowledge-point-ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(701))
                .andExpect(jsonPath("$[1]").value(702));

        verify(examApplicationService).listKnowledgePointIds(9001L);
    }
}
