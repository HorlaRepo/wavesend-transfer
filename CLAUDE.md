# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WaveSend is a money transfer application built with Spring Boot 3.2.1, Java 17, PostgreSQL, Redis, and Kafka. It integrates with Stripe, Flutterwave, and Paystack for payment processing, and Keycloak for authentication.

## Build & Development Commands

### Building & Running
```bash
# Clean and build
./mvnw clean install

# Run application (requires environment variables - see .env file)
./mvnw spring-boot:run

# Build Docker image (multi-platform)
./mvnw jib:build -Ddocker.image.tag=<version>

# Build Keycloak event listener JAR
./mvnw clean package -P event-listener
```

### Testing
```bash
# Run unit tests (excludes integration tests)
./mvnw test

# Run integration tests only
./mvnw verify

# Run specific test
./mvnw test -Dtest=TransactionServiceImplTest
```

### Database Migrations
```bash
# Run Flyway migrations manually
./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/money_transfer -Dflyway.user=shizzy -Dflyway.password=password

# Clean database (dangerous - drops all objects)
./mvnw flyway:clean
```

## Architecture

### Core Service Layers

**Money Transfer Services:**
- `TransactionService` - Core transaction processing with Command pattern
- `MoneyTransferService` - Orchestrates transfers between wallets
- `WalletService` - Manages wallet balances and operations
- `ScheduledTransferService` - Handles recurring/future-dated transfers

**Payment Processing:**
- Strategy pattern for payment providers (`StripePaymentStrategy`, `FlutterwavePaymentStrategy`)
- `StripeEventHandler` and `CheckoutCompletedHandler` process webhooks
- Payment method handlers in `service/payment/handler/`

**User & Security:**
- Keycloak integration for OAuth2/OIDC authentication
- Custom Keycloak event listener (build with `-P event-listener` profile)
- `KycVerificationService` manages user verification levels
- `AccountLimitService` enforces transaction limits based on KYC tier

**Scheduled Transfer Architecture:**
- `ScheduledTransferPublisherService` - @Scheduled task (every 60s) scans for due transfers, publishes to Kafka topic `scheduled-transfers-execution`
- `ScheduledTransferConsumer` - Kafka consumer processes transfers asynchronously
- `ScheduledTransferNotificationConsumer` - Sends notifications for scheduled transfers

### Messaging & Async
- Kafka topics: `scheduled-transfers-execution`, notifications
- Kafka consumers in `kafka/` package
- Redis caching for transactions and wallet balances (@Cacheable annotations)
- `@EnableAsync` and `@EnableScheduling` enabled in Main class

### Design Patterns in Use

**Command Pattern** (`serviceimpl/command/`):
- `CreateTransactionCommand`, `TransferMoneyCommand`, `UpdateTransactionStatusCommand`
- All implement `TransactionCommand<T>` interface

**Strategy Pattern** (`serviceimpl/strategy/`):
- Payment: `StripePaymentStrategy`, `FlutterwavePaymentStrategy`
- Fee calculation: `FeeCalculationStrategy`, `PercentageFeeStrategy`
- Card generation: `CardNumberGenerator` implementations

**Observer Pattern** (`observer/`):
- `TransactionStatusObserver`, `AuditLogTransactionObserver`
- Notified on transaction state changes

**Builder Pattern** (`serviceimpl/builder/`):
- Complex object construction

**Factory Pattern** (`serviceimpl/factory/`):
- Dynamic object creation based on types

### Security Filters (Applied in Order)
1. `RateLimitingFilter` - Bucket4j rate limiting per IP
2. `RequestValidationFilter` - Input sanitization and validation
3. `SecurityHeadersFilter` - Security headers (CSP, HSTS, etc.)

### Key Configuration
- Base path: `/api/v1/`
- Port: 8080
- Database: PostgreSQL with Flyway migrations (`src/main/resources/db/migration/`)
- Cache: Redis + Caffeine
- OAuth2 Resource Server: Validates JWT from Keycloak
- CORS: Configured to allow all origins in dev (check SecurityConfig for restrictions)

## Important Implementation Details

### Transaction Processing
- Transactions use optimistic locking and database-level constraints
- Status updates trigger observers for audit logging
- Wallet operations are transactional and cached
- Transaction reference format is validated at database level (V11 migration)

### Scheduled Transfers
- Scanner runs every 60 seconds with 2-minute lookahead window
- Due transfers are published to Kafka for async processing
- Retry logic with MAX_RETRY_COUNT=3 for failed transfers
- Status tracking: PENDING → PROCESSING → COMPLETED/FAILED

### Payment Webhooks
- Stripe webhook: `/payment/stripe-webhook` (validates signature)
- Flutterwave webhook: `/payment/flutterwave-webhook`
- All webhook endpoints are permitAll() in SecurityConfig

### Caching Strategy
- Redis for distributed cache (transactions, wallets)
- Caffeine for local in-memory cache
- Cache keys follow pattern: `transaction:{id}`, `wallet:{userId}`

### AI Financial Service
- `AiFinancialService` and `ConversationManagerService` integrate with OpenRouter API
- Provides financial insights and conversation management

## Testing
- Unit tests use Mockito and JUnit 5
- Integration tests use Testcontainers (PostgreSQL, Redis, Vault)
- Extend `AbstractTestContainers` for integration test base
- Tests are excluded from main build via maven-surefire-plugin config

## Environment Variables
All required variables are in `.env` file (not committed to prod). Key variables:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - PostgreSQL connection
- `REDIS_HOST`, `REDIS_PORT` - Redis connection
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka broker
- `JWT_ISSUER_URI` - Keycloak realm endpoint
- `STRIPE_API_KEY`, `STRIPE_WEBHOOK_SECRET` - Stripe integration
- `FLUTTERWAVE_API_KEY`, `FLUTTERWAVE_SECRET_HASH` - Flutterwave integration

## Common Pitfalls
- Scheduled transfer publisher requires Kafka to be running; startup will fail if broker is unavailable
- Keycloak JWT validation requires issuer URI to be reachable from the app
- Redis connection failures cause cache operations to fall back to database (check logs)
- Flyway baseline is enabled; first migration on existing DB won't fail
- The `event-listener` Maven profile builds a separate JAR for Keycloak deployment - don't confuse with main application JAR
