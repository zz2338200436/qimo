package com._202510007517.platform.analysis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EarlyWarningJpaRepository
        extends JpaRepository<EarlyWarningEntity, Long>, JpaSpecificationExecutor<EarlyWarningEntity> {
}
