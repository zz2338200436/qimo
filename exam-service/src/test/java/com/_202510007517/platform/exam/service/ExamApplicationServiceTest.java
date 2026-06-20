package com._202510007517.platform.exam.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com._202510007517.platform.assignment.api.dto.AssignmentStudentScoreDTO;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.exam.api.dto.ExamDTO;
import com._202510007517.platform.exam.api.dto.ExamSubmissionDTO;
import com._202510007517.platform.exam.api.dto.ExamSubmitRequestDTO;
import com._202510007517.platform.exam.api.dto.StudentScoreDTO;
import com._202510007517.platform.exam.api.dto.StudentScoreListItemDTO;
import com._202510007517.platform.exam.api.dto.TeacherExamGradeRequestDTO;
import com._202510007517.platform.exam.api.dto.TeacherExamUpsertRequestDTO;
import com._202510007517.platform.exam.domain.ExamRecord;
import com._202510007517.platform.exam.domain.ExamQuestionRecord;
import com._202510007517.platform.exam.domain.ExamSubmissionRecord;
import com._202510007517.platform.exam.repository.ExamRepository;
import com._202510007517.platform.common.event.outbox.OutboxEventEntity;
import com._202510007517.platform.common.event.outbox.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ExamApplicationServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private CourseFeignClient courseFeignClient;

    @Mock
    private AssignmentFeignClient assignmentFeignClient;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private ExamApplicationService examApplicationService;

    private ExamRecord openExam;

    @BeforeEach
    void setUp() {
        examApplicationService = new ExamApplicationService(
                examRepository,
                courseFeignClient,
                assignmentFeignClient,
                outboxEventRepository,
                objectMapper);
        openExam = new ExamRecord();
        openExam.setId(9001L);
        openExam.setTitle("StudentExamSmoke-Open");
        openExam.setDescription("student exams smoke data");
        openExam.setCourseId(2L);
        openExam.setTeacherId(7L);
        openExam.setStartTime("2026-05-15 09:00:00");
        openExam.setEndTime("2026-05-20 09:00:00");
        openExam.setPublishDate("2026-05-14 09:00:00");
        openExam.setDuration(90);
        openExam.setTotalScore(100);
        openExam.setActive(true);
        openExam.setOnline(true);
        openExam.setLocation("线上考试");
    }

    @Test
    void listStudentExamsUsesClassIdsFromCourseServiceAndEnrichesCourseName() {
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of(2L));
        when(examRepository.findByClassIds(List.of(2L))).thenReturn(List.of(openExam));
        when(examRepository.findSubmission(9001L, 42L)).thenReturn(Optional.empty());
        when(courseFeignClient.getCourse(2L)).thenReturn(courseDto(2L, "CourseSmokeA"));

        Map<String, Object> page = examApplicationService.listStudentExams(42L, 1, 10, "startTime", "DESC", null, null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0)).containsEntry("courseName", "CourseSmokeA");
        assertThat(content.get(0)).containsEntry("id", 9001L);
        assertThat(content.get(0)).containsEntry("totalScore", 100);
    }

    @Test
    void getStudentExamDetailIncludesSubmissionWhenPresent() {
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of(2L));
        when(examRepository.isExamVisibleToClasses(9001L, List.of(2L))).thenReturn(true);
        ExamSubmissionRecord submission = new ExamSubmissionRecord();
        submission.setId(9003L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        submission.setContent("{\"q1\":\"A\"}");
        submission.setSubmissionDate("2026-05-17 08:00:00");
        submission.setTimeTaken(35);
        submission.setGraded(false);

        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));
        when(courseFeignClient.getCourse(2L)).thenReturn(courseDto(2L, "CourseSmokeA"));
        when(examRepository.findSubmission(9001L, 42L)).thenReturn(Optional.of(submission));

        Map<String, Object> detail = examApplicationService.getStudentExamDetail(42L, 9001L);

        assertThat(detail).containsEntry("title", "StudentExamSmoke-Open");
        assertThat(detail).containsEntry("courseName", "CourseSmokeA");
        assertThat(detail).containsKey("submission");
    }

    @Test
    void submitUpsertsSerializedAnswers() {
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of(2L));
        when(examRepository.isExamVisibleToClasses(9001L, List.of(2L))).thenReturn(true);
        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));
        ExamSubmitRequestDTO request = new ExamSubmitRequestDTO();
        request.setStudentId(42L);
        request.setTimeTaken(48);
        Map<String, String> answers = new LinkedHashMap<>();
        answers.put("q1", "A");
        answers.put("essay", "Answer");
        request.setAnswers(answers);

        ExamSubmissionRecord saved = new ExamSubmissionRecord();
        saved.setId(9004L);
        saved.setExamId(9001L);
        saved.setStudentId(42L);
        saved.setSubmissionDate("2026-05-17 09:00:00");
        saved.setTimeTaken(48);
        saved.setGraded(false);
        when(examRepository.upsertSubmission(org.mockito.ArgumentMatchers.eq(9001L), org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.eq(48), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(saved);

        examApplicationService.submit(9001L, request);

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(examRepository).upsertSubmission(org.mockito.ArgumentMatchers.eq(9001L), org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.eq(48), contentCaptor.capture());
        assertThat(contentCaptor.getValue()).contains("\"q1\":\"A\"");
        assertThat(contentCaptor.getValue()).contains("\"essay\":\"Answer\"");
    }

    @Test
    void submitAutoGradesFullyObjectiveExam() {
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of(2L));
        when(examRepository.isExamVisibleToClasses(9001L, List.of(2L))).thenReturn(true);
        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));
        ExamSubmitRequestDTO request = new ExamSubmitRequestDTO();
        request.setStudentId(42L);
        request.setTimeTaken(48);
        Map<String, String> answers = new LinkedHashMap<>();
        answers.put("q1", "A");
        answers.put("q2", "false");
        request.setAnswers(answers);

        ExamSubmissionRecord saved = new ExamSubmissionRecord();
        saved.setId(9004L);
        saved.setExamId(9001L);
        saved.setStudentId(42L);
        saved.setSubmissionDate("2026-05-17 09:00:00");
        saved.setTimeTaken(48);
        saved.setGraded(false);
        ExamSubmissionRecord graded = new ExamSubmissionRecord();
        graded.setId(9004L);
        graded.setExamId(9001L);
        graded.setStudentId(42L);
        graded.setSubmissionDate("2026-05-17 09:00:00");
        graded.setTimeTaken(48);
        graded.setGraded(true);
        graded.setScore(15);
        graded.setTeacherComment("自动阅卷");

        when(examRepository.upsertSubmission(org.mockito.ArgumentMatchers.eq(9001L), org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.eq(48), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(saved);
        when(examRepository.findQuestionsByExamId(9001L)).thenReturn(List.of(
                questionRecord(1L, "single_choice", "Pick A", "A", 10, 1),
                questionRecord(2L, "true_false", "Is false", "false", 5, 2)
        ));
        when(examRepository.updateSubmissionGrade(9004L, 15, "自动阅卷")).thenReturn(1);
        when(examRepository.findSubmission(9001L, 42L)).thenReturn(Optional.of(graded));

        ExamSubmissionDTO result = examApplicationService.submit(9001L, request);

        assertThat(result.getGraded()).isTrue();
        assertThat(result.getScore()).isEqualTo(15);
        assertThat(result.getTeacherComment()).isEqualTo("自动阅卷");
        verify(examRepository).updateSubmissionGrade(9004L, 15, "自动阅卷");
        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().eventType()).isEqualTo("ExamFinishedEvent");
        assertThat(outboxCaptor.getValue().bindingName()).isEqualTo("exam.finished");
        assertThat(outboxCaptor.getValue().payload()).contains("\"submissionId\":9004");
        assertThat(outboxCaptor.getValue().payload()).contains("\"score\":15");
    }

    @Test
    void submitLeavesSubjectiveExamPendingForTeacherGrade() {
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of(2L));
        when(examRepository.isExamVisibleToClasses(9001L, List.of(2L))).thenReturn(true);
        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));
        ExamSubmitRequestDTO request = new ExamSubmitRequestDTO();
        request.setStudentId(42L);
        request.setTimeTaken(48);
        Map<String, String> answers = new LinkedHashMap<>();
        answers.put("q1", "A");
        answers.put("essay", "Answer");
        request.setAnswers(answers);

        ExamSubmissionRecord saved = new ExamSubmissionRecord();
        saved.setId(9004L);
        saved.setExamId(9001L);
        saved.setStudentId(42L);
        saved.setSubmissionDate("2026-05-17 09:00:00");
        saved.setTimeTaken(48);
        saved.setGraded(false);

        when(examRepository.upsertSubmission(org.mockito.ArgumentMatchers.eq(9001L), org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.eq(48), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(saved);
        when(examRepository.findQuestionsByExamId(9001L)).thenReturn(List.of(
                questionRecord(1L, "single_choice", "Pick A", "A", 10, 1),
                questionRecord(2L, "essay", "Explain CAP", "CAP", 20, 2)
        ));

        ExamSubmissionDTO result = examApplicationService.submit(9001L, request);

        assertThat(result.getGraded()).isFalse();
        assertThat(result.getScore()).isNull();
        verify(examRepository, times(0)).updateSubmissionGrade(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.<Integer>any(),
                org.mockito.ArgumentMatchers.anyString());
        verify(outboxEventRepository, times(0)).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getStudentExamDetailRejectsStudentOutsideExamClasses() {
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of(5L));
        when(examRepository.isExamVisibleToClasses(9001L, List.of(5L))).thenReturn(false);

        assertThatThrownBy(() -> examApplicationService.getStudentExamDetail(42L, 9001L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("考试不存在或无权访问");
    }

    @Test
    void submitRejectsStudentOutsideExamClasses() {
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of(5L));
        when(examRepository.isExamVisibleToClasses(9001L, List.of(5L))).thenReturn(false);

        ExamSubmitRequestDTO request = new ExamSubmitRequestDTO();
        request.setStudentId(42L);
        request.setAnswers(Map.of("q1", "A"));

        assertThatThrownBy(() -> examApplicationService.submit(9001L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("考试不存在或无权访问");
    }

    @Test
    void listStudentScoresAggregatesAssignmentAndExamScoresNewestFirst() {
        AssignmentStudentScoreDTO assignmentScore = new AssignmentStudentScoreDTO();
        assignmentScore.setType("assignment");
        assignmentScore.setRelatedId(2001L);
        assignmentScore.setTitle("Homework 1");
        assignmentScore.setCourseName("分布式框架技术");
        assignmentScore.setCompletedAt("2026-09-01 10:00:00");
        assignmentScore.setSubmitDate("2026-09-01 10:00:00");
        assignmentScore.setScore(95);
        assignmentScore.setTotalScore(100);

        StudentScoreDTO examScore = new StudentScoreDTO();
        examScore.setType("exam");
        examScore.setRelatedId(9002L);
        examScore.setTitle("StudentExamSmoke-Graded");
        examScore.setCourseId(2L);
        examScore.setCompletedAt("2026-09-02 10:00:00");
        examScore.setScore(89);
        examScore.setTotalScore(100);

        when(assignmentFeignClient.listStudentScores(42L)).thenReturn(List.of(assignmentScore));
        when(examRepository.findStudentExamScores(42L)).thenReturn(List.of(examScore));
        when(courseFeignClient.getCourse(2L)).thenReturn(courseDto(2L, "分布式框架技术"));

        List<StudentScoreListItemDTO> scores = examApplicationService.listStudentScores(42L);

        assertThat(scores).hasSize(2);
        assertThat(scores.get(0).getType()).isEqualTo("exam");
        assertThat(scores.get(0).getTitle()).isEqualTo("StudentExamSmoke-Graded");
        assertThat(scores.get(0).getSubmitDate()).isEqualTo("2026-09-02 10:00:00");
        assertThat(scores.get(0).getCourseName()).isEqualTo("分布式框架技术");
        assertThat(scores.get(1).getType()).isEqualTo("assignment");
        assertThat(scores.get(1).getTitle()).isEqualTo("Homework 1");
        assertThat(scores.get(1).getCourseName()).isEqualTo("分布式框架技术");
        assertThat(scores.get(1).getScore()).isEqualTo(95);
    }

    @Test
    void listTeacherExamsFiltersByTeacherAndEnrichesCounts() {
        ExamRecord exam = new ExamRecord();
        exam.setId(9001L);
        exam.setTitle("TeacherExamCrudSmoke");
        exam.setCourseId(2L);
        exam.setTeacherId(7L);
        exam.setStartTime("2026-05-20 09:00:00");
        exam.setEndTime("2026-05-20 10:30:00");
        exam.setPublishDate("2026-05-19 09:00:00");
        exam.setDuration(90);
        exam.setActive(true);
        exam.setOnline(true);
        exam.setLocation("");

        TeacherClassDTO classDto = new TeacherClassDTO();
        classDto.setId(2L);
        classDto.setStudentCount(2);
        classDto.setCourseName("CourseSmokeA");

        when(examRepository.findByTeacherId(7L)).thenReturn(List.of(exam));
        when(courseFeignClient.getCourse(2L)).thenReturn(courseDto(2L, "CourseSmokeA"));
        when(courseFeignClient.listTeacherClasses(7L, null, null, null, null, 2L)).thenReturn(List.of(classDto));
        when(examRepository.countSubmissionsByExamId(9001L)).thenReturn(1);

        Map<String, Object> page = examApplicationService.listTeacherExams(7L, 1, 10, "id", "DESC", null, null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0)).containsEntry("courseName", "CourseSmokeA");
        assertThat(content.get(0)).containsEntry("submittedCount", 1);
        assertThat(content.get(0)).containsEntry("totalStudents", 2);
    }

    @Test
    void listTeacherExamsAcceptsDatetimeLocalValuesWhenResolvingStatus() {
        ExamRecord exam = new ExamRecord();
        exam.setId(9001L);
        exam.setTitle("TeacherExamDatetimeLocal");
        exam.setCourseId(2L);
        exam.setTeacherId(7L);
        exam.setStartTime("2026-06-11T09:43:00");
        exam.setEndTime("2026-06-11T11:13:00");
        exam.setPublishDate("2026-06-11T09:40:00");
        exam.setDuration(90);
        exam.setActive(true);
        exam.setOnline(true);
        exam.setLocation("");

        when(examRepository.findByTeacherId(7L)).thenReturn(List.of(exam));
        when(courseFeignClient.getCourse(2L)).thenReturn(courseDto(2L, "CourseSmokeA"));
        when(courseFeignClient.listTeacherClasses(7L, null, null, null, null, 2L)).thenReturn(List.of());
        when(examRepository.countSubmissionsByExamId(9001L)).thenReturn(0);

        Map<String, Object> page = examApplicationService.listTeacherExams(7L, 1, 10, "id", "DESC", null, null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0)).containsEntry("startTime", "2026-06-11T09:43:00");
    }

    @Test
    void getTeacherExamReturnsInternalDtoForOwnedExam() {
        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));
        when(courseFeignClient.getCourse(2L)).thenReturn(courseDto(2L, "CourseSmokeA"));

        ExamDTO result = examApplicationService.getTeacherExam(7L, 9001L);

        assertThat(result.getId()).isEqualTo(9001L);
        assertThat(result.getTitle()).isEqualTo("StudentExamSmoke-Open");
        assertThat(result.getCourseId()).isEqualTo(2L);
        assertThat(result.getCourseName()).isEqualTo("CourseSmokeA");
    }

    @Test
    void listKnowledgePointIdsMergesExamAndQuestionMappings() {
        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));
        when(examRepository.findKnowledgePointIdsByExamId(9001L)).thenReturn(List.of(501L, 502L));
        when(examRepository.findQuestionsByExamId(9001L)).thenReturn(List.of(
                questionRecord(1L, "single_choice", "Pick A", "A", 10, 1, 502L),
                questionRecord(2L, "essay", "Explain CAP", "CAP", 20, 2, 503L),
                questionRecord(3L, "essay", "No mapping", "None", 10, 3, null)
        ));

        List<Long> knowledgePointIds = examApplicationService.listKnowledgePointIds(9001L);

        assertThat(knowledgePointIds).containsExactly(501L, 502L, 503L);
    }

    @Test
    void getTeacherExamRejectsUnownedExam() {
        openExam.setTeacherId(99L);
        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));

        assertThatThrownBy(() -> examApplicationService.getTeacherExam(7L, 9001L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("考试不存在");
    }

    @Test
    void createTeacherExamPersistsRecordAndExamClasses() {
        TeacherExamUpsertRequestDTO request = new TeacherExamUpsertRequestDTO();
        request.setTitle("TeacherExamCrudSmoke");
        request.setDescription("teacher exam smoke");
        request.setCourseId(2L);
        request.setStartTime("2026-05-20T01:00:00.000Z");
        request.setEndTime("2026-05-20T02:30:00.000Z");
        request.setPublishDate("2026-05-19T01:00:00.000Z");
        request.setDuration(90L);
        request.setIsActive(true);
        request.setIsOnline(true);
        request.setLocation("");

        TeacherClassDTO classDto = new TeacherClassDTO();
        classDto.setId(2L);

        when(courseFeignClient.getCourse(2L)).thenReturn(courseDto(2L, "CourseSmokeA"));
        when(examRepository.insert(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            ExamRecord record = invocation.getArgument(0);
            record.setId(9005L);
            return record;
        });
        when(courseFeignClient.listTeacherClasses(7L, null, null, null, null, 2L)).thenReturn(List.of(classDto));

        ExamRecord created = examApplicationService.createTeacherExam(7L, request);

        assertThat(created.getId()).isEqualTo(9005L);
        verify(examRepository).replaceExamClasses(9005L, Set.of(2L));
    }

    @Test
    void updateTeacherExamAcceptsDatetimeLocalValuesFromTeacherForm() {
        TeacherExamUpsertRequestDTO request = new TeacherExamUpsertRequestDTO();
        request.setTitle("TeacherExamCrudSmokeUpdated");
        request.setDescription("teacher exam updated");
        request.setCourseId(2L);
        request.setStartTime("2026-06-11T09:43:00");
        request.setEndTime("2026-06-11T11:13:00");
        request.setPublishDate("2026-06-11T09:40:00");
        request.setDuration(90L);
        request.setIsActive(true);
        request.setIsOnline(true);
        request.setLocation("");

        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));
        when(courseFeignClient.getCourse(2L)).thenReturn(courseDto(2L, "CourseSmokeA"));
        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam), Optional.of(openExam));

        examApplicationService.updateTeacherExam(7L, 9001L, request);

        ArgumentCaptor<ExamRecord> updateCaptor = ArgumentCaptor.forClass(ExamRecord.class);
        verify(examRepository).update(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getStartTime()).isEqualTo("2026-06-11 09:43:00");
        assertThat(updateCaptor.getValue().getEndTime()).isEqualTo("2026-06-11 11:13:00");
    }

    @Test
    void createTeacherExamPersistsQuestionsInExamServiceSchema() {
        TeacherExamUpsertRequestDTO request = new TeacherExamUpsertRequestDTO();
        request.setTitle("TeacherExamWithQuestions");
        request.setDescription("teacher exam with questions");
        request.setCourseId(2L);
        request.setStartTime("2026-05-20T01:00:00.000Z");
        request.setEndTime("2026-05-20T02:30:00.000Z");
        request.setPublishDate("2026-05-19T01:00:00.000Z");
        request.setDuration(90L);
        request.setIsActive(true);
        request.setIsOnline(true);
        request.setQuestions(List.of(
                questionRequest("single_choice", "Pick A", List.of("A", "B"), "A", 10, 100L),
                questionRequest("essay", "Explain CAP", null, "Consistency, availability, partition tolerance", 20, 99L)
        ));

        when(courseFeignClient.getCourse(2L)).thenReturn(courseDto(2L, "CourseSmokeA"));
        when(examRepository.insert(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            ExamRecord record = invocation.getArgument(0);
            record.setId(9006L);
            return record;
        });

        examApplicationService.createTeacherExam(7L, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExamQuestionRecord>> questionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(examRepository).replaceExamQuestions(org.mockito.ArgumentMatchers.eq(9006L), questionsCaptor.capture());
        assertThat(questionsCaptor.getValue())
                .extracting(row -> row.questionType() + ":" + row.questionText() + ":" + row.score() + ":" + row.sortOrder())
                .containsExactly(
                        "single_choice:Pick A:10:1",
                        "essay:Explain CAP:20:2");
        assertThat(questionsCaptor.getValue().get(0).optionsJson()).contains("\"A\"");
    }

    @Test
    void listTeacherExamSubmissionsReturnsTeacherVisibleRows() {
        ExamSubmissionRecord submission = new ExamSubmissionRecord();
        submission.setId(9101L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        submission.setStudentName("Student Forty Two");
        submission.setContent("answer");
        submission.setSubmissionDate("2026-05-17 08:00:00");
        submission.setTimeTaken(35);
        submission.setGraded(false);

        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));
        when(examRepository.findSubmissionsByExamId(9001L)).thenReturn(List.of(submission));

        List<ExamSubmissionRecord> submissions = examApplicationService.listTeacherExamSubmissions(7L, 9001L);

        assertThat(submissions).hasSize(1);
        assertThat(submissions.get(0).getStudentName()).isEqualTo("Student Forty Two");
    }

    @Test
    void listTeacherExamSubmissionsReturnsLegacyPagedEnvelopeWithFilters() {
        ExamSubmissionRecord submission = new ExamSubmissionRecord();
        submission.setId(9101L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        submission.setStudentName("Student Forty Two");
        submission.setExamTitle("StudentExamSmoke-Open");
        submission.setContent("answer");
        submission.setSubmissionDate("2026-05-17 08:00:00");
        submission.setTimeTaken(35);
        submission.setGraded(false);

        when(examRepository.findSubmissionsByTeacherId(7L, 9001L, 42L, false, "id", "DESC", 0, 10))
                .thenReturn(List.of(submission));
        when(examRepository.countSubmissionsByTeacherId(7L, 9001L, 42L, false)).thenReturn(1);

        Map<String, Object> page = examApplicationService.listTeacherExamSubmissions(
                7L,
                1,
                10,
                "id",
                "DESC",
                9001L,
                42L,
                false);

        assertThat(page).containsEntry("total", 1);
        assertThat(page).containsEntry("page", 1);
        assertThat(page).containsEntry("size", 10);
        assertThat(page).containsEntry("pages", 1);
        @SuppressWarnings("unchecked")
        List<ExamSubmissionRecord> submissions = (List<ExamSubmissionRecord>) page.get("submissions");
        assertThat(submissions).hasSize(1);
        assertThat(submissions.get(0).getExamTitle()).isEqualTo("StudentExamSmoke-Open");
    }

    @Test
    void updateTeacherExamSubmissionRequiresTeacherOwnedExamAndPersistsRecord() {
        ExamSubmissionRecord existing = new ExamSubmissionRecord();
        existing.setId(9101L);
        existing.setExamId(9001L);
        existing.setStudentId(42L);
        existing.setContent("{\"q1\":\"A\"}");
        existing.setSubmissionDate("2026-05-17 08:00:00");
        existing.setTimeTaken(35);
        existing.setGraded(false);

        ExamSubmissionRecord request = new ExamSubmissionRecord();
        request.setExamId(9001L);
        request.setStudentId(42L);
        request.setContent("{\"q1\":\"B\"}");
        request.setTimeTaken(41);
        request.setScore(88);
        request.setTeacherComment("manual adjustment");
        request.setGraded(true);

        ExamSubmissionRecord updated = new ExamSubmissionRecord();
        updated.setId(9101L);
        updated.setExamId(9001L);
        updated.setStudentId(42L);
        updated.setContent("{\"q1\":\"B\"}");
        updated.setTimeTaken(41);
        updated.setScore(88);
        updated.setTeacherComment("manual adjustment");
        updated.setGraded(true);

        when(examRepository.findSubmissionById(9101L)).thenReturn(Optional.of(existing), Optional.of(updated));
        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));
        when(examRepository.updateSubmission(org.mockito.ArgumentMatchers.eq(9101L), org.mockito.ArgumentMatchers.any())).thenReturn(1);

        ExamSubmissionRecord result = examApplicationService.updateTeacherExamSubmission(7L, 9101L, request);

        assertThat(result.getScore()).isEqualTo(88);
        assertThat(result.getGraded()).isTrue();
        ArgumentCaptor<ExamSubmissionRecord> updateCaptor = ArgumentCaptor.forClass(ExamSubmissionRecord.class);
        verify(examRepository).updateSubmission(org.mockito.ArgumentMatchers.eq(9101L), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getContent()).isEqualTo("{\"q1\":\"B\"}");
        assertThat(updateCaptor.getValue().getTimeTaken()).isEqualTo(41);
        assertThat(updateCaptor.getValue().getScore()).isEqualTo(88);
        assertThat(updateCaptor.getValue().getTeacherComment()).isEqualTo("manual adjustment");
    }

    @Test
    void deleteTeacherExamSubmissionRequiresTeacherOwnedExam() {
        ExamSubmissionRecord existing = new ExamSubmissionRecord();
        existing.setId(9101L);
        existing.setExamId(9001L);
        existing.setStudentId(42L);

        when(examRepository.findSubmissionById(9101L)).thenReturn(Optional.of(existing));
        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));

        examApplicationService.deleteTeacherExamSubmission(7L, 9101L);

        verify(examRepository).deleteSubmission(9101L);
    }

    @Test
    void gradeTeacherExamSubmissionMarksSubmissionGraded() {
        ExamSubmissionRecord submission = new ExamSubmissionRecord();
        submission.setId(9101L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        submission.setContent("answer");
        submission.setGraded(false);

        TeacherExamGradeRequestDTO request = new TeacherExamGradeRequestDTO();
        request.setScore(91);
        request.setTeacherComment("graded");

        when(examRepository.findSubmissionById(9101L)).thenReturn(Optional.of(submission));
        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));
        when(examRepository.updateSubmissionGrade(9101L, 91, "graded")).thenReturn(1);
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of(6001L));
        ExamSubmissionRecord graded = new ExamSubmissionRecord();
        graded.setId(9101L);
        graded.setExamId(9001L);
        graded.setStudentId(42L);
        graded.setScore(91);
        graded.setTeacherComment("graded");
        graded.setGraded(true);
        when(examRepository.findSubmissionById(9101L)).thenReturn(Optional.of(submission), Optional.of(graded));

        ExamSubmissionRecord result = examApplicationService.gradeTeacherExamSubmission(7L, 9101L, request);

        assertThat(result.getScore()).isEqualTo(91);
        assertThat(result.getGraded()).isTrue();
        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEventEntity outboxEvent = outboxCaptor.getValue();
        assertThat(outboxEvent.eventType()).isEqualTo("ExamFinishedEvent");
        assertThat(outboxEvent.bindingName()).isEqualTo("exam.finished");
        assertThat(outboxEvent.eventId()).startsWith("exam-finished-9101-91-");
        assertThat(outboxEvent.eventId()).isNotEqualTo("exam-finished-9101-91");
        assertThat(outboxEvent.payload()).contains("\"examId\":9001");
        assertThat(outboxEvent.payload()).contains("\"submissionId\":9101");
        assertThat(outboxEvent.payload()).contains("\"studentId\":42");
        assertThat(outboxEvent.payload()).contains("\"score\":91");
    }

    @Test
    void regradeExamSubmissionGeneratesDistinctOutboxEventId() {
        ExamSubmissionRecord submission = new ExamSubmissionRecord();
        submission.setId(9101L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        submission.setContent("answer");
        submission.setGraded(true);
        submission.setScore(91);
        submission.setTeacherComment("graded");

        TeacherExamGradeRequestDTO request = new TeacherExamGradeRequestDTO();
        request.setScore(94);
        request.setTeacherComment("regraded");

        when(examRepository.findSubmissionById(9101L)).thenReturn(Optional.of(submission));
        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));
        when(examRepository.updateSubmissionGrade(9101L, 94, "regraded")).thenReturn(1);
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of(6001L));
        ExamSubmissionRecord regraded = new ExamSubmissionRecord();
        regraded.setId(9101L);
        regraded.setExamId(9001L);
        regraded.setStudentId(42L);
        regraded.setScore(94);
        regraded.setTeacherComment("regraded");
        regraded.setGraded(true);
        regraded.setSubmissionDate("2026-05-19 10:15:50");
        when(examRepository.findSubmissionById(9101L)).thenReturn(Optional.of(submission), Optional.of(regraded));

        examApplicationService.gradeTeacherExamSubmission(7L, 9101L, request);

        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository, times(1)).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().eventId()).startsWith("exam-finished-9101-94-");
        assertThat(outboxCaptor.getValue().eventId()).isNotEqualTo("exam-finished-9101-94");
        assertThat(outboxCaptor.getValue().payload()).contains("\"score\":94");
    }

    @Test
    void regradeExamSubmissionWithSameScoreButChangedCommentGeneratesDistinctOutboxEventId() {
        ExamSubmissionRecord submission = new ExamSubmissionRecord();
        submission.setId(9101L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        submission.setContent("answer");
        submission.setGraded(true);
        submission.setScore(94);
        submission.setTeacherComment("first comment");

        TeacherExamGradeRequestDTO request = new TeacherExamGradeRequestDTO();
        request.setScore(94);
        request.setTeacherComment("second comment");

        when(examRepository.findSubmissionById(9101L)).thenReturn(Optional.of(submission));
        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));
        when(examRepository.updateSubmissionGrade(9101L, 94, "second comment")).thenReturn(1);
        when(courseFeignClient.listStudentClassIds(42L)).thenReturn(List.of(6001L));
        ExamSubmissionRecord regraded = new ExamSubmissionRecord();
        regraded.setId(9101L);
        regraded.setExamId(9001L);
        regraded.setStudentId(42L);
        regraded.setScore(94);
        regraded.setTeacherComment("second comment");
        regraded.setGraded(true);
        regraded.setSubmissionDate("2026-05-19 10:15:50");
        when(examRepository.findSubmissionById(9101L)).thenReturn(Optional.of(submission), Optional.of(regraded));

        examApplicationService.gradeTeacherExamSubmission(7L, 9101L, request);

        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxEventRepository, times(1)).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().eventId()).startsWith("exam-finished-9101-94-");
        assertThat(outboxCaptor.getValue().eventId()).isNotEqualTo("exam-finished-9101-94");
        assertThat(outboxCaptor.getValue().payload()).contains("\"score\":94");
    }

    @Test
    void regradeExamSubmissionWithSameScoreAndCommentDoesNotEmitDuplicateEvent() {
        ExamSubmissionRecord submission = new ExamSubmissionRecord();
        submission.setId(9101L);
        submission.setExamId(9001L);
        submission.setStudentId(42L);
        submission.setContent("answer");
        submission.setGraded(true);
        submission.setScore(91);
        submission.setTeacherComment("graded");

        TeacherExamGradeRequestDTO request = new TeacherExamGradeRequestDTO();
        request.setScore(91);
        request.setTeacherComment("graded");

        when(examRepository.findSubmissionById(9101L)).thenReturn(Optional.of(submission), Optional.of(submission));
        when(examRepository.findExam(9001L)).thenReturn(Optional.of(openExam));
        when(examRepository.updateSubmissionGrade(9101L, 91, "graded")).thenReturn(1);

        ExamSubmissionRecord result = examApplicationService.gradeTeacherExamSubmission(7L, 9101L, request);

        assertThat(result.getScore()).isEqualTo(91);
        verify(outboxEventRepository, times(0)).save(org.mockito.ArgumentMatchers.any());
    }

    private static CourseDTO courseDto(Long id, String courseName) {
        CourseDTO dto = new CourseDTO();
        dto.setId(id);
        dto.setCourseName(courseName);
        return dto;
    }

    private static TeacherExamUpsertRequestDTO.QuestionRequest questionRequest(
            String questionType,
            String questionText,
            List<String> options,
            String correctAnswer,
            Integer score,
            Long knowledgePointId) {
        TeacherExamUpsertRequestDTO.QuestionRequest request = new TeacherExamUpsertRequestDTO.QuestionRequest();
        request.setQuestionType(questionType);
        request.setQuestionText(questionText);
        request.setOptions(options);
        request.setCorrectAnswer(correctAnswer);
        request.setScore(score);
        request.setKnowledgePointId(knowledgePointId);
        return request;
    }

    private static ExamQuestionRecord questionRecord(
            Long id,
            String questionType,
            String questionText,
            String correctAnswer,
            Integer score,
            Integer sortOrder) {
        return questionRecord(id, questionType, questionText, correctAnswer, score, sortOrder, null);
    }

    private static ExamQuestionRecord questionRecord(
            Long id,
            String questionType,
            String questionText,
            String correctAnswer,
            Integer score,
            Integer sortOrder,
            Long knowledgePointId) {
        return new ExamQuestionRecord(
                id,
                9001L,
                questionText,
                questionType,
                null,
                correctAnswer,
                score,
                knowledgePointId,
                sortOrder);
    }
}
