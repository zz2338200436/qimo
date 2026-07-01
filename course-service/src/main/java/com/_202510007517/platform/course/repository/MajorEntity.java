package com._202510007517.platform.course.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "majors")
public class MajorEntity {

    @Id
    private Long id;

    @Column(name = "major_name", nullable = false, length = 100)
    private String majorName;

    public Long getId() {
        return id;
    }

    public String getMajorName() {
        return majorName;
    }
}
