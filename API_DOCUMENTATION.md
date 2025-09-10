# EventR API Documentation

## Overview

EventR is a comprehensive event management platform built with Spring Boot (Kotlin) and React TypeScript. This documentation covers all REST API endpoints, WebSocket connections, authentication flows, and data models.

**Base URL**: `https://your-domain.com/api`  
**API Version**: v1  
**Authentication**: JWT Bearer tokens  
**Content Type**: `application/json`

---

## Table of Contents

1. [Authentication & Authorization](#authentication--authorization)
2. [Event Management](#event-management)
3. [Registration Management](#registration-management)
4. [Check-in & QR Code System](#check-in--qr-code-system)
5. [Session Management](#session-management)
6. [Analytics & Reporting](#analytics--reporting)
7. [File Upload & Management](#file-upload--management)
8. [Calendar Integration](#calendar-integration)
9. [Webhook Management](#webhook-management)
10. [Resource Management](#resource-management)
11. [WebSocket Real-time Features](#websocket-real-time-features)
12. [Data Models](#data-models)
13. [Error Handling](#error-handling)
14. [Rate Limiting](#rate-limiting)

---

## Authentication & Authorization

### Overview
EventR uses JWT (JSON Web Token) based authentication. All protected endpoints require a valid JWT token in the Authorization header.

### Authentication Endpoints

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "StrongPass123",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1-555-0123",
  "company": "Acme Corp",
  "jobTitle": "Event Manager",
  "marketingEmails": true
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "USER",
    "status": "ACTIVE",
    "emailVerified": false,
    "createdAt": "2024-01-15T10:30:00"
  },
  "expiresIn": 86400
}
```

#### Login User
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "StrongPass123",
  "rememberMe": false
}
```

**Response (200 OK):** Same as register response

#### Verify Email
```http
POST /api/auth/verify-email
Content-Type: application/json

{
  "token": "email-verification-token"
}
```

#### Forgot Password
```http
POST /api/auth/forgot-password
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**Response (200 OK):**
```json
{
  "message": "Password reset email sent if account exists"
}
```

#### Reset Password
```http
POST /api/auth/reset-password
Content-Type: application/json

{
  "token": "password-reset-token",
  "newPassword": "NewStrongPass123"
}
```

#### Change Password
```http
PUT /api/auth/change-password
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "currentPassword": "OldPass123",
  "newPassword": "NewStrongPass123"
}
```

#### Get User Profile
```http
GET /api/auth/profile
Authorization: Bearer <jwt-token>
```

#### Update User Profile
```http
PUT /api/auth/profile
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe Updated",
  "phone": "+1-555-0124",
  "company": "New Company",
  "jobTitle": "Senior Manager",
  "bio": "Experienced event manager",
  "timezone": "America/New_York",
  "language": "en-US",
  "marketingEmails": true,
  "eventReminders": true,
  "weeklyDigest": false
}
```

#### Logout
```http
POST /api/auth/logout
Authorization: Bearer <jwt-token>
```

### Authorization Levels
- **Public**: No authentication required
- **User**: Requires valid JWT token
- **Admin**: Requires admin role (for administrative endpoints)

---

## Event Management

### Get All Events (with filtering)
```http
GET /api/events?category=BUSINESS&eventType=CONFERENCE&city=New York&publishedOnly=true&sortBy=startDateTime&sortOrder=asc
```

**Query Parameters:**
- `category` (optional): Event category filter
- `eventType` (optional): Event type filter  
- `city` (optional): City filter
- `tags` (optional): Comma-separated tags
- `startDate` (optional): Filter by start date (YYYY-MM-DD)
- `endDate` (optional): Filter by end date (YYYY-MM-DD)
- `q` (optional): Search query
- `latitude` (optional): Geolocation latitude
- `longitude` (optional): Geolocation longitude
- `radius` (optional): Search radius in miles
- `sortBy` (optional): Sort field (name, city, category, date)
- `sortOrder` (optional): asc/desc
- `publishedOnly` (boolean, default: true): Show only published events

**Response (200 OK):**
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Tech Conference 2024",
    "description": "Annual technology conference",
    "status": "PUBLISHED",
    "bannerImageUrl": "https://s3.amazonaws.com/bucket/banner.jpg",
    "thumbnailImageUrl": "https://s3.amazonaws.com/bucket/thumb.jpg",
    "tags": ["technology", "networking", "innovation"],
    "capacity": 500,
    "waitlistEnabled": true,
    "eventType": "CONFERENCE",
    "category": "BUSINESS",
    "venueName": "Convention Center",
    "address": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA",
    "virtualUrl": null,
    "requiresApproval": false,
    "maxRegistrations": 500,
    "organizerName": "John Smith",
    "organizerEmail": "john@example.com",
    "startDateTime": "2024-03-15T09:00:00",
    "endDateTime": "2024-03-15T17:00:00",
    "timezone": "America/New_York",
    "agenda": "9:00 AM - Welcome, 10:00 AM - Keynote...",
    "instances": [
      {
        "id": "456e7890-e89b-12d3-a456-426614174001",
        "eventId": "123e4567-e89b-12d3-a456-426614174000",
        "startDateTime": "2024-03-15T09:00:00",
        "endDateTime": "2024-03-15T17:00:00",
        "maxRegistrations": 500,
        "currentRegistrations": 127
      }
    ]
  }
]
```

### Get Event by ID
```http
GET /api/events/{eventId}
Authorization: Bearer <jwt-token>
```

### Create Event
```http
POST /api/events
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "name": "New Conference",
  "description": "A comprehensive conference on modern technology",
  "eventType": "CONFERENCE",
  "category": "BUSINESS",
  "venueName": "Grand Convention Center",
  "address": "456 Tech Blvd",
  "city": "San Francisco",
  "state": "CA",
  "zipCode": "94105",
  "country": "USA",
  "capacity": 1000,
  "waitlistEnabled": true,
  "requiresApproval": false,
  "organizerName": "Tech Events Inc",
  "organizerEmail": "events@techevents.com",
  "organizerPhone": "+1-555-0199",
  "startDateTime": "2024-06-15T09:00:00",
  "endDateTime": "2024-06-15T17:00:00",
  "timezone": "America/Los_Angeles",
  "tags": ["technology", "ai", "innovation"],
  "agenda": "Detailed agenda...",
  "formData": {
    "registrationForm": [
      {
        "type": "text",
        "label": "Dietary Restrictions",
        "required": false
      }
    ]
  }
}
```

### Update Event
```http
PUT /api/events/{eventId}
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
(Same body structure as create)

### Delete Event
```http
DELETE /api/events/{eventId}
Authorization: Bearer <jwt-token>
```

### Publish Event
```http
POST /api/events/{eventId}/publish
Authorization: Bearer <jwt-token>
```

### Clone Event
```http
POST /api/events/{eventId}/clone
Authorization: Bearer <jwt-token>
```

### Get Event Form Definition
```http
GET /api/events/{eventId}/form
Authorization: Bearer <jwt-token>
```

### Get Event Registrations
```http
GET /api/events/{eventId}/registrations
Authorization: Bearer <jwt-token>
```

### Bulk Registration Actions
```http
POST /api/events/{eventId}/registrations/bulk
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "action": "approve", // "approve", "cancel", "checkin", "email"
  "registrationIds": ["uuid1", "uuid2", "uuid3"],
  "reason": "Event cancelled due to weather",
  "emailSubject": "Important Event Update",
  "emailBody": "We regret to inform you..."
}
```

### Send Event Email
```http
POST /api/events/{eventId}/email
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "recipientType": "selected", // "selected", "all", "status"
  "statusFilter": "REGISTERED", // when recipientType is "status"
  "selectedRegistrationIds": ["uuid1", "uuid2"], // when recipientType is "selected"
  "subject": "Event Reminder",
  "body": "Don't forget about our upcoming event..."
}
```

---

## Registration Management

### Create Registration
```http
POST /api/registrations
Content-Type: application/json

{
  "eventInstanceId": "456e7890-e89b-12d3-a456-426614174001",
  "userId": "123e4567-e89b-12d3-a456-426614174000", // Optional, for authenticated users
  "userEmail": "attendee@example.com", // Required if no userId
  "userName": "Jane Doe", // Required if no userId
  "formData": {
    "dietaryRestrictions": "Vegetarian",
    "tshirtSize": "L",
    "company": "Tech Corp"
  }
}
```

**Response (200 OK):**
```json
{
  "id": "789e0123-e89b-12d3-a456-426614174002",
  "eventInstanceId": "456e7890-e89b-12d3-a456-426614174001",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "userEmail": "attendee@example.com",
  "userName": "Jane Doe",
  "status": "REGISTERED",
  "formData": {
    "dietaryRestrictions": "Vegetarian",
    "tshirtSize": "L"
  },
  "registeredAt": "2024-01-15T14:30:00"
}
```

### Get Registrations by User Email
```http
GET /api/registrations/user/{email}
```

### Get Registrations by User ID
```http
GET /api/registrations/user/id/{userId}
Authorization: Bearer <jwt-token>
```

### Cancel Registration
```http
PUT /api/registrations/{registrationId}/cancel?reason=Unable to attend
Authorization: Bearer <jwt-token>
```

---

## Check-in & QR Code System

### QR Code Check-in
```http
POST /api/checkin/qr
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "qrCode": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...",
  "deviceId": "mobile-device-123",
  "deviceName": "iPhone 13",
  "location": "Main Entrance",
  "checkedInBy": "Staff Member",
  "notes": "VIP attendee"
}
```

**Response (200 OK):**
```json
{
  "id": "check-in-uuid",
  "registrationId": "789e0123-e89b-12d3-a456-426614174002",
  "sessionId": null,
  "type": "EVENT",
  "method": "QR_CODE",
  "checkedInAt": "2024-03-15T09:15:00",
  "checkedInBy": "Staff Member",
  "userName": "Jane Doe",
  "userEmail": "attendee@example.com",
  "eventName": "Tech Conference 2024",
  "isVerified": true,
  "location": "Main Entrance",
  "notes": "VIP attendee"
}
```

### Manual Check-in
```http
POST /api/checkin/manual
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "registrationId": "789e0123-e89b-12d3-a456-426614174002",
  "type": "EVENT",
  "checkedInBy": "Reception Staff",
  "location": "Registration Desk",
  "notes": "Late arrival"
}
```

### Bulk Check-in
```http
POST /api/checkin/bulk
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "registrationIds": ["uuid1", "uuid2", "uuid3"],
  "sessionId": null,
  "type": "EVENT",
  "checkedInBy": "Bulk Check-in Staff",
  "location": "Main Hall",
  "notes": "Group check-in"
}
```

### Generate Event QR Code
```http
GET /api/checkin/qr/event/{eventId}/user/{userId}
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
{
  "qrCodeBase64": "iVBORw0KGgoAAAANSUhEUgAAAQA...",
  "qrCodeUrl": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAQA...",
  "expiresAt": "2024-03-15T23:59:59",
  "type": "EVENT_CHECKIN",
  "identifier": "event-123-user-456"
}
```

### Generate Session QR Code
```http
GET /api/checkin/qr/session/{sessionId}/user/{userId}
Authorization: Bearer <jwt-token>
```

### Generate Staff QR Code
```http
GET /api/checkin/qr/staff/event/{eventId}
Authorization: Bearer <jwt-token>
```

### Generate Attendee Badge
```http
GET /api/checkin/qr/badge/event/{eventId}/user/{userId}?userName=John Doe
Authorization: Bearer <jwt-token>
```

### Download Badge Image
```http
GET /api/checkin/qr/badge/event/{eventId}/user/{userId}/image?userName=John Doe
Authorization: Bearer <jwt-token>
```

**Response:** PNG image file download

### Get Event Check-in Stats
```http
GET /api/checkin/event/{eventId}/stats
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "totalRegistrations": 250,
  "totalCheckedIn": 187,
  "eventCheckedIn": 187,
  "sessionCheckedIn": 0,
  "checkInRate": 74.8,
  "recentCheckIns": [
    {
      "id": "recent-checkin-1",
      "userName": "John Doe",
      "checkedInAt": "2024-03-15T09:15:00",
      "method": "QR_CODE"
    }
  ],
  "checkInsByHour": {
    "09:00": 45,
    "10:00": 32,
    "11:00": 28
  },
  "checkInsByMethod": {
    "QR_CODE": 150,
    "MANUAL": 37
  }
}
```

### Get Session Attendance
```http
GET /api/checkin/session/{sessionId}/attendance
Authorization: Bearer <jwt-token>
```

### Get Attendance Report
```http
GET /api/checkin/event/{eventId}/report
Authorization: Bearer <jwt-token>
```

### Sync Offline Check-ins
```http
POST /api/checkin/sync
Authorization: Bearer <jwt-token>
Content-Type: application/json

[
  {
    "registrationId": "uuid1",
    "type": "EVENT",
    "method": "QR_CODE",
    "checkedInAt": "2024-03-15T09:30:00",
    "checkedInBy": "Mobile App",
    "deviceId": "offline-device-1",
    "needsSync": true
  }
]
```

---

## Session Management

### Get Sessions by Event
```http
GET /api/sessions/event/{eventId}
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
[
  {
    "id": "session-uuid-1",
    "eventId": "123e4567-e89b-12d3-a456-426614174000",
    "title": "Opening Keynote",
    "description": "Welcome address and industry overview",
    "startTime": "2024-03-15T09:00:00",
    "endTime": "2024-03-15T10:00:00",
    "location": "Main Auditorium",
    "capacity": 500,
    "currentRegistrations": 485,
    "speaker": "Jane Smith, CEO",
    "tags": ["keynote", "opening"],
    "sessionType": "PRESENTATION",
    "requiresSeparateRegistration": false,
    "isBreak": false,
    "isOptional": false
  }
]
```

### Get Session by ID
```http
GET /api/sessions/{sessionId}
Authorization: Bearer <jwt-token>
```

### Create Session
```http
POST /api/sessions
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "title": "AI Workshop",
  "description": "Hands-on workshop on artificial intelligence",
  "startTime": "2024-03-15T14:00:00",
  "endTime": "2024-03-15T16:00:00",
  "location": "Workshop Room A",
  "capacity": 30,
  "speaker": "Dr. Alice Johnson",
  "sessionType": "WORKSHOP",
  "requiresSeparateRegistration": true,
  "tags": ["ai", "workshop", "hands-on"]
}
```

### Update Session
```http
PUT /api/sessions/{sessionId}
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
(Same body structure as create)

### Delete Session
```http
DELETE /api/sessions/{sessionId}
Authorization: Bearer <jwt-token>
```

### Get Session Attendees
```http
GET /api/sessions/{sessionId}/attendees
Authorization: Bearer <jwt-token>
```

---

## Analytics & Reporting

### Get Executive Metrics
```http
GET /api/analytics/executive?timeframe=30d
Authorization: Bearer <jwt-token>
```

**Query Parameters:**
- `timeframe`: 7d, 30d, 90d, 1y

**Response (200 OK):**
```json
{
  "totalEvents": 45,
  "totalRegistrations": 1250,
  "totalRevenue": 125000.00,
  "attendanceRate": 82.5,
  "activeEvents": 8,
  "upcomingEvents": 12,
  "completedEvents": 25,
  "avgEventCapacity": 150.2,
  "registrationTrend": 15.3,
  "revenueTrend": 22.1
}
```

### Get Top Events
```http
GET /api/analytics/executive/events?limit=5
Authorization: Bearer <jwt-token>
```

### Get Chart Data
```http
GET /api/analytics/executive/charts?timeframe=30d
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
{
  "labels": ["Week 1", "Week 2", "Week 3", "Week 4"],
  "registrationData": [45, 67, 89, 102],
  "revenueData": [4500.00, 6700.00, 8900.00, 10200.00],
  "attendanceData": [85.2, 78.9, 82.1, 90.3]
}
```

### Get Registration Trends
```http
GET /api/analytics/registrations?timeframe=30d
Authorization: Bearer <jwt-token>
```

### Get Event-Specific Analytics
```http
GET /api/analytics/events/{eventId}
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "eventName": "Tech Conference 2024",
  "totalRegistrations": 187,
  "totalCheckIns": 142,
  "attendanceRate": 75.9,
  "sessionCount": 8,
  "avgSessionAttendance": 45.2,
  "revenue": 18700.00,
  "registrationsByDay": [
    {
      "date": "2024-01-15",
      "registrations": 23
    }
  ],
  "checkInMethods": [
    {
      "method": "QR_CODE",
      "count": 120,
      "percentage": 84.5
    },
    {
      "method": "MANUAL", 
      "count": 22,
      "percentage": 15.5
    }
  ],
  "sessionAnalytics": [
    {
      "sessionId": "session-1",
      "sessionTitle": "Opening Keynote",
      "registrations": 187,
      "checkedIn": 142,
      "attendanceRate": 75.9,
      "capacity": 200
    }
  ]
}
```

### Get Attendance Analytics
```http
GET /api/analytics/attendance?timeframe=30d
Authorization: Bearer <jwt-token>
```

---

## File Upload & Management

### Upload Event Banner
```http
POST /api/files/upload/event-banner
Authorization: Bearer <jwt-token>
Content-Type: multipart/form-data

form-data:
file: [binary file data]
```

**Response (200 OK):**
```json
{
  "url": "https://s3.amazonaws.com/eventr-uploads/banners/event-123-banner.jpg",
  "message": "Banner uploaded successfully"
}
```

### Upload Event Thumbnail
```http
POST /api/files/upload/event-thumbnail
Authorization: Bearer <jwt-token>
Content-Type: multipart/form-data

form-data:
file: [binary file data]
```

### Upload Event Image (Generic)
```http
POST /api/files/upload/event-image
Authorization: Bearer <jwt-token>
Content-Type: multipart/form-data

form-data:
file: [binary file data]
type: banner
```

### Delete File
```http
DELETE /api/files/delete?url=https://s3.amazonaws.com/bucket/file.jpg
Authorization: Bearer <jwt-token>
```

---

## Calendar Integration

### Download Event Calendar (.ics)
```http
GET /api/calendar/event/{eventId}.ics
```

**Response:** ICS calendar file download

### Download Personalized Calendar
```http
GET /api/calendar/event/{eventId}/registration/{registrationId}.ics
```

### Get Event Calendar Info
```http
GET /api/calendar/event/{eventId}/info
```

**Response (200 OK):**
```json
{
  "urls": {
    "download": "https://api.example.com/calendar/event/123.ics",
    "google": "https://calendar.google.com/calendar/render?action=TEMPLATE&text=Event...",
    "outlook": "https://outlook.live.com/calendar/0/deeplink/compose?subject=Event..."
  },
  "event": {
    "name": "Tech Conference 2024",
    "startDateTime": "2024-03-15T09:00:00",
    "endDateTime": "2024-03-15T17:00:00",
    "location": "Convention Center, 123 Main St, New York, NY 10001, USA",
    "description": "Annual technology conference"
  }
}
```

### Download User Events Calendar
```http
GET /api/calendar/user/{userId}/events.ics
Authorization: Bearer <jwt-token>
```

### Download User Registrations Calendar
```http
GET /api/calendar/user/{userId}/registrations.ics
Authorization: Bearer <jwt-token>
```

### Get Calendar Subscription
```http
GET /api/calendar/user/{userId}/subscription
Authorization: Bearer <jwt-token>
```

### Create Calendar Subscription
```http
POST /api/calendar/user/{userId}/subscription
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
{
  "url": "https://api.example.com/calendar/feed/abc123token.ics",
  "token": "abc123token",
  "expiresAt": "2025-01-15T10:30:00",
  "createdAt": "2024-01-15T10:30:00"
}
```

### Get Calendar Feed (Public)
```http
GET /api/calendar/feed/{token}.ics
```

---

## Webhook Management

### Create Webhook
```http
POST /api/webhooks
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "name": "Registration Webhook",
  "url": "https://your-app.com/webhooks/events",
  "eventTypes": ["USER_REGISTERED", "EVENT_CREATED", "REGISTRATION_CANCELLED"],
  "maxRetries": 3,
  "timeoutSeconds": 30
}
```

**Response (201 Created):**
```json
{
  "id": "webhook-uuid-123",
  "name": "Registration Webhook",
  "url": "https://your-app.com/webhooks/events",
  "status": "ACTIVE",
  "eventTypes": ["USER_REGISTERED", "EVENT_CREATED", "REGISTRATION_CANCELLED"],
  "maxRetries": 3,
  "timeoutSeconds": 30,
  "totalDeliveries": 0,
  "successfulDeliveries": 0,
  "failedDeliveries": 0,
  "successRate": 0.0,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00",
  "lastDeliveryAt": null,
  "lastSuccessAt": null
}
```

### Get All Webhooks
```http
GET /api/webhooks
Authorization: Bearer <jwt-token>
```

### Get Webhook by ID
```http
GET /api/webhooks/{webhookId}
Authorization: Bearer <jwt-token>
```

### Update Webhook
```http
PUT /api/webhooks/{webhookId}
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "name": "Updated Registration Webhook",
  "url": "https://your-app.com/webhooks/events/v2",
  "eventTypes": ["USER_REGISTERED", "EVENT_CREATED"],
  "status": "ACTIVE",
  "maxRetries": 5,
  "timeoutSeconds": 45
}
```

### Delete Webhook
```http
DELETE /api/webhooks/{webhookId}
Authorization: Bearer <jwt-token>
```

### Activate Webhook
```http
POST /api/webhooks/{webhookId}/activate
Authorization: Bearer <jwt-token>
```

### Deactivate Webhook
```http
POST /api/webhooks/{webhookId}/deactivate
Authorization: Bearer <jwt-token>
```

### Regenerate Webhook Secret
```http
POST /api/webhooks/{webhookId}/regenerate-secret
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
{
  "secret": "new-webhook-secret-key"
}
```

### Get Webhook Deliveries
```http
GET /api/webhooks/{webhookId}/deliveries
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
[
  {
    "id": "delivery-uuid-1",
    "webhookId": "webhook-uuid-123",
    "eventType": "USER_REGISTERED",
    "status": "SUCCESS",
    "attemptCount": 1,
    "maxAttempts": 3,
    "responseStatus": 200,
    "errorMessage": null,
    "createdAt": "2024-01-15T11:00:00",
    "deliveredAt": "2024-01-15T11:00:01",
    "nextRetryAt": null
  }
]
```

### Retry Webhook Delivery
```http
POST /api/webhooks/{webhookId}/deliveries/{deliveryId}/retry
Authorization: Bearer <jwt-token>
```

### Test Webhook
```http
POST /api/webhooks/{webhookId}/test
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "eventType": "USER_REGISTERED",
  "testData": {
    "userId": "test-user-123",
    "eventId": "test-event-456",
    "customField": "test-value"
  }
}
```

### Retry Failed Deliveries (All)
```http
POST /api/webhooks/retry-failed
Authorization: Bearer <jwt-token>
```

---

## Resource Management

### Get All Resources
```http
GET /api/simple-resources
Authorization: Bearer <jwt-token>
```

### Get Resource by ID
```http
GET /api/simple-resources/{resourceId}
Authorization: Bearer <jwt-token>
```

### Create Resource
```http
POST /api/simple-resources
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "name": "Conference Room A",
  "type": "ROOM",
  "description": "Large conference room with AV equipment",
  "capacity": 50,
  "location": "Building 1, Floor 2",
  "amenities": ["projector", "whiteboard", "wifi"],
  "hourlyRate": 75.00,
  "bookable": true,
  "requiresApproval": false
}
```

### Update Resource
```http
PUT /api/simple-resources/{resourceId}
Authorization: Bearer <jwt-token>
Content-Type: application/json
```
(Same body structure as create)

### Delete Resource
```http
DELETE /api/simple-resources/{resourceId}
Authorization: Bearer <jwt-token>
```

### Get Resources by Event
```http
GET /api/simple-resources/event/{eventId}
Authorization: Bearer <jwt-token>
```

### Get Available Resources
```http
GET /api/simple-resources/available?startDate=2024-03-15&endDate=2024-03-16
Authorization: Bearer <jwt-token>
```

---

## WebSocket Real-time Features

### WebSocket Connection
```javascript
// Connect to WebSocket
const socket = new WebSocket('wss://your-domain.com/ws');

// Subscribe to event updates
const subscribeMessage = {
  type: 'SUBSCRIBE',
  eventId: '123e4567-e89b-12d3-a456-426614174000'
};
socket.send(JSON.stringify(subscribeMessage));
```

### WebSocket Message Mappings

#### Subscribe to Event Updates
```javascript
// Send to: /app/events/{eventId}/subscribe
{
  "type": "SUBSCRIPTION_REQUEST",
  "eventId": "123e4567-e89b-12d3-a456-426614174000"
}

// Receive on: /topic/events/{eventId}/updates
{
  "type": "SUBSCRIPTION_CONFIRMED",
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "message": "Successfully subscribed to event updates",
  "timestamp": 1642248600000
}
```

#### Subscribe to All Events
```javascript
// Send to: /app/events/subscribe-all
{
  "type": "GLOBAL_SUBSCRIPTION_REQUEST"
}

// Receive on: /topic/events/updates
{
  "type": "GLOBAL_SUBSCRIPTION_CONFIRMED",
  "message": "Successfully subscribed to all event updates",
  "timestamp": 1642248600000
}
```

#### Ping/Pong for Connection Health
```javascript
// Send to: /app/ping
{
  "type": "PING",
  "timestamp": 1642248600000
}

// Receive on: /topic/system/ping
{
  "type": "PONG",
  "timestamp": 1642248600001,
  "originalMessage": {...}
}
```

### Real-time Event Broadcasts

#### Registration Updates
```javascript
// Broadcast format for new registrations
{
  "type": "REGISTRATION_UPDATE",
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "action": "NEW", // "NEW", "CANCELLED", "UPDATED"
  "data": {
    "registrationId": "789e0123-e89b-12d3-a456-426614174002",
    "userName": "Jane Doe",
    "userEmail": "jane@example.com",
    "timestamp": "2024-01-15T14:30:00"
  }
}
```

#### Check-in Updates
```javascript
// Broadcast format for check-ins
{
  "type": "CHECKIN_UPDATE",
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "action": "CHECKED_IN",
  "data": {
    "registrationId": "789e0123-e89b-12d3-a456-426614174002",
    "userName": "Jane Doe",
    "userEmail": "jane@example.com",
    "checkedInAt": "2024-03-15T09:15:00",
    "method": "QR_CODE",
    "location": "Main Entrance"
  }
}
```

---

## Data Models

### Event Status Enum
- `DRAFT`: Event is being created/edited
- `PUBLISHED`: Event is live and accepting registrations
- `CANCELLED`: Event has been cancelled
- `COMPLETED`: Event has finished

### Event Type Enum
- `CONFERENCE`: Multi-day professional conference
- `WORKSHOP`: Hands-on learning session
- `SEMINAR`: Educational presentation
- `NETWORKING`: Social/networking event
- `WEBINAR`: Online presentation
- `MEETING`: Business meeting
- `OTHER`: Other event types

### Event Category Enum
- `BUSINESS`: Business/professional events
- `TECHNOLOGY`: Technology-focused events
- `EDUCATION`: Educational events
- `HEALTHCARE`: Healthcare industry events
- `FINANCE`: Financial industry events
- `MARKETING`: Marketing/advertising events
- `OTHER`: Other categories

### Registration Status Enum
- `REGISTERED`: Successfully registered
- `WAITLISTED`: On waiting list
- `CANCELLED`: Registration cancelled
- `CHECKED_IN`: Attended and checked in

### Check-in Type Enum
- `EVENT`: General event check-in
- `SESSION`: Specific session check-in

### Check-in Method Enum
- `QR_CODE`: QR code scan
- `MANUAL`: Manual check-in by staff
- `SELF_SERVICE`: Self-service kiosk
- `MOBILE_APP`: Mobile app check-in

### User Role Enum
- `USER`: Regular user
- `ADMIN`: Administrator
- `ORGANIZER`: Event organizer
- `STAFF`: Event staff member

### User Status Enum
- `ACTIVE`: Active user account
- `INACTIVE`: Inactive account
- `SUSPENDED`: Suspended account
- `PENDING_VERIFICATION`: Awaiting email verification

---

## Error Handling

### Standard Error Response Format
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/events",
  "requestId": "req-12345-67890",
  "errors": [
    {
      "field": "name",
      "rejectedValue": "",
      "message": "Event name is required"
    },
    {
      "field": "startDateTime",
      "rejectedValue": null,
      "message": "Start date and time are required"
    }
  ]
}
```

### Common HTTP Status Codes

#### 200 OK
Successful GET, PUT requests

#### 201 Created
Successful POST requests that create resources

#### 204 No Content
Successful DELETE requests

#### 400 Bad Request
- Invalid request data
- Validation failures
- Missing required fields

#### 401 Unauthorized
- Missing JWT token
- Invalid JWT token
- Expired JWT token

#### 403 Forbidden
- Insufficient permissions
- Access denied to resource

#### 404 Not Found
- Resource not found
- Invalid endpoint

#### 409 Conflict
- Resource already exists
- Business rule violations (e.g., duplicate registration)

#### 422 Unprocessable Entity
- Business validation failures
- Invalid state transitions

#### 429 Too Many Requests
- Rate limiting exceeded

#### 500 Internal Server Error
- Unexpected server errors
- Database connection issues
- External service failures

### Validation Error Examples

#### Registration Validation
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid registration data",
  "path": "/api/registrations",
  "errors": [
    {
      "field": "userEmail",
      "rejectedValue": "invalid-email",
      "message": "Please provide a valid email address"
    },
    {
      "field": "eventInstanceId",
      "rejectedValue": null,
      "message": "Event instance ID is required"
    }
  ]
}
```

#### Authentication Error
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid credentials",
  "path": "/api/auth/login"
}
```

#### Business Rule Violation
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "User is already registered for this event",
  "path": "/api/registrations"
}
```

---

## Rate Limiting

### Rate Limits by Endpoint Type

#### Authentication Endpoints
- **Login**: 5 requests per minute per IP
- **Registration**: 3 requests per minute per IP
- **Password Reset**: 2 requests per minute per IP

#### General API Endpoints
- **Authenticated Users**: 1000 requests per hour
- **Public Endpoints**: 100 requests per hour per IP

#### File Upload Endpoints
- **Image Uploads**: 20 requests per hour per user
- **File Size Limit**: 10MB per file

#### WebSocket Connections
- **Max Connections**: 5 per user
- **Message Rate**: 100 messages per minute per connection

### Rate Limit Headers
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 995
X-RateLimit-Reset: 1642252200
X-RateLimit-Retry-After: 3600
```

### Rate Limit Exceeded Response
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later.",
  "path": "/api/events",
  "retryAfter": 3600
}
```

---

## Security Considerations

### HTTPS Enforcement
All API endpoints require HTTPS in production. HTTP requests are automatically redirected to HTTPS.

### JWT Token Security
- Tokens expire after 24 hours (configurable)
- Include standard JWT claims (iss, sub, exp, iat)
- Use HS256 algorithm for signing
- Store securely on client side (httpOnly cookies recommended)

### CORS Configuration
```javascript
// Allowed origins (configured per environment)
https://yourdomain.com
https://app.yourdomain.com

// Allowed methods
GET, POST, PUT, DELETE, OPTIONS

// Allowed headers
Authorization, Content-Type, X-Requested-With
```

### Security Headers
- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains; preload`
- `Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';`

### Input Validation
- All input is validated using Bean Validation annotations
- SQL injection prevention through parameterized queries
- XSS prevention through output encoding
- File upload validation (type, size, content)

---

## Development Environment Setup

### Base URLs by Environment
- **Development**: `http://localhost:8080/api`
- **Staging**: `https://staging-api.yourdomain.com/api`
- **Production**: `https://api.yourdomain.com/api`

### Authentication for Testing
```bash
# Get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123"
  }'

# Use token in requests
curl -X GET http://localhost:8080/api/events \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Health Check Endpoint
```http
GET /actuator/health

Response:
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 91943833600,
        "threshold": 10485760
      }
    }
  }
}
```

---

## Support and Troubleshooting

### Common Issues

#### 401 Unauthorized
- Check JWT token is included in Authorization header
- Verify token format: `Bearer <token>`
- Check token expiration
- Verify user account is active

#### 404 Not Found
- Verify endpoint URL is correct
- Check resource ID format (must be valid UUID)
- Ensure resource exists and user has access

#### 400 Bad Request with Validation Errors
- Check all required fields are provided
- Verify data types and formats
- Check field length constraints
- Validate enum values

#### 429 Rate Limit Exceeded
- Reduce request frequency
- Check rate limit headers for retry time
- Consider request batching where available

### Debug Headers
Include these headers for additional debugging information:

```http
X-Debug-Enabled: true
X-Trace-Request: true
```

### Support Contact
- **Documentation**: https://docs.eventr.com
- **API Status**: https://status.eventr.com  
- **Support Email**: api-support@eventr.com
- **Developer Forum**: https://community.eventr.com

---

*Last Updated: March 2024*  
*API Version: 1.0*  
*Documentation Version: 1.2*