# TripShot - Social Travel Photo Sharing App

A Kotlin-based Android social network application where users can create collaborative travel plans, share photos at real-time locations, and relive trip memories together.

## 📱 What is TripShot?

**TripShot** is a location-aware social platform designed for travel enthusiasts to share their journeys in real-time. Users create trips with friends, receive periodic notifications to post photos at specific locations, and view the entire trip experience on interactive maps. After a trip concludes, shared memories are permanently displayed on user profiles and in a curated "Shared Trips" discovery section.

### The Problem It Solves
Traditional photo sharing apps lack structure for coordinated group travel. TripShot ensures:
- **Structured storytelling:** Photos are posted at regular intervals, creating a cohesive narrative
- **Location awareness:** Every photo is automatically geotagged
- **Group coordination:** All members know when to post and can see others' contributions in real-time
- **Lasting memories:** Photos are permanently archived on profiles and in the community feed

---

## 🎯 Core Features

### 1. **User Authentication**
- Login with username/email and password
- User signup with profile creation
- Secure session management

### 2. **Trip Creation & Planning**
Users specify trip duration, and the system **automatically calculates:**
- **Number of photos** to be shared (scales with trip length)
- **Posting intervals** (time between notifications)

**Example Schedule:**
```
3-day trip:   6 photos  • Every 12 hours
7-day trip:   14 photos • Every 12 hours
14-day trip:  21 photos • Every 16 hours
30-day trip:  30 photos • Every 24 hours
```

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

## 🏗️ Architecture Overview

### Navigation Structure
```
LoginActivity (Entry Point)
├── LoginContent (email/password)
└── SignupContent (name/email/password)
     └── MainActivity (Post-auth)
         ├── HomeScreen (Active trips feed)
         ├── ExploreScreen (Public/completed trips)
         ├── CreateScreen (Trip creation form)
         └── ProfileScreen (User profile & past trips)
```

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
ARCHIVED (hidden from feed, kept in history)
```

**Key Rule:** Photos can **ONLY** be added during ACTIVE status. After COMPLETED, the trip is read-only.

---

## 🛠️ Technology Stack

### Current Implementation
- **Language:** Kotlin 2.2.10
- **UI Framework:** Jetpack Compose
- **Navigation:** Jetpack Navigation Compose (2.9.7)
- **Design:** Material 3 with custom TripShot dark theme
- **Build System:** Gradle with version catalog

### Planned Components
- **Backend:** REST API or Firebase for trips/photos/users
- **Maps:** Google Maps API for location visualization
- **Location:** Android Location Services / Fused Location Provider
- **Database:** Room (local) + Firebase Firestore (cloud)
- **Push Notifications:** Firebase Cloud Messaging (FCM)
- **Image Storage:** Firebase Storage or CDN
- **Authentication:** Firebase Auth or custom JWT

---

## 🎨 Design System

### TripShot Theme
- **Primary Color:** Orange (`#FF7D00`) - vibrant, travel-inspired
- **Background:** Dark green (`#1A1C1A`) - earthy, natural
- **Surface:** Dark gray (`#333633`) - modern, clean
- **Text Primary:** Light cream (`#E1E3DF`) - high contrast
- **Text Secondary:** Muted gray (`#C1C9C0`) - secondary information
- **Navigation Accent:** Deeper orange (`#D26319`)

---

## 📱 Screen Details

### **HomeScreen**
Displays user's active trips with:
- Trip name, description, and days remaining
- Trip progress bar (elapsed time)
- Member count and photo count
- Quick action buttons to trip details

### **ExploreScreen**
Browse the community with:
- List of all public/completed trips
- Trip cards with preview info
- Filter/search functionality (future)
- Tap to view trip details and photos

### **CreateScreen**
Trip creation form including:
- Trip name (required)
- Description (optional)
- Duration in days (required)
- Member selection/invitation
- Real-time photo schedule preview

### **ProfileScreen**
User profile featuring:
- User avatar, username, bio
- Statistics (trips created, photos shared, followers)
- Completed trips gallery (grid view)
- Trip details modal with all photos
- Account settings (future)

---

## 📍 Location & Geotags

- **Automatic capture:** GPS location recorded when photo posted
- **No manual entry:** Location is seamless and automatic
- **Privacy aware:** Users know location data is tied to trips
- **Map visualization:** All photos pinned at coordinates
- **Tap for details:** Users can see exactly where each photo was taken

---

## 🔔 Notification System

- **Scheduled timing:** Notifications sent at calculated intervals
- **Real-time reminders:** "Time to share a photo!"
- **Optional captions:** Users can add text, location is auto-captured
- **Future:** Local notifications + FCM push notifications

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest)
- Android SDK 24+ (minSdk), Target 36+
- Kotlin 2.2.10+
- Java 11 compatibility

### Installation
```bash
# Clone the repository
git clone <repo-url>
cd TripShot

# Build the project
./gradlew build

# Run on emulator/device
./gradlew installDebug
adb shell am start -n com.example.tripshot/.LoginActivity
```

### Project Structure
```
app/src/
├── main/
│   ├── java/com/example/tripshot/
│   │   ├── LoginActivity.kt       # Auth entry point
│   │   ├── MainActivity.kt        # App container & navigation
│   │   ├── screens/               # 4 main screens
│   │   └── ui/theme/              # Design system
│   └── res/values/
│       └── strings.xml            # All UI text (no hardcoding)
└── test/ & androidTest/           # Unit & UI tests
```

---

## 📈 Development Roadmap

### Phase 1: Core UI (Current)
✅ Authentication (login/signup with animations)
✅ Bottom navigation with 4 screens
✅ Custom TripShot theme (dark + orange)
- Placeholder screens ready for implementation

### Phase 2: Trip Management
- Trip creation form with automatic schedule calculation
- Trip member management
- Trip list filtering and display
- Trip status transitions

### Phase 3: Photo & Location
- Camera integration
- GPS location tracking
- Interactive map with photo pins
- Photo upload system

### Phase 4: Notifications
- Local notification scheduling
- Push notification system (FCM)
- Photo reminder UI

### Phase 5: Social & Discovery
- Public trip browsing
- User profiles with trip history
- Like/comment systems
- Follow users

### Phase 6: Backend
- User authentication backend
- Trip & photo database
- Real-time updates
- Cloud image storage

---

## 🎓 Key Concepts

### Photo Schedule Calculation
The system uses smart scheduling based on trip duration:

| Duration | Photos | Interval | Notes |
|----------|--------|----------|-------|
| 1-3 days | duration × 2 | 12 hours | Frequent updates for short trips |
| 4-7 days | duration × 2 | 12 hours | Comprehensive coverage |
| 8-14 days | duration + 7 | 16 hours | More spread out |
| 15+ days | duration | 24 hours | 1 photo per day |

**Design rationale:** Shorter trips need more frequent photos to tell a complete story. Longer trips can use daily photos while still capturing all moments.

---

## 🤝 Contributing

Contributions welcome! Areas for contribution:
- Photo capture & camera integration
- Map implementation with location services
- Backend API development
- Notification system implementation
- UI/UX enhancements
- Unit and UI tests

---

## 📄 License

TripShot is open source. See LICENSE file for details.

---

## 📧 Support

For issues or questions:
- Open a GitHub issue
- Check the AGENTS.md for technical documentation
- Review the code patterns in LoginActivity.kt and MainActivity.kt

---

## 🎯 Vision Statement

TripShot transforms group travel from disconnected moments into a structured, visual narrative. By automating when photos are captured and displaying them on maps, the app ensures no memory is lost and every journey becomes a shareable story.

---

**Last Updated:** April 11, 2026  
**Current Version:** 1.0 (Development)  
**Status:** Core UI complete, ready for feature implementation

