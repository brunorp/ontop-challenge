package com.ontop.challenge.adapters.out.client.dto.wallet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionResponse {
    @JsonProperty("wallet_transaction_id")
    private Long walletTransactionId;
    private BigDecimal amount;
    @JsonProperty("user_id")
    private Long userId;
}