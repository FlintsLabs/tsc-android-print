# Product: TSC Direct Print

## Purpose and users

Print PDF labels from Android's standard Print menu to one TSC MH261T over a reachable local network. The printer address is editable IPv4; the port is TCP 9100. UI language is Thai. Android 13+ and a 203 DPI printer are required.

## Workflow

Install the APK, set/save the printer IP, check connectivity, enable the Android Print Service, choose paper and print from a supported app. Alternatively choose a PDF inside the Print workspace. Print Test previews a diagnostic label and opens the system confirmation dialog.

## Stock and layout

Presets are 102 × 76 mm and 148 × 210 mm. Up to 20 custom sizes can be saved with 0.1 mm precision, within the application's 20–168 mm width and 20–500 mm feed-length limits. The system job's selected media determines output geometry; app defaults do not resize queued jobs.

## Constraints

No automatic test print or retry after a partial send. Calibration, Shift, Reference, sensors, ribbon, speed and density remain printer settings. A completed TCP send does not establish physical output. See [verification](docs/verification.md) for measured coverage and hardware gaps.

## Implementation

Native Android PrintService, Kotlin/Compose Material 3 UI, Java PDF/protocol core. Direct local networking; no cloud or Mac relay. The source is MIT-licensed; dependencies retain their own licenses.
