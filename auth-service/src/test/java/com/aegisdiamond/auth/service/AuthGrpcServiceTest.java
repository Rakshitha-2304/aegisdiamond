package com.aegisdiamond.auth.service;

import com.aegisdiamond.auth.entity.User;
import com.aegisdiamond.auth.grpc.*;
import com.aegisdiamond.auth.repository.UserRepository;
import com.aegisdiamond.auth.util.JwtUtil;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthGrpcServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StreamObserver<AuthResponse> authResponseObserver;

    @Mock
    private StreamObserver<ValidateResponse> validateResponseObserver;

    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private AuthGrpcService authGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtUtil = new JwtUtil();
        authGrpcService = new AuthGrpcService();

        // Use reflection to set private fields
        setPrivateField(authGrpcService, "userRepository", userRepository);
        setPrivateField(authGrpcService, "passwordEncoder", passwordEncoder);
        setPrivateField(authGrpcService, "jwtUtil", jwtUtil);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void register_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        RegisterRequest request = RegisterRequest.newBuilder()
                .setUsername("testuser")
                .setPassword("password123")
                .setEmail("test@example.com")
                .setRole("SHIPPER")
                .build();

        authGrpcService.register(request, authResponseObserver);

        ArgumentCaptor<AuthResponse> captor = ArgumentCaptor.forClass(AuthResponse.class);
        verify(authResponseObserver).onNext(captor.capture());
        verify(authResponseObserver).onCompleted();

        AuthResponse response = captor.getValue();
        assertEquals("testuser", response.getUsername());
        assertEquals("SHIPPER", response.getRole());
        assertNotNull(response.getToken());
        assertTrue(response.getMessage().contains("successfully"));
    }

    @Test
    void register_UsernameAlreadyExists() {
        User existingUser = new User();
        existingUser.setUsername("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));

        RegisterRequest request = RegisterRequest.newBuilder()
                .setUsername("testuser")
                .setPassword("password123")
                .build();

        authGrpcService.register(request, authResponseObserver);

        ArgumentCaptor<AuthResponse> captor = ArgumentCaptor.forClass(AuthResponse.class);
        verify(authResponseObserver).onNext(captor.capture());
        assertTrue(captor.getValue().getMessage().contains("already exists"));
    }

    @Test
    void login_Success() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole("ADMIN");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        LoginRequest request = LoginRequest.newBuilder()
                .setUsername("testuser")
                .setPassword("password123")
                .build();

        authGrpcService.login(request, authResponseObserver);

        ArgumentCaptor<AuthResponse> captor = ArgumentCaptor.forClass(AuthResponse.class);
        verify(authResponseObserver).onNext(captor.capture());
        assertTrue(captor.getValue().getMessage().contains("successful"));
    }

    @Test
    void login_InvalidPassword() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("password123"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        LoginRequest request = LoginRequest.newBuilder()
                .setUsername("testuser")
                .setPassword("wrongpassword")
                .build();

        authGrpcService.login(request, authResponseObserver);

        ArgumentCaptor<AuthResponse> captor = ArgumentCaptor.forClass(AuthResponse.class);
        verify(authResponseObserver).onNext(captor.capture());
        assertTrue(captor.getValue().getMessage().contains("Invalid"));
    }

    @Test
    void validateToken_Valid() {
        String token = jwtUtil.generateToken("testuser", "ADMIN");

        ValidateRequest request = ValidateRequest.newBuilder()
                .setToken(token)
                .build();

        authGrpcService.validateToken(request, validateResponseObserver);

        ArgumentCaptor<ValidateResponse> captor = ArgumentCaptor.forClass(ValidateResponse.class);
        verify(validateResponseObserver).onNext(captor.capture());
        assertTrue(captor.getValue().getIsValid());
        assertEquals("testuser", captor.getValue().getUsername());
    }

    @Test
    void validateToken_Invalid() {
        ValidateRequest request = ValidateRequest.newBuilder()
                .setToken("invalid_token")
                .build();

        authGrpcService.validateToken(request, validateResponseObserver);

        ArgumentCaptor<ValidateResponse> captor = ArgumentCaptor.forClass(ValidateResponse.class);
        verify(validateResponseObserver).onNext(captor.capture());
        assertFalse(captor.getValue().getIsValid());
    }
}
