package com._202510007517.platform.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
