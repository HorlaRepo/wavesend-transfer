# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WaveSend is a money transfer application with a Spring Boot backend and Angular frontend, deployed on Azure.

- **Backend**: Spring Boot 3.2.1, Java 17, PostgreSQL, Redis, Kafka
- **Frontend**: Angular 16, PrimeNG, Bootstrap, angular-code-input
- **Payment Providers**: Stripe, Flutterwave, Paystack
- **Authentication**: Custom JWT (access + refresh tokens) — Keycloak was removed
- **Email**: Mailtrap (via REST API) — Brevo was removed
- **AI**: OpenRouter / Gemini for financial assistant

## Repository Structure

```
/Users/francis/IdeaProjects/money-transfer/
├── backend/          ← This repo (Spring Boot)
└── frontend/angular/money-transfer/   ← Angular frontend (separate Git repo)
```

**Git Repos:**
- Backend: https://github.com/HorlaRepo/wavesend-transfer
- Frontend: https://github.com/HorlaRepo/wavesend-app

## Build & Development Commands

### Backend - Building & Running
```bash
# Clean and build
./mvnw clean install

# Run application (requires environment variables - see .env file)
./mvnw spring-boot:run

# Compile only (fast check)
./mvnw compile -q
```

### Frontend - Building & Running
```bash
cd ../frontend/angular/money-transfer

# Install dependencies
npm install

# Run dev server
ng serve

# Production build
ng build --configuration=production
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
- Custom JWT authentication (access token 1hr + refresh token 7 days)
- `JwtTokenProvider` generates/validates tokens, `JwtAuthenticationFilter` extracts from requests
- `KycVerificationService` manages user verification levels
- `AccountLimitService` enforces transaction limits based on KYC tier
- `OtpService` handles OTP for transfers/withdrawals (separate from account activation)

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
- Cache: Redis (Azure Cache for Redis with SSL on port 6380)
- Authentication: Custom JWT (not Keycloak) — see `JwtTokenProvider`, `JwtAuthenticationFilter`
- CORS: Configured in SecurityConfig (allows `https://app.wavesend.cc`)
- Email: Mailtrap REST API (`MailtrapEmailService` with `@Primary`)

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
- `AiFinancialService` and `ConversationManagerService` integrate with Gemini/OpenRouter
- Provides financial insights and conversation management
- IMPORTANT: Never expose internal IDs (wallet.id) to AI context — use user-facing identifiers (wallet.walletId)

## Testing
- Unit tests use Mockito and JUnit 5
- Integration tests use Testcontainers (PostgreSQL, Redis, Vault)
- Extend `AbstractTestContainers` for integration test base
- Tests are excluded from main build via maven-surefire-plugin config

## Environment Variables
All required variables are in `.env` file (not committed to prod). Key variables:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - PostgreSQL connection
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` - Azure Redis (SSL, port 6380)
- `KAFKA_BOOTSTRAP_SERVERS`, `EVENTHUB_CONNECTION_STRING` - Azure Event Hubs (Kafka protocol)
- `JWT_SECRET_KEY` - HMAC key for signing JWTs
- `MAILTRAP_TOKEN` - Mailtrap API token for email sending
- `STRIPE_API_KEY`, `STRIPE_WEBHOOK_SECRET` - Stripe integration
- `FLUTTERWAVE_API_KEY`, `FLUTTERWAVE_SECRET_HASH` - Flutterwave integration
- `GEMINI_API_KEY` - Gemini AI for financial assistant

## Deployment

### Infrastructure (Azure)
- **Backend**: Azure Container Apps (`wavesend-backend` in `wavesend-rg`)
- **Database**: Azure Database for PostgreSQL
- **Cache**: Azure Cache for Redis (SSL on port 6380)
- **Messaging**: Azure Event Hubs (Kafka-compatible)
- **Registry**: Azure Container Registry (`wavesendacr2026`)
- **Frontend**: Cloudflare Pages (from GitHub repo)
- **Domain**: app.wavesend.cc (frontend), backend accessed via Container Apps FQDN

### Backend Deployment Steps
```bash
# 1. Compile and verify
./mvnw compile -q

# 2. Login to Azure Container Registry
TOKEN=$(az acr login -n wavesendacr2026 --expose-token --query accessToken -o tsv)

# 3. Build and push Docker image with Jib (increment version!)
./mvnw compile jib:build \
  -Ddocker.image.tag=<VERSION> \
  -Djib.to.image=wavesendacr2026.azurecr.io/wavesend-backend:<VERSION> \
  "-Djib.to.auth.username=00000000-0000-0000-0000-000000000000" \
  "-Djib.to.auth.password=$TOKEN"

# 4. Deploy to Azure Container Apps
az containerapp update \
  --name wavesend-backend \
  --resource-group wavesend-rg \
  --image wavesendacr2026.azurecr.io/wavesend-backend:<VERSION>

# 5. Verify health
curl -s https://wavesend-backend.nicegrass-8402cffd.centralus.azurecontainerapps.io/api/v1/health
```

### Frontend Deployment
The frontend auto-deploys via Cloudflare Pages on push to `main` branch of the frontend repo.
```bash
cd ../frontend/angular/money-transfer
git add -A && git commit -m "message" && git push origin main
# Cloudflare Pages picks up the push and deploys automatically
```

### Updating Azure Environment Variables
```bash
az containerapp update \
  --name wavesend-backend \
  --resource-group wavesend-rg \
  --set-env-vars "VAR_NAME=value"
```

### Checking Logs
```bash
az containerapp logs show --name wavesend-backend --resource-group wavesend-rg --tail 50 --follow false
```

### Current Version
Check current deployed version:
```bash
az containerapp show --name wavesend-backend --resource-group wavesend-rg \
  --query properties.template.containers[0].image -o tsv
```

## End-to-End Workflow: Implementing a Feature or Fix

### Step 1: Understand the change
- Identify if it's backend-only, frontend-only, or both
- Check the relevant service/component files

### Step 2: Implement backend changes
- Edit source files in `src/main/java/com/shizzy/moneytransfer/`
- Profile for Azure: `src/main/resources/application-azure.yml`
- Compile check: `./mvnw compile -q`

### Step 3: Implement frontend changes
- Frontend location: `../frontend/angular/money-transfer/src/app/`
- Key directories:
  - `components/` - Page components (login, register, activate-account)
  - `modules/account/` - Authenticated pages (dashboard, transactions, settings)
  - `services/auth/` - AuthService, auth models
  - `services/interceptor/` - HTTP interceptor (attaches JWT, handles 401 refresh)
  - `services/token/` - Token storage (localStorage)
  - `services/inactivity/` - Session timeout monitoring

### Step 4: Commit and push
```bash
# Backend
git add -A && git commit -m "description" && git push origin main

# Frontend (separate repo!)
cd ../frontend/angular/money-transfer
git add -A && git commit -m "description" && git push origin main
```

### Step 5: Deploy backend
```bash
TOKEN=$(az acr login -n wavesendacr2026 --expose-token --query accessToken -o tsv)
./mvnw compile jib:build -Ddocker.image.tag=<VER> -Djib.to.image=wavesendacr2026.azurecr.io/wavesend-backend:<VER> "-Djib.to.auth.username=00000000-0000-0000-0000-000000000000" "-Djib.to.auth.password=$TOKEN"
az containerapp update --name wavesend-backend --resource-group wavesend-rg --image wavesendacr2026.azurecr.io/wavesend-backend:<VER>
```

### Step 6: Verify
```bash
# Health check
curl -s https://wavesend-backend.nicegrass-8402cffd.centralus.azurecontainerapps.io/api/v1/health

# Check logs for errors
az containerapp logs show --name wavesend-backend --resource-group wavesend-rg --tail 30 --follow false
```

## Frontend Architecture (Angular)

### Key Files
- `app-routing.module.ts` - Routes (login, register, activate-account, account/*)
- `app.module.ts` - Root module declarations and imports
- `modules/account/account.module.ts` - All authenticated pages
- `modules/account/pages/main/main.component.*` - Layout wrapper (header + footer + inactivity modal)
- `environments/environment.prod.ts` - Production API URL

### Auth Flow
1. Login → backend returns `accessToken` + `refreshToken`
2. Tokens stored in localStorage via `TokenService`
3. `HttpTokenInterceptor` attaches Bearer token to all API requests
4. On 401 → interceptor attempts refresh via `/auth/refresh-token`
5. If refresh fails → `AuthService.logout()` clears tokens and redirects to /login
6. `InactivityService` monitors user activity, shows timeout modal after 3 min

### Registration & Activation Flow
1. User registers → backend sends 6-digit code via Mailtrap
2. Frontend navigates to `/activate-account?email=...`
3. User enters code → `GET /auth/activate-account?token=123456`
4. If user tries to login before activating → backend resends code, frontend redirects to activation page

### Important Frontend Patterns
- HTTP interceptor checks `request.url.startsWith(environment.apiUrl)` to attach tokens
- `@Primary` on `MailtrapEmailService` means it's used everywhere unless `@Qualifier` overrides
- All email services must use `@Qualifier("mailtrapEmailService")` — Brevo is deprecated/removed

## Common Pitfalls
- Scheduled transfer publisher requires Kafka to be running; startup will fail if broker is unavailable
- Redis connection failures cause cache operations to fall back to database (check logs)
- Flyway baseline is enabled; first migration on existing DB won't fail
- ACR token expires quickly — always re-obtain before `jib:build`
- Docker daemon not required — Jib pushes directly to registry. Use `--expose-token` for auth
- Frontend interceptor must match `environment.apiUrl` — hardcoded URL patterns will break in different environments
- Spring Security's `AuthenticationManager.authenticate()` throws `DisabledException` for disabled users BEFORE your code runs — handle in catch block, not after authenticate()
- Never expose internal DB IDs to users or AI context — use user-facing identifiers
- `deploy-containerapp.sh` contains secrets — it's in .gitignore, never commit it
