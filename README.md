# Remind - Advanced Android Timer App

Remind is a powerful, modern timer management application built with **Kotlin** and **Jetpack Compose**. It features real-time tracking, intelligent milestone notifications, and a beautiful glassmorphism home screen widget.

## 🚀 Features

- **Unlimited Timers**: Create and manage multiple timers simultaneously.
- **Dynamic Sorting**: Timers are automatically sorted by proximity to completion—the most urgent timers always appear at the top.
- **Live UI**: Real-time progress bars and countdowns that update every second.
- **Glassmorphism Widget**:
    - Beautiful translucent design with a subtle border.
    - **Live Updates**: Updates every second directly on your home screen.
    - **Responsive Layouts**:
        - *Compact Mode*: Shows only progress bars when resized to small heights.
        - *Title Mode*: Shows Title and Progress Bar when resized to small widths.
        - *Full Mode*: Shows Title, Progress Bar, and Remaining Time.
- **Milestone Notifications**:
    - **50% Completed**: High-priority alert when half the time is gone.
    - **90% Completed**: Alert when only 10% of the time remains.
    - **100% Completed**: Final completion notification.
- **Pinned Notifications**: "Pin" any timer to see its live progress and remaining time in your system notification tray.
- **Intelligent Formatting**: 
    - 12-hour time picker with AM/PM support.
    - Dynamic countdown labels (hides hours/minutes when they reach zero).
- **Persistent Storage**: Uses **Android DataStore** and **Kotlin Serialization** to ensure your timers are never lost, even after a device reboot.

## 🛠️ Tech Stack

- **UI**: Jetpack Compose
- **Widget**: Android Glance
- **Concurrency**: Kotlin Coroutines & Flow
- **Architecture**: MVVM (ViewModel, Repository)
- **Local Storage**: Preferences DataStore + Kotlinx Serialization
- **Background**: Foreground Service for live notifications and widget sync

## 📸 Design

The app follows a clean aesthetic with white cards and soft shadows on a slightly grey (#F1F1F1) background. The progress bars feature vibrant gradients:
- **Active**: Pink-to-Yellow gradient.
- **Completed**: Blue-to-Green gradient.

## 📥 Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Adit-Exe/Remind.git
   ```
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Sync the Gradle project.
4. Run the app on an Android device or emulator (API 26+ required).

## 🤝 Contribution

Feel free to fork this project, report bugs, or submit pull requests to improve the features or UI!

---
Created by Adit.
