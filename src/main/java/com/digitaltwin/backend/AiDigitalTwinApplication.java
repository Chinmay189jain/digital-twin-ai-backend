package com.digitaltwin.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
@EnableCaching
public class AiDigitalTwinApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiDigitalTwinApplication.class, args);
	}

}
