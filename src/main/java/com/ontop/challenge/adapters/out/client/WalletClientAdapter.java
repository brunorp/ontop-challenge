package com.ontop.challenge.adapters.out.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ontop.challenge.adapters.out.client.dto.wallet.BalanceResponse;
import com.ontop.challenge.adapters.out.client.dto.wallet.WalletTransactionRequest;
import com.ontop.challenge.adapters.out.client.dto.wallet.WalletTransactionResponse;
import com.ontop.challenge.application.port.out.WalletClientPort;
import com.ontop.challenge.application.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Slf4j
public class WalletClientAdapter implements WalletClientPort {

    @Value("${ontop.clients.wallet-base-url}")
    private String walletBaseUrl;

    private final RestTemplate restTemplate;

    public WalletClientAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Retry(name = "walletService", fallbackMethod = "getBalanceFallback")
    @CircuitBreaker(name = "walletService", fallbackMethod = "getBalanceFallback")
    public Optional<BigDecimal> getBalance(Long userId) {
        log.info("Fetching wallet balance for userId: {}", userId);

        try {
            String url = walletBaseUrl + "/wallets/balance?user_id=" + userId;
            
            ResponseEntity<BalanceResponse> response = restTemplate.getForEntity(
                    url, 
                    BalanceResponse.class
            );

            if (response.getBody() != null && response.getBody().getBalance() != null) {
                log.info("Successfully retrieved balance for userId: {}, balance: {}", 
                        userId, response.getBody().getBalance());
                return Optional.of(response.getBody().getBalance());
            }

            log.warn("Empty response received for userId: {}", userId);
            return Optional.empty();

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("User not found in wallet service, userId: {}", userId);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error fetching wallet balance for userId: {}, error: {}", userId, e.getMessage(), e);
            throw new ExternalServiceException("Failed to fetch wallet balance for user: " + userId, e);
        }
    }

    /**
     * Fallback method for getBalance when circuit is open or retries exhausted
     */
    private Optional<BigDecimal> getBalanceFallback(Long userId, Exception e) {
        log.error("Fallback triggered for getBalance, userId: {}, error: {}", userId, e.getMessage());
        throw new ExternalServiceException("Wallet service is currently unavailable. Please try again later.", e);
    }

    @Override
    @Retry(name = "walletService", fallbackMethod = "createWalletTransactionFallback")
    @CircuitBreaker(name = "walletService", fallbackMethod = "createWalletTransactionFallback")
    public Optional<Long> createWalletTransaction(Long userId, BigDecimal amount) {
        log.info("Creating wallet transaction for userId: {}, amount: {}", userId, amount);

        try {
            String url = walletBaseUrl + "/wallets/transactions";

            WalletTransactionRequest request = WalletTransactionRequest.builder()
                    .userId(userId)
                    .amount(amount)
                    .build();

            ResponseEntity<WalletTransactionResponse> response = restTemplate.postForEntity(
                    url,
                    request,
                    WalletTransactionResponse.class
            );

            if (response.getBody() != null && response.getBody().getWalletTransactionId() != null) {
                log.info("Successfully created wallet transaction for userId: {}, transactionId: {}",
                        userId, response.getBody().getWalletTransactionId());
                return Optional.of(response.getBody().getWalletTransactionId());
            }

            log.warn("Empty response received when creating wallet transaction for userId: {}", userId);
            return Optional.empty();

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("User not found when creating wallet transaction, userId: {}", userId);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error creating wallet transaction for userId: {}, error: {}", userId, e.getMessage(), e);
            throw new ExternalServiceException("Failed to create wallet transaction for user: " + userId, e);
        }
    }

    /**
     * Fallback method for createWalletTransaction when circuit is open or retries exhausted
     */
    private Optional<Long> createWalletTransactionFallback(Long userId, BigDecimal amount, Exception e) {
        log.error("Fallback triggered for createWalletTransaction, userId: {}, amount: {}, error: {}", 
                userId, amount, e.getMessage());
        throw new ExternalServiceException("Wallet service is currently unavailable. Please try again later.", e);
    }
}
