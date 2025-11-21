package com.dehold.contentmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@EnableCaching
public class ContentManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContentManagerApplication.class, args);
	}

}
