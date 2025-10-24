package com.ontop.challenge.application.port.out;

import com.ontop.challenge.adapters.out.client.dto.payment.PaymentRequest;
import com.ontop.challenge.adapters.out.client.dto.payment.PaymentResponse;

/**
 * Port interface for payment provider operations
 */
public interface PaymentsClientPort {

    /**
     * Create a payment through the external payment provider
     *
     * @param request The payment request details
     * @return The payment response from the provider
     */
    PaymentResponse createPayment(PaymentRequest request);
}

