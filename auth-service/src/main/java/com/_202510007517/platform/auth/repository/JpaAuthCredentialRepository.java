package com._202510007517.platform.auth.repository;

import com._202510007517.platform.auth.domain.AuthCredential;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class JpaAuthCredentialRepository implements AuthCredentialRepository {

    private final AuthCredentialJpaRepository credentialJpaRepository;

    public JpaAuthCredentialRepository(AuthCredentialJpaRepository credentialJpaRepository) {
        this.credentialJpaRepository = credentialJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthCredential> findByUsername(String username) {
        return credentialJpaRepository.findByUsername(username).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AuthCredential> findByUserId(Long userId) {
        return credentialJpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, String encodedPassword) {
        credentialJpaRepository.findByUserId(userId).ifPresent(entity -> {
            entity.setPasswordHash(encodedPassword);
            credentialJpaRepository.save(entity);
        });
    }

    private AuthCredential toDomain(AuthCredentialEntity entity) {
        AuthCredential credential = new AuthCredential();
        credential.setUserId(entity.getUserId());
        credential.setUsername(entity.getUsername());
        credential.setPasswordHash(entity.getPasswordHash());
        credential.setEnabled(Boolean.TRUE.equals(entity.getEnabled()));
        return credential;
    }
}
