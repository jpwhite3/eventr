# Eventr Webhook Test Client

A development tool for receiving and debugging Eventr webhooks during local development.

## Features

- 📨 **Webhook Receiver**: Accepts webhook POST requests at `/webhook`
- 🔐 **Signature Verification**: Validates HMAC-SHA256 signatures
- 🖥️ **Web Interface**: View webhook history and details at http://localhost:3002
- 📊 **Real-time Stats**: Track webhook delivery success rates
- 🔄 **Auto-refresh**: Automatically updates webhook history every 5 seconds
- 🧹 **History Management**: Clear webhook history with one click
- 🎨 **Pretty JSON**: Syntax-highlighted webhook payloads

## Quick Start

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Set your webhook secret (optional):**
   ```bash
   export EVENTR_WEBHOOK_SECRET="your-webhook-secret"
   ```
   If not set, defaults to `dev-secret-key`

3. **Start the server:**
   ```bash
   npm start
   ```
   
   Or for development with auto-restart:
   ```bash
   npm run dev
   ```

4. **Open the web interface:**
   Visit http://localhost:3002 to view the webhook dashboard

5. **Configure Eventr webhooks:**
   Use `http://localhost:3002/webhook` as your webhook URL in Eventr

## Configuration

### Environment Variables

- `PORT`: Server port (default: 3002)
- `EVENTR_WEBHOOK_SECRET`: Secret key for webhook signature verification

### Example Webhook Configuration in Eventr

```bash
curl -X POST http://localhost:8080/api/webhooks \
  -H "Content-Type: application/json" \
  -d '{
    "url": "http://localhost:3002/webhook",
    "eventTypes": ["USER_REGISTERED", "USER_CHECKED_IN"],
    "active": true,
    "description": "Local development webhook"
  }'
```

## Web Interface

### Dashboard Features

- **Live webhook feed** with auto-refresh
- **Webhook statistics** (total, valid, invalid signatures)
- **Detailed webhook viewer** with JSON syntax highlighting
- **Signature validation status** for each webhook
- **Search and filter** capabilities
- **Export webhook data** for debugging

### Screenshots

The web interface provides:
- Real-time webhook monitoring
- Detailed payload inspection
- Signature verification status
- Error logging and debugging info

## API Endpoints

### `POST /webhook`
Main webhook endpoint that receives Eventr webhooks.

**Headers:**
- `X-Eventr-Signature`: HMAC-SHA256 signature for verification

**Response:**
```json
{
  "success": true,
  "message": "Webhook received and verified",
  "eventType": "USER_REGISTERED",
  "eventId": "event-uuid"
}
```

### `GET /health`
Health check endpoint.

**Response:**
```json
{
  "status": "healthy",
  "timestamp": "2024-08-24T10:15:30Z",
  "uptime": 1234.56,
  "webhooksReceived": 42
}
```

### `GET /api/webhooks`
Get webhook history.

**Query Parameters:**
- `limit`: Number of webhooks to return (default: 50)
- `offset`: Offset for pagination (default: 0)

**Response:**
```json
{
  "webhooks": [...],
  "total": 100,
  "limit": 50,
  "offset": 0
}
```

### `DELETE /api/webhooks`
Clear webhook history.

**Response:**
```json
{
  "success": true,
  "cleared": 42,
  "message": "Cleared 42 webhooks from history"
}
```

## Development

### Project Structure

```
webhook-client/
├── server.js          # Main server application
├── package.json       # Dependencies and scripts
├── public/
│   └── index.html    # Web interface
└── README.md         # This file
```

### Key Features Implementation

1. **Signature Verification**: Uses Node.js crypto module for HMAC-SHA256
2. **Memory Storage**: Keeps webhook history in memory (resets on restart)
3. **Real-time Updates**: Polling-based auto-refresh in the web interface
4. **Error Handling**: Comprehensive error logging and user feedback

### Example Webhook Payloads

The client can receive and display various Eventr webhook types:

#### User Registration
```json
{
  "eventType": "USER_REGISTERED",
  "eventId": "reg-event-uuid",
  "timestamp": "2024-08-24T10:15:30Z",
  "data": {
    "registrationId": "reg-uuid",
    "eventId": "event-uuid",
    "userEmail": "user@example.com",
    "userName": "John Doe"
  }
}
```

#### User Check-in
```json
{
  "eventType": "USER_CHECKED_IN",
  "eventId": "checkin-event-uuid",
  "timestamp": "2024-08-24T14:30:00Z",
  "data": {
    "checkInId": "checkin-uuid",
    "registrationId": "reg-uuid",
    "userEmail": "user@example.com",
    "checkInMethod": "QR_CODE"
  }
}
```

## Troubleshooting

### Common Issues

1. **Signature verification fails**
   - Check that `EVENTR_WEBHOOK_SECRET` matches the webhook secret in Eventr
   - Ensure the webhook payload hasn't been modified in transit

2. **Webhooks not appearing**
   - Verify the webhook URL is correct: `http://localhost:3002/webhook`
   - Check that the server is running and accessible
   - Look at server console logs for error messages

3. **Auto-refresh not working**
   - Check browser console for JavaScript errors
   - Ensure the `/api/webhooks` endpoint is accessible

### Debug Mode

Run with additional logging:
```bash
DEBUG=* npm start
```

### Testing Without Eventr

You can test the webhook client manually:

```bash
curl -X POST http://localhost:3002/webhook \
  -H "Content-Type: application/json" \
  -H "X-Eventr-Signature: sha256=$(echo -n '{"test":"data"}' | openssl dgst -sha256 -hmac 'dev-secret-key' -binary | xxd -p)" \
  -d '{"eventType":"TEST","eventId":"test-123","data":{"test":"data"}}'
```

## Production Considerations

⚠️ **This tool is for development only!**

For production webhook handling:
- Use persistent storage instead of memory
- Implement proper authentication and authorization
- Add rate limiting and request validation
- Use HTTPS endpoints
- Implement proper monitoring and alerting

## Support

For issues with the webhook test client:
- Check server console logs for errors
- Review browser console for JavaScript errors
- Ensure webhook configuration matches between Eventr and the client
- Verify network connectivity between Eventr and the webhook client