package com.ontop.challenge.adapters.out.persistence.mapper;

import com.ontop.challenge.adapters.in.dto.WithdrawalResponse;
import com.ontop.challenge.adapters.out.persistence.entity.JpaTransactionEntity;
import com.ontop.challenge.domain.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    public Transaction JpaToTransaction(JpaTransactionEntity jpaTransactionEntity) {
        return new Transaction(jpaTransactionEntity.getId(), jpaTransactionEntity.getUserId(), jpaTransactionEntity.getAmount(), jpaTransactionEntity.getFee(), jpaTransactionEntity.getNetAmount(), jpaTransactionEntity.getCurrency(), jpaTransactionEntity.getStatus(), jpaTransactionEntity.getProviderPaymentId(), jpaTransactionEntity.getWalletTxId(), jpaTransactionEntity.getFailureReason(), jpaTransactionEntity.getDestinationAccountId(), jpaTransactionEntity.getCreatedAt(), jpaTransactionEntity.getUpdatedAt(), jpaTransactionEntity.getVersion());
    }

    public WithdrawalResponse transactionToWithdrawalResponse(Transaction transaction) {
        return WithdrawalResponse.builder()
                .transactionId(transaction.getId())
                .fee(transaction.getFee())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .netAmount(transaction.getNetAmount())
                .status(transaction.getStatus())
                .build();
    }
}
