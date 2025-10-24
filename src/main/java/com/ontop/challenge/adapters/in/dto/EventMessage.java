package com.ontop.challenge.adapters.in.dto;

import com.ontop.challenge.domain.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMessage {
    private WithdrawRequest req;
    private Transaction transaction;
}
