#!/usr/bin/env bash
set -euo pipefail

echo "==> Preparing CanvasFlow environment..."

# 1. Ensure debug.keystore exists (decode from base64 if present, otherwise generate via keytool)
if [ ! -f "debug.keystore" ]; then
  if [ -f "debug.keystore.base64" ]; then
    echo "==> Decoding debug.keystore from debug.keystore.base64..."
    tr -d '[:space:]' < debug.keystore.base64 | base64 -d > debug.keystore
  else
    echo "==> Generating standard Android debug.keystore via keytool..."
    keytool -genkeypair -v \
      -keystore debug.keystore \
      -storepass android \
      -alias androiddebugkey \
      -keypass android \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -dname "CN=Android Debug,O=Android,C=US"
  fi
fi

# 2. Ensure .env exists for Secrets Gradle Plugin
if [ ! -f ".env" ] && [ -f ".env.example" ]; then
  echo "==> Creating .env from .env.example..."
  cp .env.example .env
fi

# 3. Build Debug APK using system gradle or ./gradlew
if command -v gradle >/dev/null 2>&1; then
  echo "==> Running gradle :app:assembleDebug :app:testDebugUnitTest..."
  gradle :app:assembleDebug :app:testDebugUnitTest
elif [ -f "./gradlew" ]; then
  chmod +x ./gradlew
  echo "==> Running ./gradlew :app:assembleDebug :app:testDebugUnitTest..."
  ./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon
else
  echo "Error: Neither 'gradle' nor './gradlew' was found."
  exit 1
fi

echo "==> Build & Tests succeeded (exit code 0)!"
echo "==> Debug APK output: app/build/outputs/apk/debug/app-debug.apk"
