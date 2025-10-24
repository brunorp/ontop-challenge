package com.ontop.challenge.adapters.in;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontop.challenge.adapters.in.dto.WithdrawRequest;
import com.ontop.challenge.adapters.in.dto.WithdrawalResponse;
import com.ontop.challenge.application.service.IdempotencyService;
import com.ontop.challenge.domain.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for WithdrawalController considering authentication.
 * Focuses on testing authentication, authorization, and request validation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WithdrawalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IdempotencyService idempotencyService;

    @MockitoBean
    private com.ontop.challenge.application.port.out.WalletClientPort walletClientPort;

    @MockitoBean
    private com.ontop.challenge.application.port.out.PaymentsClientPort paymentsClientPort;
    
    @MockitoBean
    private com.ontop.challenge.application.port.out.TransactionRepositoryPort transactionRepositoryPort;

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void withdraw_WithCachedResponse_ReturnsCachedResult() throws Exception {
        String idempotencyKey = "test-key-123";
        UUID accountId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        WithdrawRequest request = WithdrawRequest.builder()
                .userId(1000L)
                .accountId(accountId)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .build();

        WithdrawalResponse cachedResponse = WithdrawalResponse.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("1000.00"))
                .fee(new BigDecimal("100.00"))
                .netAmount(new BigDecimal("900.00"))
                .currency("USD")
                .build();

        when(idempotencyService.getResponse(idempotencyKey))
                .thenReturn(Optional.of(cachedResponse));

        mockMvc.perform(post("/api/v1/withdrawals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(1000.00))
                .andExpect(jsonPath("$.fee").value(100.00))
                .andExpect(jsonPath("$.netAmount").value(900.00));

        verify(idempotencyService).getResponse(idempotencyKey);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void withdraw_WithoutCachedResponse_ReturnsAccepted() throws Exception {
        String idempotencyKey = "test-key-456";
        UUID accountId = UUID.randomUUID();

        WithdrawRequest request = WithdrawRequest.builder()
                .userId(1000L)
                .accountId(accountId)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .build();

        when(idempotencyService.getResponse(idempotencyKey))
                .thenReturn(Optional.empty());

        when(walletClientPort.getBalance(1000L)).thenReturn(Optional.of(new BigDecimal("5000.00")));
        when(transactionRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/withdrawals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(idempotencyService).getResponse(idempotencyKey);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void withdraw_WithInvalidAmount_ReturnsBadRequest() throws Exception {
        String idempotencyKey = "test-key-789";
        UUID accountId = UUID.randomUUID();

        WithdrawRequest request = WithdrawRequest.builder()
                .userId(1000L)
                .accountId(accountId)
                .amount(BigDecimal.ZERO)
                .currency("USD")
                .build();

        mockMvc.perform(post("/api/v1/withdrawals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(idempotencyService, never()).getResponse(any());
    }

    @Test
    void withdraw_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        String idempotencyKey = "test-key-999";
        UUID accountId = UUID.randomUUID();

        WithdrawRequest request = WithdrawRequest.builder()
                .userId(1000L)
                .accountId(accountId)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .build();

        mockMvc.perform(post("/api/v1/withdrawals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(idempotencyService, never()).getResponse(any());
    }

    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void withdraw_WithAdminRole_IsAllowed() throws Exception {
        String idempotencyKey = "test-key-admin";
        UUID accountId = UUID.randomUUID();

        WithdrawRequest request = WithdrawRequest.builder()
                .userId(1000L)
                .accountId(accountId)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .build();

        when(idempotencyService.getResponse(idempotencyKey))
                .thenReturn(Optional.empty());

        when(walletClientPort.getBalance(1000L)).thenReturn(Optional.of(new BigDecimal("5000.00")));
        when(transactionRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/withdrawals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(idempotencyService).getResponse(idempotencyKey);
    }
}

