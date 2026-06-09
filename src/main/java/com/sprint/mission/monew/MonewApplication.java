package com.sprint.mission.monew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class MonewApplication {
	public static void main(String[] args) {
		SpringApplication.run(MonewApplication.class, args);
	}
}