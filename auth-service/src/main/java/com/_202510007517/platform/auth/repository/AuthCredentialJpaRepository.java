package com._202510007517.platform.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthCredentialJpaRepository extends JpaRepository<AuthCredentialEntity, Long> {

    Optional<AuthCredentialEntity> findByUsername(String username);

    Optional<AuthCredentialEntity> findByUserId(Long userId);
}
