# Aegis Diamond App: Project Instructions

This project is a high-security Diamond Shipping & Secure Logistic System designed for a futuristic hackathon scenario (Context Year: 2026). It leverages cutting-edge Java 25 features and a microservices-oriented architecture.

## Project Overview
The system manages the entire lifecycle of high-value diamond shipments, from registration and certification to secure transit, vault storage, customs clearance, and insurance claims. It emphasizes security, real-time tracking, and AI-driven risk assessment.

## Architecture & Modules
The project is designed as a **Modular Monolith**. While logic is separated into Gradle modules, the entire system is executed from a single entry point in the `application` module.

- `application`: **The sole runnable module**. It acts as the monolith runner, scanning all other modules for components, entities, and repositories.
- `diamond-service`: Diamond inventory and certification management logic.
- `auth-service`: Authentication & Authorization logic.
- ... (rest of the modules)

### Development Conventions
1. **Single Entry Point:** Only the `application` module contains a `@SpringBootApplication` class. Other modules must not have a `main` method or standalone application class.
2. **Component Scanning:** Ensure the `application` runner scans the `com.aegisdiamond` base package to pick up configurations from other modules.
3. **Action-Oriented Modules:** Services should follow a "verb-based" approach as outlined in `requirements.md`.
4. **Authentication:** Centrally managed in the `application` module using Basic Authentication.

## Project Status (Current State)
- Project structure is initialized with 13 modules.
- Root `build.gradle` and `settings.gradle` are configured for Java 25 and Spring Boot 4.
- Modules contain skeleton `build.gradle` files.
- Basic Spring Boot application entry point exists in the root `src`.

## TODO / Roadmap
- [ ] Define gRPC `.proto` contracts for all 12 modules.
- [ ] Implement core logic in each service following the functional requirements in `requirements.md`.
- [ ] Set up MySQL database schemas for relevant modules.
- [ ] Integrate Spring AI for risk and fraud services.
- [ ] Implement Basic Authentication security layer.
