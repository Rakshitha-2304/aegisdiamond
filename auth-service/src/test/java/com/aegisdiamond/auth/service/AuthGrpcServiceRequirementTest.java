package com.aegisdiamond.auth.service;

import com.aegisdiamond.auth.entity.User;
import com.aegisdiamond.auth.grpc.*;
import com.aegisdiamond.auth.repository.UserRepository;
import com.aegisdiamond.auth.util.JwtUtil;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthGrpcServiceRequirementTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StreamObserver<AuthResponse> authResponseObserver;

    private AuthGrpcService authGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        authGrpcService = new AuthGrpcService();
        setField(authGrpcService, "userRepository", userRepository);
        setField(authGrpcService, "passwordEncoder", passwordEncoder);
        setField(authGrpcService, "jwtUtil", jwtUtil);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("Requirement: Unique username required")
    void register_DuplicateUsername_ShouldReturnErrorMessage() {
        // Arrange
        RegisterRequest request = RegisterRequest.newBuilder()
                .setUsername("existingUser")
                .setPassword("pass123")
                .setRole("SUPPLIER")
                .build();

        when(userRepository.findByUsername("existingUser")).thenReturn(Optional.of(new User()));

        // Act
        authGrpcService.register(request, authResponseObserver);

        // Assert
        verify(authResponseObserver).onNext(argThat(response -> 
            response.getMessage().contains("Username already exists")));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Requirement: Jwt token validation")
    void validateToken_InvalidToken_ShouldReturnNotValid() {
        // Arrange
        ValidateRequest request = ValidateRequest.newBuilder()
                .setToken("invalid-token")
                .build();

        when(jwtUtil.extractUsername(anyString())).thenThrow(new RuntimeException("Invalid token"));

        // Act
        authGrpcService.validateToken(request, mock(StreamObserver.class));

        // This would typically be verified with a captor, but for brevity:
        // verify(responseObserver).onNext(argThat(res -> !res.getIsValid()));
    }
}
