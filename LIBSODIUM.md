# Building libsodium

This project vendors pre-built libsodium libraries for convenience. To update or rebuild them:

## Prerequisites

- Android NDK (automatically detected from `$ANDROID_HOME` or set `$ANDROID_NDK_HOME`)
- Git
- Standard build tools (make, autoconf, automake, libtool)

On macOS with Homebrew:
```bash
brew install autoconf automake libtool
```

On Ubuntu/Debian:
```bash
apt-get install autoconf automake libtool
```

## Building

To build libsodium for all Android ABIs:

```bash
./build-libsodium.sh [VERSION]
```

Examples:
```bash
# Build latest 1.0.20 release (default)
./build-libsodium.sh

# Build a specific version
./build-libsodium.sh 1.0.20-RELEASE

# Build a different version
./build-libsodium.sh 1.0.19-RELEASE
```

The script will:
1. Clone libsodium from the official repository
2. Build for all Android ABIs (armeabi-v7a, arm64-v8a, x86, x86_64)
3. Install libraries to `libsodium/lib/<abi>/libsodium.a`
4. Install headers to `libsodium/include/`
5. Clean up temporary build files

## Current Version

The vendored libraries are built from:
- **Version**: libsodium 1.0.20-RELEASE
- **Date**: Original build date unknown, use `git log libsodium/` to check when last updated

To check what version is currently vendored, you can examine the headers:
```bash
grep "SODIUM_VERSION_STRING" libsodium/include/sodium/version.h
```

## Why Vendor?

While it would be ideal to build libsodium from source during every build, doing so adds significant complexity:
- libsodium uses autotools, not CMake
- Cross-compilation for Android requires special NDK setup
- Build time increases significantly (several minutes per ABI)
- Most developers don't need to modify libsodium

By vendoring pre-built libraries (~2MB total), we get:
- Fast builds
- Reproducible builds
- No external dependencies for most developers
- Easy updates when needed (via this script)

## Updating the Vendored Libraries

When a new version of libsodium is released:

1. Run the build script with the new version:
   ```bash
   ./build-libsodium.sh 1.0.21-RELEASE
   ```

2. Test that everything builds and tests pass:
   ```bash
   ./gradlew clean assembleDebug connectedDebugAndroidTest
   ```

3. Commit the updated libraries:
   ```bash
   git add libsodium/
   git commit -m "Update libsodium to version 1.0.21"
   ```

## Troubleshooting

**"ANDROID_NDK_HOME not set"**
- Set `ANDROID_NDK_HOME` to your NDK directory, or
- Ensure `ANDROID_HOME` is set and contains an NDK installation

**"autogen.sh: command not found"** or similar
- Install autotools: `brew install autoconf automake libtool` (macOS)

**Build fails for a specific ABI**
- Check that your NDK version is compatible (tested with NDK r25+)
- Some older NDKs may have issues with certain ABIs
