package com.gupta.linkly;

import com.gupta.linkly.entity.Role;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableScheduling
public class LinklyApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinklyApplication.class, args);
    }

    @Bean
    CommandLineRunner run(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = User.builder()
                        .name("Admin")
                        .username("admin")
                        .email("admin@linkly.com")
                        .password(passwordEncoder.encode(System.getenv("ADMIN_PASSWORD") != null ? System.getenv("ADMIN_PASSWORD") : java.util.UUID.randomUUID().toString().substring(0, 8)))
                        .role(Role.ROLE_ADMIN)
                        .isSuspended(false)
                        .build();
                userRepository.save(admin);
                System.out.println("Admin user seeded. Use ADMIN_PASSWORD env var to login or check DB.");
            }
        };
    }
}
