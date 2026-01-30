#!/bin/bash
set -e

# Script to build libsodium for Android
# Usage: ./build-libsodium.sh [VERSION]
# Example: ./build-libsodium.sh 1.0.20-RELEASE

VERSION="${1:-1.0.20-RELEASE}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK_DIR="${SCRIPT_DIR}/build-temp"
TEMP_OUTPUT_DIR="${SCRIPT_DIR}/libsodium-new"
FINAL_OUTPUT_DIR="${SCRIPT_DIR}/libsodium"

# Android ABIs to build
ABIS=("armeabi-v7a" "arm64-v8a" "x86" "x86_64")

# Check for required tools
if [ -z "$ANDROID_NDK_HOME" ]; then
    if [ -n "$ANDROID_HOME" ]; then
        # Try to find the latest NDK
        ANDROID_NDK_HOME=$(ls -d "$ANDROID_HOME"/ndk/* 2>/dev/null | sort -V | tail -n1)
        if [ -z "$ANDROID_NDK_HOME" ]; then
            echo "Error: ANDROID_NDK_HOME not set and no NDK found in ANDROID_HOME"
            exit 1
        fi
        echo "Using NDK: $ANDROID_NDK_HOME"
        export ANDROID_NDK_HOME
    else
        echo "Error: ANDROID_NDK_HOME not set"
        exit 1
    fi
fi

echo "=================================================="
echo "Building libsodium ${VERSION} for Android"
echo "=================================================="
echo "NDK: $ANDROID_NDK_HOME"
echo "Output: $FINAL_OUTPUT_DIR"
echo ""

# Clean and create work directories
rm -rf "$WORK_DIR"
rm -rf "$TEMP_OUTPUT_DIR"
mkdir -p "$WORK_DIR"
mkdir -p "$TEMP_OUTPUT_DIR"
cd "$WORK_DIR"

# Clone libsodium
echo "Cloning libsodium ${VERSION}..."
git clone --depth 1 --branch "${VERSION}" https://github.com/jedisct1/libsodium.git
cd libsodium

# Build for each ABI
for ABI in "${ABIS[@]}"; do
    echo ""
    echo "=================================================="
    echo "Building for ${ABI}..."
    echo "=================================================="

    # Map ABI to libsodium's dist-build script
    case "$ABI" in
        armeabi-v7a)
            SCRIPT="android-armv7-a.sh"
            ;;
        arm64-v8a)
            SCRIPT="android-armv8-a.sh"
            ;;
        x86)
            SCRIPT="android-x86.sh"
            ;;
        x86_64)
            SCRIPT="android-x86_64.sh"
            ;;
        *)
            echo "Unknown ABI: $ABI"
            exit 1
            ;;
    esac

    # Run the build script
    cd dist-build
    ./"$SCRIPT"
    cd ..

    # Copy the results to temporary output directory
    BUILD_DIR="libsodium-android-${ABI}"
    mkdir -p "$TEMP_OUTPUT_DIR/lib/${ABI}"
    mkdir -p "$TEMP_OUTPUT_DIR/include"

    cp "${BUILD_DIR}/lib/libsodium.a" "$TEMP_OUTPUT_DIR/lib/${ABI}/"

    # Copy headers from the first build (they should be identical across ABIs)
    if [ ! -f "$TEMP_OUTPUT_DIR/include/sodium.h" ]; then
        cp -r "${BUILD_DIR}/include/"* "$TEMP_OUTPUT_DIR/include/"
    fi

    echo "✓ ${ABI} complete"
done

# All builds succeeded - atomically replace the old version
echo ""
echo "All ABIs built successfully. Installing..."
if [ -d "$FINAL_OUTPUT_DIR" ]; then
    mv "$FINAL_OUTPUT_DIR" "${FINAL_OUTPUT_DIR}.old"
fi
mv "$TEMP_OUTPUT_DIR" "$FINAL_OUTPUT_DIR"
rm -rf "${FINAL_OUTPUT_DIR}.old"

# Clean up
cd "$SCRIPT_DIR"
rm -rf "$WORK_DIR"

echo ""
echo "=================================================="
echo "✓ Build complete!"
echo "=================================================="
echo "Libraries installed to: $FINAL_OUTPUT_DIR"
echo ""
echo "Built ABIs:"
for ABI in "${ABIS[@]}"; do
    SIZE=$(du -h "$FINAL_OUTPUT_DIR/lib/${ABI}/libsodium.a" | cut -f1)
    echo "  - ${ABI}: ${SIZE}"
done
echo ""
echo "To verify the version:"
echo "  grep SODIUM_VERSION_STRING $FINAL_OUTPUT_DIR/include/sodium/version.h"
