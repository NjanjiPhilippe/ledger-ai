package com.np3.ledgerai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LedgeraiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LedgeraiApplication.class, args);
	}

}
