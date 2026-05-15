# 6ixBlock Architecture

## Layers

- `core`: shared state wrappers, location utilities, and dependency container.
- `domain`: app models, repository contracts, and use cases.
- `data`: Firebase repositories, Room database, DAOs, entities, and mappers.
- `ui`: Activities, Fragments, ViewModels, adapters, and XML layouts.

## Data Flow

UI events call ViewModels. ViewModels call use cases. Use cases call repository interfaces. Repository implementations combine Firebase, Room, and platform APIs, then expose domain models back to the UI through `Resource` and `UiState`.

## Location Privacy

Posts store precise coordinates for distance filtering and map markers. The public feed displays an approximate area and rough distance. The Firestore schema includes `geohash` for future geospatial query optimization.

## Offline Behavior

Room stores recently loaded posts/comments, unsent drafts, and locally hidden content IDs. The feed can show cached posts while Firestore refreshes.

## Navigation

Root tabs use fragment add/show/hide to keep Feed, Map, Create, Activity, and Profile stable. Detail and settings screens are pushed onto the main container back stack.
