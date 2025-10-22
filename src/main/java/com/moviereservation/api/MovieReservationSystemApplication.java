package com.moviereservation.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableCaching
public class MovieReservationSystemApplication {

	public static void main(final String[] args) {
		SpringApplication.run(MovieReservationSystemApplication.class, args);
	}

}
