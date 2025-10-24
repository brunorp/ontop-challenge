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
public class WalletTransactionRequest {
    @JsonProperty("user_id")
    private Long userId;
    private BigDecimal amount;
}