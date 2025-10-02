# Find My Device - API Documentation

## Base URL
```
https://api.findmydevice.demoody.com/api
```

## Authentication
Most endpoints require device authentication using Bearer tokens in the Authorization header:
```
Authorization: Bearer <device_token>
```

## Endpoints

### Device Registration

#### Register Device
Register a new device or update existing device information.

**Endpoint:** `POST /register_device`

**Request Body:**
```json
{
    "device_id": "unique-device-identifier",
    "device_name": "My Phone",
    "auth_token": "generated-auth-token",
    "model": "Samsung Galaxy S21",
    "os_version": "Android 12",
    "fcm_token": "firebase-cloud-messaging-token"
}
```

**Response:**
```json
{
    "status": "ok",
    "device_token": "server-generated-device-token"
}
```

### Location Management

#### Upload Location
Upload current device location to the server.

**Endpoint:** `POST /devices/{device_id}/location`

**Headers:**
```
Authorization: Bearer <device_token>
```

**Request Body:**
```json
{
    "lat": 40.7128,
    "lng": -74.0060,
    "accuracy": 12.5,
    "timestamp": 1690000000,
    "provider": "gps"
}
```

**Response:**
```json
{
    "status": "ok"
}
```

### Command Management

#### Poll for Commands
Check for pending remote commands for the device.

**Endpoint:** `GET /devices/{device_id}/commands/poll?since_id=123`

**Headers:**
```
Authorization: Bearer <device_token>
```

**Query Parameters:**
- `since_id` (optional): Only return commands with ID greater than this value

**Response:**
```json
{
    "status": "ok",
    "data": [
        {
            "id": 124,
            "type": "LOCK",
            "payload": {},
            "created_at": "2023-07-20 10:30:00"
        },
        {
            "id": 125,
            "type": "SHOW_MESSAGE",
            "payload": {
                "message": "This device is being tracked"
            },
            "created_at": "2023-07-20 10:35:00"
        }
    ]
}
```

#### Acknowledge Command
Acknowledge that a command has been received and executed.

**Endpoint:** `POST /devices/{device_id}/commands/{command_id}/ack`

**Headers:**
```
Authorization: Bearer <device_token>
```

**Request Body:**
```json
{
    "status": "executed",
    "message": "Command completed successfully",
    "executed_at": 1690000000
}
```

**Response:**
```json
{
    "status": "ok"
}
```

#### Create Command (Dashboard)
Create a new remote command for a device (typically called from web dashboard).

**Endpoint:** `POST /devices/{device_id}/commands`

**Request Body:**
```json
{
    "type": "LOCK",
    "payload": {}
}
```

**Response:**
```json
{
    "status": "ok",
    "command_id": 126
}
```

### SIM Change Reporting

#### Report SIM Change
Report when a SIM card change is detected.

**Endpoint:** `POST /devices/{device_id}/sim_change`

**Headers:**
```
Authorization: Bearer <device_token>
```

**Request Body:**
```json
{
    "old_sim": {
        "serial": "old-sim-serial",
        "operator": "Old Carrier",
        "number": "+1234567890"
    },
    "new_sim": {
        "serial": "new-sim-serial",
        "operator": "New Carrier",
        "number": "+0987654321"
    },
    "location": {
        "lat": 40.7128,
        "lng": -74.0060,
        "accuracy": 15.0,
        "timestamp": 1690000000,
        "provider": "network"
    }
}
```

**Response:**
```json
{
    "status": "ok"
}
```

### Device Status

#### Get Device Status
Retrieve current status and information about a device.

**Endpoint:** `GET /devices/{device_id}/status`

**Headers:**
```
Authorization: Bearer <device_token>
```

**Response:**
```json
{
    "status": "ok",
    "data": {
        "device_name": "My Phone",
        "model": "Samsung Galaxy S21",
        "os_version": "Android 12",
        "is_active": true,
        "last_seen": "2023-07-20 10:45:00",
        "last_location": {
            "lat": 40.7128,
            "lng": -74.0060,
            "accuracy": 12.5,
            "provider": "gps",
            "recorded_at": "2023-07-20 10:40:00"
        }
    }
}
```

## Command Types

### LOCK
Lock the device immediately using Device Administrator privileges.

**Payload:** Empty object `{}`

### ALARM
Play a loud alarm sound on the device.

**Payload:** 
```json
{
    "duration": 120  // Optional: duration in seconds (default: 120)
}
```

### GET_LOCATION
Request immediate location update from the device.

**Payload:** Empty object `{}`

### SHOW_MESSAGE
Display a message on the device screen.

**Payload:**
```json
{
    "message": "This device is being tracked for security purposes"
}
```

### WIPE
Completely wipe the device (requires Device Administrator privileges).

**Payload:** 
```json
{
    "confirmation": "WIPE_CONFIRMED"  // Required for safety
}
```

## Error Responses

All error responses follow this format:
```json
{
    "status": "error",
    "message": "Description of the error"
}
```

### Common HTTP Status Codes
- `200 OK`: Request successful
- `400 Bad Request`: Invalid request data
- `401 Unauthorized`: Invalid or missing authentication
- `404 Not Found`: Resource not found
- `405 Method Not Allowed`: HTTP method not supported
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Server error

## Rate Limiting

API endpoints are rate-limited to prevent abuse:
- **Default limit**: 60 requests per hour per device
- **Location uploads**: 120 requests per hour per device
- **Command polling**: 240 requests per hour per device

When rate limit is exceeded, the API returns HTTP 429 with:
```json
{
    "status": "error",
    "message": "Rate limit exceeded"
}
```

## Data Retention

- **Locations**: Stored for 30 days, then automatically deleted
- **Commands**: Executed commands stored for 7 days, then deleted
- **Logs**: Stored for 30 days, then automatically deleted
- **Device information**: Retained while device is active

## Security Considerations

### HTTPS Only
All API communication must use HTTPS in production.

### Token Security
- Device tokens should be stored securely on the device
- Tokens should be rotated periodically
- Never expose tokens in logs or error messages

### Input Validation
- All inputs are validated server-side
- Coordinates are validated for reasonable ranges
- Command payloads are sanitized

### Privacy
- Location data is encrypted at rest
- Access logs are maintained for security auditing
- Data can be deleted upon user request

## SDK and Libraries

### Android
The Android app includes built-in API client classes:
- `ApiClient`: Main API client with authentication
- `ApiService`: Retrofit interface for all endpoints
- Model classes for request/response objects

### JavaScript
The web dashboard includes:
- `dashboard.js`: Complete API integration
- Error handling and retry logic
- Real-time updates via polling

## Testing

### Test Endpoints
A test environment is available at:
```
https://test-api.findmydevice.demoody.com/api
```

### Postman Collection
A Postman collection with all endpoints and example requests is available in the `docs/` folder.

### cURL Examples

**Register Device:**
```bash
curl -X POST https://api.findmydevice.demoody.com/api/register_device \
  -H "Content-Type: application/json" \
  -d '{
    "device_id": "test-device-123",
    "device_name": "Test Phone",
    "auth_token": "test-token-456"
  }'
```

**Upload Location:**
```bash
curl -X POST https://api.findmydevice.demoody.com/api/devices/test-device-123/location \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer device-token-789" \
  -d '{
    "lat": 40.7128,
    "lng": -74.0060,
    "accuracy": 12.5,
    "timestamp": 1690000000
  }'
```

## Support

For API support:
- Check the status page: https://status.findmydevice.demoody.com
- Review error logs in your application
- Contact support: api-support@demoody.com

---

**Version**: 1.0  
**Last Updated**: 02 October 2025
