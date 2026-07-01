package com._202510007517.platform.auth.repository;

import com._202510007517.platform.auth.domain.AuthCredential;

import java.util.Optional;

public interface AuthCredentialRepository {
    Optional<AuthCredential> findByUsername(String username);

    Optional<AuthCredential> findByUserId(Long userId);

    void updatePassword(Long userId, String encodedPassword);
}
