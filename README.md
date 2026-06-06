# DevFocus

![Build](https://github.com/AkshayAshokCode/DevFocus/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/30114.svg)](https://plugins.jetbrains.com/plugin/30114)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/30114.svg)](https://plugins.jetbrains.com/plugin/30114)

DevFocus is a Pomodoro timer plugin for all JetBrains IDEs that helps developers stay focused, take structured breaks, and track their productivity — without leaving the IDE.

<!-- Plugin description -->
# DevFocus — Pomodoro Timer for JetBrains IDEs

Most Pomodoro timers don't survive an IDE restart, skip the long break entirely, and require you to leave the editor just to pause. DevFocus fixes all three — and stays out of your way while doing it.

---

## What makes it different

**💾 Survives restarts and crashes.** Timer state is saved to disk continuously. Reopen the IDE — even the next day — and your session is exactly where you left it, paused and ready to resume.

**🔁 Proper long breaks.** After a full round of sessions, DevFocus fires a long break automatically — the defining feature of the Pomodoro technique that most timer plugins quietly omit.

**🕐 Tells you when you'll be done.** Once running, the tool window shows two live timestamps: when the current session ends and when the full round finishes. No mental math — just "Ends at 3:45 PM" and "Free by 5:30 PM" so you can plan your day around your focus blocks.

**⏭ Skip break from the notification.** When you're in flow, click **Skip Break** directly in the IDE notification balloon. No need to open the tool window or break your focus.

**⌨️ Full keyboard control.** Start/Pause, Reset, and Skip Break are registered as IDE actions — assign your own shortcuts via **Settings → Keymap → DevFocus** to avoid conflicts with your existing bindings. All three are also reachable via **Tools → DevFocus** and Find Action (`Ctrl+Shift+A` / `⌘⇧A`).

**📍 Status bar always present.** Live countdown visible while you code, even with the tool window closed. When idle, shows your daily session count — a quiet reminder of what you've already accomplished.

---

## Everything else

- **Three built-in modes** — Classic Pomodoro (25/5), Deep Work (50/10), or fully Custom durations
- **Saved custom modes** — name and save your own timing configurations; switch to them in one click from the mode dropdown
- **Visual circular timer** — arc depletes clockwise, colour-coded by phase
- **Customizable ring colors** — pick any Focus and Break accent color from a color picker in Settings
- **Session indicator** — dot row showing completed, active, and upcoming sessions
- **🍅 Daily session counter** — resets at midnight, shown in tool window and status bar
- **Auto-start toggle** — choose whether work sessions start automatically after a break or wait for you
- **Actionable notifications** — Skip Break, Skip Long Break, Start Session — inline in the balloon
- **Responsive tool window** — adapts between compact, vertical, and horizontal layouts as you resize

---

## Supported IDEs

18 IDEs — IntelliJ IDEA, Android Studio, PyCharm, WebStorm, CLion, Rider, GoLand, PHPStorm, RubyMine, DataSpell, DataGrip, and more.

<!-- Plugin description end -->

## Installation

- **IDE Plugin System:** <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > search **DevFocus** > <kbd>Install</kbd>

- **JetBrains Marketplace:** [plugins.jetbrains.com/plugin/30114](https://plugins.jetbrains.com/plugin/30114)

- **Manually:** Download the [latest release](https://github.com/AkshayAshokCode/DevFocus/releases/latest) and install via <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## License

DevFocus is licensed under the Apache License 2.0.  
Copyright (c) 2026 Akshay Ashok — see the LICENSE file for details.
