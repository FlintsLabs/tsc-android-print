# TSC Direct Print — native workspace

Mode: Operate. Native Android Material 3, Kotlin/Compose, system typography.

## Direction
A compact print workspace: warm paper surfaces, deep green printer panel, generous task spacing, and the actual label as the main visual on Print Test. Keep factual status separate from network reachability; service enabled does not mean the printer is online.

Three destinations: พิมพ์ / Print Test / ตั้งค่า. Bottom navigation on compact phones and navigation rail from 600dp. Each screen has a clear heading and one filled primary action. System Back returns to the print screen. White label previews remain white in dark mode because they represent paper.

## Interaction
- Print: saved printer and stock, service/permission recovery, choose a PDF, quick access to diagnostics, three steps for printing from other apps.
- Print Test: one diagnostic page with 2mm inset borders, an 80mm ruler, a valid QR, and a black shelf badge with white numeral. The preview and PDF share one drawing implementation. Open Android Print to review the destination and copies; no print on navigation, automatic retry, or direct background dispatch.
- Settings: a printer list opens an independent details route with name, IPv4, port, paper default, connection check, diagnostic print and deletion. Android service/permission management remains on the list page. Unsaved changes disable probe/test actions so the target is unambiguous.
- Async progress and errors use text plus semantic live-region feedback; disabled actions include visible prerequisite instructions. No connectivity claims until an explicit probe completes.

## Tokens and accessibility
Material color roles defined in PrintTheme.kt for both themes. 12–16dp shapes, 24dp content gutters, 48dp minimum actions (56dp primary). Thai system type scales with font settings. Insets handle status/navigation bars and keyboard. Scrollable content on all destinations, centered maximum640dp content on wide screens. No custom animation, entrance effect or motion-dependent navigation; standard Material interactions honor the system animation scale.

## Acceptance
Inspect all three screens on a real Android emulator, light/dark, large text, compact and wide windows. Validate generated PDF through the existing raster/TSPL pipeline, QR decoding, all borders, ruler and reverse-text contrast. Exercise the actual Print Test button through system Print into a loopback receiver. Never infer physical alignment from software tests.

## Paper sizes (1.2.0)
Shared selector on Print and Print Test; custom editor in Settings. Preserve the existing native palette/navigation. Width is across the print head and height is the feed length; keep that caption beside every selector. Stock defaults are102×76 and148×210mm. Custom decimal fields accept one decimal digit, show inline validation, and save/select atomically. Selection persists across destinations/relaunch and becomes the default media advertised to Android. The final Android job's chosen media controls encoding. Diagnostic preview adapts to physical aspect; system Print changes regenerate it so rulers and borders remain measurements, not scaled artwork.

## Multiple printers (1.3.0)

Preserve the native green/paper theme and three destinations. A shared printer selector shows name and IP:port on Print and Print Test. Switching printers restores that profile's paper default and clears obsolete feedback. Settings provides a separate add/details route with name, IPv4, port and default paper; deletion requires confirmation and at least one profile remains. Reject duplicate endpoints, malformed IPv4/ports and names over48 characters inline without losing input. Busy operations disable target changes. Connection results name the exact probed endpoint. Explicitly tell users to select/review that destination in Android's Print dialog, which may remember a previous printer. No printer is advertised as reachable based solely on service-enabled state.

## Printer settings navigation (1.3.3)

Settings exposes saved printers as navigation rows with name, endpoint, paper default and a right-chevron. No radio controls or print-selection action appear in Settings. Put a full-width Add printer action and saved count before the list. The parent page scrolls for up to20 profiles and large text. A details route edits that profile independently of the active print selection; adding a profile also preserves selection. Save/cancel and Back return to the list; Back from the list returns to Print. Draft fields survive activity recreation and are discarded on leaving the editor without saving. Custom stock is saved globally but applied to the profile only on Save printer. Delete requires confirmation; deleting the active profile names the fallback printer. Detail probe/test actions use the saved viewed profile and are disabled with unsaved changes. Diagnostic printing opens Android Print for explicit destination review and confirmation. Print and Print Test retain the shared compact selector with a down-chevron and printer count; changing it restores the selected profile's paper default.
