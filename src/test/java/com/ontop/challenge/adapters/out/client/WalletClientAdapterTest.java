package com.ontop.challenge.adapters.out.client;

import com.ontop.challenge.adapters.out.client.dto.wallet.BalanceResponse;
import com.ontop.challenge.adapters.out.client.dto.wallet.WalletTransactionRequest;
import com.ontop.challenge.adapters.out.client.dto.wallet.WalletTransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletClientAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    private WalletClientAdapter walletClientAdapter;

    private static final Long USER_ID = 1000L;

    @BeforeEach
    void setUp() {
        walletClientAdapter = new WalletClientAdapter(restTemplate);
        try {
            java.lang.reflect.Field field = WalletClientAdapter.class.getDeclaredField("walletBaseUrl");
            field.setAccessible(true);
            field.set(walletClientAdapter, "http://wallet-api");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getBalance_Success_ReturnsBalance() {
        BalanceResponse balanceResponse = new BalanceResponse();
        balanceResponse.setBalance(new BigDecimal("5000.00"));

        ResponseEntity<BalanceResponse> responseEntity = new ResponseEntity<>(balanceResponse, HttpStatus.OK);
        when(restTemplate.getForEntity(
                eq("http://wallet-api/wallets/balance?user_id=" + USER_ID),
                eq(BalanceResponse.class)
        )).thenReturn(responseEntity);

        Optional<BigDecimal> result = walletClientAdapter.getBalance(USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void getBalance_WhenUserNotFound_ReturnsEmpty() {
        when(restTemplate.getForEntity(
                anyString(),
                eq(BalanceResponse.class)
        )).thenThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "", null, null, null));

        Optional<BigDecimal> result = walletClientAdapter.getBalance(USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void getBalance_WhenResponseBodyIsNull_ReturnsEmpty() {
        ResponseEntity<BalanceResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.getForEntity(
                anyString(),
                eq(BalanceResponse.class)
        )).thenReturn(responseEntity);

        Optional<BigDecimal> result = walletClientAdapter.getBalance(USER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void createWalletTransaction_Success_ReturnsTransactionId() {
        BigDecimal amount = new BigDecimal("1000.00");
        WalletTransactionResponse transactionResponse = new WalletTransactionResponse();
        transactionResponse.setWalletTransactionId(59974L);
        transactionResponse.setAmount(amount);

        ResponseEntity<WalletTransactionResponse> responseEntity =
                new ResponseEntity<>(transactionResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(
                eq("http://wallet-api/wallets/transactions"),
                any(WalletTransactionRequest.class),
                eq(WalletTransactionResponse.class)
        )).thenReturn(responseEntity);

        Optional<Long> result = walletClientAdapter.createWalletTransaction(USER_ID, amount);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(59974L);
    }

    @Test
    void createWalletTransaction_WhenTransactionNotFound_ReturnsEmpty() {
        BigDecimal amount = new BigDecimal("1000.00");
        when(restTemplate.postForEntity(
                anyString(),
                any(WalletTransactionRequest.class),
                eq(WalletTransactionResponse.class)
        )).thenThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "", null, null, null));

        Optional<Long> result = walletClientAdapter.createWalletTransaction(USER_ID, amount);

        assertThat(result).isEmpty();
    }

    @Test
    void createWalletTransaction_WhenResponseBodyIsNull_ReturnsEmpty() {
        BigDecimal amount = new BigDecimal("1000.00");
        ResponseEntity<WalletTransactionResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.postForEntity(
                anyString(),
                any(WalletTransactionRequest.class),
                eq(WalletTransactionResponse.class)
        )).thenReturn(responseEntity);

        Optional<Long> result = walletClientAdapter.createWalletTransaction(USER_ID, amount);

        assertThat(result).isEmpty();
    }
}

