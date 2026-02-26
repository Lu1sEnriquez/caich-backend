package com.api_caich;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableJpaRepositories(basePackages = { "com.api_caich.repository" })
@EntityScan(basePackages = { "com.api_caich.entity" })
public class ApiCaichApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiCaichApplication.class, args);
	}
}
