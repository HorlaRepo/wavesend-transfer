# Frontend Migration Guide - Keycloak to JWT

**Backend Version:** v1.0.5  
**Date:** April 29, 2026  
**Status:** ✅ Backend Ready | ⏳ Frontend Update Required

---

## 🎯 Summary

The backend has been successfully migrated from Keycloak OAuth2 to JWT authentication. All endpoints are working and deployed on Azure. The frontend needs minor updates to use the new authentication and user check endpoints.

---

## ✅ What's Already Working (No Changes Needed)

### 1. Countries Endpoint
**Endpoint:** `GET /countries`  
**Auth:** None required  
**Status:** ✅ Working  
**Frontend:** No changes needed

### 2. Transfer Endpoints
**Endpoints:**
- `POST /transfers/initiate`
- `POST /transfers/verify`

**Auth:** JWT Bearer token  
**Status:** ✅ Working  
**Frontend:** Just update Authorization header to use JWT

### 3. Scheduled Transfers
**Endpoints:**
- `POST /scheduled-transfers/initiate`
- `POST /scheduled-transfers/verify`
- `GET /scheduled-transfers`
- `DELETE /scheduled-transfers/{id}`

**Auth:** JWT Bearer token  
**Status:** ✅ Working  
**Frontend:** Just update Authorization header

### 4. KYC Verification
**Endpoints:**
- `GET /kyc-verification/status`
- `POST /kyc-verification/id-document` (multipart/form-data)
- `POST /kyc-verification/address-document` (multipart/form-data)

**Auth:** JWT Bearer token  
**Status:** ✅ Working  
**Frontend:** Just update Authorization header

---

## 🔧 Required Frontend Changes

### Change 1: Update User Check Endpoint

#### Old (Keycloak):
```typescript
// Path: /keycloak/check-user
this.keycloakEventsService.checkUser({ email: email }).subscribe({...});
```

#### New (Custom JWT):
```typescript
// Path: /users/check-user
this.http.get<ApiResponseUserRepresentation>(
  `${this.apiUrl}/users/check-user?email=${email}`
).subscribe({...});
```

#### Files to Update:
1. **`src/app/modules/account/pages/send-money/send-money.component.ts`**
   - Line ~62: Replace `keycloakEventsService.checkUser()` call
   - Remove KeycloakEventsControllerService import

2. **`src/app/modules/account/components/schedule-transfer/schedule-transfer.component.ts`**
   - Replace `keycloakEventsService.checkUser()` call
   - Remove KeycloakEventsControllerService import

#### Response Format (Same as before):
```json
{
  "success": true,
  "message": "User found",
  "data": {
    "id": "uuid-string",
    "username": "user@example.com",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "emailVerified": true,
    "enabled": true
  }
}
```

---

### Change 2: Remove Keycloak Service

#### Files to Clean Up:
1. **`src/app/services/services/keycloak-events-controller.service.ts`**
   - Can be deleted (no longer used)

2. **`src/app/services/services.ts`**
   - Remove: `export { KeycloakEventsControllerService } ...`

3. **`src/app/services/api.module.ts`**
   - Remove from providers array

---

### Change 3: Update Environment Configuration

#### Update API Base URL:
**File:** `src/environments/environment.prod.ts`

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://wavesend-backend.ashyground-f0c03aa4.centralus.azurecontainerapps.io/api/v1'
};
```

---

## 📋 API Payload Reference

### 1. Login
**Endpoint:** `POST /auth/login`

**Request:**
```json
{
  "username": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1...",
    "refreshToken": "eyJhbGciOiJIUzI1...",
    "tokenType": "Bearer"
  }
}
```

---

### 2. Initiate Transfer
**Endpoint:** `POST /transfers/initiate`  
**Auth:** Required

**Request:**
```json
{
  "senderEmail": "sender@example.com",
  "receiverEmail": "receiver@example.com",
  "amount": 100.50,
  "narration": "Payment for services"
}
```

---

### 3. Verify Transfer  
**Endpoint:** `POST /transfers/verify`  
**Auth:** Required

**Request:**
```json
{
  "transactionId": "uuid-or-id",
  "otp": "123456"
}
```

---

### 4. Schedule Transfer
**Endpoint:** `POST /scheduled-transfers/initiate`  
**Auth:** Required

**Request:**
```json
{
  "senderEmail": "sender@example.com",
  "receiverEmail": "receiver@example.com",
  "amount": 500.00,
  "description": "Monthly rent",
  "scheduledDateTime": "2026-05-01 10:00:00",
  "recurrenceType": "MONTHLY",
  "recurrenceEndDate": "2026-12-31 23:59:59",
  "totalOccurrences": 8
}
```

**Recurrence Types:**
- `NONE` - One-time transfer
- `DAILY` - Daily recurring
- `WEEKLY` - Weekly recurring
- `MONTHLY` - Monthly recurring

---

### 5. Upload KYC Documents
**Endpoint:** `POST /kyc-verification/id-document`  
**Auth:** Required  
**Content-Type:** `multipart/form-data`

**Form Data:**
- **Field Name:** `file`
- **File Types:** PDF, JPG, PNG
- **Max Size:** 10MB

---

## 🔐 Authentication Flow

### Old (Keycloak):
1. User logs in via Keycloak
2. Keycloak returns OAuth2 tokens
3. Frontend uses OAuth2 token in requests

### New (JWT):
1. User logs in via `/auth/login`
2. Backend returns JWT access & refresh tokens
3. Frontend stores tokens (localStorage/sessionStorage)
4. Frontend sends: `Authorization: Bearer {accessToken}`

---

## 🧪 Testing the Endpoints

### Test User Check (No Auth):
```bash
curl "https://wavesend-backend.ashyground-f0c03aa4.centralus.azurecontainerapps.io/api/v1/users/check-user?email=test@example.com"
```

**Expected:** 
```json
{
  "success": false,
  "message": "User not found with email: test@example.com"
}
```

### Test Countries (No Auth):
```bash
curl "https://wavesend-backend.ashyground-f0c03aa4.centralus.azurecontainerapps.io/api/v1/countries"
```

**Expected:** List of 15 countries

### Test Login:
```bash
curl -X POST "https://wavesend-backend.ashyground-f0c03aa4.centralus.azurecontainerapps.io/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"your-email@example.com","password":"your-password"}'
```

---

## 📦 DTOs Match Status

All backend DTOs match the frontend models (generated from OpenAPI):

| DTO | Backend | Frontend | Status |
|-----|---------|----------|--------|
| `CreateTransactionRequestBody` | ✅ | ✅ | Synced |
| `ScheduledTransferRequestDTO` | ✅ | ✅ | Synced |
| `TransferVerificationRequest` | ✅ | ✅ | Synced |
| `ScheduledTransferVerificationRequest` | ✅ | ✅ | Synced |
| `UserRepresentation` | ✅ | ✅ | Synced |

---

## 🚀 Quick Frontend Update Script

```bash
cd /Users/francis/IdeaProjects/money-transfer/frontend/angular/money-transfer

# 1. Update send-money component
# Replace keycloakEventsService.checkUser() with direct HTTP call to /users/check-user

# 2. Update schedule-transfer component  
# Replace keycloakEventsService.checkUser() with direct HTTP call to /users/check-user

# 3. Update environment
# Set apiUrl to Azure backend URL

# 4. Remove Keycloak dependencies
# Remove KeycloakEventsControllerService from services and modules
```

---

## ✅ Backend Endpoints Summary

### Public Endpoints (No Auth):
- `GET /countries` - Get all supported countries
- `GET /countries/{countryName}` - Get country by name  
- `GET /countries/mobile-money-options/{acronym}` - Get mobile money options
- `GET /users/check-user?email={email}` - **NEW** - Check if user exists
- `POST /auth/login` - User login
- `POST /auth/register` - User registration
- `POST /auth/activate-account` - Activate account
- Webhook endpoints (Stripe, Flutterwave, etc.)

### Protected Endpoints (JWT Required):
- All `/transfers/**` endpoints
- All `/scheduled-transfers/**` endpoints  
- All `/kyc-verification/**` endpoints
- All `/wallet/**` endpoints
- All `/beneficiaries/**` endpoints
- All `/users/**` endpoints (except check-user)

---

## 🎯 Migration Checklist

- [x] Backend migrated to JWT
- [x] User check endpoint created
- [x] Security config updated
- [x] All endpoints tested
- [x] Backend deployed to Azure
- [ ] Frontend updated to use `/users/check-user`
- [ ] Frontend removed Keycloak imports
- [ ] End-to-end testing completed

---

## 📞 Support

**Backend URL:** `https://wavesend-backend.ashyground-f0c03aa4.centralus.azurecontainerapps.io/api/v1/`  
**Backend Version:** v1.0.5  
**Deployment Status:** ✅ Healthy

For detailed payload specifications, see `FRONTEND_BACKEND_SYNC.md`
