# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

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

[unreleased]: https://github.com/Chrisimx/ScanBridge/compare/v1.1.0...HEAD

[1.1.0]: https://github.com/Chrisimx/ScanBridge/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/Chrisimx/ScanBridge/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/Chrisimx/ScanBridge/commits/v1.0.0/