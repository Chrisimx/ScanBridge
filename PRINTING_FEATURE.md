# AirPrint/IPP Everywhere Printing Feature Implementation

## Overview
This implementation adds printing functionality to ScanBridge using Android's Print Framework, which automatically supports AirPrint and IPP Everywhere printers through the system's print services.

## Changes Made

### 1. UI Changes
- **ExportSettingsPopup.kt**: Added a third print button (🖨️) alongside existing PDF and Archive export buttons
- **Strings.xml**: Added "Print" text in English and German translations

### 2. Core Functionality
- **ScanningScreen.kt**: 
  - Added `doPrint()` function that uses Android PrintManager
  - Created `ScanDocumentPrintAdapter` class that converts scanned images to PDF for printing
  - Integrated print callback into export options popup

### 3. Key Features
- **AirPrint/IPP Everywhere Support**: Uses Android Print Framework which automatically discovers and supports AirPrint and IPP Everywhere printers
- **Multi-page Printing**: Supports printing multiple scanned pages as a single print job
- **Page Range Selection**: Users can select which pages to print through the system print dialog
- **Error Handling**: Same validation as other export functions (no scans, job running, etc.)
- **PDF Generation**: Converts scanned images to PDF format for printing using existing iText library

### 4. User Experience
When users tap the share/export button in the scanning screen, they now see three options:
1. 📄 PDF Export
2. 🖼️ Archive Export  
3. 🖨️ Print (NEW)

Selecting print will:
1. Open the Android system print dialog
2. Show available printers (including AirPrint/IPP printers)
3. Allow users to select printer settings, page ranges, etc.
4. Send the scanned documents to the selected printer

## Technical Implementation
- Uses `PrintManager.print()` with a custom `PrintDocumentAdapter`
- Converts scanned images to PDF format using iText library
- Handles cancellation and error scenarios
- Supports page range printing
- Automatic scaling to fit page size (A4 with 1-inch margins)

## Compatibility
- **Minimum Android Version**: Works with existing app requirements (Android 28+)
- **Printer Support**: Any printer supported by Android Print Framework, including:
  - AirPrint printers (Apple ecosystem)
  - IPP Everywhere printers
  - Manufacturer-specific print services
  - Cloud printing services

## Testing
- Code compiles successfully
- Maintains compatibility with existing export functionality
- Ready for manual testing with actual printers

The implementation follows the existing app patterns and provides a seamless user experience for printing scanned documents.