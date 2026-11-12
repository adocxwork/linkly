package com.gupta.linkly.system;

import com.gupta.linkly.entity.Role;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the admin account on first start, only if ADMIN_PASSWORD is provided. (The old code
 * generated a random password nobody could ever see, leaving the admin locked out.)
 * An existing admin is never modified.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String username;
    @Value("${app.admin.email:admin@linkly.local}")
    private String email;
    @Value("${app.admin.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (userRepository.existsByRole(Role.ROLE_ADMIN)) {
                return;
            }
            if (password == null || password.length() < 8) {
                log.warn("No admin account exists. Set ADMIN_PASSWORD (min 8 chars) and restart to create one.");
                return;
            }
            if (userRepository.findByUsername(username).isPresent()) {
                log.warn("Cannot seed admin: username '{}' is already used by a normal account.", username);
                return;
            }
            userRepository.save(User.builder()
                    .name("Admin")
                    .username(username)
                    .email(email.toLowerCase())
                    .password(passwordEncoder.encode(password))
                    .role(Role.ROLE_ADMIN)
                    .build());
            log.info("Admin account '{}' created.", username);
        } catch (RuntimeException e) {
            log.error("Admin seeding failed: {}", e.getMessage());
        }
    }
}
