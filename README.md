# Journal - Android Diary Application

## Overview

Journal is a modern Android diary application built with Jetpack Compose that allows users to
create, manage, and organize their personal journal entries. The app provides a clean, intuitive
interface for recording daily thoughts, attaching images, and tagging locations to preserve
memories.

## Features

### Core Functionality

- **Journal Entries**: Create and manage text-based journal entries
- **Image Attachments**: Add multiple images to each journal entry
- **Location Tagging**: Automatically or manually add location information to entries
- **Date Selection**: Choose custom dates for journal entries
- **Bookmark System**: Mark favorite entries for quick access
- **Swipe Actions**: Intuitive swipe gestures for marking and deleting entries

### User Experience

- **Modern UI**: Built entirely with Jetpack Compose for a fluid, modern interface
- **Lazy Loading**: Efficient loading of journal entries with pagination support
- **Smooth Animations**: Polished animations for card interactions and transitions
- **Bottom Sheet**: Convenient entry creation via an expandable bottom sheet
- **Snackbar Notifications**: User-friendly feedback with action support

## Architecture

The application follows a clean architecture approach with clear separation of concerns:

### Components

- **UI Layer**: Compose-based UI components and screens
- **Data Layer**: Room database for persistent storage
- **Repository Pattern**: Abstraction layer between data sources and UI
- **Utilities**: Helper classes for permissions, location, and image handling

### Key Classes

- `JournalData`: Core data model representing journal entries
- `JournalDataSource`: Custom data source implementation with pagination
- `JournalDatabase`: Room database implementation for data persistence
- `MainScreen`: Primary UI container and navigation hub
- `CustomLazyCardList`: Custom implementation of lazy loading list

## Technologies

### Core Libraries

- **Jetpack Compose**: Modern UI toolkit for Android development
- **Room**: SQLite object mapping library for local data persistence
- **Coil**: Image loading library optimized for Compose
- **AMap Location**: Location services integration (高德地图定位服务)
- **Kotlin Coroutines**: Asynchronous programming

### Development Environment

- Kotlin 1.9+
- Android SDK 35 (Android 15)
- Minimum SDK 29 (Android 10)
- Gradle with Kotlin DSL

## Getting Started

### Prerequisites

- Android Studio Iguana (2023.2.1) or newer
- JDK 21

### Installation

1. Clone the repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run the application on an emulator or physical device

### Configuration

To use location features, you need to configure AMap API:

1. Obtain an API key from [AMap Developer Console](https://lbs.amap.com/)
2. Uncomment and update the API key in AndroidManifest.xml

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Icons from Material Design
- Sample images included for testing purposes