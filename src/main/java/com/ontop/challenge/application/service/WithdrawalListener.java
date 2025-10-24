package com.ontop.challenge.application.service;

import com.ontop.challenge.adapters.in.dto.EventMessage;
import com.ontop.challenge.adapters.in.dto.WithdrawalResponse;
import com.ontop.challenge.domain.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WithdrawalListener {

    private final WithdrawService withdrawService;
    private final IdempotencyService idempotencyService;

    public WithdrawalListener(WithdrawService withdrawService,
                              IdempotencyService idempotencyService) {
        this.withdrawService = withdrawService;
        this.idempotencyService = idempotencyService;
    }

    @Async
    @EventListener
    public void handleWalletWithdraw(EventMessage message){
        String idempotencyKey = message.getReq().getIdempotencyKey();
        log.info("Processing withdrawal event in background for idempotency key: {}", idempotencyKey);

        try {
            WithdrawalResponse response = withdrawService.executeWithdrawal(message.getReq(), message.getTransaction());

            idempotencyService.saveResponse(idempotencyKey, response);
            log.info("Saved withdrawal response to Redis for idempotency key: {}, status: {}",
                    idempotencyKey, response.getStatus());
        } catch (Exception e) {
            log.error("Error processing withdrawal for idempotency key: {}, error: {}", 
                    idempotencyKey, e.getMessage(), e);

            if (idempotencyKey != null) {
                WithdrawalResponse errorResponse = WithdrawalResponse.builder()
                        .status(TransactionStatus.FAILED)
                        .build();
                idempotencyService.saveResponse(idempotencyKey, errorResponse);
            }
        }
    }
}
