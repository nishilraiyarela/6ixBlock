# 6ixBlock

6ixBlock is a Toronto-based Android community board app for nearby posts, local events, help requests, free items, safety updates, and neighbourhood recommendations.

The app is built with native Kotlin and XML using MVVM architecture, Firebase, Room, Google Maps, and location-aware features. It focuses on real community-app workflows such as creating location-tagged posts, browsing nearby updates, commenting, liking, saving posts, viewing activity updates, and discovering posts on a map.

## Screenshots

> Add final screenshots before publishing.

| Feed | Create Post | Map |
|---|---|---|
| `docs/screenshots/feed.png` | `docs/screenshots/create-post.png` | `docs/screenshots/map.png` |

| Post Detail | Activity | Profile |
|---|---|---|
| `docs/screenshots/post-detail.png` | `docs/screenshots/activity.png` | `docs/screenshots/profile.png` |

## Tech Stack

- Kotlin
- XML Views
- MVVM Architecture
- Firebase Authentication
- Cloud Firestore
- Room Database
- Google Maps SDK
- Fused Location Provider
- Material Components
- Glide
- JUnit

## Features

- Toronto-first nearby community feed
- Location-tagged posts with approximate public area labels
- Category filtering for posts and map markers
- Real-time likes, comments, activity updates, and unread badges using Firestore listeners
- Saved posts and profile sections
- Post detail screen with comments
- Google Maps view with category-based pins
- Create Post flow with drafts and map-based area selection
- Activity tab for likes and comments on user posts
- Light and dark theme support
- Room-backed local caching for posts, comments, drafts, and hidden content
- Settings for theme, radius, notifications, and profile info

## Architecture

The project follows a layered Android architecture:

```text
app/
├── core/       shared utilities, settings, dependency container, state wrappers
├── data/       Firebase, Room, repositories, DAOs, entities, mappers
├── domain/     models, repository contracts, use cases
└── ui/         activities, fragments, adapters, ViewModels, XML layouts
