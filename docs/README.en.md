# TSC Direct Print — English guide

[ภาษาไทย / screenshots](../README.md) · [Download APK](https://github.com/FlintsLabs/tsc-android-print/releases/latest) · [Build](building.md) · [Verification](verification.md)

An independent Android Print Service for **TSC MH261T, 203 DPI**. It converts PDF pages to monochrome TSPL bitmaps and sends them directly over **TCP 9100**. A Mac, USB cable or print server is not needed. The UI is currently Thai, with light/dark themes and adaptive navigation.

## Install and connect

1. Download TSC-Direct-Print-1.2.0.apk from Releases and install it on **Android 13+**. Allow installation from the app opening the file when Android requests it.
2. Open **ตั้งค่า** (Settings), enter your printer's IPv4 address, then press **บันทึก IP** (Save IP). Replace the bundled default 192.168.70.37 with your own printer address.
3. Press **ตรวจการเชื่อมต่อ** (Check connection). This checks TCP 9100 without printing.
4. Press **จัดการบริการพิมพ์** (Manage print services) and turn **TSC Direct Print** on in Android Settings.
5. Grant local-network access if Android requests it. The phone must reach the printer; guest Wi-Fi/client isolation may block it.

Select the installed TSC service instead of adding an IP through Android's Default Print Service, which uses different protocols/discovery.

For CipherLab RS38, see the [installation guide](rs38.md). The app requires Android 13+ and an available Android print framework; RS38 hardware/firmware has not been acceptance-tested.

## Print

Use the normal **Print** action in a browser/PDF viewer that supports Android printing. Select TSC MH261T, review paper size, orientation, pages and copies, then confirm. To select a PDF directly, open **พิมพ์ → เลือก PDF เพื่อพิมพ์** (Print → Choose PDF). The same system dialog appears.

This service cannot add printing to a browser or WebView that does not provide it.

## Paper sizes

Presets: **102 × 76 mm** and **148 × 210 mm (A5)**. Add custom sizes through **ตั้งค่า → เพิ่มขนาดกระดาษเอง** (Settings → Add custom size), then **บันทึกและใช้ขนาดนี้** (Save and use).

Width runs across the print head; height follows feed. App bounds: **20–168 mm wide**, **20–500 mm high**, one decimal place, up to **20 saved custom sizes**. These are app limits, not the printer's maximum label length.

Sizes are shared by Print, Print Test and system Paper size. The final job's media selection is authoritative. For A5, keep physical 148 × 210 mm and change portrait/landscape separately. Match loaded stock and calibrate the printer sensor when changing rolls.

## Print Test

Open Print Test, choose paper, press **พิมพ์ฉลากทดสอบ**, and confirm the printer/media and **one copy** in Android's dialog. The diagnostic includes four borders around 2 mm from the edges, an 80 mm ruler (shorter/labeled on narrow stock), a QR containing TSC-TEST-001, and white text on black fill.

Opening preview does not print. The README label image is rendered output, not a photograph of physical stock.

## Limits and recovery

- PDF: 100 MiB / 100 pages; up to 100 copies and 1,000 total labels per job.
- Fit, center, rotate; threshold-128 monochrome without photo dithering. Copies are collated; pages are staged before connecting.
- Partial sends are never retried automatically. Canceling cannot retract data already accepted by the printer; inspect output before resending.
- Completed means TCP transmission, not physical confirmation. Paper/ribbon/cover status polling is not implemented.
- No telemetry/cloud upload. Temporary files stay in private cache and are cleaned after use. TCP 9100 is not encrypted.
- Shift, Reference, gap sensor, ribbon, speed and density are not modified.
- Other TSPL models and 300/600 DPI printers have not been tested.

On Xiaomi/Redmi/HyperOS, printing that works only while USB is attached may involve app freezing. Check **App info → Power → No restrictions**. The observed Redmi case also required restarting Android Print Spooler, closing the old dialog and creating a new job. A fresh web job was then user-confirmed to print without USB. This is observed recovery, not an OEM-wide guarantee; see [troubleshooting](troubleshooting.md).

Missing backgrounds in the source PDF must be corrected in the originating website's print CSS/PDF generation. The service cannot recover omitted content.

## Development / license

See [building](building.md), [architecture](architecture.md), [verification](verification.md) and [changelog](../CHANGELOG.md). Project code uses [MIT](../LICENSE); dependencies retain their own licenses, listed in [NOTICE](../NOTICE). No vendor driver binaries, firmware or proprietary SDK are included. This project is not affiliated with or endorsed by TSC.
