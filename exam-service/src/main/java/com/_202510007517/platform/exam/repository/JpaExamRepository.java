package com._202510007517.platform.exam.repository;

import com._202510007517.platform.exam.api.dto.StudentScoreDTO;
import com._202510007517.platform.exam.domain.ExamQuestionRecord;
import com._202510007517.platform.exam.domain.ExamRecord;
import com._202510007517.platform.exam.domain.ExamSubmissionRecord;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Repository
public class JpaExamRepository implements ExamRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ExamJpaRepository examJpaRepository;
    private final ExamSubmissionJpaRepository submissionJpaRepository;
    private final ExamClassJpaRepository examClassJpaRepository;
    private final ExamKnowledgePointJpaRepository knowledgePointJpaRepository;
    private final ExamQuestionJpaRepository questionJpaRepository;
    private final UserLookupJpaRepository userLookupJpaRepository;

    public JpaExamRepository(ExamJpaRepository examJpaRepository,
                             ExamSubmissionJpaRepository submissionJpaRepository,
                             ExamClassJpaRepository examClassJpaRepository,
                             ExamKnowledgePointJpaRepository knowledgePointJpaRepository,
                             ExamQuestionJpaRepository questionJpaRepository,
                             UserLookupJpaRepository userLookupJpaRepository) {
        this.examJpaRepository = examJpaRepository;
        this.submissionJpaRepository = submissionJpaRepository;
        this.examClassJpaRepository = examClassJpaRepository;
        this.knowledgePointJpaRepository = knowledgePointJpaRepository;
        this.questionJpaRepository = questionJpaRepository;
        this.userLookupJpaRepository = userLookupJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamRecord> findByClassIds(List<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> examIds = new LinkedHashSet<>();
        for (ExamClassEntity examClass : examClassJpaRepository.findByClassIdIn(new LinkedHashSet<>(classIds))) {
            examIds.add(examClass.getExamId());
        }
        if (examIds.isEmpty()) {
            return List.of();
        }
        return examJpaRepository.findByIdIn(examIds).stream()
                .sorted(examOrderComparator())
                .map(this::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamRecord> findByTeacherId(Long teacherId) {
        if (teacherId == null) {
            return List.of();
        }
        return examJpaRepository.findByTeacherIdOrderByStartTimeDescIdDesc(teacherId).stream()
                .map(this::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isExamVisibleToClasses(Long examId, List<Long> classIds) {
        if (examId == null || classIds == null || classIds.isEmpty()) {
            return false;
        }
        LinkedHashSet<Long> visibleClassIds = new LinkedHashSet<>(classIds);
        return examClassJpaRepository.findByExamId(examId).stream()
                .anyMatch(row -> visibleClassIds.contains(row.getClassId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExamRecord> findExam(Long examId) {
        if (examId == null) {
            return Optional.empty();
        }
        return examJpaRepository.findById(examId).map(this::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExamSubmissionRecord> findSubmission(Long examId, Long studentId) {
        if (examId == null || studentId == null) {
            return Optional.empty();
        }
        return submissionJpaRepository.findByExamIdAndStudentId(examId, studentId)
                .map(entity -> toRecord(entity, titleForExam(examId), nameForStudent(studentId)));
    }

    @Override
    @Transactional
    public ExamSubmissionRecord upsertSubmission(Long examId, Long studentId, Integer timeTaken, String contentJson) {
        LocalDateTime now = LocalDateTime.now();
        Optional<ExamSubmissionEntity> existing = submissionJpaRepository.findByExamIdAndStudentId(examId, studentId);
        if (existing.isPresent()) {
            ExamSubmissionEntity entity = existing.get();
            entity.setContent(contentJson);
            entity.setSubmissionDate(now);
            entity.setTimeTaken(timeTaken);
            entity.setGraded(false);
            entity.setScore(null);
            entity.setTeacherComment(null);
            submissionJpaRepository.save(entity);
            return findSubmission(examId, studentId).orElseThrow();
        }

        ExamSubmissionEntity entity = new ExamSubmissionEntity();
        entity.setExamId(examId);
        entity.setStudentId(studentId);
        entity.setContent(contentJson);
        entity.setSubmissionDate(now);
        entity.setTimeTaken(timeTaken);
        entity.setGraded(false);
        entity.setScore(null);
        entity.setTeacherComment(null);
        submissionJpaRepository.save(entity);
        return findSubmission(examId, studentId).orElseThrow();
    }

    @Override
    @Transactional
    public ExamRecord insert(ExamRecord exam) {
        ExamEntity saved = examJpaRepository.save(toEntity(exam));
        exam.setId(saved.getId());
        return findExam(saved.getId()).orElseThrow();
    }

    @Override
    @Transactional
    public void update(ExamRecord exam) {
        if (exam.getId() == null) {
            return;
        }
        examJpaRepository.findById(exam.getId()).ifPresent(entity -> {
            apply(entity, exam);
            examJpaRepository.save(entity);
        });
    }

    @Override
    @Transactional
    public void delete(Long examId) {
        if (examId == null) {
            return;
        }
        submissionJpaRepository.deleteByExamId(examId);
        examClassJpaRepository.deleteByExamId(examId);
        knowledgePointJpaRepository.deleteByExamId(examId);
        questionJpaRepository.deleteByExamId(examId);
        examJpaRepository.deleteById(examId);
    }

    @Override
    @Transactional
    public void replaceExamClasses(Long examId, Set<Long> classIds) {
        examClassJpaRepository.deleteByExamId(examId);
        if (classIds == null || classIds.isEmpty()) {
            return;
        }
        List<ExamClassEntity> rows = new LinkedHashSet<>(classIds).stream()
                .map(classId -> new ExamClassEntity(examId, classId))
                .toList();
        examClassJpaRepository.saveAll(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findKnowledgePointIdsByExamId(Long examId) {
        return knowledgePointJpaRepository.findByExamIdOrderByKnowledgePointIdAsc(examId).stream()
                .map(ExamKnowledgePointEntity::getKnowledgePointId)
                .toList();
    }

    @Override
    @Transactional
    public void replaceExamKnowledgePoints(Long examId, List<Long> knowledgePointIds) {
        knowledgePointJpaRepository.deleteByExamId(examId);
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) {
            return;
        }
        List<ExamKnowledgePointEntity> rows = new LinkedHashSet<>(knowledgePointIds).stream()
                .map(knowledgePointId -> new ExamKnowledgePointEntity(examId, knowledgePointId))
                .toList();
        knowledgePointJpaRepository.saveAll(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamQuestionRecord> findQuestionsByExamId(Long examId) {
        return questionJpaRepository.findByExamIdOrderBySortOrderAscIdAsc(examId).stream()
                .map(this::toRecord)
                .toList();
    }

    @Override
    @Transactional
    public void replaceExamQuestions(Long examId, List<ExamQuestionRecord> questions) {
        questionJpaRepository.deleteByExamId(examId);
        if (questions == null || questions.isEmpty()) {
            return;
        }
        List<ExamQuestionEntity> rows = new ArrayList<>();
        for (ExamQuestionRecord question : questions) {
            ExamQuestionEntity entity = new ExamQuestionEntity();
            entity.setExamId(examId);
            entity.setQuestionText(question.questionText());
            entity.setQuestionType(question.questionType());
            entity.setOptionsJson(question.optionsJson());
            entity.setCorrectAnswer(question.correctAnswer());
            entity.setScore(question.score() == null ? 0 : question.score());
            entity.setKnowledgePointId(question.knowledgePointId());
            entity.setSortOrder(question.sortOrder() == null ? 0 : question.sortOrder());
            rows.add(entity);
        }
        questionJpaRepository.saveAll(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public int countSubmissionsByExamId(Long examId) {
        return Math.toIntExact(submissionJpaRepository.countByExamId(examId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamSubmissionRecord> findSubmissionsByExamId(Long examId) {
        String examTitle = titleForExam(examId);
        Map<Long, String> studentNames = loadStudentNames(
                submissionJpaRepository.findByExamIdOrderBySubmissionDateDescIdDesc(examId).stream()
                        .map(ExamSubmissionEntity::getStudentId)
                        .toList());
        return submissionJpaRepository.findByExamIdOrderBySubmissionDateDescIdDesc(examId).stream()
                .map(entity -> toRecord(entity, examTitle, studentNames.get(entity.getStudentId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamSubmissionRecord> findSubmissionsByTeacherId(Long teacherId,
                                                                 Long examId,
                                                                 Long studentId,
                                                                 Boolean graded,
                                                                 String sortBy,
                                                                 String order,
                                                                 int offset,
                                                                 int limit) {
        if (teacherId == null) {
            return List.of();
        }
        Map<Long, ExamEntity> teacherExams = loadTeacherExamMap(teacherId);
        List<ExamSubmissionEntity> filtered = filterTeacherSubmissions(teacherExams, examId, studentId, graded);
        filtered.sort(submissionComparator(sortBy, order, teacherExams));
        int safeOffset = Math.max(offset, 0);
        int safeLimit = Math.max(limit, 1);
        if (safeOffset >= filtered.size()) {
            return List.of();
        }
        int endIndex = Math.min(filtered.size(), safeOffset + safeLimit);
        Map<Long, String> studentNames = loadStudentNames(filtered.stream().map(ExamSubmissionEntity::getStudentId).toList());
        return filtered.subList(safeOffset, endIndex).stream()
                .map(entity -> toRecord(entity, titleForExam(entity.getExamId(), teacherExams), studentNames.get(entity.getStudentId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int countSubmissionsByTeacherId(Long teacherId, Long examId, Long studentId, Boolean graded) {
        if (teacherId == null) {
            return 0;
        }
        return filterTeacherSubmissions(loadTeacherExamMap(teacherId), examId, studentId, graded).size();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExamSubmissionRecord> findSubmissionById(Long submissionId) {
        if (submissionId == null) {
            return Optional.empty();
        }
        return submissionJpaRepository.findById(submissionId)
                .map(entity -> toRecord(entity, titleForExam(entity.getExamId()), nameForStudent(entity.getStudentId())));
    }

    @Override
    @Transactional
    public int updateSubmission(Long submissionId, ExamSubmissionRecord submission) {
        Optional<ExamSubmissionEntity> existing = submissionJpaRepository.findById(submissionId);
        if (existing.isEmpty()) {
            return 0;
        }
        ExamSubmissionEntity entity = existing.get();
        entity.setExamId(submission.getExamId());
        entity.setStudentId(submission.getStudentId());
        entity.setContent(submission.getContent());
        entity.setTimeTaken(submission.getTimeTaken());
        entity.setGraded(Boolean.TRUE.equals(submission.getGraded()));
        entity.setScore(submission.getScore());
        entity.setTeacherComment(submission.getTeacherComment());
        submissionJpaRepository.save(entity);
        return 1;
    }

    @Override
    @Transactional
    public int updateSubmissionGrade(Long submissionId, Integer score, String teacherComment) {
        Optional<ExamSubmissionEntity> existing = submissionJpaRepository.findById(submissionId);
        if (existing.isEmpty()) {
            return 0;
        }
        ExamSubmissionEntity entity = existing.get();
        entity.setGraded(true);
        entity.setScore(score);
        entity.setTeacherComment(teacherComment);
        submissionJpaRepository.save(entity);
        return 1;
    }

    @Override
    @Transactional
    public int deleteSubmission(Long submissionId) {
        if (submissionId == null || !submissionJpaRepository.existsById(submissionId)) {
            return 0;
        }
        submissionJpaRepository.deleteById(submissionId);
        return 1;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentScoreDTO> findStudentExamScores(Long studentId) {
        Map<Long, ExamEntity> exams = loadExamMap(
                submissionJpaRepository.findByStudentIdAndGradedTrueOrderBySubmissionDateDescIdDesc(studentId).stream()
                        .map(ExamSubmissionEntity::getExamId)
                        .toList());
        return submissionJpaRepository.findByStudentIdAndGradedTrueOrderBySubmissionDateDescIdDesc(studentId).stream()
                .map(entity -> toStudentScore(entity, exams.get(entity.getExamId())))
                .toList();
    }

    private List<ExamSubmissionEntity> filterTeacherSubmissions(Map<Long, ExamEntity> teacherExams,
                                                                Long examId,
                                                                Long studentId,
                                                                Boolean graded) {
        if (teacherExams.isEmpty()) {
            return new ArrayList<>();
        }
        List<ExamSubmissionEntity> submissions = submissionJpaRepository.findByExamIdIn(teacherExams.keySet());
        return new ArrayList<>(submissions.stream()
                .filter(entity -> examId == null || Objects.equals(entity.getExamId(), examId))
                .filter(entity -> studentId == null || Objects.equals(entity.getStudentId(), studentId))
                .filter(entity -> graded == null || Objects.equals(Boolean.TRUE.equals(entity.getGraded()), graded))
                .toList());
    }

    private Comparator<ExamEntity> examOrderComparator() {
        return (left, right) -> {
            int startTimeCompare = compareNullableDateTimeDesc(left.getStartTime(), right.getStartTime());
            if (startTimeCompare != 0) {
                return startTimeCompare;
            }
            return Long.compare(right.getId(), left.getId());
        };
    }

    private Comparator<ExamSubmissionEntity> submissionComparator(String sortBy,
                                                                  String order,
                                                                  Map<Long, ExamEntity> examMap) {
        Comparator<ExamSubmissionEntity> comparator = switch (sortBy == null ? "" : sortBy) {
            case "examId" -> comparingNullable(ExamSubmissionEntity::getExamId);
            case "studentId" -> comparingNullable(ExamSubmissionEntity::getStudentId);
            case "submissionDate" -> comparingNullable(ExamSubmissionEntity::getSubmissionDate);
            case "timeTaken" -> comparingNullable(ExamSubmissionEntity::getTimeTaken);
            case "graded" -> comparingNullable(entity -> Boolean.TRUE.equals(entity.getGraded()) ? 1 : 0);
            case "score" -> comparingNullable(ExamSubmissionEntity::getScore);
            case "examTitle" -> comparingNullable(entity -> titleForExam(entity.getExamId(), examMap));
            case "id" -> comparingNullable(ExamSubmissionEntity::getId);
            default -> comparingNullable(ExamSubmissionEntity::getId);
        };
        if (!"ASC".equalsIgnoreCase(order)) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparing(ExamSubmissionEntity::getId, Comparator.reverseOrder());
    }

    private <T extends Comparable<? super T>> Comparator<ExamSubmissionEntity> comparingNullable(
            java.util.function.Function<ExamSubmissionEntity, T> extractor) {
        return (left, right) -> compareNullable(extractor.apply(left), extractor.apply(right));
    }

    private static <T extends Comparable<? super T>> int compareNullable(T left, T right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private static int compareNullableDateTimeDesc(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private Map<Long, ExamEntity> loadTeacherExamMap(Long teacherId) {
        LinkedHashMap<Long, ExamEntity> exams = new LinkedHashMap<>();
        for (ExamEntity exam : examJpaRepository.findByTeacherIdOrderByStartTimeDescIdDesc(teacherId)) {
            exams.put(exam.getId(), exam);
        }
        return exams;
    }

    private Map<Long, ExamEntity> loadExamMap(Collection<Long> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<Long, ExamEntity> exams = new LinkedHashMap<>();
        for (ExamEntity exam : examJpaRepository.findByIdIn(new LinkedHashSet<>(examIds))) {
            exams.put(exam.getId(), exam);
        }
        return exams;
    }

    private Map<Long, String> loadStudentNames(Collection<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<Long, String> names = new LinkedHashMap<>();
        for (UserLookupEntity user : userLookupJpaRepository.findByIdIn(new LinkedHashSet<>(studentIds))) {
            names.put(user.getId(), user.getName());
        }
        return names;
    }

    private String titleForExam(Long examId) {
        return examJpaRepository.findById(examId).map(ExamEntity::getTitle).orElse(null);
    }

    private String titleForExam(Long examId, Map<Long, ExamEntity> examMap) {
        ExamEntity exam = examMap.get(examId);
        if (exam != null) {
            return exam.getTitle();
        }
        return titleForExam(examId);
    }

    private String nameForStudent(Long studentId) {
        return userLookupJpaRepository.findById(studentId).map(UserLookupEntity::getName).orElse(null);
    }

    private ExamRecord toRecord(ExamEntity entity) {
        ExamRecord record = new ExamRecord();
        record.setId(entity.getId());
        record.setTitle(entity.getTitle());
        record.setDescription(entity.getDescription());
        record.setCourseId(entity.getCourseId());
        record.setTeacherId(entity.getTeacherId());
        record.setStartTime(format(entity.getStartTime()));
        record.setEndTime(format(entity.getEndTime()));
        record.setPublishDate(format(entity.getPublishDate()));
        record.setActive(entity.getActive());
        record.setOnline(entity.getOnline());
        record.setLocation(entity.getLocation());
        record.setDuration(entity.getDuration());
        record.setTotalScore(entity.getTotalScore());
        return record;
    }

    private ExamQuestionRecord toRecord(ExamQuestionEntity entity) {
        return new ExamQuestionRecord(
                entity.getId(),
                entity.getExamId(),
                entity.getQuestionText(),
                entity.getQuestionType(),
                entity.getOptionsJson(),
                entity.getCorrectAnswer(),
                entity.getScore(),
                entity.getKnowledgePointId(),
                entity.getSortOrder());
    }

    private ExamSubmissionRecord toRecord(ExamSubmissionEntity entity, String examTitle, String studentName) {
        ExamSubmissionRecord record = new ExamSubmissionRecord();
        record.setId(entity.getId());
        record.setExamId(entity.getExamId());
        record.setStudentId(entity.getStudentId());
        record.setStudentName(studentName);
        record.setExamTitle(examTitle);
        record.setContent(entity.getContent());
        record.setSubmissionDate(format(entity.getSubmissionDate()));
        record.setTimeTaken(entity.getTimeTaken());
        record.setGraded(entity.getGraded());
        record.setScore(entity.getScore());
        record.setTeacherComment(entity.getTeacherComment());
        return record;
    }

    private StudentScoreDTO toStudentScore(ExamSubmissionEntity entity, ExamEntity exam) {
        StudentScoreDTO dto = new StudentScoreDTO();
        dto.setType("exam");
        dto.setRelatedId(entity.getExamId());
        dto.setTitle(exam == null ? null : exam.getTitle());
        dto.setCourseId(exam == null ? null : exam.getCourseId());
        dto.setCourseName(null);
        dto.setCompletedAt(format(entity.getSubmissionDate()));
        dto.setScore(entity.getScore());
        dto.setTotalScore(100);
        dto.setRank(null);
        return dto;
    }

    private ExamEntity toEntity(ExamRecord exam) {
        ExamEntity entity = new ExamEntity();
        apply(entity, exam);
        return entity;
    }

    private void apply(ExamEntity entity, ExamRecord exam) {
        entity.setId(exam.getId());
        entity.setTitle(exam.getTitle());
        entity.setDescription(exam.getDescription());
        entity.setCourseId(exam.getCourseId());
        entity.setTeacherId(exam.getTeacherId());
        entity.setStartTime(parse(exam.getStartTime()));
        entity.setEndTime(parse(exam.getEndTime()));
        entity.setPublishDate(parse(exam.getPublishDate()));
        entity.setDuration(exam.getDuration());
        entity.setTotalScore(exam.getTotalScore());
        entity.setActive(exam.getActive());
        entity.setOnline(exam.getOnline());
        entity.setLocation(exam.getLocation());
    }

    private LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
    }

    private String format(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }
}
