# ScanBridge Android App

ScanBridge is a Material You Android application for scanning documents using the eSCL/AirScan protocol. It's written in Kotlin using Jetpack Compose and built with Gradle.

**Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.**

## Working Effectively

### Environment Setup
- **CRITICAL**: Use Java 21 (Temurin recommended). The project will NOT build with Java 17 or older due to dependency requirements.
- Install Android SDK with the following required components:
  - Android SDK Platform 35 (`platforms;android-35`)
  - Android SDK Build-Tools 35 (`build-tools;35.0.0`) 
  - Android SDK Platform-Tools (`platform-tools`)
- Set `ANDROID_HOME` environment variable, NOT `ANDROID_SDK_ROOT` (deprecated)

### Java 21 Installation
```bash
sudo apt-get update && sudo apt-get install -y openjdk-21-jdk
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
java -version  # Should show Java 21
```

### Android SDK Setup
```bash
# Download and install Android command line tools
cd /opt && sudo wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
sudo unzip commandlinetools-linux-11076708_latest.zip
sudo mkdir -p android-sdk/cmdline-tools
sudo mv cmdline-tools android-sdk/cmdline-tools/latest

# Set environment variables
export ANDROID_HOME=/opt/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Accept licenses and install required components
yes | sdkmanager --licenses
sdkmanager "build-tools;35.0.0" "platforms;android-35" "platform-tools"
```

### Build Commands
- **Build debug APK**: `./gradlew assembleDebug` -- takes ~40 seconds. NEVER CANCEL. Set timeout to 90+ seconds.
- **Build release APK**: `./gradlew assembleRelease` -- takes ~50 seconds. NEVER CANCEL. Set timeout to 90+ seconds.
- **Full build and test**: `./gradlew build` -- takes ~60 seconds. NEVER CANCEL. Set timeout to 120+ seconds.
- **Clean build**: `./gradlew clean` -- takes ~10 seconds.
- **Run unit tests**: `./gradlew test` -- takes ~60 seconds. NEVER CANCEL. Set timeout to 120+ seconds.

### Code Quality
- **Format check**: Download ktlint and run `./ktlint --editorconfig=./.editorconfig`
  ```bash
  curl -sSLO https://github.com/pinterest/ktlint/releases/download/1.5.0/ktlint
  chmod a+x ktlint
  ./ktlint --editorconfig=./.editorconfig
  ```
- **ALWAYS** run formatting check before committing or CI will fail (.github/workflows/format.yml)

## Validation

### Build Validation
- **ALWAYS** run the complete environment setup before building
- **ALWAYS** validate that builds complete successfully with `./gradlew build`
- **ALWAYS** run unit tests with `./gradlew test` to ensure changes don't break functionality
- **Manual validation requirement**: Test the application UI using Android instrumented tests when available

### Output Validation
- Debug APK location: `app/build/outputs/apk/debug/app-debug.apk` (~48MB)
- Release APK location: `app/build/outputs/apk/release/app-release-unsigned.apk` (~38MB)
- Test reports: `app/build/reports/tests/testDebugUnitTest/index.html`
- Lint reports: `app/build/reports/lint-results-debug.html`

### Expected Warnings
The build produces expected Kotlin warnings about deprecated Java APIs in `ScannerDiscoveryBackend.kt`:
- `'var host: InetAddress!' is deprecated`
- `'fun resolveService(...) is deprecated`

These warnings are expected and do not indicate build failures.

## Common Tasks

### Repository Structure
```
.
├── README.md                    # Project documentation
├── CONTRIBUTING.md             # Contribution guidelines  
├── build.gradle.kts            # Root build configuration
├── app/
│   ├── build.gradle.kts        # App module build config
│   └── src/
│       ├── main/               # Main source code
│       ├── test/               # Unit tests
│       └── androidTest/        # Instrumented tests
├── .github/
│   ├── workflows/              # CI/CD pipelines
│   │   ├── android.yml         # Build and test workflow
│   │   └── format.yml          # Code formatting check
│   └── actions/
│       └── build/              # Reusable build action
├── gradle/
│   └── libs.versions.toml      # Dependency versions
└── .editorconfig               # Code formatting rules
```

### Dependency Management
- Dependencies are managed in `gradle/libs.versions.toml`
- Main dependencies include:
  - eSCLKt library for scanner communication
  - Jetpack Compose for UI
  - Kotlin coroutines and serialization
  - Material Design 3 components

### Testing Strategy
- **Unit tests**: Located in `app/src/test/` - test business logic
- **Android instrumented tests**: Located in `app/src/androidTest/` - test UI and integration
- **Mock server**: Build script includes eSCL mock server for testing scanner functionality

### CI/CD Information
- **Build pipeline**: `.github/workflows/android.yml` - builds APKs and runs tests
- **Format pipeline**: `.github/workflows/format.yml` - validates code formatting with ktlint
- **Build timing**: CI typically completes in 3-5 minutes
- **Java version**: CI uses Java 21 (required)

### Troubleshooting
- **Build fails with Java version error**: Ensure Java 21 is installed and JAVA_HOME is set correctly
- **Android SDK not found**: Verify ANDROID_HOME is set and required SDK components are installed
- **License errors**: Run `yes | sdkmanager --licenses` to accept all licenses
- **Kotlin compilation timeout**: Kotlin compilation can take 15-30 seconds - do not cancel
- **Test failures with UnsupportedClassVersionError**: Indicates wrong Java version - must use Java 21

### Development Workflow
1. **Setup environment**: Install Java 21 and Android SDK with required components
2. **Clean build**: `./gradlew clean && ./gradlew build` to verify setup
3. **Make changes**: Edit Kotlin source files following existing patterns
4. **Test locally**: `./gradlew test` to run unit tests
5. **Format check**: Run ktlint to validate code formatting
6. **Build APK**: `./gradlew assembleDebug` to create installable APK
7. **Validate changes**: Install APK and test scanning functionality if UI changes made

### Key Locations for Common Changes
- **UI Components**: `app/src/main/java/io/github/chrisimx/scanbridge/`
- **Scanning Logic**: Uses eSCLKt library - check `ScanningScreen.kt` and related ViewModels
- **Settings**: `AppSettingsScreen.kt` for app configuration
- **Scanner Discovery**: `ScannerDiscoveryBackend.kt` for finding network scanners
- **Test Cases**: `app/src/test/java/org/github/chrisimx/scanbridge/`

**CRITICAL REMINDER**: Never cancel long-running build tasks. Android/Kotlin compilation requires patience and adequate timeouts.