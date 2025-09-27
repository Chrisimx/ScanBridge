# Copilot Instructions for ScanBridge

## Project Overview

ScanBridge is an Android scanning application written in Kotlin using Jetpack Compose and Material You design. The app enables users to scan documents over the AirScan/eSCL protocol using network scanners.

### Key Technologies
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Build System**: Gradle with Kotlin DSL
- **Target**: Android (API 35)
- **Architecture**: Modern Android architecture with ViewModels, Compose UI
- **Protocol**: eSCL/AirScan for scanner communication

## Development Guidelines

### Code Style and Formatting
- Use **ktlint** for Kotlin code formatting (version 1.5.0)
- Follow the `.editorconfig` configuration
- Run `./ktlint --editorconfig=./.editorconfig` to check formatting
- Run `./ktlint --editorconfig=./.editorconfig --format` to fix formatting
- Code should follow Material Design 3 patterns for UI components

### Project Structure
```
app/src/main/java/io/github/chrisimx/scanbridge/
├── data/model/          # Data models (e.g., PaperFormat)
├── stores/              # Data stores and repositories  
├── theme/               # UI theme configuration (Typography, colors)
├── uicomponents/        # Reusable UI components
│   └── dialog/          # Dialog components
└── util/                # Utility classes
```

### Building and Testing

#### Build Commands
- `./gradlew assembleDebug` - Build debug APK
- `./gradlew assembleRelease` - Build release APK
- `./gradlew test` - Run unit tests
- `./gradlew connectedAndroidTest` - Run instrumentation tests

#### CI/CD
- GitHub Actions run on every push/PR to main branch
- Build process includes APK creation and signing
- Formatting is checked automatically with ktlint
- Tests must pass before merging

### Coding Conventions

#### File Headers
All Kotlin files should include the GPL-3.0 license header:
```kotlin
/*
 *     Copyright (C) 2024-2025 Christian Nagel and contributors
 *
 *     This file is part of ScanBridge.
 *
 *     ScanBridge is free software: you can redistribute it and/or modify it under the terms of
 *     the GNU General Public License as published by the Free Software Foundation, either
 *     version 3 of the License, or (at your option) any later version.
 *
 *     ScanBridge is distributed in the hope that it will be useful, but WITHOUT ANY
 *     WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *     FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along with ScanBridge.
 *     If not, see <https://www.gnu.org/licenses/>.
 *
 *     SPDX-License-Identifier: GPL-3.0-or-later
 */
```

#### Package Structure
- Use the base package: `io.github.chrisimx.scanbridge`
- Follow standard Android package organization
- Group related functionality in appropriate subpackages

#### Compose UI Guidelines
- Use Material 3 components and theming
- Leverage custom typography defined in `theme/Typography.kt`
- Use `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` when needed
- Follow proper state management patterns with `remember` and `mutableStateOf`

### Dependencies and Libraries
- **Compose BOM**: For Jetpack Compose components
- **Material 3**: For UI components and theming
- **eSCLKt**: For scanner communication (custom library)
- **Kotlinx Serialization**: For data serialization
- **Timber**: For logging
- **OkHttp**: For HTTP communications

### Testing
- Unit tests in `app/src/test/java/`
- Instrumentation tests in `app/src/androidTest/java/`
- Test files follow the pattern: `*Test.kt`
- Use descriptive test method names

### Documentation
- Update `CHANGELOG.md` for notable changes following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format
- Maintain clear commit messages in imperative form
- Reference issue numbers in commits when applicable
- Code comments should explain complex business logic, not obvious code

### Contributing
- Fork the repository and create feature branches
- Ensure GPG-signed commits when possible
- Follow the guidelines in `CONTRIBUTING.md`
- Be respectful and considerate in all interactions
- Submit detailed bug reports and feature requests

### Security and Licensing
- This is GPL-3.0-or-later licensed software
- Do not introduce dependencies with incompatible licenses
- Be mindful of scanner security when handling network communications
- Follow Android security best practices

### Scanner Support
- The app supports eSCL/AirScan protocol scanners
- Focus on broad scanner compatibility
- Handle various paper formats and scanning capabilities
- Support features like duplex scanning, resolution settings, and input sources

## Common Patterns

### UI Components
```kotlin
@Composable
fun MyComponent(parameter: String, onAction: () -> Unit) {
    // Use Material 3 components
    // Follow established theming patterns
    // Handle state properly with remember/mutableStateOf
}
```

### Error Handling
- Use proper exception handling for scanner communications
- Provide user-friendly error messages
- Log errors appropriately with Timber

### State Management
- Use ViewModels for business logic
- Compose UI should be stateless when possible
- Handle configuration changes properly

When contributing to ScanBridge, focus on maintaining the clean architecture, following Material Design principles, and ensuring broad scanner compatibility.