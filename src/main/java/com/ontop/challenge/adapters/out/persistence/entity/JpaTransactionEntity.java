package com.ontop.challenge.adapters.out.persistence.entity;

import com.ontop.challenge.domain.Transaction;
import com.ontop.challenge.domain.TransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JpaTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fee;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column
    private String providerPaymentId;

    @Column
    private Long walletTxId;

    @Column
    private String failureReason;

    @Column(nullable = false)
    private UUID destinationAccountId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public JpaTransactionEntity(Transaction transaction) {
        this.id = transaction.getId();
        this.userId = transaction.getUserId();
        this.amount = transaction.getAmount();
        this.currency = transaction.getCurrency();
        this.destinationAccountId = transaction.getDestinationAccountId();
        this.fee = transaction.getFee();
        this.netAmount = transaction.getNetAmount();
        this.status = transaction.getStatus();
        this.providerPaymentId = transaction.getProviderPaymentId();
        this.walletTxId = transaction.getWalletTxId();
        this.failureReason = transaction.getFailureReason();
        this.createdAt = transaction.getCreatedAt();
        this.updatedAt = transaction.getUpdatedAt();
        this.version = transaction.getVersion();
    }
}
