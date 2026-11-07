# CanvasFlow — Multi-Screen Drawing & Writing Studio

**CanvasFlow** is an adaptive Android drawing, calligraphy, and handwriting application built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Room**. It adapts dynamically across mobile phones, foldables, and tablets using explicit float screen-coverage allocations (`0.65f`–`0.98f` / `65%`–`98%`), smooth two-finger pinch-to-zoom (`0.25x`–`8.0x`), and a precision HSV/RGB/HEX studio color picker.

---

## Features

- **Multi-Screen Float & Coverage Engine**: Real-time posture detection (`MOB-P`, `MOB-L`, `FOLD`, `TAB-XL`) with adjustable float screen-coverage presets (*Max Immersion 96.5%*, *Studio Balanced 90.0%*, *Inspector Split 78.0%*, and custom float slider).
- **Pinch-to-Zoom & Pan Navigation**: Multi-touch pinch zoom (`25%` to `800%`) centered on the gesture focal point, with automatic multi-touch disambiguation, 1-finger Pan Hand toggle, and live viewport minimap.
- **Precision Studio Color Picker**: Interactive 2D Saturation–Value matrix, 360° Hue bar, Ink Opacity float slider, HEX input, automatic color harmonies (*Complement*, *Analogous*, *Triadic*), and saved pigment swatches.
- **Drawing & Handwriting Tools**: Fountain Nib, Fine Liner, Highlighter Marker, Studio Brush, Charcoal, Ruler Line, Rectangle, Circle, Eraser, and Typography Callout Stamps across 6 paper templates (*5mm Dot Grid*, *Ruled Writing*, *Archival Cream*, *Quad Graph*, *Isometric 3D*, *Midnight Slate*).

---

## Compiling & Building the Debug APK

### 1. Automatic Build on GitHub Actions
Every push or pull request triggers `.github/workflows/android-debug-apk.yml`, which:
1. Sets up JDK 21 and the Android SDK.
2. Automatically decodes `debug.keystore` from `debug.keystore.base64`.
3. Runs unit and Robolectric tests (`:app:testDebugUnitTest`).
4. Compiles the signed debug APK (`:app:assembleDebug`) and uploads `CanvasFlow-debug-apk` (`app-debug.apk`) as a downloadable workflow artifact.

### 2. Building Locally via CLI
Clone the repository and run the helper script or Gradle Wrapper:

```bash
chmod +x ./scripts/build-debug-apk.sh
./scripts/build-debug-apk.sh
```

Or directly with Gradle Wrapper:

```bash
chmod +x ./gradlew
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The generated debug APK will be located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```
