#!/usr/bin/env bash
#
# build.sh - Build the Task Widget APK
#
# Usage:
#   ./build.sh          # Build debug APK
#   ./build.sh release  # Build release APK
#
# Prerequisites:
#   - ANDROID_HOME or ANDROID_SDK_ROOT environment variable must be set
#   - Android SDK with build-tools and platform 34 installed
#   - Java 8+ (JDK) installed
#
# The built APK will be at:
#   app/build/outputs/apk/debug/app-debug.apk
#   app/build/outputs/apk/release/app-release-unsigned.apk

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ---------- Detect Android SDK ----------
if [[ -z "${ANDROID_HOME:-}" ]] && [[ -z "${ANDROID_SDK_ROOT:-}" ]]; then
    # Try common locations
    for candidate in \
        "H:/ansdk" \
        "$HOME/Android/Sdk" \
        "$HOME/android-sdk" \
        "$HOME/Library/Android/sdk" \
        "/usr/local/lib/android/sdk" \
        "/opt/android-sdk"; do
        if [[ -d "$candidate" ]]; then
            export ANDROID_HOME="$candidate"
            break
        fi
    done
fi

ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$ANDROID_HOME" ]] || [[ ! -d "$ANDROID_HOME" ]]; then
    echo "ERROR: Cannot find Android SDK."
    echo "Set ANDROID_HOME or ANDROID_SDK_ROOT environment variable."
    exit 1
fi
export ANDROID_HOME
echo "Using Android SDK: $ANDROID_HOME"

# ---------- Detect Java ----------
if ! command -v java &>/dev/null; then
    echo "ERROR: Java not found. Install JDK 8+ and add to PATH."
    exit 1
fi
echo "Using Java: $(java -version 2>&1 | head -1)"

# ---------- Gradle wrapper ----------
if [[ ! -f "./gradlew" ]]; then
    echo "Gradle wrapper not found, downloading..."
    
    GRADLE_VERSION="8.0"
    GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
    GRADLE_ZIP="/tmp/gradle-${GRADLE_VERSION}-bin.zip"
    GRADLE_DIR="/tmp/gradle-${GRADLE_VERSION}"

    if [[ ! -d "$GRADLE_DIR" ]]; then
        echo "Downloading Gradle $GRADLE_VERSION..."
        curl -fsSL -o "$GRADLE_ZIP" "$GRADLE_URL"
        unzip -q -o "$GRADLE_ZIP" -d /tmp
    fi

    # Generate gradlew
    "$GRADLE_DIR/bin/gradle" wrapper --gradle-version "$GRADLE_VERSION"
fi

chmod +x ./gradlew

# ---------- Build ----------
BUILD_TYPE="${1:-debug}"

echo ""
echo "========================================="
echo "  Building Task Widget APK ($BUILD_TYPE)"
echo "========================================="
echo ""

if [[ "$BUILD_TYPE" == "release" ]]; then
    ./gradlew assembleRelease --no-daemon
    APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
else
    ./gradlew assembleDebug --no-daemon
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

echo ""
if [[ -f "$APK_PATH" ]]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "========================================="
    echo "  BUILD SUCCESSFUL"
    echo "  APK: $APK_PATH"
    echo "  Size: $APK_SIZE"
    echo "========================================="
    echo ""
    echo "To install on connected device:"
    echo "  adb install $APK_PATH"
else
    echo "ERROR: APK not found at expected path: $APK_PATH"
    echo "Check the build output above for errors."
    exit 1
fi
