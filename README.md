# BotStudio - Discord Bot IDE & Simulator for Android

A feature-rich Android application for Discord bot development, featuring a real-time multi-language code editor with IntelliSense, interactive terminal, Discord simulator with slash commands and message components, visual embed builder, key-value storage, and package management.

---

## Building the APK

### 1. Using GitHub Actions (Automated CI/CD)
This repository includes a ready-to-run GitHub Actions workflow (`.github/workflows/build-apk.yml`).

1. Push your repository to GitHub.
2. In your GitHub repository, navigate to the **Actions** tab.
3. Select **Build Android APK** and click **Run workflow** (or simply push to `main` / `master`).
4. Once completed, download the generated APK (`BotStudio-debug-apk.zip`) from the **Artifacts** section at the bottom of the run summary.

---

### 2. Building Locally with `gradlew`

#### Prerequisites:
- JDK 21 (Eclipse Temurin recommended)
- Android SDK (API 36 / 34+)

#### Steps:
```bash
# Ensure gradlew has execution permissions
chmod +x gradlew

# (Optional) Restore debug.keystore if starting fresh
if [ -f debug.keystore.base64 ] && [ ! -f debug.keystore ]; then
  base64 -d debug.keystore.base64 > debug.keystore
fi

# Run tests
./gradlew test

# Build debug APK
./gradlew assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```
