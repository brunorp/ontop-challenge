package com.ontop.challenge.adapters.out.client;

import com.ontop.challenge.adapters.out.client.dto.payment.PaymentRequest;
import com.ontop.challenge.adapters.out.client.dto.payment.PaymentResponse;
import com.ontop.challenge.application.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentsClientAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    private PaymentsClientAdapter paymentsClientAdapter;

    private static final String PAYMENTS_BASE_URL = "http://payments-api";

    @BeforeEach
    void setUp() {
        paymentsClientAdapter = new PaymentsClientAdapter(restTemplate);
        try {
            java.lang.reflect.Field field = PaymentsClientAdapter.class.getDeclaredField("paymentsBaseUrl");
            field.setAccessible(true);
            field.set(paymentsClientAdapter, PAYMENTS_BASE_URL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createPayment_Success_ReturnsPaymentResponse() {
        PaymentRequest request = createPaymentRequest();
        PaymentResponse expectedResponse = createSuccessfulPaymentResponse();

        ResponseEntity<PaymentResponse> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(
                eq(PAYMENTS_BASE_URL + "/payments"),
                eq(request),
                eq(PaymentResponse.class)
        )).thenReturn(responseEntity);

        PaymentResponse result = paymentsClientAdapter.createPayment(request);

        assertThat(result).isNotNull();
        assertThat(result.getRequestInfo().getStatus()).isEqualTo("Processing");
        assertThat(result.getPaymentInfo().getId()).isEqualTo("payment-123");
        assertThat(result.getPaymentInfo().getAmount()).isEqualByComparingTo(new BigDecimal("900.00"));

        verify(restTemplate).postForEntity(
                eq(PAYMENTS_BASE_URL + "/payments"),
                eq(request),
                eq(PaymentResponse.class)
        );
    }

    @Test
    void createPayment_WithValidRequest_SendsCorrectData() {
        PaymentRequest request = createPaymentRequest();
        PaymentResponse expectedResponse = createSuccessfulPaymentResponse();

        ResponseEntity<PaymentResponse> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(
                anyString(),
                any(PaymentRequest.class),
                eq(PaymentResponse.class)
        )).thenReturn(responseEntity);

        paymentsClientAdapter.createPayment(request);

        ArgumentCaptor<PaymentRequest> requestCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(restTemplate).postForEntity(
                eq(PAYMENTS_BASE_URL + "/payments"),
                requestCaptor.capture(),
                eq(PaymentResponse.class)
        );

        PaymentRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getAmount()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(capturedRequest.getSource()).isNotNull();
        assertThat(capturedRequest.getDestination()).isNotNull();
    }

    @Test
    void createPayment_WhenResponseIsNull_ThrowsException() {
        PaymentRequest request = createPaymentRequest();

        ResponseEntity<PaymentResponse> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.postForEntity(
                anyString(),
                any(PaymentRequest.class),
                eq(PaymentResponse.class)
        )).thenReturn(responseEntity);

        assertThatThrownBy(() -> paymentsClientAdapter.createPayment(request))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Empty response from payment provider");
    }

    @Test
    void createPayment_WhenServerError_ThrowsException() {
        PaymentRequest request = createPaymentRequest();

        when(restTemplate.postForEntity(
                anyString(),
                any(PaymentRequest.class),
                eq(PaymentResponse.class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error"));

        assertThatThrownBy(() -> paymentsClientAdapter.createPayment(request))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Payment provider server error");
    }

    @Test
    void createPayment_WhenServiceUnavailable_ThrowsException() {
        PaymentRequest request = createPaymentRequest();

        when(restTemplate.postForEntity(
                anyString(),
                any(PaymentRequest.class),
                eq(PaymentResponse.class)
        )).thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable"));

        assertThatThrownBy(() -> paymentsClientAdapter.createPayment(request))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Payment provider server error")
                .hasMessageContaining("503");
    }

    @Test
    void createPayment_WhenUnexpectedError_ThrowsException() {
        PaymentRequest request = createPaymentRequest();

        when(restTemplate.postForEntity(
                anyString(),
                any(PaymentRequest.class),
                eq(PaymentResponse.class)
        )).thenThrow(new RuntimeException("Network error"));

        assertThatThrownBy(() -> paymentsClientAdapter.createPayment(request))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Failed to create payment");
    }

    @Test
    void createPayment_WithCompletePaymentInfo_ReturnsAllDetails() {
        PaymentRequest request = createPaymentRequest();
        PaymentResponse expectedResponse = PaymentResponse.builder()
                .requestInfo(PaymentResponse.RequestInfo.builder()
                        .status("Processing")
                        .build())
                .paymentInfo(PaymentResponse.PaymentInfo.builder()
                        .id("payment-456")
                        .amount(new BigDecimal("900.00"))
                        .currency("USD")
                        .build())
                .build();

        ResponseEntity<PaymentResponse> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
        when(restTemplate.postForEntity(
                anyString(),
                any(PaymentRequest.class),
                eq(PaymentResponse.class)
        )).thenReturn(responseEntity);

        PaymentResponse result = paymentsClientAdapter.createPayment(request);

        assertThat(result.getRequestInfo()).isNotNull();
        assertThat(result.getRequestInfo().getStatus()).isEqualTo("Processing");
        assertThat(result.getPaymentInfo()).isNotNull();
        assertThat(result.getPaymentInfo().getId()).isEqualTo("payment-456");
        assertThat(result.getPaymentInfo().getCurrency()).isEqualTo("USD");
    }

    // Helper methods
    private PaymentRequest createPaymentRequest() {
        return PaymentRequest.builder()
                .source(PaymentRequest.Source.builder()
                        .type("COMPANY")
                        .sourceInformation(PaymentRequest.Source.SourceInformation.builder()
                                .name("ONTOP INC")
                                .build())
                        .account(PaymentRequest.Source.Account.builder()
                                .accountNumber("0245253419")
                                .routingNumber("028444018")
                                .currency("USD")
                                .build())
                        .build())
                .destination(PaymentRequest.Destination.builder()
                        .name("TONY STARK")
                        .account(PaymentRequest.Destination.Account.builder()
                                .accountNumber("1885226711")
                                .routingNumber("211927207")
                                .currency("USD")
                                .build())
                        .build())
                .amount(new BigDecimal("900.00"))
                .build();
    }

    private PaymentResponse createSuccessfulPaymentResponse() {
        return PaymentResponse.builder()
                .requestInfo(PaymentResponse.RequestInfo.builder()
                        .status("Processing")
                        .build())
                .paymentInfo(PaymentResponse.PaymentInfo.builder()
                        .id("payment-123")
                        .amount(new BigDecimal("900.00"))
                        .currency("USD")
                        .build())
                .build();
    }
}

