# Minimal Launcher 📟

A high-performance, terminal-style Android launcher built with **Jetpack Compose**. 

## 🚀 Why this exists
Most launchers are bloatware. This one is built for speed:
- **Tiny Footprint:** Uses ~15MB of RAM.
- **Arch Inspired:** Built-in system diagnostics (CPU, RAM, Uptime).
- **CLI Driven:** Launch apps and settings via terminal commands.

## ⌨️ Custom Aliases
Define your own short-codes in `AppDrawer.kt`:
- `c` -> Camera
- `t` -> Termux
- `gh` -> GitHub

## 🛠 Build
```bash
./gradlew assembleDebug
