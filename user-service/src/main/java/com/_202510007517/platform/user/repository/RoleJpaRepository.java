package com._202510007517.platform.user.repository;

import com._202510007517.platform.user.domain.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {

    @Query(value = """
            SELECT r.name
            FROM roles r
            JOIN user_roles ur ON r.id = ur.role_id
            WHERE ur.user_id = :userId
            ORDER BY r.id
            """, nativeQuery = true)
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);
}
