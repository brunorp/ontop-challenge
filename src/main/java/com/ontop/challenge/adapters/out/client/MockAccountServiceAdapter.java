package com.ontop.challenge.adapters.out.client;

import com.ontop.challenge.application.port.out.AccountServicePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Mock adapter for account service
 */
@Component
@Slf4j
public class MockAccountServiceAdapter implements AccountServicePort {

    @Override
    public Optional<AccountDetails> getAccountDetails(UUID accountId) {
        log.info("Fetching account details for accountId: {} (using mock data)", accountId);
        
        // TODO: Replace with actual API call to account service
        // For now, return mock data
        return Optional.of(new AccountDetails(
                "TONY STARK",
                "1885226711",
                "211927207",
                "USD"
        ));
    }
}

