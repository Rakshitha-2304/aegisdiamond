package com.aegisdiamond.auth.service;

import com.aegisdiamond.auth.entity.User;
import com.aegisdiamond.auth.grpc.*;
import com.aegisdiamond.auth.repository.UserRepository;
import com.aegisdiamond.auth.util.JwtUtil;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@GrpcService
@jakarta.annotation.security.PermitAll
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AuthGrpcService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequest request, StreamObserver<AuthResponse> responseObserver) {
        logger.info("Registering user: {}", request.getUsername());
        // Detailed Validation
        validateRegistrationRequest(request);

        if ("admin".equalsIgnoreCase(request.getRole())) {
            logger.warn("Registration attempt as 'admin' rejected for user: {}", request.getUsername());
            throw new com.aegisdiamond.auth.exception.ValidationException("Registration as 'admin' is not allowed.");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            logger.warn("Registration failed: Username {} already exists", request.getUsername());
            throw new com.aegisdiamond.auth.exception.UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());

        userRepository.save(user);
        logger.info("User {} registered successfully", user.getUsername());

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        responseObserver.onNext(AuthResponse.newBuilder()
                .setToken(token)
                .setUsername(user.getUsername())
                .setRole(user.getRole())
                .setMessage("User registered successfully")
                .build());
        responseObserver.onCompleted();
    }

    private void validateRegistrationRequest(RegisterRequest request) {
        String password = request.getPassword();
        String email = request.getEmail();

        // Password Validation: At least 8 characters, capital, small, number, special char
        if (password.length() < 8) {
            throw new com.aegisdiamond.auth.exception.ValidationException("Password must be at least 8 characters long.");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new com.aegisdiamond.auth.exception.ValidationException("Password must contain at least one uppercase letter.");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new com.aegisdiamond.auth.exception.ValidationException("Password must contain at least one lowercase letter.");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new com.aegisdiamond.auth.exception.ValidationException("Password must contain at least one number.");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-={}\\[\\]|\\\\:;\"'<>,.?/~`].*")) {
            throw new com.aegisdiamond.auth.exception.ValidationException("Password must contain at least one special character.");
        }

        // Email Validation: user@domain.com
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$")) {
            throw new com.aegisdiamond.auth.exception.ValidationException("Invalid email format. Must be user@domain.com");
        }
    }

    @Override
    public void login(LoginRequest request, StreamObserver<AuthResponse> responseObserver) {
        logger.info("Login attempt for user: {}", request.getUsername());
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isPresent() && passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            User user = userOpt.get();
            logger.info("Login successful for user: {}", user.getUsername());
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

            responseObserver.onNext(AuthResponse.newBuilder()
                    .setToken(token)
                    .setUsername(user.getUsername())
                    .setRole(user.getRole())
                    .setMessage("Login successful")
                    .build());
            responseObserver.onCompleted();
        } else {
            logger.warn("Login failed for user: {}", request.getUsername());
            throw new com.aegisdiamond.auth.exception.InvalidCredentialsException("Invalid username or password");
        }
    }

    @Override
    public void validateToken(ValidateRequest request, StreamObserver<ValidateResponse> responseObserver) {
        try {
            String token = request.getToken();
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);

            boolean isValid = jwtUtil.validateToken(token, username);
            logger.debug("Token validation for user {}: {}", username, isValid);

            responseObserver.onNext(ValidateResponse.newBuilder()
                    .setIsValid(isValid)
                    .setUsername(username)
                    .setRole(role)
                    .build());
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            responseObserver.onNext(ValidateResponse.newBuilder()
                    .setIsValid(false)
                    .build());
        }
        responseObserver.onCompleted();
    }
}
