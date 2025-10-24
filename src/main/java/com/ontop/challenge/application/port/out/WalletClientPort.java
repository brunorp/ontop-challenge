package com.ontop.challenge.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Port interface for wallet operations
 */
public interface WalletClientPort {

    /**
     * Get the current balance for a user
     *
     * @param userId The user ID
     * @return Optional containing the balance, or empty if user not found
     */
    Optional<BigDecimal> getBalance(Long userId);

    /**
     * Create a wallet transaction (debit from user's wallet)
     *
     * @param userId The user ID
     * @param amount The amount to debit
     * @return Optional containing the wallet transaction ID, or empty if failed
     */
    Optional<Long> createWalletTransaction(Long userId, BigDecimal amount);
}

