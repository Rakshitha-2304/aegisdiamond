package com.aegisdiamond.auth.service;

import com.aegisdiamond.auth.entity.User;
import com.aegisdiamond.auth.grpc.*;
import com.aegisdiamond.auth.repository.UserRepository;
import com.aegisdiamond.auth.util.JwtUtil;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@GrpcService
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequest request, StreamObserver<AuthResponse> responseObserver) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setMessage("Username already exists")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        responseObserver.onNext(AuthResponse.newBuilder()
                .setToken(token)
                .setUsername(user.getUsername())
                .setRole(user.getRole())
                .setMessage("User registered successfully")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void login(LoginRequest request, StreamObserver<AuthResponse> responseObserver) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isPresent() && passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            User user = userOpt.get();
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

            responseObserver.onNext(AuthResponse.newBuilder()
                    .setToken(token)
                    .setUsername(user.getUsername())
                    .setRole(user.getRole())
                    .setMessage("Login successful")
                    .build());
        } else {
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setMessage("Invalid username or password")
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void validateToken(ValidateRequest request, StreamObserver<ValidateResponse> responseObserver) {
        try {
            String token = request.getToken();
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);

            boolean isValid = jwtUtil.validateToken(token, username);

            responseObserver.onNext(ValidateResponse.newBuilder()
                    .setIsValid(isValid)
                    .setUsername(username)
                    .setRole(role)
                    .build());
        } catch (Exception e) {
            responseObserver.onNext(ValidateResponse.newBuilder()
                    .setIsValid(false)
                    .build());
        }
        responseObserver.onCompleted();
    }
}
