# Product: TSC Direct Print

## Purpose and users

Print PDF labels from Android's standard Print menu to saved TSC MH261T printers over a reachable local network. Up to 20 named IPv4/port profiles share a fixed 203 DPI pipeline; TCP 9100 is the default. Each profile has an independent default paper size. UI language is Thai. Android 13+ and a 203 DPI printer are required.

## Workflow

Install the APK, open a printer from Settings to edit its endpoint and paper default, check connectivity, enable the Android Print Service, choose a printer in Print / Print Test and print from a supported app. Browsing or saving printer settings does not change the active print selection. Alternatively choose a PDF inside the Print workspace. Both workspaces select a saved printer and restore its paper default. The final destination is always chosen in Android Print; app selection cannot programmatically override that dialog. Print Test previews a diagnostic label and opens the system confirmation dialog.

## Stock and layout

Presets are 102 × 76 mm and 148 × 210 mm. Up to 20 custom sizes can be saved with 0.1 mm precision, within the application's 20–168 mm width and 20–500 mm feed-length limits. The system job's selected media determines output geometry; app defaults do not resize queued jobs.

## Constraints

No automatic test print or retry after a partial send. Calibration, Shift, Reference, sensors, ribbon, speed and density remain printer settings. A completed TCP send does not establish physical output. See [verification](docs/verification.md) for measured coverage and hardware gaps.

## Implementation

Native Android PrintService, Kotlin/Compose Material 3 UI, Java PDF/protocol core. Direct local networking; no cloud or Mac relay. The source is MIT-licensed; dependencies retain their own licenses.
