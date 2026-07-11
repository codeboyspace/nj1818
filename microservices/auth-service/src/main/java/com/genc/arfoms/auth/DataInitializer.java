package com.genc.arfoms.auth;

import com.genc.arfoms.auth.model.User;
import com.genc.arfoms.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository userRepository;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.name}")
    private String adminName;

    @Value("${admin.role}")
    private String adminRole;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing database default configurations...");
        Optional<User> existingAdmin = userRepository.findByUsername(adminUsername);
        if (existingAdmin.isEmpty()) {
            User adminUser = new User(adminUsername, adminPassword, adminName, adminRole);
            userRepository.save(adminUser);
            logger.info("Default Admin User created successfully.");
        } else {
            logger.info("Default Admin User already exists.");
        }
    }
}
