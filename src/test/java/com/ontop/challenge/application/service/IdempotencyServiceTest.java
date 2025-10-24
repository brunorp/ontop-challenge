package com.ontop.challenge.application.service;

import com.ontop.challenge.adapters.in.dto.WithdrawalResponse;
import com.ontop.challenge.domain.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        idempotencyService = new IdempotencyService(redisTemplate);
    }

    @Test
    void getResponse_WhenKeyExists_ReturnsResponse() {
        String idempotencyKey = "test-key-123";
        WithdrawalResponse expectedResponse = WithdrawalResponse.builder()
                .transactionId(UUID.randomUUID())
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("1000.00"))
                .fee(new BigDecimal("100.00"))
                .netAmount(new BigDecimal("900.00"))
                .currency("USD")
                .build();

        when(valueOperations.get("idempotency:test-key-123")).thenReturn(expectedResponse);

        Optional<WithdrawalResponse> result = idempotencyService.getResponse(idempotencyKey);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedResponse);
        assertThat(result.get().getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(valueOperations).get("idempotency:test-key-123");
    }

    @Test
    void getResponse_WhenKeyDoesNotExist_ReturnsEmpty() {
        String idempotencyKey = "non-existent-key";
        when(valueOperations.get("idempotency:non-existent-key")).thenReturn(null);

        Optional<WithdrawalResponse> result = idempotencyService.getResponse(idempotencyKey);

        assertThat(result).isEmpty();
        verify(valueOperations).get("idempotency:non-existent-key");
    }

    @Test
    void getResponse_WhenKeyIsNull_ReturnsEmpty() {
        Optional<WithdrawalResponse> result = idempotencyService.getResponse(null);

        assertThat(result).isEmpty();
        verify(valueOperations, never()).get(any());
    }

    @Test
    void getResponse_WhenKeyIsEmpty_ReturnsEmpty() {
        Optional<WithdrawalResponse> result = idempotencyService.getResponse("");

        assertThat(result).isEmpty();
        verify(valueOperations, never()).get(any());
    }

    @Test
    void getResponse_WhenValueIsNotWithdrawalResponse_ReturnsEmpty() {
        String idempotencyKey = "test-key-123";
        when(valueOperations.get("idempotency:test-key-123")).thenReturn("wrong-type");

        Optional<WithdrawalResponse> result = idempotencyService.getResponse(idempotencyKey);

        assertThat(result).isEmpty();
        verify(valueOperations).get("idempotency:test-key-123");
    }

    @Test
    void getResponse_WhenRedisThrowsException_ReturnsEmpty() {
        String idempotencyKey = "test-key-123";
        when(valueOperations.get("idempotency:test-key-123"))
                .thenThrow(new RuntimeException("Redis connection error"));

        Optional<WithdrawalResponse> result = idempotencyService.getResponse(idempotencyKey);

        assertThat(result).isEmpty();
        verify(valueOperations).get("idempotency:test-key-123");
    }

    @Test
    void saveResponse_Success_SavesWithCorrectTTL() {
        String idempotencyKey = "test-key-123";
        WithdrawalResponse response = WithdrawalResponse.builder()
                .transactionId(UUID.randomUUID())
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("1000.00"))
                .build();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

        idempotencyService.saveResponse(idempotencyKey, response);

        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), durationCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo("idempotency:test-key-123");
        assertThat(valueCaptor.getValue()).isEqualTo(response);
        assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void saveResponse_WhenKeyIsNull_DoesNotSave() {
        WithdrawalResponse response = WithdrawalResponse.builder()
                .status(TransactionStatus.COMPLETED)
                .build();

        idempotencyService.saveResponse(null, response);

        verify(valueOperations, never()).set(any(), any(), any());
    }

    @Test
    void saveResponse_WhenKeyIsEmpty_DoesNotSave() {
        WithdrawalResponse response = WithdrawalResponse.builder()
                .status(TransactionStatus.COMPLETED)
                .build();

        idempotencyService.saveResponse("", response);

        verify(valueOperations, never()).set(any(), any(), any());
    }

    @Test
    void saveResponse_WhenResponseIsNull_DoesNotSave() {
        idempotencyService.saveResponse("test-key", null);

        verify(valueOperations, never()).set(any(), any(), any());
    }

    @Test
    void saveResponse_WhenRedisThrowsException_HandlesGracefully() {
        String idempotencyKey = "test-key-123";
        WithdrawalResponse response = WithdrawalResponse.builder()
                .status(TransactionStatus.COMPLETED)
                .build();

        doThrow(new RuntimeException("Redis connection error"))
                .when(valueOperations).set(any(), any(), any(Duration.class));

        idempotencyService.saveResponse(idempotencyKey, response);

        verify(valueOperations).set(eq("idempotency:test-key-123"), eq(response), any(Duration.class));
    }
}

