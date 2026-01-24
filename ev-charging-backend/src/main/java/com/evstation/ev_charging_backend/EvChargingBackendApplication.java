package com.evstation.ev_charging_backend;

import com.evstation.ev_charging_backend.entity.User;
import com.evstation.ev_charging_backend.enums.Role;
import com.evstation.ev_charging_backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class EvChargingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(EvChargingBackendApplication.class, args);
		System.out.println("The application is running successfully");
	}

	// ✅ Bean for seeding default admin
	@Bean
	CommandLineRunner seedAdmin(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
		return args -> {
			boolean exists = userRepository.existsByEmail("admin123@gmail.com") ||
					userRepository.existsByPhone("9816382890");

			if (!exists) { // check both email and phone
				User admin = User.builder()
						.firstName("Admin")
						.lastName("User")
						.email("admin123@gmail.com")
						.phone("9816382890")
						.password(passwordEncoder.encode("admin123"))
						.role(Role.ADMIN)
						.build();
				userRepository.save(admin);
				System.out.println("Admin user created: admin123@gmail.com / admin123");
			} else {
				System.out.println("Admin user already exists, skipping seeding.");
			}
		};
	}

}
