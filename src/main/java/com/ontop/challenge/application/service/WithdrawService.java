package com.ontop.challenge.application.service;

import com.ontop.challenge.adapters.out.persistence.mapper.TransactionMapper;
import com.ontop.challenge.application.port.out.AccountServicePort;
import com.ontop.challenge.application.port.out.PaymentsClientPort;
import com.ontop.challenge.application.port.out.WalletClientPort;
import com.ontop.challenge.infrastructure.config.WithdrawalConfig;
import com.ontop.challenge.adapters.out.client.dto.payment.PaymentRequest;
import com.ontop.challenge.adapters.out.client.dto.payment.PaymentResponse;
import com.ontop.challenge.application.port.out.TransactionRepositoryPort;
import com.ontop.challenge.application.exception.InsufficientFundsException;
import com.ontop.challenge.domain.Transaction;
import com.ontop.challenge.domain.TransactionStatus;
import com.ontop.challenge.adapters.in.dto.WithdrawRequest;
import com.ontop.challenge.adapters.in.dto.WithdrawalResponse;
import com.ontop.challenge.application.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class WithdrawService {

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final WalletClientPort walletClient;
    private final PaymentsClientPort paymentsClient;
    private final AccountServicePort accountService;
    private final WithdrawalConfig config;
    private final TransactionMapper mapper;

    public WithdrawService(
            TransactionRepositoryPort transactionRepositoryPort,
            WalletClientPort walletClient,
            PaymentsClientPort paymentsClient,
            AccountServicePort accountService,
            WithdrawalConfig config, TransactionMapper mapper) {
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.walletClient = walletClient;
        this.paymentsClient = paymentsClient;
        this.accountService = accountService;
        this.config = config;
        this.mapper = mapper;
    }

    @Transactional
    public WithdrawalResponse executeWithdrawal(WithdrawRequest req, Transaction transaction) {
        WithdrawalContext context = prepareWithdrawal(req);
        ensureSufficientFunds(req.getUserId(), context.totalDebit);
        return processWithdrawal(transaction, req, context);
    }

    public Transaction createInitialTransaction(WithdrawRequest req) {
        WithdrawalContext context = prepareWithdrawal(req);

        ensureSufficientFunds(req.getUserId(), context.totalDebit);

        return createPendingTransaction(context);

    }

    private WithdrawalContext prepareWithdrawal(WithdrawRequest req) {
        BigDecimal fee = calculateFee(req.getAmount());
        BigDecimal netAmount = req.getAmount().subtract(fee);
        BigDecimal totalDebit = req.getAmount();

        log.info("Starting withdrawal for userId: {}, amount: {}", req.getUserId(), req.getAmount());

        return new WithdrawalContext(req, fee, netAmount, totalDebit);
    }

    private void ensureSufficientFunds(Long userId, BigDecimal requiredAmount) {
        BigDecimal balance = walletClient.getBalance(userId)
                .orElseThrow(() -> new IllegalArgumentException("User wallet not found: " + userId));

        if (balance.compareTo(requiredAmount) < 0) {
            log.warn("Insufficient funds for userId: {}, balance: {}, required: {}", userId, balance, requiredAmount);
            throw new InsufficientFundsException(
                    String.format("Insufficient funds. Balance: %s, Required: %s", balance, requiredAmount));
        }
    }

    private Transaction createPendingTransaction(WithdrawalContext context) {
        Transaction transaction = new Transaction(
            null,
            context.request.getUserId(),
            context.request.getAmount(),
            context.fee,
            context.netAmount,
            context.request.getCurrency(),
            TransactionStatus.PENDING,
            null,
            null,
            null,
            context.request.getAccountId(),
            null,
            null,
            null);

        transaction = transactionRepositoryPort.save(transaction);
        log.info("Created PENDING transaction, transactionId: {}", transaction.getId());
        return transaction;
    }

    private WithdrawalResponse processWithdrawal(Transaction transaction, WithdrawRequest req, WithdrawalContext context) {
        try {
            transaction.setStatus(TransactionStatus.PROCESSING);
            transaction = transactionRepositoryPort.save(transaction);

            debitWallet(transaction, req.getUserId(), context.totalDebit);
            sendPayment(transaction, req, context.netAmount);

            transaction = transactionRepositoryPort.save(transaction);
            log.info("Withdrawal completed, transactionId: {}, status: {}",
                    transaction.getId(), transaction.getStatus());

            return mapper.transactionToWithdrawalResponse(transaction);

        } catch (ExternalServiceException e) {
            return handleFailure(transaction, e);
        }
    }

    private void debitWallet(Transaction transaction, Long userId, BigDecimal amount) {
        Long walletTxId = walletClient.createWalletTransaction(userId, amount)
                .orElseThrow(() -> new ExternalServiceException("Failed to create wallet transaction"));

        transaction.setWalletTxId(walletTxId);
        transactionRepositoryPort.save(transaction);
        log.info("Wallet debited, walletTxId: {}", walletTxId);
    }

    private void sendPayment(Transaction transaction, WithdrawRequest req, BigDecimal netAmount) {
        PaymentRequest paymentRequest = buildPaymentRequest(req, netAmount);
        PaymentResponse paymentResponse = paymentsClient.createPayment(paymentRequest);

        if (paymentResponse.getPaymentInfo() != null) {
            transaction.setProviderPaymentId(paymentResponse.getPaymentInfo().getId());
        }

        if (isPaymentSuccessful(paymentResponse)) {
            transaction.setStatus(TransactionStatus.COMPLETED);
            log.info("Payment successful, transaction COMPLETED");
        } else {
            String reason = extractFailureReason(paymentResponse);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(reason);
            log.warn("Payment failed, reason: {}", reason);
        }
    }

    private boolean isPaymentSuccessful(PaymentResponse response) {
        return response.getRequestInfo() != null
                && "Processing".equalsIgnoreCase(response.getRequestInfo().getStatus());
    }

    private String extractFailureReason(PaymentResponse response) {
        return response.getRequestInfo() != null
                ? response.getRequestInfo().getError()
                : "Unknown payment status error";
    }

    private WithdrawalResponse handleFailure(Transaction transaction, ExternalServiceException e) {
        log.error("External service error, transactionId: {}, error: {}",
                transaction.getId(), e.getMessage());

        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(e.getMessage());
        transaction = transactionRepositoryPort.save(transaction);

        return mapper.transactionToWithdrawalResponse(transaction);
    }

    private record WithdrawalContext(
            WithdrawRequest request,
            BigDecimal fee,
            BigDecimal netAmount,
            BigDecimal totalDebit
    ) {}

    /**
     * Calculate withdrawal fee based on configured percentage
     */
    private BigDecimal calculateFee(BigDecimal amount) {
        return amount
                .multiply(config.getFeePercentage())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Helper method to build PaymentRequest from WithdrawRequest
     */
    private PaymentRequest buildPaymentRequest(WithdrawRequest req, BigDecimal netAmount) {
        // Build source (company account from configuration)
        WithdrawalConfig.CompanyAccount companyAcct = config.getCompanyAccount();
        
        PaymentRequest.Source.SourceInformation sourceInfo = PaymentRequest.Source.SourceInformation.builder()
                .name(companyAcct.getName())
                .build();

        PaymentRequest.Source.Account sourceAccount = PaymentRequest.Source.Account.builder()
                .accountNumber(companyAcct.getAccountNumber())
                .currency(companyAcct.getCurrency())
                .routingNumber(companyAcct.getRoutingNumber())
                .build();

        PaymentRequest.Source source = PaymentRequest.Source.builder()
                .type("COMPANY")
                .sourceInformation(sourceInfo)
                .account(sourceAccount)
                .build();

        // Build destination (user's bank account)
        AccountServicePort.AccountDetails accountDetails = accountService
                .getAccountDetails(req.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found: " + req.getAccountId()));

        PaymentRequest.Destination.Account destAccount = PaymentRequest.Destination.Account.builder()
                .accountNumber(accountDetails.accountNumber())
                .currency(accountDetails.currency())
                .routingNumber(accountDetails.routingNumber())
                .build();

        PaymentRequest.Destination destination = PaymentRequest.Destination.builder()
                .name(accountDetails.accountHolderName())
                .account(destAccount)
                .build();

        return PaymentRequest.builder()
                .source(source)
                .destination(destination)
                .amount(netAmount)
                .build();
    }
}

