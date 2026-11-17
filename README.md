# TapPop

**Spot the Shade. Tap to Score.**

TapPop is an Android color-recognition and reaction game developed for the **UCCD3223 Mobile Applications Development** individual practical assignment. Players must spot and tap the one tile with a different color shade before the time runs out.

## Features

- **8 levels**, progressing from a **2 × 2** grid to a **9 × 9** grid
- **5 seconds per level** and a live countdown indicator
- Randomly positioned target tile after every correct tap
- **Easy and Hard modes** with different difficulty and progression rules
- Pause/resume, confirmation before ending a game, and game results
- A visual **How to Play** guide with page navigation and an interactive tile example
- Music and sound-effect settings
- Separate **Top 25 leaderboards** with locally saved player names and scores

## Game Modes

| Rule | Easy Mode | Hard Mode |
|---|---|---|
| Color difference | Noticeable throughout | Becomes more subtle across levels |
| Correct tap | +1 point | +1 point |
| Wrong tap | No penalty | −1 point (minimum score: 0) |
| Level progression | Player chooses to continue after each level | Automatically advances through Level 8 |
| Leaderboard | Easy Top 25 | Hard Top 25 |

Each level lasts **5 seconds**. The target is the tile with a different shade from the others. The game ends after Level 8 or when the player chooses to end it early.

## Technologies

- **Language:** Java
- **User interface:** Android XML layouts and Java-based dialogs
- **Development environment:** Android Studio
- **Local database:** SQLite
- **Version control:** Git and GitHub

## Getting Started

1. Clone this repository:
   ```bash
   git clone https://github.com/kahyikang/Tap-Target-Game.git
   ```
2. Open the cloned project folder in **Android Studio**.
3. Allow Gradle to sync and install any required Android SDK components.
4. Select an Android emulator or connect an Android device with USB debugging enabled.
5. Click **Run** to build and launch TapPop.

To install the game manually on an Android phone, build an APK in Android Studio using **Build → Build APK(s)** (menu wording may vary), then transfer and install the generated APK on the device.

## Main Source Files

| File | Purpose |
|---|---|
| `HomeActivity.java` | Home screen, game-mode selection, guide, settings, and leaderboard display |
| `MainActivity.java` | Game board, timer, scoring, pause, and level transitions |
| `GameMode.java` | Easy/Hard mode selection |
| `ModeScoreDatabaseHelper.java` | Persistent scores and separate Top 25 rankings |
| `ScoreDatabaseHelper.java` | Legacy score storage and compatibility |
| `GameAudioManager.java` | Music and sound effects |
| `activity_home.xml`, `activity_main.xml` | Main screen layouts |

Java classes are located under `app/src/main/java/`, and Android resources under `app/src/main/res/`.

## Leaderboard

Scores are stored locally using SQLite. Each mode has its own Top 25 list. The game also includes initial sample entries for demonstrating the leaderboards; qualifying player scores can replace them.

## Project Information

**Course:** UCCD3223 Mobile Applications Development  
**Assignment:** Individual Practical Assignment  
**App name:** TapPop  
**Repository:** [Tap-Target-Game](https://github.com/kahyikang/Tap-Target-Game)

> Developed as an academic project for learning Android application development.
