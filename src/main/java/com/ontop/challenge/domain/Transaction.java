package com.ontop.challenge.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Transaction {
    private UUID id;
    private Long userId;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal netAmount;
    private String currency;
    private TransactionStatus status;
    private String providerPaymentId;
    private Long walletTxId;
    private String failureReason;
    private UUID destinationAccountId;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    public Transaction () {}

    public Transaction(UUID id, Long userId, BigDecimal amount, BigDecimal fee, BigDecimal netAmount, String currency, TransactionStatus status, String providerPaymentId, Long walletTxId, String failureReason, UUID destinationAccountId, Instant createdAt, Instant updatedAt, Long version) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.fee = fee;
        this.netAmount = netAmount;
        this.currency = currency;
        this.status = status;
        this.providerPaymentId = providerPaymentId;
        this.walletTxId = walletTxId;
        this.failureReason = failureReason;
        this.destinationAccountId = destinationAccountId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }

    public Long getWalletTxId() {
        return walletTxId;
    }

    public void setWalletTxId(Long walletTxId) {
        this.walletTxId = walletTxId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(UUID destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

