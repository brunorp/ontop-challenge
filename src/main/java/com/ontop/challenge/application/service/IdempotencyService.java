package com.ontop.challenge.application.service;

import com.ontop.challenge.adapters.in.dto.WithdrawalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Service to handle idempotency using Redis
 */
@Service
@Slf4j
public class IdempotencyService {

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(1);

    private final RedisTemplate<String, Object> redisTemplate;

    public IdempotencyService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Check if an idempotency key exists and return the cached response
     *
     * @param idempotencyKey The idempotency key
     * @return Optional containing the cached response if it exists
     */
    public Optional<WithdrawalResponse> getResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            log.warn("getResponse called with null or empty idempotency key");
            return Optional.empty();
        }

        String key = buildKey(idempotencyKey);
        
        try {
            Object value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                log.info("No cached response found for idempotency key: {}", idempotencyKey);
                return Optional.empty();
            }
            
            if (value instanceof WithdrawalResponse) {
                log.info("Found cached response for idempotency key: {}", idempotencyKey);
                return Optional.of((WithdrawalResponse) value);
            }
            
            log.warn("Value in Redis is not a WithdrawalResponse, it's a: {}", value.getClass().getName());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving idempotency key {} from Redis: {}", idempotencyKey, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Save a withdrawal response with the idempotency key
     *
     * @param idempotencyKey The idempotency key
     * @param response The withdrawal response to cache
     */
    public void saveResponse(String idempotencyKey, WithdrawalResponse response) {
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            log.warn("Attempted to save response with null or empty idempotency key");
            return;
        }

        if (response == null) {
            log.warn("Attempted to save null response for idempotency key: {}", idempotencyKey);
            return;
        }

        String key = buildKey(idempotencyKey);

        try {
            redisTemplate.opsForValue().set(key, response, DEFAULT_TTL);
            log.info("Successfully saved response for idempotency key: {} with TTL: {}, status: {}", 
                    idempotencyKey, DEFAULT_TTL, response.getStatus());

        } catch (Exception e) {
            log.error("Error saving idempotency key {} to Redis: {}", idempotencyKey, e.getMessage(), e);
        }
    }

    private String buildKey(String idempotencyKey) {
        return IDEMPOTENCY_PREFIX + idempotencyKey;
    }
}

