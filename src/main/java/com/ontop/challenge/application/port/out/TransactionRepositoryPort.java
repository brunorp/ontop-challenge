package com.ontop.challenge.application.port.out;

import com.ontop.challenge.domain.Transaction;

public interface TransactionRepositoryPort {
    /**
     * Save a transaction
     *
     * @param tx The transaction to save
     * @return The saved transaction
     */
    Transaction save(Transaction tx);
}
