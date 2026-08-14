package com.weatherspoon.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the library checkout/return service.
 */
@SpringBootApplication
public class LibrarySvcApplication {

	/**
	 * Boots the Spring application context.
	 *
	 * @param args standard command-line arguments, passed through to Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(LibrarySvcApplication.class, args);
	}

}
