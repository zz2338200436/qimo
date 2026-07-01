package com._202510007517.platform.exam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com._202510007517.platform.assignment.api.dto.AssignmentStudentScoreDTO;
import com._202510007517.platform.assignment.api.feign.AssignmentFeignClient;
import com._202510007517.platform.common.event.outbox.OutboxEventEntity;
import com._202510007517.platform.common.event.outbox.OutboxEventRepository;
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
import com._202510007517.platform.course.api.dto.CourseDTO;
import com._202510007517.platform.course.api.dto.TeacherClassDTO;
import com._202510007517.platform.course.api.feign.CourseFeignClient;
import com._202510007517.platform.events.EventAggregate;
import com._202510007517.platform.events.exam.ExamFinishedEvent;
import com._202510007517.platform.events.exam.ExamFinishedPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ExamApplicationService {

    private final ExamRepository examRepository;
    private final CourseFeignClient courseFeignClient;
    private final AssignmentFeignClient assignmentFeignClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final AssessmentAttachmentService attachmentService;

    public ExamApplicationService(ExamRepository examRepository,
                                  CourseFeignClient courseFeignClient,
                                  AssignmentFeignClient assignmentFeignClient,
                                  OutboxEventRepository outboxEventRepository,
                                  ObjectMapper objectMapper) {
        this(examRepository,
                courseFeignClient,
                assignmentFeignClient,
                outboxEventRepository,
                objectMapper,
                AssessmentAttachmentService.none());
    }

    @Autowired
    public ExamApplicationService(ExamRepository examRepository,
                                  CourseFeignClient courseFeignClient,
                                  AssignmentFeignClient assignmentFeignClient,
                                  OutboxEventRepository outboxEventRepository,
                                  ObjectMapper objectMapper,
                                  AssessmentAttachmentService attachmentService) {
        this.examRepository = examRepository;
        this.courseFeignClient = courseFeignClient;
        this.assignmentFeignClient = assignmentFeignClient;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.attachmentService = attachmentService;
    }

    public Map<String, Object> listStudentExams(Long studentId,
                                                Integer page,
                                                Integer size,
                                                String sortBy,
                                                String order,
                                                Long courseId,
                                                Boolean submitted,
                                                Boolean isActive) {
        List<Long> classIds = loadStudentClassIds(studentId);
        List<ExamRecord> all = examRepository.findByClassIds(classIds);
        Map<Long, String> courseNames = loadCourseNames(extractCourseIds(all));
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (ExamRecord exam : all) {
            ExamSubmissionRecord submission = examRepository.findSubmission(exam.getId(), studentId).orElse(null);
            if (courseId != null && !courseId.equals(exam.getCourseId())) {
                continue;
            }
            if (isActive != null && !isActive.equals(exam.getActive())) {
                continue;
            }
            if (submitted != null) {
                boolean hasSubmission = submission != null;
                if (submitted != hasSubmission) {
                    continue;
                }
            }
            filtered.add(toStudentExamListItem(exam, submission, courseNames.get(exam.getCourseId())));
        }
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        int startIndex = Math.min((safePage - 1) * safeSize, filtered.size());
        int endIndex = Math.min(startIndex + safeSize, filtered.size());
        List<Map<String, Object>> pageContent = filtered.subList(startIndex, endIndex);
        return buildPageResponse(pageContent, safePage, safeSize, filtered.size());
    }

    public Map<String, Object> getStudentExamDetail(Long studentId, Long examId) {
        assertStudentCanAccessExam(studentId, examId);
        ExamRecord exam = examRepository.findExam(examId)
                .orElseThrow(() -> new IllegalArgumentException("考试不存在"));
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", exam.getId());
        detail.put("title", exam.getTitle());
        detail.put("description", exam.getDescription());
        detail.put("startTime", exam.getStartTime());
        detail.put("endTime", exam.getEndTime());
        detail.put("duration", exam.getDuration());
        detail.put("isActive", exam.getActive());
        detail.put("isOnline", exam.getOnline());
        detail.put("location", exam.getLocation());
        detail.put("publishDate", exam.getPublishDate());
        detail.put("courseId", exam.getCourseId());
        detail.put("attachments", attachmentService.getAttachmentDtos(examId));
        String courseName = loadCourseNames(Set.of(exam.getCourseId())).get(exam.getCourseId());
        if (courseName != null) {
            detail.put("courseName", courseName);
        }
        examRepository.findSubmission(examId, studentId).ifPresent(submission -> detail.put("submission", toSubmissionMap(submission)));
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamSubmissionDTO submit(Long examId, ExamSubmitRequestDTO request) {
        return submit(examId, request, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamSubmissionDTO submit(Long examId,
                                    ExamSubmitRequestDTO request,
                                    MultipartFile[] files) {
        assertStudentCanAccessExam(request.getStudentId(), examId);
        ExamRecord exam = examRepository.findExam(examId)
                .orElseThrow(() -> new IllegalArgumentException("考试不存在"));
        String contentJson = serializeAnswers(request.getAnswers());
        ExamSubmissionRecord saved = examRepository.upsertSubmission(examId, request.getStudentId(), request.getTimeTaken(), contentJson);
        try {
            attachmentService.saveSubmissionAttachments(saved.getId(), request.getStudentId(), files);
        } catch (IOException ex) {
            throw new IllegalStateException("保存考试提交附件失败", ex);
        }
        ExamSubmissionRecord finalSubmission = autoGradeIfPossible(exam, saved, request.getAnswers());
        ExamSubmissionDTO dto = toSubmissionDto(finalSubmission);
        dto.setAttachments(attachmentService.getSubmissionAttachmentDtos(finalSubmission.getId()));
        return dto;
    }

    public List<StudentScoreListItemDTO> listStudentScores(Long studentId) {
        List<StudentScoreListItemDTO> scores = new ArrayList<>();
        for (AssignmentStudentScoreDTO assignmentScore : loadAssignmentScores(studentId)) {
            scores.add(toAssignmentScoreItem(assignmentScore));
        }
        List<StudentScoreDTO> examScores = examRepository.findStudentExamScores(studentId);
        Map<Long, String> courseNames = loadCourseNames(extractCourseIdsFromExamScores(examScores));
        for (StudentScoreDTO examScore : examScores) {
            scores.add(toExamScoreItem(examScore, courseNames.get(examScore.getCourseId())));
        }
        scores.sort(Comparator
                .comparing(ExamApplicationService::scoreSortKey)
                .reversed()
                .thenComparing(item -> item.getId() == null ? Long.MIN_VALUE : item.getId(), Comparator.reverseOrder()));
        return scores;
    }

    public List<StudentScoreDTO> listStudentExamScores(Long studentId) {
        return examRepository.findStudentExamScores(studentId);
    }

    public Map<String, Object> listTeacherExams(Long teacherId,
                                                Integer page,
                                                Integer size,
                                                String sortBy,
                                                String order,
                                                Long courseId,
                                                String status,
                                                Boolean isOnline) {
        List<ExamRecord> all = examRepository.findByTeacherId(teacherId);
        Map<Long, String> courseNames = loadCourseNames(extractCourseIds(all));
        Map<Long, Integer> submittedCounts = new LinkedHashMap<>();
        Map<Long, Integer> totalStudents = new LinkedHashMap<>();
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (ExamRecord exam : all) {
            if (courseId != null && !courseId.equals(exam.getCourseId())) {
                continue;
            }
            if (isOnline != null && !isOnline.equals(exam.getOnline())) {
                continue;
            }
            if (status != null && !status.isBlank() && !matchesTeacherExamStatus(exam, status)) {
                continue;
            }
            submittedCounts.computeIfAbsent(exam.getId(), examRepository::countSubmissionsByExamId);
            totalStudents.computeIfAbsent(exam.getId(), id -> loadTeacherExamStudentCount(teacherId, exam.getCourseId()));
            Map<String, Object> row = toTeacherExamListItem(
                    exam,
                    courseNames.get(exam.getCourseId()),
                    submittedCounts.getOrDefault(exam.getId(), 0),
                    totalStudents.getOrDefault(exam.getId(), 0)
            );
            filtered.add(row);
        }
        return buildPageResponse(filtered, safePage(page), safeSize(size), filtered.size());
    }

    public Map<String, Object> getTeacherExamDetail(Long teacherId, Long examId) {
        ExamRecord exam = examRepository.findExam(examId)
                .orElseThrow(() -> new IllegalArgumentException("考试不存在"));
        if (teacherId != null && exam.getTeacherId() != null && !teacherId.equals(exam.getTeacherId())) {
            throw new IllegalArgumentException("考试不存在");
        }
        Map<String, Object> detail = toTeacherExamDetail(exam, loadCourseNames(Set.of(exam.getCourseId())).get(exam.getCourseId()));
        detail.put("attachments", attachmentService.getAttachmentDtos(examId));
        return detail;
    }

    public ExamDTO getTeacherExam(Long teacherId, Long examId) {
        ExamRecord exam = examRepository.findExam(examId)
                .orElseThrow(() -> new IllegalArgumentException("考试不存在"));
        if (teacherId == null || (exam.getTeacherId() != null && !teacherId.equals(exam.getTeacherId()))) {
            throw new IllegalArgumentException("考试不存在");
        }
        String courseName = loadCourseNames(Set.of(exam.getCourseId())).get(exam.getCourseId());
        ExamDTO dto = toExamDto(exam, courseName);
        dto.setAttachments(attachmentService.getAttachmentDtos(examId));
        return dto;
    }

    public List<Long> listKnowledgePointIds(Long examId) {
        examRepository.findExam(examId)
                .orElseThrow(() -> new IllegalArgumentException("考试不存在"));
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        List<Long> examKnowledgePointIds = examRepository.findKnowledgePointIdsByExamId(examId);
        if (examKnowledgePointIds != null) {
            ids.addAll(examKnowledgePointIds.stream().filter(java.util.Objects::nonNull).toList());
        }
        for (ExamQuestionRecord question : examRepository.findQuestionsByExamId(examId)) {
            if (question.knowledgePointId() != null) {
                ids.add(question.knowledgePointId());
            }
        }
        return List.copyOf(ids);
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamRecord createTeacherExam(Long teacherId, TeacherExamUpsertRequestDTO request) {
        return createTeacherExam(teacherId, request, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamRecord createTeacherExam(Long teacherId,
                                        TeacherExamUpsertRequestDTO request,
                                        MultipartFile[] files) {
        validateTeacherCourseOwnership(teacherId, request.getCourseId());
        ExamRecord draft = toExamRecord(teacherId, request);
        ExamRecord created = examRepository.insert(draft);
        examRepository.replaceExamClasses(created.getId(), collectTeacherCourseClassIds(teacherId, request.getCourseId()));
        examRepository.replaceExamQuestions(created.getId(), toQuestionRecords(created.getId(), request));
        try {
            attachmentService.saveAttachments(created.getId(), teacherId, files);
        } catch (IOException ex) {
            throw new IllegalStateException("保存考试附件失败", ex);
        }
        return created;
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamRecord updateTeacherExam(Long teacherId, Long examId, TeacherExamUpsertRequestDTO request) {
        return updateTeacherExam(teacherId, examId, request, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamRecord updateTeacherExam(Long teacherId,
                                        Long examId,
                                        TeacherExamUpsertRequestDTO request,
                                        MultipartFile[] files) {
        ExamRecord existing = examRepository.findExam(examId)
                .orElseThrow(() -> new IllegalArgumentException("考试不存在"));
        if (existing.getTeacherId() != null && !teacherId.equals(existing.getTeacherId())) {
            throw new IllegalArgumentException("考试不存在");
        }
        validateTeacherCourseOwnership(teacherId, request.getCourseId());
        ExamRecord updated = toExamRecord(teacherId, request);
        updated.setId(examId);
        examRepository.update(updated);
        examRepository.replaceExamClasses(examId, collectTeacherCourseClassIds(teacherId, request.getCourseId()));
        examRepository.replaceExamQuestions(examId, toQuestionRecords(examId, request));
        try {
            attachmentService.saveAttachments(examId, teacherId, files);
        } catch (IOException ex) {
            throw new IllegalStateException("保存考试附件失败", ex);
        }
        return examRepository.findExam(examId).orElseThrow();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTeacherExam(Long teacherId, Long examId) {
        ExamRecord existing = examRepository.findExam(examId)
                .orElseThrow(() -> new IllegalArgumentException("考试不存在"));
        if (existing.getTeacherId() != null && !teacherId.equals(existing.getTeacherId())) {
            throw new IllegalArgumentException("考试不存在");
        }
        attachmentService.deleteAttachments(examId);
        examRepository.delete(examId);
    }

    public List<ExamSubmissionRecord> listTeacherExamSubmissions(Long teacherId, Long examId) {
        ExamRecord exam = examRepository.findExam(examId)
                .orElseThrow(() -> new IllegalArgumentException("考试不存在"));
        if (exam.getTeacherId() != null && !teacherId.equals(exam.getTeacherId())) {
            throw new IllegalArgumentException("考试不存在");
        }
        return withSubmissionAttachments(examRepository.findSubmissionsByExamId(examId));
    }

    public Map<String, Object> listTeacherExamSubmissions(Long teacherId,
                                                          Integer page,
                                                          Integer size,
                                                          String sortBy,
                                                          String order,
                                                          Long examId,
                                                          Long studentId,
                                                          Boolean graded) {
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        int offset = (safePage - 1) * safeSize;
        List<ExamSubmissionRecord> submissions = withSubmissionAttachments(examRepository.findSubmissionsByTeacherId(
                teacherId,
                examId,
                studentId,
                graded,
                sortBy,
                order,
                offset,
                safeSize));
        int total = examRepository.countSubmissionsByTeacherId(teacherId, examId, studentId, graded);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("submissions", submissions);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("pages", total > 0 ? (int) Math.ceil((double) total / safeSize) : 0);
        return result;
    }

    public ExamSubmissionRecord getTeacherExamSubmissionDetail(Long teacherId, Long submissionId) {
        ExamSubmissionRecord submission = examRepository.findSubmissionById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("考试提交记录不存在"));
        ExamRecord exam = examRepository.findExam(submission.getExamId())
                .orElseThrow(() -> new IllegalArgumentException("考试不存在"));
        if (exam.getTeacherId() != null && !teacherId.equals(exam.getTeacherId())) {
            throw new IllegalArgumentException("考试提交记录不存在");
        }
        return withSubmissionAttachments(submission);
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamSubmissionRecord updateTeacherExamSubmission(Long teacherId,
                                                            Long submissionId,
                                                            ExamSubmissionRecord request) {
        ExamSubmissionRecord existing = examRepository.findSubmissionById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("考试提交记录不存在"));
        ExamRecord existingExam = examRepository.findExam(existing.getExamId())
                .orElseThrow(() -> new IllegalArgumentException("考试不存在"));
        if (existingExam.getTeacherId() != null && !teacherId.equals(existingExam.getTeacherId())) {
            throw new IllegalArgumentException("考试提交记录不存在");
        }
        Long targetExamId = request.getExamId() == null ? existing.getExamId() : request.getExamId();
        ExamRecord targetExam = examRepository.findExam(targetExamId)
                .orElseThrow(() -> new IllegalArgumentException("考试不存在"));
        if (targetExam.getTeacherId() != null && !teacherId.equals(targetExam.getTeacherId())) {
            throw new IllegalArgumentException("考试提交记录不存在");
        }
        ExamSubmissionRecord update = new ExamSubmissionRecord();
        update.setExamId(targetExamId);
        update.setStudentId(request.getStudentId() == null ? existing.getStudentId() : request.getStudentId());
        update.setContent(request.getContent() == null ? existing.getContent() : request.getContent());
        update.setTimeTaken(request.getTimeTaken() == null ? existing.getTimeTaken() : request.getTimeTaken());
        update.setGraded(request.getGraded() == null ? existing.getGraded() : request.getGraded());
        update.setScore(request.getScore());
        update.setTeacherComment(request.getTeacherComment());
        examRepository.updateSubmission(submissionId, update);
        return withSubmissionAttachments(examRepository.findSubmissionById(submissionId).orElseThrow());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTeacherExamSubmission(Long teacherId, Long submissionId) {
        ExamSubmissionRecord submission = examRepository.findSubmissionById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("考试提交记录不存在"));
        ExamRecord exam = examRepository.findExam(submission.getExamId())
                .orElseThrow(() -> new IllegalArgumentException("考试不存在"));
        if (exam.getTeacherId() != null && !teacherId.equals(exam.getTeacherId())) {
            throw new IllegalArgumentException("考试提交记录不存在");
        }
        examRepository.deleteSubmission(submissionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamSubmissionRecord gradeTeacherExamSubmission(Long teacherId, Long submissionId, TeacherExamGradeRequestDTO request) {
        ExamSubmissionRecord submission = examRepository.findSubmissionById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("考试提交记录不存在"));
        ExamRecord exam = examRepository.findExam(submission.getExamId())
                .orElseThrow(() -> new IllegalArgumentException("考试不存在"));
        if (exam.getTeacherId() != null && !teacherId.equals(exam.getTeacherId())) {
            throw new IllegalArgumentException("考试提交记录不存在");
        }
        Integer previousScore = submission.getScore();
        String previousTeacherComment = submission.getTeacherComment();
        examRepository.updateSubmissionGrade(submissionId, request.getScore(), request.getTeacherComment());
        ExamSubmissionRecord gradedSubmission = examRepository.findSubmissionById(submissionId).orElseThrow();
        persistExamFinishedEvent(exam, submission, gradedSubmission, previousScore, previousTeacherComment);
        return withSubmissionAttachments(gradedSubmission);
    }

    private List<ExamSubmissionRecord> withSubmissionAttachments(List<ExamSubmissionRecord> submissions) {
        for (ExamSubmissionRecord submission : submissions) {
            withSubmissionAttachments(submission);
        }
        return submissions;
    }

    private ExamSubmissionRecord withSubmissionAttachments(ExamSubmissionRecord submission) {
        if (submission != null && submission.getId() != null) {
            submission.setAttachments(attachmentService.getSubmissionAttachmentDtos(submission.getId()));
        }
        return submission;
    }

    private List<Long> loadStudentClassIds(Long studentId) {
        if (studentId == null) {
            return List.of();
        }
        try {
            List<Long> classIds = courseFeignClient.listStudentClassIds(studentId);
            return classIds == null ? List.of() : classIds;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private void assertStudentCanAccessExam(Long studentId, Long examId) {
        List<Long> classIds = loadStudentClassIds(studentId);
        if (!examRepository.isExamVisibleToClasses(examId, classIds)) {
            throw new IllegalArgumentException("考试不存在或无权访问");
        }
    }

    private Map<Long, String> loadCourseNames(Set<Long> courseIds) {
        Map<Long, String> courseNames = new LinkedHashMap<>();
        for (Long courseId : courseIds) {
            if (courseId == null) {
                continue;
            }
            try {
                CourseDTO course = courseFeignClient.getCourse(courseId);
                if (course != null) {
                    courseNames.put(courseId, course.getCourseName());
                }
            } catch (RuntimeException ignored) {
                // Keep student exam APIs available even if enrichment fails.
            }
        }
        return courseNames;
    }

    private static Set<Long> extractCourseIds(List<ExamRecord> exams) {
        java.util.LinkedHashSet<Long> courseIds = new java.util.LinkedHashSet<>();
        for (ExamRecord exam : exams) {
            if (exam.getCourseId() != null) {
                courseIds.add(exam.getCourseId());
            }
        }
        return courseIds;
    }

    private int loadTeacherExamStudentCount(Long teacherId, Long courseId) {
        int total = 0;
        try {
            List<TeacherClassDTO> classes = courseFeignClient.listTeacherClasses(teacherId, null, null, null, null, courseId);
            if (classes == null) {
                return 0;
            }
            for (TeacherClassDTO clazz : classes) {
                if (clazz.getStudentCount() == null) {
                    continue;
                }
                total += clazz.getStudentCount();
            }
        } catch (RuntimeException ignored) {
            return 0;
        }
        return total;
    }

    private List<AssignmentStudentScoreDTO> loadAssignmentScores(Long studentId) {
        try {
            List<AssignmentStudentScoreDTO> scores = assignmentFeignClient.listStudentScores(studentId);
            return scores == null ? List.of() : scores;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private Set<Long> collectTeacherCourseClassIds(Long teacherId, Long courseId) {
        LinkedHashSet<Long> classIds = new LinkedHashSet<>();
        try {
            List<TeacherClassDTO> classes = courseFeignClient.listTeacherClasses(teacherId, null, null, null, null, courseId);
            if (classes != null) {
                for (TeacherClassDTO clazz : classes) {
                    if (clazz.getId() != null) {
                        classIds.add(clazz.getId());
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // no-op
        }
        return classIds;
    }

    private void validateTeacherCourseOwnership(Long teacherId, Long courseId) {
        CourseDTO course = courseFeignClient.getCourse(courseId);
        if (course == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        if (course.getTeacherId() != null && !teacherId.equals(course.getTeacherId())) {
            throw new IllegalArgumentException("课程不存在");
        }
    }

    private static ExamRecord toExamRecord(Long teacherId, TeacherExamUpsertRequestDTO request) {
        ExamRecord record = new ExamRecord();
        record.setTeacherId(teacherId);
        record.setTitle(request.getTitle());
        record.setDescription(request.getDescription());
        record.setCourseId(request.getCourseId());
        record.setStartTime(normalizeIsoDateTime(request.getStartTime()));
        record.setEndTime(normalizeIsoDateTime(request.getEndTime()));
        record.setPublishDate(normalizeIsoDateTime(request.getPublishDate()));
        record.setDuration(request.getDuration() == null ? null : request.getDuration().intValue());
        record.setTotalScore(100);
        record.setActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
        record.setOnline(request.getIsOnline() == null ? Boolean.TRUE : request.getIsOnline());
        record.setLocation(request.getLocation());
        return record;
    }

    private List<ExamQuestionRecord> toQuestionRecords(Long examId, TeacherExamUpsertRequestDTO request) {
        if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
            return List.of();
        }
        List<ExamQuestionRecord> questions = new ArrayList<>();
        int sortOrder = 1;
        for (TeacherExamUpsertRequestDTO.QuestionRequest question : request.getQuestions()) {
            questions.add(new ExamQuestionRecord(
                    null,
                    examId,
                    firstNonBlank(question.getQuestionText(), question.getContent()),
                    firstNonBlank(question.getQuestionType(), question.getType(), "unknown"),
                    serializeQuestionOptions(question.getOptions()),
                    firstNonBlank(question.getCorrectAnswer(), question.getAnswer()),
                    question.getScore() == null ? 0 : question.getScore(),
                    question.getKnowledgePointId(),
                    sortOrder++));
        }
        return questions;
    }

    private ExamSubmissionRecord autoGradeIfPossible(ExamRecord exam,
                                                     ExamSubmissionRecord saved,
                                                     Map<String, String> answers) {
        AutoGradeResult autoGrade = calculateAutoGrade(examRepository.findQuestionsByExamId(exam.getId()), answers);
        if (!autoGrade.graded() || saved.getId() == null) {
            return saved;
        }

        Integer previousScore = saved.getScore();
        String previousTeacherComment = saved.getTeacherComment();
        examRepository.updateSubmissionGrade(saved.getId(), autoGrade.score(), "自动阅卷");
        ExamSubmissionRecord graded = examRepository.findSubmission(exam.getId(), saved.getStudentId())
                .orElseGet(() -> {
                    saved.setGraded(true);
                    saved.setScore(autoGrade.score());
                    saved.setTeacherComment("自动阅卷");
                    return saved;
                });
        persistExamFinishedEvent(exam, saved, graded, previousScore, previousTeacherComment);
        return graded;
    }

    private static AutoGradeResult calculateAutoGrade(List<ExamQuestionRecord> questions, Map<String, String> answers) {
        if (questions == null || questions.isEmpty()) {
            return AutoGradeResult.pending();
        }
        int score = 0;
        for (ExamQuestionRecord question : questions) {
            if (!isObjectiveQuestion(question.questionType()) || isBlank(question.correctAnswer())) {
                return AutoGradeResult.pending();
            }
            String studentAnswer = answerForQuestion(question, answers);
            if (answersMatch(question.correctAnswer(), studentAnswer)) {
                score += question.score() == null ? 0 : question.score();
            }
        }
        return new AutoGradeResult(true, score);
    }

    private static boolean isObjectiveQuestion(String questionType) {
        if (questionType == null || questionType.isBlank()) {
            return false;
        }
        String normalized = questionType.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return normalized.contains("choice")
                || normalized.contains("true_false")
                || normalized.contains("judge")
                || normalized.contains("select")
                || questionType.contains("选择")
                || questionType.contains("判断");
    }

    private static String answerForQuestion(ExamQuestionRecord question, Map<String, String> answers) {
        if (answers == null || answers.isEmpty()) {
            return null;
        }
        for (String key : answerKeys(question)) {
            if (answers.containsKey(key)) {
                return answers.get(key);
            }
        }
        return null;
    }

    private static List<String> answerKeys(ExamQuestionRecord question) {
        List<String> keys = new ArrayList<>();
        if (question.id() != null) {
            keys.add(String.valueOf(question.id()));
            keys.add("q" + question.id());
        }
        if (question.sortOrder() != null) {
            keys.add(String.valueOf(question.sortOrder()));
            keys.add("q" + question.sortOrder());
        }
        return keys;
    }

    private static boolean answersMatch(String correctAnswer, String studentAnswer) {
        if (studentAnswer == null) {
            return false;
        }
        return normalizeAnswer(correctAnswer).equals(normalizeAnswer(studentAnswer));
    }

    private static String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.trim()
                .replace(" ", "")
                .replace("，", ",")
                .toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeIsoDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (!normalized.endsWith("Z") && !normalized.matches(".*[+-]\\d{2}:?\\d{2}$")) {
            String localDateTime = normalized.replace('T', ' ');
            if (localDateTime.length() == 16) {
                localDateTime += ":00";
            }
            return LocalDateTime.parse(localDateTime, formatter).format(formatter);
        }
        return formatter.withZone(ZoneOffset.UTC).format(Instant.parse(normalized));
    }

    private static boolean matchesTeacherExamStatus(ExamRecord exam, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        Instant now = Instant.now();
        Instant start = exam.getStartTime() == null ? null : parseExamDateTime(exam.getStartTime());
        Instant end = exam.getEndTime() == null ? null : parseExamDateTime(exam.getEndTime());
        return switch (status) {
            case "upcoming", "即将开始" -> start != null && start.isAfter(now);
            case "ongoing", "进行中" -> start != null && end != null && !start.isAfter(now) && !end.isBefore(now);
            case "completed", "已结束", "graded", "已评分" -> end != null && end.isBefore(now);
            default -> true;
        };
    }

    private static String resolveExamStatus(ExamRecord exam) {
        if (Boolean.FALSE.equals(exam.getActive())) {
            return "inactive";
        }
        Instant now = Instant.now();
        Instant start = exam.getStartTime() == null ? null : parseExamDateTime(exam.getStartTime());
        Instant end = exam.getEndTime() == null ? null : parseExamDateTime(exam.getEndTime());
        if (start != null && start.isAfter(now)) {
            return "upcoming";
        }
        if (start != null && end != null && !start.isAfter(now) && !end.isBefore(now)) {
            return "ongoing";
        }
        if (end != null && end.isBefore(now)) {
            return "completed";
        }
        return "scheduled";
    }

    private static Instant parseExamDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.endsWith("Z") || normalized.matches(".*[+-]\\d{2}:?\\d{2}$")) {
            return Instant.parse(normalized);
        }

        String localDateTime = normalized.replace('T', ' ');
        if (localDateTime.length() == 16) {
            localDateTime += ":00";
        }
        return LocalDateTime.parse(localDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }

    private static int safePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private static int safeSize(Integer size) {
        return size == null || size < 1 ? 10 : Math.min(size, 100);
    }

    private static Set<Long> extractCourseIdsFromExamScores(List<StudentScoreDTO> scores) {
        java.util.LinkedHashSet<Long> courseIds = new java.util.LinkedHashSet<>();
        for (StudentScoreDTO score : scores) {
            if (score.getCourseId() != null) {
                courseIds.add(score.getCourseId());
            }
        }
        return courseIds;
    }

    private Map<String, Object> toStudentExamListItem(ExamRecord exam,
                                                      ExamSubmissionRecord submission,
                                                      String courseName) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", exam.getId());
        item.put("title", exam.getTitle());
        item.put("description", exam.getDescription());
        item.put("startTime", exam.getStartTime());
        item.put("endTime", exam.getEndTime());
        item.put("duration", exam.getDuration());
        item.put("isActive", exam.getActive());
        item.put("isOnline", exam.getOnline());
        item.put("location", exam.getLocation());
        item.put("publishDate", exam.getPublishDate());
        item.put("courseId", exam.getCourseId());
        item.put("courseName", courseName);
        item.put("teacherId", exam.getTeacherId());
        item.put("totalScore", exam.getTotalScore());
        item.put("maxScore", exam.getTotalScore());
        item.put("attachments", attachmentService.getAttachmentDtos(exam.getId()));
        item.put("submission", submission == null ? null : toSubmissionMap(submission));
        return item;
    }

    private Map<String, Object> toSubmissionMap(ExamSubmissionRecord submission) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", submission.getId());
        map.put("submissionDate", submission.getSubmissionDate());
        map.put("timeTaken", submission.getTimeTaken());
        map.put("score", submission.getScore());
        map.put("teacherComment", submission.getTeacherComment());
        map.put("graded", submission.getGraded());
        map.put("content", submission.getContent());
        map.put("attachments", attachmentService.getSubmissionAttachmentDtos(submission.getId()));
        return map;
    }

    private static ExamSubmissionDTO toSubmissionDto(ExamSubmissionRecord submission) {
        ExamSubmissionDTO dto = new ExamSubmissionDTO();
        dto.setId(submission.getId());
        dto.setExamId(submission.getExamId());
        dto.setStudentId(submission.getStudentId());
        dto.setContent(submission.getContent());
        dto.setSubmissionDate(submission.getSubmissionDate());
        dto.setTimeTaken(submission.getTimeTaken());
        dto.setGraded(submission.getGraded());
        dto.setScore(submission.getScore());
        dto.setTeacherComment(submission.getTeacherComment());
        return dto;
    }

    private static ExamDTO toExamDto(ExamRecord exam, String courseName) {
        ExamDTO dto = new ExamDTO();
        dto.setId(exam.getId());
        dto.setTitle(exam.getTitle());
        dto.setDescription(exam.getDescription());
        dto.setCourseId(exam.getCourseId());
        dto.setCourseName(courseName);
        dto.setStartTime(exam.getStartTime());
        dto.setEndTime(exam.getEndTime());
        dto.setPublishDate(exam.getPublishDate());
        dto.setIsActive(exam.getActive());
        dto.setIsOnline(exam.getOnline());
        dto.setDuration(exam.getDuration());
        dto.setTotalScore(exam.getTotalScore());
        dto.setStatus(resolveExamStatus(exam));
        return dto;
    }

    private static StudentScoreListItemDTO toAssignmentScoreItem(AssignmentStudentScoreDTO score) {
        StudentScoreListItemDTO item = new StudentScoreListItemDTO();
        item.setId(score.getRelatedId());
        item.setCourse(score.getCourseName());
        item.setCourseName(score.getCourseName());
        item.setType(score.getType());
        item.setName(score.getTitle());
        item.setExamName(score.getTitle());
        item.setTitle(score.getTitle());
        item.setExamDate(score.getCompletedAt());
        item.setSubmitDate(score.getSubmitDate() != null ? score.getSubmitDate() : score.getCompletedAt());
        item.setSubmissionDate(score.getCompletedAt());
        item.setCompletedAt(score.getCompletedAt());
        item.setScore(score.getScore());
        item.setTotal(score.getTotalScore());
        item.setTotalScore(score.getTotalScore());
        item.setStatus(score.getScore() == null ? "submitted" : "graded");
        item.setRank(score.getRank());
        return item;
    }

    private static StudentScoreListItemDTO toExamScoreItem(StudentScoreDTO score, String courseName) {
        StudentScoreListItemDTO item = new StudentScoreListItemDTO();
        item.setId(score.getRelatedId());
        item.setCourse(courseName);
        item.setCourseName(courseName);
        item.setType(score.getType());
        item.setName(score.getTitle());
        item.setExamName(score.getTitle());
        item.setTitle(score.getTitle());
        item.setExamDate(score.getCompletedAt());
        item.setSubmitDate(score.getCompletedAt());
        item.setSubmissionDate(score.getCompletedAt());
        item.setCompletedAt(score.getCompletedAt());
        item.setScore(score.getScore());
        item.setTotal(score.getTotalScore());
        item.setTotalScore(score.getTotalScore());
        item.setStatus(score.getScore() == null ? "submitted" : "graded");
        item.setRank(score.getRank());
        return item;
    }

    private static Map<String, Object> toTeacherExamListItem(ExamRecord exam,
                                                             String courseName,
                                                             Integer submittedCount,
                                                             Integer totalStudents) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", exam.getId());
        item.put("title", exam.getTitle());
        item.put("description", exam.getDescription());
        item.put("courseId", exam.getCourseId());
        item.put("courseName", courseName);
        item.put("startTime", exam.getStartTime());
        item.put("endTime", exam.getEndTime());
        item.put("publishDate", exam.getPublishDate());
        item.put("isActive", exam.getActive());
        item.put("isOnline", exam.getOnline());
        item.put("location", exam.getLocation());
        item.put("duration", exam.getDuration());
        item.put("teacherId", exam.getTeacherId());
        item.put("submittedCount", submittedCount);
        item.put("totalStudents", totalStudents);
        item.put("maxScore", exam.getTotalScore());
        item.put("totalScore", exam.getTotalScore());
        return item;
    }

    private static Map<String, Object> toTeacherExamDetail(ExamRecord exam, String courseName) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", exam.getId());
        detail.put("title", exam.getTitle());
        detail.put("description", exam.getDescription());
        detail.put("courseId", exam.getCourseId());
        detail.put("courseName", courseName);
        detail.put("teacherId", exam.getTeacherId());
        detail.put("teacherName", "课程教师");
        detail.put("startTime", exam.getStartTime());
        detail.put("endTime", exam.getEndTime());
        detail.put("publishDate", exam.getPublishDate());
        detail.put("duration", exam.getDuration());
        detail.put("isActive", exam.getActive());
        detail.put("isOnline", exam.getOnline());
        detail.put("location", exam.getLocation());
        detail.put("maxScore", exam.getTotalScore());
        detail.put("totalScore", exam.getTotalScore());
        detail.put("examType", "unit");
        return detail;
    }

    private static String scoreSortKey(StudentScoreListItemDTO item) {
        if (item.getSubmitDate() != null && !item.getSubmitDate().isBlank()) {
            return item.getSubmitDate();
        }
        if (item.getExamDate() != null && !item.getExamDate().isBlank()) {
            return item.getExamDate();
        }
        if (item.getSubmissionDate() != null && !item.getSubmissionDate().isBlank()) {
            return item.getSubmissionDate();
        }
        if (item.getCompletedAt() != null && !item.getCompletedAt().isBlank()) {
            return item.getCompletedAt();
        }
        return "";
    }

    private String serializeAnswers(Map<String, String> answers) {
        if (answers == null || answers.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("考试答案格式错误");
        }
    }

    private String serializeQuestionOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("考试题目选项格式错误");
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void persistExamFinishedEvent(ExamRecord exam,
                                          ExamSubmissionRecord previousSubmission,
                                          ExamSubmissionRecord submission,
                                          Integer previousScore,
                                          String previousTeacherComment) {
        if (submission.getId() == null
                || exam.getId() == null
                || submission.getStudentId() == null
                || exam.getCourseId() == null
                || submission.getScore() == null) {
            return;
        }

        if (sameGrade(previousScore, previousTeacherComment, submission.getScore(), submission.getTeacherComment())) {
            return;
        }

        Long classId = resolveStudentClassId(submission.getStudentId());
        if (classId == null) {
            return;
        }

        Instant finishedAt = parseSubmissionInstant(submission.getSubmissionDate());
        String eventId = buildExamFinishedEventId(submission);
        ExamFinishedEvent event = new ExamFinishedEvent(
                eventId,
                finishedAt,
                new EventAggregate("exam_submission", String.valueOf(submission.getId())),
                new ExamFinishedPayload(
                        exam.getId(),
                        submission.getId(),
                        submission.getStudentId(),
                        exam.getCourseId(),
                        classId,
                        submission.getScore(),
                        exam.getTotalScore() == null ? 100 : exam.getTotalScore(),
                        finishedAt
                )
        );

        outboxEventRepository.save(new OutboxEventEntity(
                null,
                event.eventId(),
                event.aggregate().type(),
                event.aggregate().id(),
                event.eventType(),
                "exam.finished",
                toJson(event),
                "{}",
                0,
                0,
                finishedAt,
                null,
                finishedAt,
                finishedAt,
                null
        ));
    }

    private static boolean sameGrade(Integer previousScore,
                                     String previousTeacherComment,
                                     Integer currentScore,
                                     String currentTeacherComment) {
        return java.util.Objects.equals(previousScore, currentScore)
                && java.util.Objects.equals(normalizeTeacherComment(previousTeacherComment), normalizeTeacherComment(currentTeacherComment));
    }

    private static String buildExamFinishedEventId(ExamSubmissionRecord submission) {
        return "exam-finished-" + submission.getId() + "-" + submission.getScore()
                + "-" + shortSha256(normalizeTeacherComment(submission.getTeacherComment()));
    }

    private static String normalizeTeacherComment(String value) {
        return value == null ? "" : value.trim();
    }

    private static String shortSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(12);
            for (int i = 0; i < 6; i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }

    private Long resolveStudentClassId(Long studentId) {
        List<Long> classIds = loadStudentClassIds(studentId);
        return classIds == null || classIds.isEmpty() ? null : classIds.get(0);
    }

    private static Instant parseSubmissionInstant(String submissionDate) {
        if (submissionDate == null || submissionDate.isBlank()) {
            return Instant.now();
        }
        try {
            return parseExamDateTime(submissionDate);
        } catch (RuntimeException ignored) {
            return Instant.now();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化考试完成事件失败", ex);
        }
    }

    private record AutoGradeResult(boolean graded, int score) {
        static AutoGradeResult pending() {
            return new AutoGradeResult(false, 0);
        }
    }

    private static Map<String, Object> buildPageResponse(List<Map<String, Object>> content, int page, int size, int totalElements) {
        Map<String, Object> result = new LinkedHashMap<>();
        int totalPages = totalElements > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        int offset = (page - 1) * size;
        result.put("content", content);
        Map<String, Object> pageable = new LinkedHashMap<>();
        pageable.put("pageNumber", page - 1);
        pageable.put("pageSize", size);
        pageable.put("offset", offset);
        pageable.put("paged", true);
        pageable.put("unpaged", false);
        Map<String, Object> sort = new LinkedHashMap<>();
        sort.put("empty", false);
        sort.put("sorted", true);
        sort.put("unsorted", false);
        pageable.put("sort", sort);
        result.put("pageable", pageable);
        result.put("sort", sort);
        result.put("totalPages", totalPages);
        result.put("totalElements", totalElements);
        result.put("size", size);
        result.put("number", page - 1);
        result.put("first", page == 1);
        result.put("last", page >= totalPages || totalPages == 0);
        result.put("numberOfElements", content.size());
        result.put("empty", content.isEmpty());
        return result;
    }
}
