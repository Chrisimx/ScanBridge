# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.5.0] - 2025-10-12
### Added
- Italian localization (c2881f5d). Thanks so much to @LucaMaroglio for the contribution!
- Per-App language setting (3caba03c)
- US paper formats (Letter, Legal) (a7025b69)
- Saving scan settings as default for new sessions (b3e2f3a1)
- Cancel button (0630997a)
- Source code and sponsor button in the settings screen (f9ccbeb4)

### Changed
- Design improvements (8c22b503, 722b4879, 9ca9de8b, fc93547e, 16552ace, 201b098a)

## Fixed
- A bug where an invalid resolution could be selected after changing the input source (635abe39)
- Improved scanner support (349986e5)

## [1.4.0] - 2025-08-27

### Added
- Scan session restoring (65e23af4)
- Discovery of HTTPS scanners / secure scanners (0b30a9ab)
- Custom scanners can now be saved (4db5876e)
- Customizable PDF chunk size (18543620)
- Option to deactivate HTTPS certificate validation (7b57a193)

### Fixed

#### Improved Scanner support
- Canon LiDE20/N676U: Fixed scanning communication / Relative Location header support ([#78](https://github.com/Chrisimx/ScanBridge/issues/78))

#### Further fixes
- Invalid scanner caps don't crash the app anymore (4657d726)
- Correctly cancle jobs when encountering any failure (fc1eb632)
- Handle full recomposition correctly (fe3defa9)

## [1.3.0] - 2025-04-24

### Fixed

#### Improved Scanner support
- HP DeskJet 2700 ([#62](https://github.com/Chrisimx/ScanBridge/issues/62))
- HP Color Laser MFP 179fnw ([#68](https://github.com/Chrisimx/ScanBridge/issues/68)))
- HP Officejet 8020 ([#71](https://github.com/Chrisimx/ScanBridge/issues/71))
- Canon MF628cw ([#65](https://github.com/Chrisimx/ScanBridge/issues/65))
- Canon MF269dw ([#72](https://github.com/Chrisimx/ScanBridge/issues/72))
- ...

### Changed
- Ignore link-local addresses (39b10e6d)
- Always use highest quality color mode instead scanner default value (on some scanners the default was black-and-white) (7e516c05)

## [1.2.0] - 2025-02-14

### Added

- Debug logging option ([#41](https://github.com/Chrisimx/ScanBridge/issues/41))
- Timeout setting ([#46](https://github.com/Chrisimx/ScanBridge/issues/46))

### Fixed

#### Scanner support
- **Confirmed fixed:** HP Color Laserjet MFP M283fdw ([#39](https://github.com/Chrisimx/ScanBridge/issues/39)), HP DeskJet 3630 ([#47](https://github.com/Chrisimx/ScanBridge/issues/47)), EPSON ET-2650 ([#57](https://github.com/Chrisimx/ScanBridge/issues/57))
- **Maybe fixed:** Kyocera ECOSYS M5521cdn ([#50](https://github.com/Chrisimx/ScanBridge/issues/50)), HP Neverstop Laser MFP 1200w ([#55](https://github.com/Chrisimx/ScanBridge/issues/55))

#### Others issues

- Use published port instead of 80/443 (76baa05f)
- Layout of full screen error dialog with very long error messages (311f36c)
- Crashes on older Android versions with API level < 34 because of unsupported API calls (64bebf55c, 9b5bd7577, bfde9e4a0ec)

### Changed

- Allow user-defined root CAs for HTTPS connections, disallowed before ([#48](https://github.com/Chrisimx/ScanBridge/issues/48))

## [1.1.0] - 2025-01-28

### Added

- Confirmation dialog for deleting pages in scanning interface (#34)

### Fixed

- Fix layout of the page previews in the scanning interface (center content, prevent overlapping
  other text elements like the information header) (cf26121f, a9436b30)
- Improve scanner support (bd2de720, 9bc201f4), at least partial support for HP Color Laserjet MFP
  M283fdw (#37, #39)
- Handle eSCL resources at root level correctly (2f010502, 5d6285e6, 42f92812) (#35)

### Changed

- Update dependencies: gradle, esclkt, kotlinx-serialization-json and the versions plugin (2f010502,
  bd2de720, 9bc201f4)

## [1.0.1] - 2025-01-15

### Fixed

- Fix stretching of pages in the PDF export if aspect ration doesn't match the expected one (#33)
- Fix crash when a scanner advertises an invalid address, ignore such scanners. If the address has
  a IPv6 scope ID strip it (46d5847a)
- Fix typo in english app description (fe98fa44)

### Changed

- Update gradlew (5b1266cc, a9f46dc2)
- Update dependencies: material3, composeBom, activityCompose, kotlinReflect (c214641d)

## [1.0.0] - 2025-01-06

### Added

- Initial release of ScanBridge
- Discover scanners supporting eSCL in your network
- Scan multiple pages and arrange them however you like
- Scan settings: input source, duplex scan, resolution, scan intent, scanning dimensions
- Export to PDF or JPEG ZIP archive
- Share scans with other apps
- Handle temporary files properly
- Localization for English and German

[unreleased]: https://github.com/Chrisimx/ScanBridge/compare/v1.2.0...HEAD

[1.3.0]: https://github.com/Chrisimx/ScanBridge/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/Chrisimx/ScanBridge/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/Chrisimx/ScanBridge/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/Chrisimx/ScanBridge/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/Chrisimx/ScanBridge/commits/v1.0.0/