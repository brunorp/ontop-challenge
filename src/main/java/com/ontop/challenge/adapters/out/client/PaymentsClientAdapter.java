package com.ontop.challenge.adapters.out.client;

import com.ontop.challenge.adapters.out.client.dto.payment.PaymentRequest;
import com.ontop.challenge.adapters.out.client.dto.payment.PaymentResponse;
import com.ontop.challenge.application.port.out.PaymentsClientPort;
import com.ontop.challenge.application.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class PaymentsClientAdapter implements PaymentsClientPort {

    @Value("${ontop.clients.payments-base-url}")
    private String paymentsBaseUrl;

    private final RestTemplate restTemplate;

    public PaymentsClientAdapter(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @Retry(name = "paymentsService", fallbackMethod = "createPaymentFallback")
    @CircuitBreaker(name = "paymentsService", fallbackMethod = "createPaymentFallback")
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("Creating payment for amount: {}", 
                request.getAmount());

        try {
            String url = paymentsBaseUrl + "/payments";

            ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(
                    url,
                    request,
                    PaymentResponse.class
            );

            if (response.getBody() != null) {
                log.info("Payment created successfully, status: {}, paymentId: {}",
                        response.getBody().getRequestInfo() != null ? 
                                response.getBody().getRequestInfo().getStatus() : "unknown",
                        response.getBody().getPaymentInfo() != null ? 
                                response.getBody().getPaymentInfo().getId() : "unknown");
                return response.getBody();
            }

            log.error("Empty response received from payment provider");
            throw new ExternalServiceException("Empty response from payment provider");

        } catch (HttpServerErrorException e) {
            log.error("Payment provider returned 5xx error, status: {}, message: {}", 
                    e.getStatusCode(), e.getMessage(), e);
            throw new ExternalServiceException(
                    "Payment provider server error: " + e.getStatusCode() + " - " + e.getMessage(), e);

        } catch (ExternalServiceException e) {
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error creating payment: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to create payment", e);
        }
    }

    /**
     * Fallback method for createPayment when circuit is open or retries exhausted
     */
    private PaymentResponse createPaymentFallback(PaymentRequest request, Exception e) {
        log.error("Fallback triggered for createPayment, amount: {}, error: {}", 
                request.getAmount(), e.getMessage());
        throw new ExternalServiceException("Payment service is currently unavailable. Please try again later.", e);
    }
}
