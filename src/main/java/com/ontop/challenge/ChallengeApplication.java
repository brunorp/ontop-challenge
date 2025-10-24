package com.ontop.challenge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.ontop.challenge")
@EnableJpaRepositories(basePackages = "com.ontop.challenge.adapters.out.persistence")
@EntityScan(basePackages = {"com.ontop.challenge.adapters.out.persistence.entity", "com.ontop.challenge.domain"})
public class ChallengeApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChallengeApplication.class, args);
	}

}
