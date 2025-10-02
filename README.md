# Find My Device - by Demoody

**Version:** 0.1 - (Draft)

---

## Table of contents
1. [Project Overview](#project-overview)
2. [Goals & Vision](#goals--vision)
3. [Key Features (MVP & Future)](#key-features-mvp--future)
4. [High-level Architecture](#high-level-architecture)
5. [Tech Stack](#tech-stack)
6. [Security, Privacy & Play Store Considerations](#security-privacy--play-store-considerations)
7. [Limitations & Root-only Capabilities](#limitations--root-only-capabilities)
8. [System Requirements](#system-requirements)
9. [Project Structure (recommended)](#project-structure-recommended)
10. [API Specification (MVP)](#api-specification-mvp)
11. [Database Schema (MVP)](#database-schema-mvp)
12. [Android App - Setup & Implementation Notes](#android-app---setup--implementation-notes)
13. [Web Dashboard - Setup & Notes](#web-dashboard---setup--notes)
14. [SMS Command Handling](#sms-command-handling)
15. [Testing Strategy](#testing-strategy)
16. [CI / CD](#ci--cd)
17. [Roadmap & Milestones](#roadmap--milestones)
18. [Contribution Guide](#contribution-guide)
19. [License & Contact](#license--contact)

---

# Project Overview
**Find  MyDevice** by **Demoody** is a cross-device anti-theft & tracking solution for Android devices. The product bundles:

- A **mobile app** (Android, Java) that runs in foreground/foreground-service and periodically uploads device location to a secure backend.
- A **backend (PHP + MySQL)** that stores device metadata, location history and executes remote commands.
- A **web dashboard** for the user to track devices on a map and execute remote operations (lock, alarm, get location, wipe).

The Android app also implements anti-theft techniques (SIM-change notification, device admin for remote lock/wipe, fake shutdown overlay for theft deterrence) while respecting Android platform constraints on non-rooted devices.


# Goals & Vision
- Provide a reliable "Find My Device" experience for Android devices worldwide.
- Offer robust anti-theft features compatible with non-rooted devices (with optional full-protection for rooted devices).
- Make the product safe, privacy-conscious and compliant with Play Store policies.


# Key Features (MVP & Future)
**MVP** (what we'll implement first):
- Device registration & authentication
- Periodic location upload (GPS + network) via foreground service / WorkManager
- Web dashboard showing latest location on a map (Google Maps or Leaflet)
- Remote commands: Get Location, Lock Device (DeviceAdmin), Ring Alarm, Show Message
- SIM change detection (notify server with new SIM info + location)
- Detect screen-off / power press and present a *fake shutdown overlay* (deterrent) with password prompt
- SMS-based retrieval command (optional backup if internet is unavailable)

**v1+ (future / optional):**
- True Power-Off prevention (rooted devices / custom ROM) via Magisk/Xposed module or system integration
- Encrypted location storage and end-to-end encrypted device-server channel
- Live streaming of device location for a short time window
- Geofencing and automated alerts
- Multi-account family/shared devices and subscription features


# High-level Architecture

1. **Android app** (Java): collects location, monitors SIM/boot/power events, provides UI for device management, communicates securely with backend.
2. **Backend (PHP)**: REST API endpoints for device registration, location ingestion, command queue, and logs. MySQL for data storage.
3. **Web Dashboard** (React / plain PHP + Bootstrap): user login, device list, map view, command controls.
4. **Push system**: Firebase Cloud Messaging (FCM) to send urgent remote commands and wake the device.


# Tech Stack
- Mobile: Android (Java), WorkManager, Foreground Service, DeviceAdminReceiver
- Server: PHP 8.x (Laravel or plain slim PHP), MySQL / MariaDB
- Web Dashboard: React or PHP + Bootstrap (MVP: Bootstrap + jQuery is fine)
- Maps: Google Maps JavaScript API (or Leaflet + OpenStreetMap to avoid key costs)
- Push: Firebase Cloud Messaging (FCM)
- Optional Root Features: Magisk module / Xposed hooks (for power manager interception)


# Security, Privacy & Play Store Considerations
This app will request sensitive permissions (location, background location, READ_PHONE_STATE, DEVICE_ADMIN). Follow these rules:

- **Least privilege**: request only what’s necessary and explain to the user *why* on the permission screen.
- **Privacy policy**: publish a public privacy policy and link it in Play Store listing and in the app.
- **Background location**: clearly explain usage; follow Play Store policy and provide in-app education screens before requesting `ACCESS_BACKGROUND_LOCATION`.
- **Device admin**: ask user to grant Device Administrator privileges for lock/wipe features. Provide clear in-app flow about how to remove it.
- **Play Store restrictions**: you cannot block shutdown on non-root devices - avoid claiming you can fully prevent power-off in Store copy. For root-only features, declare that explicitly and gate the feature.
- **Data handling**: use HTTPS (TLS 1.2+), encrypt sensitive fields at rest if possible, and rotate keys.
- **Abuse prevention**: implement server-side checks to prevent unauthorized remote commands (2FA, session expiry, device tokens), rate limiting and logging.


# Limitations & Root-only Capabilities
**Non-root devices:**
- You CANNOT reliably intercept the system shutdown path or prevent power-off initiated by the hardware long-press across all devices.
- Workaround: intercept screen-off / short-press events and show a fake shutdown overlay immediately; the real shutdown will still proceed on determined attackers, but the overlay deters casual thieves.

**Rooted devices / custom ROM:**
- By modifying framework services (e.g., `PowerManagerService`) or creating a Magisk module / Xposed module you can hook and block the shutdown call and enforce a password prompt. This requires root and is out-of-scope for Play Store distribution.

Document these limitations clearly in the user-facing docs.


# System Requirements
- Android app: Min SDK `23` (Android 6.0) recommended, Target SDK `34` (Android 14) for modern APIs.
- Server: PHP 8.x, MySQL 5.7+/8, Nginx/Apache
- FCM: Firebase account + Android app registration
- Maps: Google Maps API key (or OpenStreetMap + Leaflet)


# Project Structure (recommended)
```
/findmydevice/
  /android-app/          # Android Studio project (Java)
  /backend/              # PHP REST API (Laravel or Slim)
  /web-dashboard/        # React or PHP + Bootstrap
  /docs/                 # architecture diagrams, API docs (OpenAPI), README
  /infra/                # terraform / docker-compose for local dev
  /scripts/              # helper scripts
```


# API Specification (MVP)
All calls over HTTPS. Use JWT or device tokens for device authentication.

**Auth & Registration**
- `POST /api/register_device`
  - Body: `{ "device_id": "<uuid>", "device_name": "My Phone", "auth_token": "<generated>" }`
  - Response: `{ "status": "ok", "device_token": "<token>" }`

**Location ingestion**
- `POST /api/devices/{device_id}/location`
  - Headers: `Authorization: Bearer <device_token>`
  - Body: `{ "lat": 12.345, "lng": 67.890, "accuracy": 12.5, "timestamp": 1690000000 }`
  - Response: `{ "status": "ok" }`

**Command queue (user -> device)**
- `POST /api/devices/{device_id}/commands`
  - Body: `{ "type": "LOCK|ALARM|GET_LOCATION|SHOW_MESSAGE|WIPE", "payload": { ... } }`
  - Response: `{ "status":"queued", "command_id": 123 }

- `GET /api/devices/{device_id}/commands/poll?since_id=123`
  - Device polls for pending commands.

**SIM-Chang Notification**
- `POST /api/devices/{device_id}/sim_change`
  - Body: `{ "old_sim": { ... }, "new_sim": { "number": "+91xxxx", "operator": "Vodafone" }, "location": {lat,lng}}`

**Device Status**
- `GET /api/devices/{device_id}/status`
  - Response includes last_seen, last_location, battery, sim_info


# Database Schema (MVP)

**users**
- id, name, email, password_hash, created_at, updated_at

**devices**
- id (uuid), user_id, device_name, device_token, imei, model, os_version, is_active, last_seen, created_at

**locations**
- id, device_id, lat, lng, accuracy, provider, recorded_at

**commands**
- id, device_id, type, payload(json), status(pending/acknowledged/executed/failed), created_at, executed_at

**logs**
- id, device_id, event_type, message, event_at


# Android App - Setup & Implementation Notes

## 1. Project Setup
- Language: Java
- Use AndroidX libraries
- gradle config:
  - `minSdkVersion 23`
  - `targetSdkVersion 34`
- Key dependencies:
  - WorkManager (`androidx.work:work-runtime`)
  - Firebase messaging (`com.google.firebase:firebase-messaging`)
  - Retrofit / OkHttp for REST
  - Room (optional) for local data caching

## 2. Permissions (Runtime)
- `ACCESS_FINE_LOCATION` (foreground)
- `ACCESS_COARSE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION` (explain to user before requesting)
- `FOREGROUND_SERVICE` (for persistent tracking)
- `RECEIVE_BOOT_COMPLETED` (start service after boot)
- `READ_PHONE_STATE` (for IMEI/sim info) - note: IMEI access is restricted on newer APIs; prefer `Settings.Secure.ANDROID_ID` and TelephonyManager methods where permitted.
- `USE_FULL_SCREEN_INTENT` or SYSTEM_ALERT_WINDOW` (avoid SYSTEM_ALERT_WINDOW unless absolutely necessary - it can trigger Play Store restrictions)

**Important**: Always request runtime permissions with an explanatory screen.

## 3. DeviceAdminReceiver
- Implement a `DeviceAdminReceiver` subclass to support `lockNow()` and `resetPassword()` operations.
- Request user activation via `DevicePolicyManager` intent.

## 4. Foreground Service & WorkManager
- Use a foreground service for continuous tracking when user explicitly enables it. Use WorkManager for periodic background location uploads (deferrable).
- When starting the foreground service, show a persistent notification explaining tracking is active.

## 5. Location Strategy
- Use fused location (if using Google Play services) or `LocationManager` fallback.
- Send location updates to `/api/devices/{id}/location` with timestamp and accuracy.
- Use exponential backoff when network not available. Persist unsent locations locally.

## 6. SIM Change Detection
- On boot and on `READ_PHONE_STATE` changes, compare saved SIM serial or number with current one. If changed, send `sim_change` to server.

## 7. Fake Shutdown Overlay
- Listen for `Intent.ACTION_SCREEN_OFF` and `ACTION_SHUTDOWN` (note: ACTION_SHUTDOWN is broadcast to receivers, but cannot be aborted on non-root devices).
- Immediately launch a fullscreen `Activity` with the appearance of a shutdown screen.
- Show password prompt; if correct, call `finishAffinity()` (or a disguised flow). This does not really prevent a hardware shutdown; it's a deterrent for casual theft.
- Carefully handle `onPause`/`onStop` to try to remain visible (but don't abuse system APIs; follow Play Store rules).

## 8. Polling Commands / Push
- Prefer push (FCM) for remote commands. If FCM not available, fallback to polling endpoint every X minutes.
- When receiving a command, execute securely and return status via `POST /api/devices/{id}/commands/{cmd}/ack`.

## 9. Secure Storage
- Store tokens in `EncryptedSharedPreferences` (AndroidX Security) or Android KeyStore.


# Web Dashboard - Setup & Notes
- MVP: simple Bootstrap + jQuery interface to list devices and show a map.
- Actions: view last location, request immediate location, ring device, lock device, show message, wipe device.
- Authentication: session + 2FA option.
- Map: use Google Maps JS API or Leaflet + OSM. Show markers, last-known timestamp, accuracy circle.
- Server-side: REST endpoints, admin APIs for debugging.


# SMS Command Handling
- Allow a secure SMS command interface as a backup (e.g., send SMS with a one-time code). Be careful - SMS commands are easily spoofed. Protect with:
  - Pre-shared secret token in the SMS body.
  - Check incoming message sender number (whitelist) if possible.
  - Rate-limit SMS triggers.

Sample SMS format:
```
FIND <device-id> <secret-token>
LOCK <device-id> <secret-token>
```

The app listens for incoming SMS (SMS_RECEIVED), verifies the token, and executes the command.


# Testing Strategy
- **Unit tests**: business logic (command parsing, payload validation)
- **Android instrumentation tests**: UI flows, service lifecycle tests
- **Integration tests**: REST endpoints using local dev environment (Docker Compose)
- **Manual QA**: permission flows, boot & SIM change, denied permissions, offline scenarios


# CI / CD
- Use GitHub Actions:
  - Build Android artifact on merge to `main` and run lint/tests
  - Build Docker image for backend and run automated tests
  - On `release` tag, upload APK to internal test track (via Google Play API) and deploy backend to staging/production servers


# Roadmap & Milestones
**MVP (2–4 weeks)**
- README + project skeletons
- Backend basic API + MySQL
- Android app: registration, location upload, DeviceAdmin lock, basic dashboard
- Web dashboard: device list + map

**v1.0 (1–2 months)**
- Robust background location and offline queueing
- SIM-change & boot handling tested across devices
- Push notifications and command queue reliability
- Basic account & security hardening

**v2.0+**
- Root-only shutdown prevention module (Magisk/Xposed)
- E2E encryption, subscription management, multi-device accounts
- Mobile apps for iOS (optional) or additional management features


# Contribution Guide
- Fork repository
- Create branch: `feature/xxx` or `bugfix/yyy`
- Follow code style (Java: Android style guide)
- Create PR with description and linked issue
- CI must pass before merge


# License & Contact
- MIT License — you may use and modify the code for your organization. Include attribution if you redistribute.
- For commercial / closed-source product consider proprietary license.

**Contact**: Project owner / maintainer - wasim@demoody.com

---
## Author
**Develope By** - [Sk Wasim Akram](https://github.com/skwasimakram13)

- 👨‍💻 All of my projects are available at [https://skwasimakram.com](https://skwasimakram.com)

- 📝 I regularly write articles on [https://blog.skwasimakram.com](https://blog.skwasimakram.com)

- 📫 How to reach me **hello@skwasimakram.com**

- 🧑‍💻 Google Developer Profile [https://g.dev/skwasimakram](https://g.dev/skwasimakram)

- 📲 LinkedIn [https://www.linkedin.com/in/sk-wasim-akram](https://www.linkedin.com/in/sk-wasim-akram)

---

💡 *Built with ❤️ and creativity by Wassu.*

---

*This README is a product-level blueprint. It contains technical and legal notes and must be kept up-to-date as code and external policies evolve.*


<!-- End of README -->
