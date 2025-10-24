package com.ontop.challenge.application.service;

import com.ontop.challenge.adapters.in.dto.EventMessage;
import com.ontop.challenge.adapters.in.dto.WithdrawRequest;
import com.ontop.challenge.adapters.in.dto.WithdrawalResponse;
import com.ontop.challenge.domain.Transaction;
import com.ontop.challenge.domain.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static com.ontop.challenge.utils.TransactionUtils.createTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawalListenerTest {

    @Mock
    private WithdrawService withdrawService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private WithdrawalListener withdrawalListener;

    private final Transaction pendingTransaction = createTransaction(TransactionStatus.PENDING);


    @Test
    void handleWalletWithdraw_Success_SavesResponseToRedis() {
        String idempotencyKey = "test-key-123";

        WithdrawRequest request = WithdrawRequest.builder()
                .userId(1000L)
                .accountId(UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .idempotencyKey(idempotencyKey)
                .build();

        WithdrawalResponse expectedResponse = WithdrawalResponse.builder()
                .transactionId(UUID.randomUUID())
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("1000.00"))
                .fee(new BigDecimal("100.00"))
                .netAmount(new BigDecimal("900.00"))
                .currency("USD")
                .build();

        EventMessage message = EventMessage.builder().req(request).transaction(pendingTransaction).build();

        when(withdrawService.executeWithdrawal(request, pendingTransaction)).thenReturn(expectedResponse);

        withdrawalListener.handleWalletWithdraw(message);

        verify(withdrawService).executeWithdrawal(request, pendingTransaction);
        
        ArgumentCaptor<WithdrawalResponse> responseCaptor = ArgumentCaptor.forClass(WithdrawalResponse.class);
        verify(idempotencyService).saveResponse(eq(idempotencyKey), responseCaptor.capture());
        
        WithdrawalResponse savedResponse = responseCaptor.getValue();
        assertThat(savedResponse.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(savedResponse.getAmount()).isEqualTo(new BigDecimal("1000.00"));
    }

    @Test
    void handleWalletWithdraw_WhenServiceThrowsException_SavesFailedResponseToRedis() {
        // Given
        String idempotencyKey = "test-key-123";
        WithdrawRequest request = WithdrawRequest.builder()
                .userId(1000L)
                .accountId(UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .idempotencyKey(idempotencyKey)
                .build();

        EventMessage message = EventMessage.builder().req(request).transaction(pendingTransaction).build();

        when(withdrawService.executeWithdrawal(request, pendingTransaction))
                .thenThrow(new RuntimeException("Service error"));

        withdrawalListener.handleWalletWithdraw(message);

        verify(withdrawService).executeWithdrawal(request, pendingTransaction);
        
        ArgumentCaptor<WithdrawalResponse> responseCaptor = ArgumentCaptor.forClass(WithdrawalResponse.class);
        verify(idempotencyService).saveResponse(eq(idempotencyKey), responseCaptor.capture());
        
        WithdrawalResponse savedResponse = responseCaptor.getValue();
        assertThat(savedResponse.getStatus()).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    void handleWalletWithdraw_WhenExceptionAndNoIdempotencyKey_DoesNotCallSave() {
        WithdrawRequest request = WithdrawRequest.builder()
                .userId(1000L)
                .accountId(UUID.randomUUID())
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .idempotencyKey(null)
                .build();
        EventMessage message = EventMessage.builder().req(request).transaction(pendingTransaction).build();

        when(withdrawService.executeWithdrawal(request, pendingTransaction))
                .thenThrow(new RuntimeException("Service error"));

        withdrawalListener.handleWalletWithdraw(message);

        verify(withdrawService).executeWithdrawal(request, pendingTransaction);
        verify(idempotencyService, never()).saveResponse(any(), any());
    }
}

