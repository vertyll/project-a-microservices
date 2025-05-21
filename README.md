<p align="center">
<img src="https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white">
<img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white">
<img src="https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white">
<img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white">
</p>

# Project A Microservices

A microservices-based architecture for Project A, following Domain-Driven Design (DDD) principles, Separation of Concerns (SoC), SOLID principles, and the Saga pattern for distributed transactions.

## Architecture

The project is split into the following components:

1. **API Gateway** - Entry point for all client requests, handles routing to appropriate services and JWT token validation
2. **Auth Service** - Manages authentication and authorization with JWT tokens and refresh tokens
3. **User Service** - Handles user profile management and user-related operations
4. **Role Service** - Manages roles and permissions across the system
5. **Mail Service** - Handles email sending operations and templates
6. **Common Library** - Shared code, contracts, and utilities used across all microservices

Each microservice has its own PostgreSQL database and communicates with other services via Apache Kafka for event-driven architecture.

### Detailed Description of Components

#### Common Library
- Provides shared code, DTOs, event definitions, and utilities for all microservices
- Implements common patterns like the Saga pattern and Outbox pattern
- Contains reusable components for Kafka integration, exception handling, and API responses
- Ensures consistency in how services communicate and process events

#### Auth Service
- Responsible for user authentication and authorization using JWT and refresh tokens
- Manages the user credentials, account activation, and session management
- Database stores:
  - User credentials (email, hashed passwords)
  - Refresh tokens
  - Verification tokens (for account activation, password reset, etc.)
  - User roles (mirrored from Role Service)
- Provides endpoints for registration, login, logout, account activation, password reset, and email change

#### Role Service
- Manages the roles and permissions throughout the system
- Database stores:
  - Role definitions
  - Permission definitions
  - User-role assignments
- Provides APIs for creating, updating, and assigning roles
- Publishes role-related events to Kafka for other services to consume

#### User Service
- Manages user profiles and user-related information not needed for authentication
- Database stores:
  - User personal information (first name, last name, etc.)
  - User preferences
  - Other user-specific data not required for authentication
- Consumes user-related events from other services
- Provides APIs for managing user profiles

#### Mail Service
- Responsible for sending emails based on templates
- Database stores:
  - Email logs
  - Delivery status
  - Email templates
- Consumes mail request events from other services
- Supports various email templates (welcome, password reset, account activation, etc.)

## Technology Stack

- **Backend**: Spring Boot, Kotlin, Gradle Kotlin DSL
- **Database**: PostgreSQL (separate instance for each service)
- **Message Broker**: Apache Kafka (for event-driven communication)
- **API Documentation**: OpenAPI (Swagger)
- **Containerization**: Docker/Podman, Docker Compose/Podman Compose
- **Authentication**: JWT and refresh tokens
- **Testing**: JUnit, Testcontainers

## Development Setup

### Prerequisites

- Docker and Docker Compose
- JDK 21 (LTS)
- Gradle

### Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/project-a-microservices.git
   # and
   cd project-a-microservices
   ```

2. Start the infrastructure using Docker Compose:
   ```bash
   docker-compose up -d
   ```

3. Access the services:
   - API Gateway: http://localhost:8080
   - Auth Service: http://localhost:8082
   - User Service: http://localhost:8083
   - Role Service: http://localhost:8084
   - Mail Service: http://localhost:8085
   - Kafka UI: http://localhost:8090
   - MailDev: http://localhost:1080

### Development Workflow

1. Make changes to the relevant service code
2. Build the service:
   ```bash
   ./gradlew :<service-name>:build
   ```
3. Restart the service container:
   ```bash
   docker-compose up -d --build <service-name>
   ```

## Architecture Design

### Domain-Driven Design (DDD)

Each microservice is designed around a specific business domain with:
- A clear bounded context
- Domain models that represent business entities
- A layered architecture (domain, application, infrastructure)
- Domain-specific language
- Encapsulated business logic

The project structure follows DDD principles:
- `domain`: Core business models, repositories, and services
- `application`: Controllers and application services
- `infrastructure`: Technical concerns like Kafka, database configurations

### SOLID Principles and Separation of Concerns

The codebase adheres to:
- **Single Responsibility Principle**: Each class has a single responsibility
- **Open/Closed Principle**: Classes are open for extension but closed for modification
- **Liskov Substitution Principle**: Subtypes are substitutable for their base types
- **Interface Segregation Principle**: Specific interfaces rather than general ones
- **Dependency Inversion Principle**: Depends on abstractions, not concretions

### Saga Pattern for Distributed Transactions

For distributed transactions that span multiple services, we use the Saga pattern:

1. A service publishes a domain event to Kafka
2. Other services consume the event and perform their operations
3. If an operation fails, compensating transactions are triggered to maintain consistency

Example: User Registration Saga
- Auth Service: Creates new user credentials and publishes UserRegisteredEvent
- User Service: Consumes event and creates user profile
- Role Service: Consumes event and assigns default roles
- Mail Service: Consumes event and sends welcome email

### Event-Driven Communication

Services communicate asynchronously through Kafka events:

- **UserRegisteredEvent**: Triggered when a new user registers
- **UserActivatedEvent**: Triggered when a user activates their account
- **MailRequestedEvent**: Triggered when an email needs to be sent
- **RoleCreatedEvent**: Triggered when a new role is created
- **RoleAssignedEvent**: Triggered when a role is assigned to a user
- **UserProfileUpdatedEvent**: Triggered when a user profile is updated

## API Documentation

Each service provides its own Swagger UI for API documentation:

- Auth Service: http://localhost:8082/api/auth/swagger-ui.html
- User Service: http://localhost:8083/api/users/swagger-ui.html
- Role Service: http://localhost:8084/api/roles/swagger-ui.html
- Mail Service: http://localhost:8085/api/mail/swagger-ui.html

## Monitoring

- Each service exposes health and metrics endpoints through Spring Boot Actuator
- Health checks can be accessed at `/actuator/health` on each service
