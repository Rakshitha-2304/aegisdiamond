package com.aegisdiamond.app.config;

import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
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
                
                if (auth != null) {
                    System.out.println("DEBUG: Authenticated user: " + auth.getName() + " with roles: " + auth.getAuthorities());
                } else {
                    System.out.println("DEBUG: No authentication found in gRPC headers");
                }

                // Create a wrapper for the listener to propagate the security context
                ServerCall.Listener<ReqT> listener;
                
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(auth);
                
                // Set context for the initial startCall
                SecurityContext oldContext = SecurityContextHolder.getContext();
                SecurityContextHolder.setContext(context);
                try {
                    listener = next.startCall(call, headers);
                } finally {
                    SecurityContextHolder.setContext(oldContext);
                }

                return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
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
                        SecurityContext previousContext = SecurityContextHolder.getContext();
                        SecurityContextHolder.setContext(context);
                        try {
                            r.run();
                        } catch (AccessDeniedException e) {
                            System.out.println("DEBUG: Access Denied: " + e.getMessage());
                            call.close(Status.PERMISSION_DENIED.withDescription(e.getMessage()).withCause(e), new Metadata());
                        } catch (Exception e) {
                            System.out.println("DEBUG: Unexpected Error in gRPC call: " + e.getMessage());
                            throw e;
                        } finally {
                            SecurityContextHolder.setContext(previousContext);
                        }
                    }
                };
            }
        };
    }
}
