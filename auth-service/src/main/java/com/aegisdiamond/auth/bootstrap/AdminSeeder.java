package com.aegisdiamond.auth.bootstrap;

import com.aegisdiamond.auth.entity.User;
import com.aegisdiamond.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("Aegis@Admin2026"));
            admin.setEmail("admin@aegisdiamond.com");
            admin.setRole("admin");
            userRepository.save(admin);
        }
    }
}
