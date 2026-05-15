6ixBlock
6ixBlock is a Toronto-based Android community board for nearby posts, local events, free items, safety updates, help requests, and neighbourhood recommendations. The app is built with native Kotlin and XML, with Firebase and Room handling the live/community data layer and local caching.

The project focuses on the kind of product work a real local community app would need: location-aware browsing, fast feed updates, saved posts, comments, activity notifications, map discovery, profile settings, and a UI that works in both light and dark mode.

Screenshots
Add final screenshots to docs/screenshots/ before publishing the repository.

Recommended screenshots:

Screen	File name
Feed / Home	docs/screenshots/feed.png
Create Post	docs/screenshots/create-post.png
Map	docs/screenshots/map.png
Post Detail	docs/screenshots/post-detail.png
Activity	docs/screenshots/activity.png
Profile	docs/screenshots/profile.png
Dark Mode	docs/screenshots/dark-mode.png
Once the images are added, this section can be changed to a visual grid.

Tech Stack
Kotlin and XML views
MVVM with ViewModels, repositories, use cases, and domain models
Firebase Authentication
Cloud Firestore
Room Database
Google Maps SDK
Fused Location Provider
Material Components
Glide
JUnit unit tests
Features
Toronto-first nearby feed with category filtering and pull-to-refresh
Location-tagged community posts with approximate public area labels
Live likes, comments, unread activity, and Feed bell badge updates using Firestore listeners
Google Maps view with category-based markers for nearby posts
Create Post flow with drafts, category selection, and map-based area selection
Activity tab for likes and comments on the user's posts
Saved posts and profile sections
Settings for theme, radius, notification preferences, and profile info
Room-backed cache for recently loaded posts, comments, drafts, and hidden content
Light and dark theme support
Architecture
The app uses a layered structure:

core: shared utilities, app settings, dependency container, state wrappers
domain: models, repository interfaces, and use cases
data: Firebase repositories, Room database, entities, DAOs, and mappers
ui: activities, fragments, adapters, XML layouts, and ViewModels
UI events flow through ViewModels into use cases and repository interfaces. Repository implementations combine Firebase, Room, location services, and local preferences, then expose clean domain models back to the UI.

Firebase Setup
Create a Firebase project.
Add an Android app with package name:
com.sixblock.app
Download google-services.json.
Place it here:
app/google-services.json
Enable Firebase Authentication:
Email/password
Google sign-in, if you want Google login
Create a Firestore database.
Deploy Firestore rules:
firebase deploy --only firestore:rules
The project is designed to work on the Firebase Spark plan for in-app real-time updates. True push notifications while the receiving app is fully closed require a trusted backend such as Cloud Functions.

Google Maps Setup
Create a Maps API key and add it to local.properties:

MAPS_API_KEY=your_maps_key_here
Do not commit real API keys or Firebase credentials.

Running The App
Open the project in Android Studio.
Add google-services.json.
Add MAPS_API_KEY to local.properties.
Sync Gradle.
Run the app configuration on a device or emulator.
For best testing, use two different accounts on two devices to verify live likes, comments, activity updates, and unread badges.

Tests
The project includes unit tests for:

local/domain mappers
geolocation and distance helpers
time-ago formatting
create-post validation/use case behavior
Run tests from Android Studio or with:

./gradlew test
Project Notes
Image upload is not included because Firebase Storage requires billing for this project setup.
Firestore listeners are used for Spark-plan real-time feed, comment, like, and activity updates.
Phone push notifications for another user's like/comment require a backend sender and are intentionally separated from the free-plan implementation.
The public feed shows approximate area labels instead of exposing exact coordinates in post cards.
