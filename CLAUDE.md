# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Aegis Diamond App is a high-security Diamond Shipping & Secure Logistic System built for a 2026 futuristic hackathon scenario. It uses a **modular monolith** architecture — 13 Gradle modules with a single Spring Boot entry point.

## Build & Development Commands

```bash
# Build all modules
./gradlew build

# Run the application (only application module is runnable)
./gradlew :application:bootRun

# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :diamond-service:test
./gradlew :shipping-service:test

# Run a single test class
./gradlew :module-name:test --tests "com.aegisdiamond.package.ClassName"

# Run a single test method
./gradlew :module-name:test --tests "com.aegisdiamond.package.ClassName.methodName"

# Generate gRPC stubs from proto files (run after modifying .proto files)
./gradlew generateProto
```

## Architecture

### Module Structure (13 modules)

- **`application`** — Sole runnable module. Contains `@SpringBootApplication` main class, centralized security config (JWT + Basic Auth), and aggregates all service modules.
- **`diamond-service`** — Diamond inventory, certification, 4Cs validation (cut, clarity, color, carat)
- **`auth-service`** — JWT authentication, role-based access (SUPPLIER, SHIPPER, VAULT_MANAGER, INSURANCE_AGENT, CUSTOMS_OFFICER, ADMIN)
- **`shipping-service`** — Shipment lifecycle with sealed class state machine: `CREATED → VERIFIED → SEALED → IN_TRANSIT → CUSTOMS → DELIVERED → CLOSED`
- **`tracking-service`** — Real-time GPS/IoT tracking, route deviation detection, ETA calculation
- **`vault-service`** — Vault registration, diamond storage/retrieval, geo-location security checks
- **`risk-service`** — AI-driven risk scoring via Spring AI (formula: `w1⋅Value + w2⋅Route + w3⋅History`)
- **`fraud-service`** — AI-powered fraud detection, tamper detection, suspicious pattern analysis
- **`customs-service`** — Customs compliance, declaration processing, country-specific regulations
- **`insurance-service`** — Valuation (`Base × Carat × Quality`), policy management, claims processing
- **`payment-service`** — Escrow payments, multi-party settlements, fraud checks
- **`notification-service`** — Real-time alerts (security, shipment updates, risk spikes)
- **`analytics-service`** — Aggregation reports (shipment analytics, risk reports, revenue insights, compliance reports)

### Key Architectural Patterns

- **Single Entry Point**: Only `application` module has `@SpringBootApplication`. All other modules are library modules auto-scanned via `@ComponentScan("com.aegisdiamond")`.
- **gRPC Communication**: All inter-service APIs exposed via gRPC services defined in `src/main/proto/*.proto` files. Uses `grpc-server-spring-boot-starter` and protobuf Gradle plugin.
- **Sealed Classes for States**: Shipment and diamond states modeled as Java sealed classes (Java 25 feature) — see `shipping-service/dto/` and `diamond-service/dto/` packages.
- **Action-Oriented Services**: Modules follow verb-based RPC methods (e.g., `registerDiamond`, `sealShipment`, `calculateRiskScore`).
- **Spring AI Integration**: `risk-service` and `fraud-service` use Spring AI (bom version `2.0.0-M5`) for intelligent analysis.
- **Database**: MySQL with JPA/Hibernate, per-module schemas defined in each service's `application.properties`.

### Java 25 Features Used

- **Records** for DTOs
- **Sealed Classes** for state modeling
- **Pattern Matching** (switch/instanceof)
- **Virtual Threads** for high-volume processing
- **Structured Concurrency** for parallel validation
- **String Templates** (preview)

### Security

- Centralized JWT authentication in `application` module (`JwtAuthenticationFilter`, `CentralSecurityConfig`)
- JWT utility in `auth-service` (`JwtUtil`)
- Role-based gRPC access control
- Basic Auth configured as fallback

### Important Conventions

- Package root for all modules: `com.aegisdiamond.*`
- Each service module follows: `entity/`, `repository/`, `service/`, `dto/`, `grpc/` layout
- gRPC stubs are generated to `build/generated/source/proto/` — do not manually edit these
- After modifying `.proto` files, run `./gradlew generateProto` to regenerate stubs
- MySQL datasource config per module in `src/main/resources/application.properties`

