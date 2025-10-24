package com.ontop.challenge.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Configuration for withdrawal business rules.
 */
@Configuration
@ConfigurationProperties(prefix = "ontop.withdrawal")
@Data
public class WithdrawalConfig {

    /**
     * Fee percentage
     */
    private BigDecimal feePercentage;

    /**
     * Company bank account information
     */
    private CompanyAccount companyAccount;

    @Data
    public static class CompanyAccount {
        private String name;
        private String accountNumber;
        private String routingNumber;
        private String currency;
    }
}

