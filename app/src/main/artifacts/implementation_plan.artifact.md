# Implementation Plan - Notification Settings Page

Add a settings page to manage notification milestones for timers. Notifications can be based on percentage or time offsets.

## User Review Required

- **Global vs. Per-Timer Settings**: The plan assumes these notification settings are global for all timers, as implied by "This page will have all the notifications listed whether it is in build or added by the user".
- **Rescheduling**: When settings change, all active timers will have their milestone alarms recalculated and rescheduled.
- **Default Notifications**: I will initialize the settings with the requested defaults if none exist.

## Proposed Changes

### Core Models & Repository

#### [Timer.kt](file:///D:/Code/Android Studio/One/app/src/main/java/com/example/remind/Timer.kt)
- Add `NotificationSetting` sealed class and subclasses (`PercentageSetting`, `TimeSetting`).
- Add enums `TimeType` and `TimeUnitType`.

#### [TimerRepository.kt](file:///D:/Code/Android Studio/One/app/src/main/java/com/example/remind/TimerRepository.kt)
- Add `NOTIFICATION_SETTINGS_KEY` to DataStore.
- Implement `notificationSettingsFlow` and `saveNotificationSettings`.
- Provide default settings initialization.

---

### Logic

#### [TimerViewModel.kt](file:///D:/Code/Android Studio/One/app/src/main/java/com/example/remind/TimerViewModel.kt)
- Add `notificationSettings` state flow.
- Add `isSettingsOpen` state flow for navigation.
- Implement CRUD operations for notification settings.
- Refactor `scheduleMilestoneAlarms` to use dynamic settings.
- Refactor `cancelMilestoneAlarms` to handle dynamic settings (using setting IDs for alarm request codes).
- Add logic to reschedule all timers when settings are modified.

---

### UI Components

#### [UIComponents.kt](file:///D:/Code/Android Studio/One/app/src/main/java/com/example/remind/UIComponents.kt)
- **MainScreen**: Add the 3-dot settings button to the header.
- **SettingsScreen**: New screen to display and manage notification groups.
- **NotificationGroup**: Component to display a list of notifications and an "Add" button.
- **AddEditNotificationDialogs**: Dialogs for adding/editing percentage and time-based notifications.

---

### Verification Plan

#### Manual Verification
1.  **Open Settings**: Click the 3-dot button on the home screen to ensure it opens the settings page.
2.  **View Defaults**: Verify that 50%, 90%, 100% and the three time-based defaults are present.
3.  **Add Notification**: Add a new percentage notification (e.g., 75%) and a new time notification (e.g., Before 5 minutes).
4.  **Edit Notification**: Change an existing notification's value and verify it updates.
5.  **Delete Notification**: Remove a notification and verify it disappears.
6.  **Alarm Scheduling**: Create a short timer and verify that the newly added notification milestones trigger (via logs or actual notifications if possible in the emulator).
7.  **Navigation**: Verify that clicking back from settings returns to the home screen.
