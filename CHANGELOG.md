<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# DevFocus Changelog

## [Unreleased]

## [2.1.0]
### Changed
- Cleaned up boilerplate scaffold files (MyProjectActivity, MyProjectService)
- Removed default keyboard shortcuts to avoid conflicts with existing IDE bindings — assign via Settings → Keymap → DevFocus
- Removed unnecessary optional Android plugin dependency declaration

### Fixed
- Removed stale "Don't forget to remove sample code" warning logged on every project open

## [2.0.1]
### Fixed
- Removed unnecessary optional Android dependency declaration that required a config-file attribute
- Coroutine exception handler added to suppress debug metadata version mismatch crash on pause

## [2.0.0]
### Added
- **Long break support** — Classic Pomodoro now fires a long break (15 min) after completing a full round of 4 sessions; Deep Work fires a 30-min long break after 2 sessions
- **Skip Break** — button in the tool window and notification action to skip any break and jump straight to the next work session
- **Notification action buttons** — "Skip Break", "Skip Long Break", and "Start Session" (when auto-start is off) actions are now clickable directly from the balloon notification
- **IDE actions** — Start/Pause, Reset, and Skip Break registered as IDE actions under Tools → DevFocus and Find Action; assign your own shortcuts via Settings → Keymap → DevFocus
- **Daily session counter** — tracks how many work sessions you've completed today; shown in the tool window and status bar
- **Auto-start toggle** — new setting to control whether the next work session starts automatically after a break, or waits for a manual start
- **Timer state persistence** — timer state (remaining time, session, phase) is saved and restored across IDE restarts; a running timer resumes as paused
- **Status bar always visible** — shows "🍅 X today" when idle instead of disappearing; work/break/long-break each have a distinct emoji and label
- **Phase label** — explicit "Focus / Break / Long Break" text label with colour coding beneath the circular timer
- **Responsive layout** — tool window adapts to three modes: compact (<160 px), vertical, and horizontal (side-by-side timer and controls)

### Fixed
- Session counter no longer double-increments when a break ends (sessions were previously skipping every other number)
- Circular timer background arc now uses theme-aware colour instead of hardcoded light grey (was invisible in dark themes)
- Layout rebuild on panel resize no longer accumulates duplicate action listeners on buttons
- Skip Break from a notification now correctly starts the new work timer (the state guard in `start()` was blocking it)
- Custom settings panel no longer flashes visible for a frame when a session ends and a break starts

### Changed
- Circular timer scales dynamically with the panel size (capped at 180 px diameter) instead of using a fixed 180 px
- Session indicator dots resize dynamically when sessions-per-round changes (was clipping at >6 sessions with a fixed 200 px width)

## [1.2.3]
### Added
- Notification sound support with enable/disable setting
- Sound plays on work session complete and break complete

## [1.2.2]
### Added
- Classic Pomodoro and Deep Work preset modes
- Custom mode with configurable session, break, and sessions-per-round
- Circular timer panel with progress arc
- Session indicator dots showing completed, current, and upcoming sessions
- Status bar widget showing live timer when active
