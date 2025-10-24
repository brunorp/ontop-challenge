package com.ontop.challenge.application.service;

import com.ontop.challenge.adapters.in.dto.EventMessage;
import com.ontop.challenge.adapters.in.dto.WithdrawRequest;
import com.ontop.challenge.adapters.in.dto.WithdrawalResponse;
import com.ontop.challenge.adapters.out.client.dto.payment.PaymentRequest;
import com.ontop.challenge.adapters.out.client.dto.payment.PaymentResponse;
import com.ontop.challenge.adapters.out.persistence.mapper.TransactionMapper;
import com.ontop.challenge.application.exception.ExternalServiceException;
import com.ontop.challenge.application.exception.InsufficientFundsException;
import com.ontop.challenge.application.port.out.AccountServicePort;
import com.ontop.challenge.application.port.out.PaymentsClientPort;
import com.ontop.challenge.application.port.out.TransactionRepositoryPort;
import com.ontop.challenge.application.port.out.WalletClientPort;
import com.ontop.challenge.domain.Transaction;
import com.ontop.challenge.domain.TransactionStatus;
import com.ontop.challenge.infrastructure.config.WithdrawalConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.ontop.challenge.utils.TransactionUtils.createTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawServiceTest {

    @Mock
    private TransactionRepositoryPort transactionRepositoryPort;

    @Mock
    private WalletClientPort walletClient;

    @Mock
    private TransactionMapper mapper;

    @Mock
    private PaymentsClientPort paymentsClient;

    @Mock
    private AccountServicePort accountService;

    @Mock
    private WithdrawalConfig config;

    @InjectMocks
    private WithdrawService withdrawService;

    private WithdrawRequest validRequest;
    private UUID transactionId;

    private final Transaction pendingTransaction = createTransaction(TransactionStatus.PENDING);

    @BeforeEach
    void setUp() {
        UUID accountId = UUID.randomUUID();
        transactionId = UUID.randomUUID();

        validRequest = WithdrawRequest.builder()
                .userId(1000L)
                .accountId(accountId)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .idempotencyKey("test-idempotency-key")
                .build();

        WithdrawalConfig.CompanyAccount companyAccount =
                new WithdrawalConfig.CompanyAccount();
        companyAccount.setName("ONTOP INC");
        companyAccount.setAccountNumber("0245253419");
        companyAccount.setRoutingNumber("028444018");
        companyAccount.setCurrency("USD");

        lenient().when(config.getFeePercentage()).thenReturn(new BigDecimal("0.10"));
        lenient().when(config.getCompanyAccount()).thenReturn(companyAccount);

        AccountServicePort.AccountDetails accountDetails = new AccountServicePort.AccountDetails(
                "TONY STARK",
                "1885226711",
                "211927207",
                "USD"
        );
        lenient().when(accountService.getAccountDetails(any(UUID.class))).thenReturn(Optional.of(accountDetails));
    }

    @Test
    void executeWithdrawal_HappyPath_ReturnsCompletedStatus() {
        BigDecimal balance = new BigDecimal("5000.00");
        long walletTxId = 59974L;
        String paymentId = "70cfe468";

        Transaction processingTransaction = createTransaction(TransactionStatus.PROCESSING);
        Transaction completedTransaction = createTransaction(TransactionStatus.COMPLETED);
        completedTransaction.setWalletTxId(walletTxId);
        completedTransaction.setProviderPaymentId(paymentId);

        when(walletClient.getBalance(1000L)).thenReturn(Optional.of(balance));
        when(transactionRepositoryPort.save(any(Transaction.class)))
                .thenReturn(pendingTransaction)
                .thenReturn(processingTransaction)
                .thenReturn(processingTransaction)
                .thenReturn(completedTransaction);
        when(walletClient.createWalletTransaction(1000L, new BigDecimal("1000.00"))).thenReturn(Optional.of(walletTxId));

        PaymentResponse paymentResponse = createSuccessfulPaymentResponse(paymentId);
        when(paymentsClient.createPayment(any(PaymentRequest.class))).thenReturn(paymentResponse);

        WithdrawalResponse expectedResponse = WithdrawalResponse.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("1000.00"))
                .fee(new BigDecimal("100.00"))
                .netAmount(new BigDecimal("900.00"))
                .currency("USD")
                .build();
        when(mapper.transactionToWithdrawalResponse(any(Transaction.class))).thenReturn(expectedResponse);

        WithdrawalResponse response = withdrawService.executeWithdrawal(validRequest, pendingTransaction);

        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(transactionId);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(response.getFee()).isEqualTo(new BigDecimal("100.00"));
        assertThat(response.getNetAmount()).isEqualTo(new BigDecimal("900.00"));
        assertThat(response.getCurrency()).isEqualTo("USD");

        verify(walletClient).getBalance(1000L);
        verify(walletClient).createWalletTransaction(1000L, new BigDecimal("1000.00"));
        verify(paymentsClient).createPayment(any(PaymentRequest.class));
        verify(transactionRepositoryPort, times(3)).save(any(Transaction.class));
    }

    @Test
    void executeWithdrawal_InsufficientFunds_ThrowsException() {
        BigDecimal insufficientBalance = new BigDecimal("500.00");

        when(walletClient.getBalance(1000L)).thenReturn(Optional.of(insufficientBalance));

        assertThatThrownBy(() -> withdrawService.executeWithdrawal(validRequest, pendingTransaction))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");

        verify(walletClient).getBalance(1000L);
        verify(walletClient, never()).createWalletTransaction(anyLong(), any(BigDecimal.class));
        verify(paymentsClient, never()).createPayment(any(PaymentRequest.class));
        verify(transactionRepositoryPort, never()).save(any(Transaction.class));
    }

    @Test
    void executeWithdrawal_ExternalPaymentsFailure_MarksAsFailed() {
        BigDecimal balance = new BigDecimal("5000.00");
        long walletTxId = 59974L;

        Transaction pendingTransaction = createTransaction(TransactionStatus.PENDING);
        Transaction processingTransaction = createTransaction(TransactionStatus.PROCESSING);
        Transaction failedTransaction = createTransaction(TransactionStatus.FAILED);
        failedTransaction.setWalletTxId(walletTxId);
        failedTransaction.setFailureReason("Failed to create payment");

        when(walletClient.getBalance(1000L)).thenReturn(Optional.of(balance));
        when(transactionRepositoryPort.save(any(Transaction.class)))
                .thenReturn(pendingTransaction)
                .thenReturn(processingTransaction)
                .thenReturn(processingTransaction)
                .thenReturn(failedTransaction);
        when(walletClient.createWalletTransaction(1000L, new BigDecimal("1000.00"))).thenReturn(Optional.of(walletTxId));
        when(paymentsClient.createPayment(any(PaymentRequest.class)))
                .thenThrow(new ExternalServiceException("Failed to create payment"));

        WithdrawalResponse failedResponse = WithdrawalResponse.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.FAILED)
                .amount(new BigDecimal("1000.00"))
                .fee(new BigDecimal("100.00"))
                .netAmount(new BigDecimal("900.00"))
                .currency("USD")
                .build();
        when(mapper.transactionToWithdrawalResponse(any(Transaction.class))).thenReturn(failedResponse);

        WithdrawalResponse response = withdrawService.executeWithdrawal(validRequest, pendingTransaction);

        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(transactionId);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);

        verify(walletClient).getBalance(1000L);
        verify(walletClient).createWalletTransaction(1000L, new BigDecimal("1000.00"));
        verify(paymentsClient).createPayment(any(PaymentRequest.class));
        verify(transactionRepositoryPort, times(3)).save(any(Transaction.class));
    }

    @Test
    void executeWithdrawal_WalletTransactionFailure_MarksAsFailed() {
        BigDecimal balance = new BigDecimal("5000.00");

        Transaction pendingTransaction = createTransaction(TransactionStatus.PENDING);
        Transaction processingTransaction = createTransaction(TransactionStatus.PROCESSING);
        Transaction failedTransaction = createTransaction(TransactionStatus.FAILED);
        failedTransaction.setFailureReason("Failed to create wallet transaction");

        when(walletClient.getBalance(1000L)).thenReturn(Optional.of(balance));
        when(transactionRepositoryPort.save(any(Transaction.class)))
                .thenReturn(pendingTransaction)
                .thenReturn(processingTransaction)
                .thenReturn(failedTransaction);
        when(walletClient.createWalletTransaction(1000L, new BigDecimal("1000.00")))
                .thenThrow(new ExternalServiceException("Failed to create wallet transaction"));

        WithdrawalResponse failedResponse = WithdrawalResponse.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.FAILED)
                .amount(new BigDecimal("1000.00"))
                .fee(new BigDecimal("100.00"))
                .netAmount(new BigDecimal("900.00"))
                .currency("USD")
                .build();
        when(mapper.transactionToWithdrawalResponse(any(Transaction.class))).thenReturn(failedResponse);

        WithdrawalResponse response = withdrawService.executeWithdrawal(validRequest, pendingTransaction);

        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(transactionId);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);

        verify(walletClient).getBalance(1000L);
        verify(walletClient).createWalletTransaction(1000L, new BigDecimal("1000.00"));
        verify(paymentsClient, never()).createPayment(any(PaymentRequest.class));
        verify(transactionRepositoryPort, times(2)).save(any(Transaction.class));
    }

    @Test
    void executeWithdrawal_PaymentStatusNotProcessing_MarksAsFailed() {
        BigDecimal balance = new BigDecimal("5000.00");
        long walletTxId = 59974L;

        Transaction pendingTransaction = createTransaction(TransactionStatus.PENDING);
        Transaction processingTransaction = createTransaction(TransactionStatus.PROCESSING);
        Transaction failedTransaction = createTransaction(TransactionStatus.FAILED);
        failedTransaction.setWalletTxId(walletTxId);
        failedTransaction.setFailureReason("Payment declined");

        when(walletClient.getBalance(1000L)).thenReturn(Optional.of(balance));
        when(transactionRepositoryPort.save(any(Transaction.class)))
                .thenReturn(pendingTransaction)
                .thenReturn(processingTransaction)
                .thenReturn(processingTransaction)
                .thenReturn(failedTransaction);
        when(walletClient.createWalletTransaction(1000L, new BigDecimal("1000.00"))).thenReturn(Optional.of(walletTxId));

        PaymentResponse paymentResponse = PaymentResponse.builder()
                .requestInfo(PaymentResponse.RequestInfo.builder()
                        .status("Failed")
                        .error("Payment declined")
                        .build())
                .build();
        when(paymentsClient.createPayment(any(PaymentRequest.class))).thenReturn(paymentResponse);

        WithdrawalResponse failedResponse = WithdrawalResponse.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.FAILED)
                .amount(new BigDecimal("1000.00"))
                .fee(new BigDecimal("100.00"))
                .netAmount(new BigDecimal("900.00"))
                .currency("USD")
                .build();
        when(mapper.transactionToWithdrawalResponse(any(Transaction.class))).thenReturn(failedResponse);

        WithdrawalResponse response = withdrawService.executeWithdrawal(validRequest, pendingTransaction);

        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(transactionId);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);

        verify(walletClient).getBalance(1000L);
        verify(walletClient).createWalletTransaction(1000L, new BigDecimal("1000.00"));
        verify(paymentsClient).createPayment(any(PaymentRequest.class));
        verify(transactionRepositoryPort, times(3)).save(any(Transaction.class));
    }

    @Test
    void executeWithdrawal_UserNotFound_ThrowsException() {
        when(walletClient.getBalance(1000L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawService.executeWithdrawal(validRequest, pendingTransaction))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User wallet not found");

        verify(walletClient).getBalance(1000L);
        verify(transactionRepositoryPort, never()).save(any(Transaction.class));
    }

    private PaymentResponse createSuccessfulPaymentResponse(String paymentId) {
        return PaymentResponse.builder()
                .requestInfo(PaymentResponse.RequestInfo.builder()
                        .status("Processing")
                        .build())
                .paymentInfo(PaymentResponse.PaymentInfo.builder()
                        .id(paymentId)
                        .amount(new BigDecimal("900.00"))
                        .currency("USD")
                        .build())
                .build();
    }
}

