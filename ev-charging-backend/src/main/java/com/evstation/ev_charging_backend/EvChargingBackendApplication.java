package com.evstation.ev_charging_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EvChargingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(EvChargingBackendApplication.class, args);
		System.out.println("the application is running successfully");
	}

}
