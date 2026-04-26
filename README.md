# TripShot - Social Travel Photo Sharing App

A Kotlin-based Android social network application where users can create collaborative travel plans, share photos at real-time locations, and relive trip memories together.

## What is TripShot?

**TripShot** is a location-aware social platform designed for travel enthusiasts to share their journeys in real-time. Users create trips with friends, receive periodic notifications to post photos at specific locations, and view the entire trip experience on interactive maps. After a trip concludes, shared memories are permanently displayed on user profiles and in a curated "Shared Trips" discovery section.

---

## Core Features

### 1. **User Authentication**
- Login with username/email and password
- User signup with profile creation
- Secure session management

### 2. **Trip Creation & Planning**
Users specify trip duration, and the system **automatically calculates:**
- **Number of photos** to be shared (scales with trip length)
- **Posting intervals** (time between notifications)

### 3. **Trip Collaboration**
- Add other users as trip members
- View all members and their contributions
- Trip creator manages membership

### 4. **Scheduled Photo Sharing**
- Users receive notifications: *"Time to post a photo!"*
- Capture photo with automatic GPS location recording
- Add optional captions
- Photos posted in real-time to trip map

### 5. **Interactive Trip Map**
- View all user photos pinned at GPS locations
- Zoom and pan to explore trip geography
- Tap photos to view details (user, caption, timestamp)
- Real-time photo updates as members post

### 6. **Trip History & Discovery**
- **After trip ends:** All photos appear on each participant's profile
- **Shared Trips feed:** Completed trips discoverable by all users
- **Permanent archival:** Photos remain accessible indefinitely
- **Community exploration:** Browse other users' trips

---

### User Flow

**Creating a Trip:**
1. Navigate to Create Screen
2. Enter trip name, description, duration (days)
3. System calculates photo schedule automatically
4. Add trip members
5. Trip goes LIVE with ACTIVE status

**During Active Trip:**
1. User receives notification (scheduled time)
2. Opens app and captures photo
3. App automatically records GPS location
4. User adds optional caption
5. Photo appears on trip map instantly
6. All members see the update in real-time

**After Trip Ends:**
1. Trip status changes from ACTIVE → COMPLETED
2. Photos locked (no new additions)
3. Photos appear on all participants' profiles
4. Trip appears in "Shared Trips" discovery feed
5. Community can browse and explore the trip

---

## 📊 Trip Status Lifecycle

```
ACTIVE
  ↓ (when end date reached)
COMPLETED (read-only, photos stay on profile)
  ↓ (user/admin action)
ARCHIVED (hidden from feed, kept in my trips screen)
```

**Key Rule:** Photos can **ONLY** be added during ACTIVE status. After COMPLETED, the trip is read-only.

---

### Photo Schedule Calculation
The system uses smart scheduling based on trip duration:

| Duration | Photos | Interval | Notes |
|----------|--------|----------|-------|
| 1-3 days | duration × 2 | 12 hours | Frequent updates for short trips |
| 4-7 days | duration × 2 | 12 hours | Comprehensive coverage |
| 8-14 days | duration + 7 | 16 hours | More spread out |
| 15+ days | duration | 24 hours | 1 photo per day |

## 🛠️ Technology Stack

### Current Implementation
- **Language:** Kotlin 2.2.10
- **UI Framework:** Jetpack Compose
- **Navigation:** Jetpack Navigation Compose (2.9.7)
- **Design:** Material 3 with custom TripShot dark theme
- **Build System:** Gradle with version catalog
- **Backend:** Firebase for trips/photos/users
- **Maps:** Google Maps API for location visualization
- **Location:** Android Location Services / Fused Location Provider
- **Database:** Firebase Firestore (cloud)
- **Push Notifications:** Firebase Cloud Messaging (FCM)
- **Image Storage:** Firebase Storage or CDN
- **Authentication:** Firebase Auth or custom JWT

---

