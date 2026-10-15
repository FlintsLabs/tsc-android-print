# Verification

## In-app updater — 1.3.4 (2026-10-15)

- `:core:test` passed **33 cases**; debug assembly, Android test APK assembly and `:app:lintDebug` passed. Signed `:app:assembleRelease` and `:app:lintRelease` passed for **1.3.4 (9)**. Certificate SHA-256 remains `8e2dac87fd0dc053177e879b1e2aa0e931c0fa8c9af061f8bf5cafb920dc37f6`. Pre-commit verification APK SHA-256: `ba03d5686e5abbfabe34fd1c31ca115145db42623f0ca53d987196c064e7091a`. The final tagged build records its checksum in the Release asset `SHA256SUMS.txt`; rebuilding after commit also updates embedded Git revision metadata.
- Dedicated API 36 emulator full instrumentation completed with `OK (45 tests)`: **44 passed and one opt-in network test skipped by assumption**. The opt-in test was then run separately and passed **1/1**, downloading actual GitHub Latest `v1.3.3`, 23,155,499 bytes, SHA-256 `530171f145c3262df64d9cc2943cbb2333dcef8395f8032ef7cbb68f0073ab41`. This exercises Android HTTPS, GitHub API/CDN redirects and the published checksum, without modifying GitHub.
- Updater cases cover stable numeric comparisons, exact checksum names, bounded/truncated/canceled streams, corrupt download cleanup, package/signer/downgrade rejection, private FileProvider scope, offline retry, lifecycle recreation, process-style restore without network, progress, permission/install callbacks and retry states. Existing system Print tests still send only to emulator loopback receivers.
- Emulator-only native install acceptance staged a **test-only debug-signed no-code 1.3.5 fixture**, restored it through production verification, opened Android's actual unknown-source permission screen, then native Update confirmation. Package Manager remained at code 9 until confirmation and reported code 10 afterwards. SharedPreferences bytes matched before/after. The real debug 1.3.4 app was restored afterwards. This fixture is not a public Release or a product APK; test APK fixtures are excluded from the signed release APK.
- Inspected actual Settings renders with live GitHub checks on compact light/dark (1080 × 2160 / 480 dpi), 1.5× text, and wide light (1920 × 1200 / 240 dpi). Check controls remained readable and reachable by scrolling. Progress, ready and error controls were also captured. System animation scales were disabled; no custom motion was added. The wide capture helper initially swiped outside the centered scroll container; targeting the actual scroll bounds completed the capture without product changes.
- Evidence: ignored `build/evidence/updater-*.log`, `updater-native-result.json`, `updater-native-*.png/xml/txt` and update-state captures. The initial TDD compile failure is recorded in `updater-red.log`.
- At the end of feature verification, **1.3.4 had not yet been installed on physical RS38/HyperOS devices or published as a GitHub Release**. See the [v1.3.4 Release](https://github.com/FlintsLabs/tsc-android-print/releases/tag/v1.3.4) for subsequent publication/deployment evidence. OEM/MDM installation restrictions, API 33/37 updater runtime, low-storage installation failure and full upgrade to a future real public Release remain unverified. Updating can stop the app/service; finish jobs before installation.

## Per-printer settings navigation — 1.3.3 (2026-10-15)

- Settings rows navigate to independent printer details. Name, IP, port and paper changes remain drafts until Save printer; adding/editing another profile preserves the active print selection. Probe and diagnostic actions capture the saved viewed profile, and are disabled while that form has unsaved changes. Back returns to the list before returning to Print.
- The new navigation regression failed on the previous release because tapping a row did not open its settings. The complete Android API36 instrumentation run passed **35/35 tests in 140.205 s**, including profile browsing/editing without selection changes, draft recreation/back navigation, add/edit/delete without selecting the new profile, custom stock application on save, and the actual software keyboard on name/IP/port.
- A real Android Print flow was launched from the details page for a second loopback profile while the first profile remained selected in the app. After explicit system confirmation, the second endpoint received one A5 diagnostic label; the decoy endpoint received no job data. The raster/TSPL bytes matched the diagnostic generated for the viewed profile's host and media. A separate details probe test connected to the viewed profile's loopback IP/port and retained the app selection.
- `:core:test` (30 existing cases, up-to-date), `:app:lintRelease` and signed `:app:assembleRelease` passed. Package **1.3.3 (8)** uses the existing signing certificate. APK SHA-256: `e60f92db033229873849f5f447578531116c2551e7dc5ce2a494dd3c294c9e19`.
- Inspected actual list/details/action renders in compact light/dark at 1080 × 2160 / 480 dpi, wide at 1920 × 1200 / 240 dpi, and 1.5× text. Large-text navigation required scrolling to the second printer; the capture helper was updated to scroll and the remaining captures completed. Single-profile and 20-profile list states were inspected; Add is disabled at the limit with recovery text. No product-code change was needed after the visual pass. System animation scales were disabled and no custom motion was introduced.
- Signed 1.3.3 was subsequently installed over 1.3.1 on four physical Android devices through Wireless ADB using `install -r`. Package Manager reported versionName 1.3.3 / versionCode 8 on every device, and each app was launched with a live process confirmed afterwards. No physical print job was submitted during deployment; physical print acceptance for this revision is still pending.
- Evidence: ignored `build/evidence/printer-details-*.log` and `build/evidence/printer-details-1.3.3/`, plus deployment readbacks in the task. Loopback verification does not establish physical output.

## Printer settings UI — 1.3.2 (2026-10-15)

- Settings now exposes saved printers as a radio list, including each endpoint and paper default, with Add printer above the list. The selected state means app selection, not network reachability. Print and Print Test keep a compact selector with a chevron and count.
- New regression failed against the old UI because saved printers were hidden in a menu. The final focused run passed **5/5** tests on API36 at **1080 × 2160 / 480 dpi**: direct list selection and paper restoration after activity recreation, selection across both workspaces, add/edit/delete, IP validation/persistence, and the real software keyboard on name/IP/port fields. Radio rows were verified through Android accessibility's checked state.
- Inspected rendered captures in compact light/dark, wide **1920 × 1200 / 240 dpi**, and **1.5× text**. Checked single-printer and 20-printer states; at 20 profiles Add is disabled with recovery text and the page remains scrollable. Existing system animation scales were disabled; no custom motion was added.
- `:core:test` (30 cases, up-to-date), `:app:lintRelease` and signed `:app:assembleRelease` passed. Package **1.3.2 (7)** uses the existing signing identity. SHA-256: `61319d5e3e43943d66f5dd40f4196bb2d43296780ecea0e336b9695d463222e9`.
- Evidence is under ignored `build/evidence/settings-redesign-1.3.2/` and `build/evidence/settings-redesign-*.log`. Visual fixtures use loopback addresses; they do not establish physical printer availability. This UI revision has not yet been installed or print-tested on a physical device. The full instrumentation suite was not rerun for this UI-only revision.

## Keyboard hotfix — 1.3.1 (2026-10-15)

- Reproduced on a physical CipherLab RS38 / Android 13: the keyboard started at y=1251, but the form viewport ended at y=867, leaving **384 px** unused. This matched the 128 dp Scaffold footer at the device's 480 dpi density.
- The app applied ordinary Scaffold padding followed by IME padding without consuming the former. `consumeWindowInsets(innerPadding)` now accounts for the footer before applying the remaining IME inset. No printer, media, transport or rendering logic changed.
- New `KeyboardInsetsTest` failed on 1.3.0 with the same **384 px** gap on a 1080 × 2160 / 480 dpi emulator. After the fix it passed using the real software keyboard on the name, IP and port fields.
- Signed **1.3.1 (6)** was installed over 1.3.0 through Wireless Debugging on the RS38. All three physical fields received focus and were checked with the keyboard visible. Recorded gaps were 19 px in the first name-field capture, then **0 px** for IP and port; the former large blank area was absent in screenshots. Closing the keyboard restored the bottom navigation. Existing printer settings were preserved and no physical print was submitted.
- Core results remain **30 tests passed**. Debug/release lint, assembly and APK v2 signature verification passed. The full Android rerun passed **31/32** tests; its remaining failure was a test expecting an off-screen paper selector to be visible on the shorter 360 dp-wide RS38 layout. After making that test scroll to the selector, the two multi-printer UI tests passed (**2/2**). All 32 distinct cases passed across those runs; this is not a claim of a single 32/32 full run.
- Test fixtures now snapshot existing settings, then use one isolated loopback printer and stock defaults. This avoids collisions with previously saved multi-printer demo profiles and restores the original catalog after each test.
- APK SHA-256: `64a3cdfd132da99188234bb3ccd7d9950f5477db8d503274b28b9d1ea1ea1ef3`. Signing identity is unchanged from 1.3.0.

## Local release — 1.3.0 (2026-10-15)

- **30/30 core tests** passed with zero failures. **31/31 Android instrumentation tests** passed with no failures/skips on a dedicated Android 16 / API 36 AOSP arm64 emulator. The full Android run completed in 186.986 seconds. After a color-contrast adjustment, the two multi-printer UI tests were rerun with system animations disabled.
- Debug assembly, Android test assembly, debug lint, release lint and signed release assembly passed. No physical labels were sent during this verification.
- New coverage: catalog validation and the 20/21-printer boundary; per-printer paper defaults; one-time legacy migration retaining the original IP, paper and `mh261t` ID; shared Print/Print Test selectors; UI add/edit/delete; A5 defaults surviving Activity recreation; per-endpoint ordering, concurrency and queue limits.
- Native Android Print routed the job to `127.0.0.2:9101` while a decoy receiver listened on `127.0.0.1:9100` and the app default still selected the first profile. Only the confirmed destination received print data. All simulated sends remained inside the emulator.
- UI tests use accessibility scroll/click actions for Compose forms; text fields can intercept coordinate swipes. Native Print remains driven through UI Automator. Discovery fixtures use unique names to avoid selecting removed printers retained in Android history.
- Manual UI checks passed on compact phone and 1600 × 1200 wide layouts, light/dark themes, 1.3× font size and disabled motion. A low-contrast printer selector inside the green card was corrected and visually rechecked. These were debug-app captures; README release screenshots remain labeled with their original version.
- Package `com.revolic.tscprint`, **1.3.0 (5)**, minSdk **33**, targetSdk **37**.
- APK: `TSC-Direct-Print-1.3.0.apk`; SHA-256: `f4c7d71e9e96518081a9ada7b6cfc43d8d68d97595d3a95528e8a9809293edea`.
- APK v2 signature verified. Certificate SHA-256: `8e2dac87fd0dc053177e879b1e2aa0e931c0fa8c9af061f8bf5cafb920dc37f6`, matching the earlier release signing identity. No private signing material is included.

Final Android command after installing the debug app and test APKs on the dedicated emulator (animation setup is documented in [building.md](building.md)):

```sh
adb -s emulator-5556 shell am instrument -w \
  com.revolic.tscprint.test/androidx.test.runner.AndroidJUnitRunner
# OK (31 tests)
```

The app restores the selected profile's paper and advertises that profile's default in printer capabilities. Android Print controls the final printer/media for each job and may remember earlier options; defaults do not override a user's explicit system-dialog selection.

## Historical baseline — 1.2.0

Version **1.2.0** uses Android PrintService and sends TSPL over TCP 9100. Automated checks, emulator checks and physical acceptance are separate evidence layers.

### Coverage

**22 core unit tests** and **23 Android instrumentation tests** cover:

| Layer | Checks |
| --- | --- |
| Core | 1-bit packing, white padding, headers, geometry/rotation, media IDs, validation, page/copy order, cancellation and TCP deadlines |
| PDF raster | Four borders, page identity, corrupt PDF, cancellation cleanup, dynamic stock geometry |
| Print Test | PDF → raster → TSPL, QR decoding, white-on-black badge, rulers and media changes |
| Android system Print | Actual dialog → loopback receiver; selected pages/copies, media changes, no bytes before confirmation |
| UI / preferences | IP validation, Activity recreation, presets, custom-size validation and shared selection |

See [core tests](../core/src/test/java/com/revolic/tscprint/core) and [Android tests](../app/src/androidTest/java/com/revolic/tscprint). Network tests send only to `127.0.0.1:9100` on a dedicated emulator and restore temporary preferences. The GitHub workflow builds/lints and runs core tests; it does not run a printer or emulator.

### Public-release preparation — 2026-10-15

- Debug assembly and lint passed before publication.
- Core tests were rerun, rather than relying solely on Gradle's up-to-date results.
- The first fresh Android run passed 22/23 tests. The settings test checked preferences immediately after a UI click, before its callback completed. The test now waits for the existing save acknowledgment before asserting persisted state; app behavior was not changed.
- Final rerun: **23/23 Android tests passed** on Android 16 / API 36, zero failures/skips. **22/22 core tests passed**. Lint and debug assembly passed. Total: **45 tests**.

### APK / runtime / hardware

- Package `com.revolic.tscprint`, version **1.2.0 (3)**, minSdk **33**, targetSdk **37**; release build.
- APK SHA-256: `39a632c85c93d593d7a75f0d4c756f359795d405892c730d0bb4de5b0aa5b600`.
- Signing certificate SHA-256: `8e2dac87fd0dc053177e879b1e2aa0e931c0fa8c9af061f8bf5cafb920dc37f6`.
- APK v2 signature verified with Android apksigner before public distribution. No production signing key is included.
- Emulator coverage: Android 16 / API 36 AOSP, arm64. README screenshots are actual release-app/system UI captures, not design mockups.
- Earlier signed upgrades from 1.1.0 to 1.2.0 preserved saved IP/paper preferences.
- A Redmi 15 5G / Android 16 / HyperOS 3.0 printed a new web label to an MH261T without USB after the per-app battery and spooler recovery in [troubleshooting.md](troubleshooting.md). The user confirmed physical output on 2026-10-14.

## Current verification gaps

Android 13 / API33 was physically tested on a CipherLab RS38 using 1.2.1 on 2026-10-15: the user confirmed one 102×76 mm label with all four borders. The multiple-printer 1.3.0 path still requires physical acceptance with several printers. Android 17 / API37 runtime behavior has not been physically tested. The API 37 permission path is compiled/reviewed, not runtime-certified. Physical A5/custom stock, other printer models, 300/600 DPI and all OEM print dialogs are not certified. Long idle/reboot persistence after HyperOS recovery was not retested.

Correct bitmap borders do not establish feed/calibration accuracy; a completed TCP send does not prove physical output. Encrypted PDFs and unusually complex native rendering are not acceptance fixtures. This project is not a vendor certification program.
