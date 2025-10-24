package com.ontop.challenge.utils;

import com.ontop.challenge.domain.Transaction;
import com.ontop.challenge.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionUtils {

    public static Transaction createTransaction(TransactionStatus status) {
        return new Transaction(
                UUID.randomUUID(),
                1000L,
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                new BigDecimal("900.00"),
                "USD",
                status,
                null,
                null,
                null,
                UUID.randomUUID(),
                Instant.now(),
                Instant.now(),
                null
        );
    }
}
