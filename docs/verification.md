# Verification

Version **1.2.0** uses Android PrintService and sends TSPL over TCP 9100. Automated checks, emulator checks and physical acceptance are separate evidence layers.

## Coverage

**22 core unit tests** and **23 Android instrumentation tests** cover:

| Layer | Checks |
| --- | --- |
| Core | 1-bit packing, white padding, headers, geometry/rotation, media IDs, validation, page/copy order, cancellation and TCP deadlines |
| PDF raster | Four borders, page identity, corrupt PDF, cancellation cleanup, dynamic stock geometry |
| Print Test | PDF → raster → TSPL, QR decoding, white-on-black badge, rulers and media changes |
| Android system Print | Actual dialog → loopback receiver; selected pages/copies, media changes, no bytes before confirmation |
| UI / preferences | IP validation, Activity recreation, presets, custom-size validation and shared selection |

See [core tests](../core/src/test/java/com/revolic/tscprint/core) and [Android tests](../app/src/androidTest/java/com/revolic/tscprint). Network tests send only to `127.0.0.1:9100` on a dedicated emulator and restore temporary preferences. The GitHub workflow builds/lints and runs core tests; it does not run a printer or emulator.

## Public-release preparation — 2026-10-15

- Debug assembly and lint passed before publication.
- Core tests were rerun, rather than relying solely on Gradle's up-to-date results.
- The first fresh Android run passed 22/23 tests. The settings test checked preferences immediately after a UI click, before its callback completed. The test now waits for the existing save acknowledgment before asserting persisted state; app behavior was not changed.
- Final rerun: **23/23 Android tests passed** on Android 16 / API 36, zero failures/skips. **22/22 core tests passed**. Lint and debug assembly passed. Total: **45 tests**.

## APK / runtime / hardware

- Package `com.revolic.tscprint`, version **1.2.0 (3)**, minSdk **33**, targetSdk **37**; release build.
- APK SHA-256: `39a632c85c93d593d7a75f0d4c756f359795d405892c730d0bb4de5b0aa5b600`.
- Signing certificate SHA-256: `8e2dac87fd0dc053177e879b1e2aa0e931c0fa8c9af061f8bf5cafb920dc37f6`.
- APK v2 signature verified with Android apksigner before public distribution. No production signing key is included.
- Emulator coverage: Android 16 / API 36 AOSP, arm64. README screenshots are actual release-app/system UI captures, not design mockups.
- Earlier signed upgrades from 1.1.0 to 1.2.0 preserved saved IP/paper preferences.
- A Redmi 15 5G / Android 16 / HyperOS 3.0 printed a new web label to an MH261T without USB after the per-app battery and spooler recovery in [troubleshooting.md](troubleshooting.md). The user confirmed physical output on 2026-10-14.

## Gaps

Android 13 / API 33 and Android 17 / API 37 runtime behavior has not been physically tested. The API 37 permission path is compiled/reviewed, not runtime-certified. Physical A5/custom stock, other printer models, 300/600 DPI and all OEM print dialogs are not certified. Long idle/reboot persistence after HyperOS recovery was not retested.

Correct bitmap borders do not establish feed/calibration accuracy; a completed TCP send does not prove physical output. Encrypted PDFs and unusually complex native rendering are not acceptance fixtures. This project is not a vendor certification program.
