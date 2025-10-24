package com.ontop.challenge.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for account service operations
 * Used to retrieve destination bank account details
 */
public interface AccountServicePort {

    /**
     * Get bank account details by account ID
     *
     * @param accountId The account identifier
     * @return Optional containing account details if found
     */
    Optional<AccountDetails> getAccountDetails(UUID accountId);

    /**
     * Bank account details
     */
    record AccountDetails(
            String accountHolderName,
            String accountNumber,
            String routingNumber,
            String currency
    ) {}
}

