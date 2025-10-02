# Find My Device - Setup Guide

## Prerequisites

### Android Development
- Android Studio Arctic Fox or later
- Java 8 or later
- Android SDK with API level 23+ (Android 6.0+)
- Firebase account for FCM (optional but recommended)

### Backend Development
- PHP 8.0 or later
- MySQL 5.7+ or MariaDB
- Web server (Apache/Nginx)
- Composer (optional, for dependencies)

## Android App Setup

### 1. Open the Project
1. Open Android Studio
2. Select "Open an existing Android Studio project"
3. Navigate to the `android-app` folder and select it

### 2. Configure Firebase (Optional)
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing one
3. Add an Android app to your project
4. Use package name: `com.demoody.findmydevice`
5. Download `google-services.json` and place it in `android-app/app/`
6. Follow Firebase setup instructions for FCM

### 3. Update Configuration
1. Open `android-app/app/src/main/java/com/demoody/findmydevice/utils/PreferenceManager.java`
2. Update the default server URL to match your backend deployment
3. Configure any other settings as needed

### 4. Build and Run
1. Connect an Android device or start an emulator
2. Click "Run" in Android Studio
3. Grant all requested permissions for full functionality

## Backend Setup

### 1. Database Setup
1. Create a MySQL database named `findmydevice`
2. Update database credentials in `backend/config/database.php`
3. The tables will be created automatically when you first access the API

### 2. Web Server Configuration

#### Apache
Create a `.htaccess` file in the `backend` directory:
```apache
RewriteEngine On
RewriteCond %{REQUEST_FILENAME} !-f
RewriteCond %{REQUEST_FILENAME} !-d
RewriteRule ^api/(.*)$ index.php [QSA,L]
```

#### Nginx
Add this to your server configuration:
```nginx
location /api/ {
    try_files $uri $uri/ /index.php?$query_string;
}
```

### 3. File Permissions
Ensure the web server has read/write access to:
- `backend/` directory
- System temp directory (for rate limiting)

### 4. Test the API
Visit `http://your-domain/backend/api/` in a browser. You should see a JSON error response indicating the endpoint was not found (this is expected).

## Web Dashboard Setup

### 1. Deploy Files
1. Copy the `web-dashboard` folder to your web server
2. Ensure it's accessible via HTTP/HTTPS

### 2. Configure API URL
1. Open `web-dashboard/js/dashboard.js`
2. Update the `API_BASE_URL` constant to match your backend URL

### 3. Access Dashboard
Visit the dashboard in your browser. Note that this is a basic implementation without authentication - in production, you should add proper user authentication.

## Security Considerations

### Production Deployment
1. **HTTPS**: Always use HTTPS in production
2. **Database Security**: Use strong passwords and limit database access
3. **API Authentication**: Implement proper user authentication for the web dashboard
4. **Rate Limiting**: Configure proper rate limiting for API endpoints
5. **Input Validation**: Validate all inputs on both client and server side
6. **Error Handling**: Don't expose sensitive information in error messages

### Privacy Compliance
1. Create and publish a privacy policy
2. Implement data retention policies
3. Provide user data export/deletion capabilities
4. Comply with GDPR, CCPA, and other applicable regulations

## Testing

### Android App Testing
1. Test all permission flows
2. Test location tracking in various scenarios
3. Test device admin functionality
4. Test SMS command reception
5. Test fake shutdown overlay

### Backend Testing
1. Test all API endpoints with various inputs
2. Test authentication and authorization
3. Test rate limiting
4. Test database operations
5. Load test with multiple concurrent requests

### Integration Testing
1. Test end-to-end location tracking
2. Test remote command execution
3. Test SIM change detection
4. Test offline/online scenarios

## Troubleshooting

### Common Android Issues
- **Location not updating**: Check permissions and location services
- **Device admin not working**: Ensure device admin is properly enabled
- **SMS commands not working**: Check SMS permissions and test with correct format
- **App crashes**: Check logs in Android Studio

### Common Backend Issues
- **Database connection failed**: Check credentials and database server
- **API not accessible**: Check web server configuration and .htaccess
- **Commands not executing**: Check device authentication and command format

### Common Integration Issues
- **Device not registering**: Check network connectivity and API URL
- **Location not uploading**: Check authentication tokens and API endpoints
- **Commands not received**: Check FCM configuration and device connectivity

## Performance Optimization

### Android App
- Optimize location update frequency based on battery usage
- Use WorkManager for background tasks
- Implement exponential backoff for network requests
- Cache data locally when possible

### Backend
- Add database indexes for frequently queried fields
- Implement proper caching strategies
- Use connection pooling for database connections
- Monitor and optimize slow queries

### Web Dashboard
- Implement pagination for large device lists
- Use WebSocket for real-time updates
- Optimize map rendering for many devices
- Implement client-side caching

## Monitoring and Maintenance

### Logging
- Enable comprehensive logging on all components
- Monitor error rates and response times
- Set up alerts for critical failures
- Regularly review logs for security issues

### Backup and Recovery
- Regular database backups
- Test backup restoration procedures
- Document recovery procedures
- Monitor backup integrity

### Updates and Patches
- Keep all dependencies updated
- Monitor security advisories
- Test updates in staging environment
- Plan for gradual rollouts

## Support and Documentation

For additional help:
1. Check the main README.md for project overview
2. Review the code comments for implementation details
3. Check GitHub issues for known problems
4. Contact the development team for specific questions

---

**Note**: This is a development/demo version. For production use, additional security hardening, testing, and compliance measures are required.