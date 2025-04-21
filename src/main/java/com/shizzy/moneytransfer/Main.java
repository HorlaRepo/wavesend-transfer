package com.shizzy.moneytransfer;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class Main {

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

	// @Bean
	// CommandLineRunner runner(@Qualifier("EmailServiceImpl") EmailService emailService) {
	// 	Map<String, Object> details  = new HashMap<>();
	// 	details.put("username", "Fran");
	// 	details.put("activationCode", 289009);
	// 	details.put("confirmationUrl", "https://your-confirmation-url.com");

	// 	return args -> {
	// 		//System.out.println("Principal: " + principal.getName());
	// 		// emailService.sendEmail(
	// 		// 		"meetfran6@gmail.com",
	// 		// 		details,
	// 		// 		EmailTemplateName.ACTIVATE_ACCOUNT,
	// 		// 		"Test Email"
	// 		// );
	// 	};
	// }

}
