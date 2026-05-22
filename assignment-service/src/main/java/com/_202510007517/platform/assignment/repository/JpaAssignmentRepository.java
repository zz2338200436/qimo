package com._202510007517.platform.assignment.repository;

import com._202510007517.platform.assignment.domain.AssignmentRecord;
import com._202510007517.platform.assignment.domain.AssignmentSubmissionRecord;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaAssignmentRepository implements AssignmentRepository {

    private final AssignmentJpaRepository assignmentJpaRepository;
    private final AssignmentSubmissionJpaRepository submissionJpaRepository;
    private final AssignmentClassJpaRepository assignmentClassJpaRepository;
    private final AssignmentClassStudentJpaRepository classStudentJpaRepository;
    private final AssignmentKnowledgePointJpaRepository knowledgePointJpaRepository;

    public JpaAssignmentRepository(AssignmentJpaRepository assignmentJpaRepository,
                                   AssignmentSubmissionJpaRepository submissionJpaRepository,
                                   AssignmentClassJpaRepository assignmentClassJpaRepository,
                                   AssignmentClassStudentJpaRepository classStudentJpaRepository,
                                   AssignmentKnowledgePointJpaRepository knowledgePointJpaRepository) {
        this.assignmentJpaRepository = assignmentJpaRepository;
        this.submissionJpaRepository = submissionJpaRepository;
        this.assignmentClassJpaRepository = assignmentClassJpaRepository;
        this.classStudentJpaRepository = classStudentJpaRepository;
        this.knowledgePointJpaRepository = knowledgePointJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignmentRecord> findById(Long assignmentId) {
        return assignmentJpaRepository.findById(assignmentId).map(this::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentRecord> findByCourseId(Long courseId) {
        return assignmentJpaRepository.findByCourseIdOrderByIdDesc(courseId).stream()
                .map(this::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentRecord> findByTeacherId(Long teacherId) {
        return assignmentJpaRepository.findByTeacherIdOrderByIdDesc(teacherId).stream()
                .map(this::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentRecord> findByStudentId(Long studentId) {
        List<Long> classIds = classStudentJpaRepository.findByStudentIdOrderByClassId(studentId).stream()
                .map(AssignmentClassStudentEntity::getClassId)
                .distinct()
                .toList();
        return findByClassIds(classIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentRecord> findByClassIds(List<Long> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> assignmentIds = new LinkedHashSet<>();
        for (AssignmentClassEntity assignmentClass : assignmentClassJpaRepository.findByClassIdIn(classIds)) {
            assignmentIds.add(assignmentClass.getAssignmentId());
        }
        if (assignmentIds.isEmpty()) {
            return List.of();
        }
        return assignmentJpaRepository.findByIdIn(assignmentIds).stream()
                .sorted((left, right) -> Long.compare(right.getId(), left.getId()))
                .map(this::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionRecord> findSubmissionsByAssignmentId(Long assignmentId) {
        return submissionJpaRepository.findByAssignmentIdOrderBySubmissionDateDescIdDesc(assignmentId).stream()
                .map(this::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignmentSubmissionRecord> findSubmissionByAssignmentAndStudent(Long assignmentId, Long studentId) {
        return submissionJpaRepository.findByAssignmentIdAndStudentIdOrderByIdDesc(assignmentId, studentId)
                .stream()
                .findFirst()
                .map(this::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionRecord> findSubmissionsByStudentId(Long studentId) {
        return submissionJpaRepository.findByStudentIdOrderBySubmissionDateDescIdDesc(studentId).stream()
                .map(this::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionRecord> findGradedSubmissionsByStudentId(Long studentId) {
        return submissionJpaRepository.findByStudentIdAndGradedTrueAndScoreIsNotNullOrderBySubmissionDateDescIdDesc(studentId)
                .stream()
                .map(this::toRecord)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AssignmentSubmissionRecord> findSubmissionById(Long submissionId) {
        return submissionJpaRepository.findById(submissionId).map(this::toRecord);
    }

    @Override
    @Transactional
    public AssignmentSubmissionRecord insertSubmission(AssignmentSubmissionRecord submission) {
        AssignmentSubmissionEntity saved = submissionJpaRepository.save(toEntity(submission));
        submission.setId(saved.getId());
        return submission;
    }

    @Override
    @Transactional
    public void updateSubmission(AssignmentSubmissionRecord submission) {
        submissionJpaRepository.findById(submission.getId()).ifPresent(entity -> {
            apply(entity, submission);
            submissionJpaRepository.save(entity);
        });
    }

    @Override
    @Transactional
    public void updateAssignment(AssignmentRecord assignment) {
        assignmentJpaRepository.findById(assignment.getId()).ifPresent(entity -> {
            entity.setSubmissionCount(assignment.getSubmissionCount());
            entity.setGradedCount(assignment.getGradedCount());
            entity.setStatus(assignment.getStatus());
            assignmentJpaRepository.save(entity);
        });
    }

    @Override
    @Transactional
    public AssignmentRecord insertAssignment(AssignmentRecord assignment) {
        AssignmentEntity saved = assignmentJpaRepository.save(toEntity(assignment));
        assignment.setId(saved.getId());
        return assignment;
    }

    @Override
    @Transactional
    public void updateAssignmentDetails(AssignmentRecord assignment) {
        assignmentJpaRepository.findById(assignment.getId()).ifPresent(entity -> {
            apply(entity, assignment);
            assignmentJpaRepository.save(entity);
        });
    }

    @Override
    @Transactional
    public void replaceAssignmentClasses(Long assignmentId, List<Long> classIds) {
        assignmentClassJpaRepository.deleteByAssignmentId(assignmentId);
        if (classIds == null || classIds.isEmpty()) {
            return;
        }
        List<AssignmentClassEntity> rows = new LinkedHashSet<>(classIds).stream()
                .map(classId -> new AssignmentClassEntity(assignmentId, classId))
                .toList();
        assignmentClassJpaRepository.saveAll(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findKnowledgePointIdsByAssignmentId(Long assignmentId) {
        return knowledgePointJpaRepository.findByAssignmentIdOrderByKnowledgePointIdAsc(assignmentId).stream()
                .map(AssignmentKnowledgePointEntity::getKnowledgePointId)
                .toList();
    }

    @Override
    @Transactional
    public void replaceAssignmentKnowledgePoints(Long assignmentId, List<Long> knowledgePointIds) {
        knowledgePointJpaRepository.deleteByAssignmentId(assignmentId);
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) {
            return;
        }
        List<AssignmentKnowledgePointEntity> rows = new LinkedHashSet<>(knowledgePointIds).stream()
                .map(knowledgePointId -> new AssignmentKnowledgePointEntity(assignmentId, knowledgePointId))
                .toList();
        knowledgePointJpaRepository.saveAll(rows);
    }

    @Override
    @Transactional
    public void deleteAssignmentCascade(Long assignmentId) {
        knowledgePointJpaRepository.deleteByAssignmentId(assignmentId);
        submissionJpaRepository.deleteByAssignmentId(assignmentId);
        assignmentClassJpaRepository.deleteByAssignmentId(assignmentId);
        assignmentJpaRepository.deleteById(assignmentId);
    }

    private AssignmentRecord toRecord(AssignmentEntity entity) {
        AssignmentRecord record = new AssignmentRecord();
        record.setId(entity.getId());
        record.setTitle(entity.getTitle());
        record.setDescription(entity.getDescription());
        record.setCourseId(entity.getCourseId());
        record.setDueDate(entity.getDueDate());
        record.setPublishDate(entity.getPublishDate());
        record.setIsActive(entity.getActive());
        record.setTeacherId(entity.getTeacherId());
        record.setMaxScore(entity.getMaxScore());
        record.setSubmissionCount(toInteger(submissionJpaRepository.countByAssignmentId(entity.getId())));
        record.setGradedCount(toInteger(submissionJpaRepository.countByAssignmentIdAndGradedTrue(entity.getId())));
        record.setStatus(entity.getStatus());
        record.setTotalStudents(entity.getTotalStudents());
        return record;
    }

    private AssignmentSubmissionRecord toRecord(AssignmentSubmissionEntity entity) {
        AssignmentSubmissionRecord record = new AssignmentSubmissionRecord();
        record.setId(entity.getId());
        record.setAssignmentId(entity.getAssignmentId());
        record.setStudentId(entity.getStudentId());
        record.setContent(entity.getContent());
        record.setSubmissionDate(entity.getSubmissionDate());
        record.setGraded(entity.getGraded());
        record.setIsLate(entity.getLate());
        record.setLatePenalty(entity.getLatePenalty());
        record.setScore(entity.getScore());
        record.setTeacherComment(entity.getTeacherComment());
        return record;
    }

    private AssignmentEntity toEntity(AssignmentRecord assignment) {
        AssignmentEntity entity = new AssignmentEntity();
        apply(entity, assignment);
        return entity;
    }

    private void apply(AssignmentEntity entity, AssignmentRecord assignment) {
        entity.setId(assignment.getId());
        entity.setTitle(assignment.getTitle());
        entity.setDescription(assignment.getDescription());
        entity.setCourseId(assignment.getCourseId());
        entity.setDueDate(assignment.getDueDate());
        entity.setPublishDate(assignment.getPublishDate());
        entity.setActive(assignment.getIsActive());
        entity.setTeacherId(assignment.getTeacherId());
        entity.setMaxScore(assignment.getMaxScore());
        entity.setSubmissionCount(assignment.getSubmissionCount());
        entity.setGradedCount(assignment.getGradedCount());
        entity.setStatus(assignment.getStatus());
        entity.setTotalStudents(assignment.getTotalStudents());
    }

    private AssignmentSubmissionEntity toEntity(AssignmentSubmissionRecord submission) {
        AssignmentSubmissionEntity entity = new AssignmentSubmissionEntity();
        apply(entity, submission);
        return entity;
    }

    private void apply(AssignmentSubmissionEntity entity, AssignmentSubmissionRecord submission) {
        entity.setId(submission.getId());
        entity.setAssignmentId(submission.getAssignmentId());
        entity.setStudentId(submission.getStudentId());
        entity.setContent(submission.getContent());
        entity.setSubmissionDate(submission.getSubmissionDate());
        entity.setGraded(submission.getGraded());
        entity.setLate(submission.getIsLate());
        entity.setLatePenalty(submission.getLatePenalty());
        entity.setScore(submission.getScore());
        entity.setTeacherComment(submission.getTeacherComment());
    }

    private static Integer toInteger(long value) {
        return Math.toIntExact(value);
    }
}
