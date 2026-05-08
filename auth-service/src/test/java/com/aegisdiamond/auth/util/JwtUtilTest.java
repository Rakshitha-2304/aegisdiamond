package com.aegisdiamond.auth.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    void generateToken_NotNull() {
        String token = jwtUtil.generateToken("testuser", "ADMIN");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractUsername_ReturnsCorrectUsername() {
        String token = jwtUtil.generateToken("testuser", "ADMIN");
        String username = jwtUtil.extractUsername(token);
        assertEquals("testuser", username);
    }

    @Test
    void extractRole_ReturnsCorrectRole() {
        String token = jwtUtil.generateToken("testuser", "SHIPPER");
        String role = jwtUtil.extractRole(token);
        assertEquals("SHIPPER", role);
    }

    @Test
    void validateToken_ValidToken() {
        String token = jwtUtil.generateToken("testuser", "ADMIN");
        assertTrue(jwtUtil.validateToken(token, "testuser"));
    }

    @Test
    void validateToken_InvalidUsername() {
        String token = jwtUtil.generateToken("testuser", "ADMIN");
        assertFalse(jwtUtil.validateToken(token, "wronguser"));
    }

    @Test
    void isTokenExpired_NotExpired() throws Exception {
        String token = jwtUtil.generateToken("testuser", "ADMIN");
        // Use reflection to access private method
        java.lang.reflect.Method method = JwtUtil.class.getDeclaredMethod("isTokenExpired", String.class);
        method.setAccessible(true);
        assertFalse((Boolean) method.invoke(jwtUtil, token));
    }

    @Test
    void extractExpiration_ReturnsFutureDate() {
        String token = jwtUtil.generateToken("testuser", "ADMIN");
        assertNotNull(jwtUtil.extractExpiration(token));
        assertTrue(jwtUtil.extractExpiration(token).after(new java.util.Date()));
    }
}
