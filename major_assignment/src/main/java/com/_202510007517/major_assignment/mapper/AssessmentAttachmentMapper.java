package com._202510007517.major_assignment.mapper;

import com._202510007517.major_assignment.entity.AssessmentAttachment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AssessmentAttachmentMapper {

    @Update("""
            CREATE TABLE IF NOT EXISTS assessment_attachments (
                id BIGINT NOT NULL AUTO_INCREMENT,
                assessment_type VARCHAR(32) NOT NULL,
                assessment_id BIGINT NOT NULL,
                original_filename VARCHAR(255) NOT NULL,
                stored_filename VARCHAR(255) NOT NULL,
                relative_path VARCHAR(500) NOT NULL,
                content_type VARCHAR(255) NULL,
                file_size BIGINT NULL,
                uploaded_by BIGINT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                INDEX idx_assessment_attachments_owner (assessment_type, assessment_id)
            )
            """)
    void ensureTable();

    @Insert("""
            INSERT INTO assessment_attachments(
                assessment_type, assessment_id, original_filename, stored_filename,
                relative_path, content_type, file_size, uploaded_by
            ) VALUES (
                #{assessmentType}, #{assessmentId}, #{originalFilename}, #{storedFilename},
                #{relativePath}, #{contentType}, #{fileSize}, #{uploadedBy}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AssessmentAttachment attachment);

    @Select("""
            SELECT id,
                   assessment_type AS assessmentType,
                   assessment_id AS assessmentId,
                   original_filename AS originalFilename,
                   stored_filename AS storedFilename,
                   relative_path AS relativePath,
                   content_type AS contentType,
                   file_size AS fileSize,
                   uploaded_by AS uploadedBy,
                   created_at AS createdAt
            FROM assessment_attachments
            WHERE assessment_type = #{assessmentType}
              AND assessment_id = #{assessmentId}
            ORDER BY id ASC
            """)
    List<AssessmentAttachment> findByAssessment(@Param("assessmentType") String assessmentType,
                                                @Param("assessmentId") Long assessmentId);

    @Select("""
            SELECT id,
                   assessment_type AS assessmentType,
                   assessment_id AS assessmentId,
                   original_filename AS originalFilename,
                   stored_filename AS storedFilename,
                   relative_path AS relativePath,
                   content_type AS contentType,
                   file_size AS fileSize,
                   uploaded_by AS uploadedBy,
                   created_at AS createdAt
            FROM assessment_attachments
            WHERE id = #{id}
            """)
    AssessmentAttachment findById(@Param("id") Long id);

    @Delete("DELETE FROM assessment_attachments WHERE assessment_type = #{assessmentType} AND assessment_id = #{assessmentId}")
    void deleteByAssessment(@Param("assessmentType") String assessmentType, @Param("assessmentId") Long assessmentId);
}
