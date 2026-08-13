# Walkthrough - Notification Settings Page

I have implemented a new notification settings page that allows users to manage dynamic notification milestones for their timers. This feature supports both percentage-based and time-offset-based notifications.

## Changes Made

### Core Models & Storage
- Updated [Timer.kt](file:///D:/Code/Android Studio/One/app/src/main/java/com/example/remind/Timer.kt) to include `NotificationSetting` (Percentage and Time-based).
- Enhanced [TimerRepository.kt](file:///D:/Code/Android Studio/One/app/src/main/java/com/example/remind/TimerRepository.kt) to store and retrieve notification settings using DataStore, including default values (50%, 90%, 100%, and several time offsets).

### Logic & Navigation
- Refactored [TimerViewModel.kt](file:///D:/Code/Android Studio/One/app/src/main/java/com/example/remind/TimerViewModel.kt) to handle dynamic alarm scheduling. When settings are added, edited, or deleted, all active timers are automatically rescheduled with the new milestones.
- Added navigation logic to toggle between the main screen and the settings screen.

### UI Enhancements
- Added a 3-dot settings button to the header in [UIComponents.kt](file:///D:/Code/Android Studio/One/app/src/main/java/com/example/remind/UIComponents.kt).
- Implemented the `SettingsScreen` with two groups of notifications:
    - **Percentage Based**: Triggers when a timer reaches a specific percentage completion.
    - **Time Based**: Triggers at a specific offset (Before/After) in minutes, days, or months.
- Added interactive dialogs for adding and editing notifications, and simple deletion functionality.

## Verification Results

### Automated Tests
- No automated tests were added, as per the existing codebase patterns.

### Manual Verification
1.  **Navigation**: Successfully opened the settings page via the 3-dot icon and returned to the home screen using the back button.
2.  **Defaults**: Verified that the initial default notifications (50%, 90%, 100%, After 10 days, Before 1 day, Before 10 minutes) are correctly displayed.
3.  **CRUD Operations**:
    - **Add**: Verified that the "Add" buttons open the correct dialogs for both percentage and time-based settings.
    - **Edit**: Verified that the "Edit" buttons open dialogs with pre-filled values.
    - **Delete**: Verified that clicking the "Delete" button removes the notification from the list.
4.  **UI State**: Confirmed that the settings are persistent and that the UI updates immediately when settings change.
5.  **Dynamic Scheduling**: The `TimerViewModel` now uses these settings to schedule alarms via `AlarmManager`, ensuring all future milestones are caught based on user preference.

### Screenshots
````carousel
![Home Screen with Settings Button](file:///D:/Code/Android Studio/One/app/src/main/artifacts/home_screen.png)
<!-- slide -->
![Settings Screen with Groups](file:///D:/Code/Android Studio/One/app/src/main/artifacts/settings_screen.png)
````
*(Note: Screenshots were captured during manual verification in the emulator.)*
