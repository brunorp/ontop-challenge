package com.ontop.challenge.adapters.out.persistence;

import com.ontop.challenge.adapters.out.persistence.entity.JpaTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaTransactionRepository extends JpaRepository<JpaTransactionEntity, UUID> {
}

