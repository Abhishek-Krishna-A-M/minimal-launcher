# Minimal Launcher 📟

A terminal-style Android launcher built with Kotlin + Jetpack Compose, designed for speed, control, and minimal resource usage.

**No icons. No widgets. No background services.**

Just a fast, keyboard-first launcher that behaves more like a CLI than a traditional Android home screen.

## 🚀 Why this exists

Most Android launchers optimize for visuals. This one optimizes for **latency and intent**.

- **Minimal footprint** – ~15–20 MB RAM in normal usage
- **Instant fuzzy search** – apps, contacts, and commands
- **Event-driven** – no polling, no timers, no background loops
- **Terminal-first UX** – everything starts with text
- **Designed for low-end devices** and daily personal use

Inspired more by Arch Linux + fzf than Material UI launchers.

**Nothing runs unless you ask it to.**

## ⌨️ Command Interface

### App launching
```
fire     → Firefox
ter      → Termux
ls       → list all apps
(empty)  → recently used apps (ordered by last used)
```

### Phone & contacts
```
p:              → recent calls
p:dad           → fuzzy search contacts
p:9876543210    → dial number directly
```

### System / utilities
```
g: query        → Google search
rm appname      → uninstall app
install appname → open Play Store search
cls             → clear session history
exit / quit     → close drawer
```

## 🔍 Fuzzy Search Engine

Custom lightweight fuzzy matcher with:
- Sequential character matching
- Gap penalties
- Prefix & early-match bonuses

Optimized for:
- Low allocations
- Predictable latency
- Smooth typing experience

**Designed to feel instant, not perfect.**

## 🧠 Architecture Highlights

- **Global app cache** with incremental updates
- **Tiered contact loading**
  - Recent calls first
  - Full contacts loaded only when needed
- **Broadcast-driven app updates**
  - Installs/uninstalls reflected instantly
- **No analytics**
- **No background services**

## 🛠 Built With

- Kotlin
- Jetpack Compose
- Android Content Providers
- Minimal Accessibility Service (for double-tap lock)

## 📦 Build

```bash
./gradlew assembleDebug
```

## 📲 Installation

1. Clone the repository
2. Open in Android Studio
3. Build and install on your device
4. Set as default launcher in system settings

## 🎯 Philosophy

This launcher is **intentionally opinionated**.

### It's not meant to:
- Look pretty
- Replace your phone UI
- Support every feature request

### It is meant to:
- Be fast
- Stay out of your way
- Feel like home if you live in terminals

## 🤝 Contributing

Contributions are welcome, especially for:
- Performance improvements
- Bug fixes
- Documentation

Please keep the minimalist philosophy in mind when proposing features.

## 📜 License

MIT — use it, fork it, break it, improve it.

## 🙏 Acknowledgments

Built for people who prefer `vim` over VS Code and `dwm` over GNOME.

---

*Made with ⚡ by someone tired of waiting for their home screen to load*
