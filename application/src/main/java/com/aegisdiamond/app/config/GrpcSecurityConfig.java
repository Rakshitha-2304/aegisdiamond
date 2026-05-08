package com.aegisdiamond.app.config;

import com.aegisdiamond.auth.util.JwtUtil;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

@Configuration
public class GrpcSecurityConfig {

    @Autowired
    private JwtUtil jwtUtil;

    @Bean
    public GrpcAuthenticationReader grpcAuthenticationReader() {
        return (call, headers) -> {
            String authHeader = headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER));
            if (authHeader == null) {
                authHeader = headers.get(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER));
            }

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                try {
                    String username = jwtUtil.extractUsername(jwt);
                    if (username != null && jwtUtil.validateToken(jwt, username)) {
                        String role = jwtUtil.extractRole(jwt);
                        return new UsernamePasswordAuthenticationToken(username, null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
                    }
                } catch (Exception e) {
                    // Invalid token
                }
            }
            return null;
        };
    }
}
