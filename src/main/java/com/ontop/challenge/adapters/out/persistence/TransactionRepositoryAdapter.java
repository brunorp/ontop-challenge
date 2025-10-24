package com.ontop.challenge.adapters.out.persistence;

import com.ontop.challenge.adapters.out.persistence.mapper.TransactionMapper;
import com.ontop.challenge.adapters.out.persistence.entity.JpaTransactionEntity;
import com.ontop.challenge.domain.Transaction;
import com.ontop.challenge.application.port.out.TransactionRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter implementing TransactionRepositoryPort by delegating to JPA repository
 */
@Component
public class TransactionRepositoryAdapter implements TransactionRepositoryPort {

    private final JpaTransactionRepository jpaTransactionRepository;

    private final TransactionMapper transactionMapper;

    public TransactionRepositoryAdapter(JpaTransactionRepository jpaTransactionRepository, TransactionMapper transactionMapper) {
        this.jpaTransactionRepository = jpaTransactionRepository;
        this.transactionMapper = transactionMapper;
    }

    @Override
    public Transaction save(Transaction tx) {
        JpaTransactionEntity jpaTransactionEntity = new JpaTransactionEntity(tx);
        JpaTransactionEntity response = jpaTransactionRepository.save(jpaTransactionEntity);
        return transactionMapper.JpaToTransaction(response);
    }
}

