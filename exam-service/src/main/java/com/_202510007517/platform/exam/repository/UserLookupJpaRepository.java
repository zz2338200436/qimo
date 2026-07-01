package com._202510007517.platform.exam.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserLookupJpaRepository extends JpaRepository<UserLookupEntity, Long> {

    List<UserLookupEntity> findByIdIn(Collection<Long> ids);
}
