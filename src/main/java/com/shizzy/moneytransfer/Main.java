package com.shizzy.moneytransfer;


import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.shizzy.moneytransfer.service.AccountLimitAssignmentService;
import com.shizzy.moneytransfer.service.KycVerificationService;


@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class Main {

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

	@Bean
	CommandLineRunner runner(KycVerificationService kycVerificationService,
			AccountLimitAssignmentService accountLimitAssignmentService) {

		return args -> {
			
		};
	}

}
