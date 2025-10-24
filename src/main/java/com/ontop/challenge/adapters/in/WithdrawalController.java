package com.ontop.challenge.adapters.in;

import com.ontop.challenge.adapters.in.dto.EventMessage;
import com.ontop.challenge.adapters.in.dto.WithdrawRequest;
import com.ontop.challenge.adapters.in.dto.WithdrawalResponse;
import com.ontop.challenge.adapters.out.persistence.mapper.TransactionMapper;
import com.ontop.challenge.application.service.IdempotencyService;
import com.ontop.challenge.application.service.WithdrawService;
import com.ontop.challenge.domain.Transaction;
import com.ontop.challenge.domain.TransactionStatus;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/withdrawals")
@Slf4j
public class WithdrawalController {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final WithdrawService withdrawService;
    private final IdempotencyService idempotencyService;
    private final TransactionMapper mapper;

    public WithdrawalController(ApplicationEventPublisher applicationEventPublisher, WithdrawService withdrawService,
                                IdempotencyService idempotencyService, TransactionMapper mapper) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.withdrawService = withdrawService;
        this.idempotencyService = idempotencyService;
        this.mapper = mapper;
    }

    /**
     * Create a withdrawal
     *
     * @param req withdrawal request
     * @param idempotencyKey idempotency key for preventing duplicate operations (REQUIRED)
     * @return withdrawal response
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<WithdrawalResponse> withdraw(
            @RequestBody @Valid WithdrawRequest req,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey) {

        log.info("Withdrawal request from authenticated userId: {}, amount: {}, idempotencyKey: {}",
                req.getUserId(), req.getAmount(), idempotencyKey);

        req.setIdempotencyKey(idempotencyKey);

        Optional<WithdrawalResponse> cachedResponse = idempotencyService.getResponse(idempotencyKey);
        if (cachedResponse.isPresent()) {
            log.info("Returning cached response for idempotency key: {}", idempotencyKey);
            return ResponseEntity.status(HttpStatus.CREATED).body(cachedResponse.get());
        }

        Transaction response = withdrawService.createInitialTransaction(req);
        EventMessage message = EventMessage.builder().req(req).transaction(response).build();
        applicationEventPublisher.publishEvent(message);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.transactionToWithdrawalResponse(response));
    }
}

