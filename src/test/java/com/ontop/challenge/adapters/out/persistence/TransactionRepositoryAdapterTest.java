package com.ontop.challenge.adapters.out.persistence;

import com.ontop.challenge.adapters.out.persistence.entity.JpaTransactionEntity;
import com.ontop.challenge.adapters.out.persistence.mapper.TransactionMapper;
import com.ontop.challenge.domain.Transaction;
import com.ontop.challenge.domain.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionRepositoryAdapterTest {

    @Mock
    private JpaTransactionRepository jpaRepository;

    @Mock
    private TransactionMapper mapper;

    private TransactionRepositoryAdapter repositoryAdapter;

    @BeforeEach
    void setUp() {
        repositoryAdapter = new TransactionRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    void save_MapsAndSavesTransaction() {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Instant now = Instant.now();

        Transaction transaction = new Transaction(
                transactionId,
                1000L,
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                new BigDecimal("900.00"),
                "USD",
                TransactionStatus.PENDING,
                null,
                null,
                null,
                accountId,
                now,
                now,
                0L
        );

        JpaTransactionEntity entity = JpaTransactionEntity.builder()
                .id(transactionId)
                .userId(1000L)
                .amount(new BigDecimal("1000.00"))
                .fee(new BigDecimal("100.00"))
                .netAmount(new BigDecimal("900.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .destinationAccountId(accountId)
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();

        when(jpaRepository.save(any(JpaTransactionEntity.class))).thenReturn(entity);
        when(mapper.JpaToTransaction(entity)).thenReturn(transaction);

        Transaction result = repositoryAdapter.save(transaction);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(transactionId);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

}

