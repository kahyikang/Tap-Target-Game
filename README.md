# Tap the Target 🎯

An Android reaction-speed game developed in **Java** with **XML layouts** for the UCCD3223 Mobile Applications Development individual practical assignment.

## Gameplay

Tap the highlighted square to earn a point. A new square is highlighted randomly after every successful tap. Each required level lasts five seconds, and the score carries over between levels.

| Level | Grid | Number of tiles | Duration |
|---|---|---:|---:|
| 1 | 2 × 2 | 4 | 5 seconds |
| 2 | 3 × 3 | 9 | 5 seconds |
| 3 | 4 × 4 | 16 | 5 seconds |
| 4 | 5 × 5 | 25 | 5 seconds |

## Current features

- Random highlighted target, including possible repeats
- Score tracking across levels
- Five-second countdown per level
- Pause and resume with remaining time preserved
- Confirmation before ending a game
- Continue or end after each level, and game over after the final level

## Planned features

- Home screen and improved game UI
- Persistent Top 25 leaderboard and player-name entry
- Additional levels after the required first four
- Audio and animations

## How to run

1. Open this **complete Android Studio project folder** in Android Studio.
2. Allow Gradle to sync and install any required Android SDK components.
3. Select an emulator or connected Android device, then press **Run**.

> This repository should contain the complete Android Studio project, including its Gradle build scripts and wrapper, not only `MainActivity.java` and `activity_main.xml`.

## Project structure

- `app/src/main/java/` — Java application code
- `app/src/main/res/layout/` — XML screen layouts
- `app/src/main/AndroidManifest.xml` — Android application configuration
- `app/build.gradle` or `app/build.gradle.kts` — app build configuration

## Development status

Work in progress. The four-level gameplay is the first milestone; additional functionality is being developed.
