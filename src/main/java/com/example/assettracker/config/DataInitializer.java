package com.example.assettracker.config;

import com.example.assettracker.entity.Role;
import com.example.assettracker.entity.User;
import com.example.assettracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Public registration always creates ROLE_STAFF accounts (otherwise anyone could make
 * themselves an admin). This seeds the first ROLE_ADMIN account so the system is usable.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.username}") String adminUsername,
                           @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername(adminUsername)) {
            userRepository.save(new User(adminUsername, passwordEncoder.encode(adminPassword), Role.ROLE_ADMIN));
            log.info("Seeded initial admin account '{}'", adminUsername);
        }
    }
}
