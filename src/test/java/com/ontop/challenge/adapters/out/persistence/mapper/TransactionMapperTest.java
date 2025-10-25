package com.ontop.challenge.adapters.out.persistence.mapper;

import com.ontop.challenge.adapters.out.persistence.entity.JpaTransactionEntity;
import com.ontop.challenge.domain.Transaction;
import com.ontop.challenge.domain.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTest {

    private TransactionMapper transactionMapper;

    @BeforeEach
    void setUp() {
        transactionMapper = new TransactionMapper();
    }

    @Test
    void jpaToTransaction_MapsAllFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        UUID destinationAccountId = UUID.randomUUID();
        Instant now = Instant.now();
        
        JpaTransactionEntity entity = JpaTransactionEntity.builder()
                .id(id)
                .userId(1000L)
                .amount(new BigDecimal("1000.00"))
                .fee(new BigDecimal("100.00"))
                .netAmount(new BigDecimal("900.00"))
                .currency("USD")
                .status(TransactionStatus.COMPLETED)
                .providerPaymentId("payment-123")
                .walletTxId(59974L)
                .failureReason(null)
                .destinationAccountId(destinationAccountId)
                .createdAt(now)
                .updatedAt(now)
                .version(1L)
                .build();

        Transaction result = transactionMapper.JpaToTransaction(entity);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getUserId()).isEqualTo(1000L);
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(result.getFee()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getNetAmount()).isEqualTo(new BigDecimal("900.00"));
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.getProviderPaymentId()).isEqualTo("payment-123");
        assertThat(result.getWalletTxId()).isEqualTo(59974L);
        assertThat(result.getFailureReason()).isNull();
        assertThat(result.getDestinationAccountId()).isEqualTo(destinationAccountId);
        assertThat(result.getCreatedAt()).isEqualTo(now);
        assertThat(result.getUpdatedAt()).isEqualTo(now);
        assertThat(result.getVersion()).isEqualTo(1L);
    }
}

