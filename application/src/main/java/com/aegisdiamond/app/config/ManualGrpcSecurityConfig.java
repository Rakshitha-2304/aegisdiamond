package com.aegisdiamond.app.config;

import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader;
import net.devh.boot.grpc.server.security.interceptors.ExceptionTranslatingServerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import io.grpc.*;

@Configuration
public class ManualGrpcSecurityConfig {

    @Autowired
    private GrpcAuthenticationReader grpcAuthenticationReader;

    @Bean
    @GrpcGlobalServerInterceptor
    @Order(10)
    public ServerInterceptor authenticatingServerInterceptor() {
        return new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {
                
                Authentication auth = grpcAuthenticationReader.readAuthentication(call, headers);
                
                return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(next.startCall(call, headers)) {
                    @Override
                    public void onMessage(ReqT message) {
                        runWithContext(() -> super.onMessage(message));
                    }

                    @Override
                    public void onHalfClose() {
                        runWithContext(() -> super.onHalfClose());
                    }

                    @Override
                    public void onCancel() {
                        runWithContext(() -> super.onCancel());
                    }

                    @Override
                    public void onComplete() {
                        runWithContext(() -> super.onComplete());
                    }

                    @Override
                    public void onReady() {
                        runWithContext(() -> super.onReady());
                    }

                    private void runWithContext(Runnable r) {
                        if (auth != null) {
                            SecurityContext context = SecurityContextHolder.createEmptyContext();
                            context.setAuthentication(auth);
                            SecurityContextHolder.setContext(context);
                        }
                        try {
                            r.run();
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    }
                };
            }
        };
    }

    @Bean
    @GrpcGlobalServerInterceptor
    @Order(5)
    public ServerInterceptor exceptionTranslatingServerInterceptor() {
        return new ExceptionTranslatingServerInterceptor();
    }
}
