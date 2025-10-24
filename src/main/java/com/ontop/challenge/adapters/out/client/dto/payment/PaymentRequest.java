package com.ontop.challenge.adapters.out.client.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payment request POJO for external payment provider
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    private Source source;
    private Destination destination;
    private BigDecimal amount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        private String type;
        private SourceInformation sourceInformation;
        private Account account;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SourceInformation {
            private String name;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Account {
            private String accountNumber;
            private String currency;
            private String routingNumber;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Destination {
        private String name;
        private Account account;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Account {
            private String accountNumber;
            private String currency;
            private String routingNumber;
        }
    }
}

