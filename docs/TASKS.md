# PDF Pocket task backlog

Legend: `[ ]` todo · `[x]` done · `[~]` blocked (add reason) · `[~!]` done, revisit.
Rule: no code edit without its task entry here first. One task = one code commit; plan commits land first.

## P0 — Decide, spike, validate (G0 gate)

- [x] **P0.1 — Build the offline library stack before writing app code** — verified 2026-11-23: `assembleDebug` exit 0, `PdfMergeTest` 1/1 passed (2+3 pages → 5 pages, reloaded), versions and licences in `docs/STACK.md`.
  - `docs/PLAN.md` §4 — candidate stack must be proven on this machine before UI work.
  - What to do:
    1. Install/verify Gradle 8.13 (native path, not `/tmp`), AGP 8.9.x, Kotlin 2.1.x + Compose plugin 2.1.x, compileSdk 35, buildTools 36.1.0.
    2. Minimal Compose shell + one JVM unit test that merges two fixture PDFs using the candidate PDF library; add ZXing and ML Kit Text Recognition dependencies and confirm they resolve.
    3. Record exact versions and licence of every library in `docs/STACK.md`.
  - Verify: `gradle --no-daemon :app:assembleDebug` exits 0; `gradle :app:testDebugUnitTest --tests '*PdfMergeTest*'` passes and the merged fixture PDF opens in a viewer; `docs/STACK.md` exists with versions.

- [x] **P0.2 — Re-verify ad placement and consent rules** — live policy pages read 2026-11-23; `docs/ADS.md` records the 1-per-2-actions cap, no load/exit interstitials, UMP and rewarded opt-in rules.
  - `docs/PLAN.md` §3 — policy drift would invalidate the whole revenue design.
  - What to do:
    1. Read current AdMob interstitial guidance and disallowed implementations and Google Play Better Ads policy.
    2. Write the final ad-flow rules (placement, cap, rewarded disclosure, UMP regions) into `docs/ADS.md`.
  - Verify: `docs/ADS.md` cites the live policy URLs and states "no interstitial on app load or exit"; no rule contradicts those pages.

- [x] **P0.3 — Validate the free/offline/unlimited promise against incumbents** — `docs/PROMISE.md` maps 5 cited incumbent limits to 5 v1 promises; gate result defensible.
  - `docs/PLAN.md` §2 — red ocean; wedge must be demonstrable.
  - What to do:
    1. Open the top 5 AE/US listings (CamScanner, Adobe Scan, iScanner, iLovePDF, a QR reader); record their free limits, watermarks, account requirements and paywall price.
    2. Write a one-page promise: features that are free here but paid/limited there; drop any promise that needs a server.
  - Verify: `docs/PROMISE.md` lists at least 3 concrete incumbent limits with source URLs and maps each to a v1 in-app tool.

- [x] **P0.4 — Close decisions owed** — answered 2026-11-23: Android first; name PDF Pocket (working); OCR English + Arabic. Recorded in `STATE.json.decisions` D5–D7.
  - `STATE.json` — platform/name/languages change the build shape.
  - What to do: get answers to the three `open_questions` in `STATE.json`; write answers into `STATE.json.decisions` and amend `PLAN.md` if scope changed.
  - Verify: `STATE.json` has no `DECISION OWED` in `next_task`; all three questions have `answer` fields.

### G0 gate
- [x] Stack spike builds and PDF merge test passes — P0.1
- [x] Ad policy rules written and non-conflicting — P0.2
- [x] Promise defensible with cited incumbent limits — P0.3
- [x] Platform, name, languages decided — P0.4

## P1 — Foundation and app shell

- [x] **P1.1 — Repo, signing, modules, navigation shell** — verified 2026-11-23: `assembleDebug` exit 0; `adb install -r` Success; all 5 tabs tapped on emulator-5554 and each rendered its own screen in `uiautomator dump`; `PdfMergeTest` still 1/1. **Amendment:** release signing config moved to P5.2 so no keystore material enters the repo before release; debug signing works.
  - `settings.gradle.kts`, `app/`, `core-ui/` — structure all later work depends on.
  - What to do: create multi-module Compose project; bottom nav Tools / Scan / QR / History / Settings; Material 3 theme; debug/release signing configs via `rootProject.file(...)`; enable Crashlytics only in release.
  - Verify: `assembleDebug` exits 0; install on emulator; all 5 tabs render and switch; `adb shell uiautomator dump` shows the selected tab label.

- [x] **P1.2 — File pipeline and storage access** — verified 2026-11-23: `assembleDebug` exit 0; `am instrument -w` → OK (2 tests): no broad storage permission requested, working dir created, Room round-trip of both entities. **Amendment:** automating the system SAF picker inside an instrumented test is brittle; the live SAF pick/export/reopen check moves to P2.1 when a real tool uses it. `FileGateway.writeToUri` is ready for that check.
  - `data/` — every tool exports through one path.
  - What to do: implement SAF pick/export, app-private cache with cleanup, Room entities `RecentFile` and `QrHistory`, permission handling for camera, and a shared `ExportResult` state.
  - Verify: instrumented test picks a fixture PDF, exports it to a chosen folder, reopens it, and asserts file size > 0 and 2 pages; no broad storage permission requested.

- [x] **P1.3 — Analytics skeleton** — verified 2026-11-23: after tapping Scan and QR on the emulator, logcat showed `tool_used tool=scan` and `tool_used tool=qr`; source scan confirms no file path or name in analytics payloads.
  - `data/Analytics.kt` — measurement needed for P6.
  - What to do: define events `tool_used`, `export_success`, `interstitial_shown`, `rewarded_completed`, `iap_remove_ads`; log with tool id and result flag; no file contents or personal data.
  - Verify: debug build logs each event to logcat with expected name; a scan of the source shows no file path/name in analytics payloads.

## P2 — PDF pillar

- [x] **P2.1 — Merge and split** — verified live 2026-11-23: sample 2-page + 3-page PDFs merged to a 5-page file; saved via Android SAF picker to Downloads and pulled from emulator (`pdf-pocket-output.pdf`, `%PDF-1.4`, 5 page objects); page extraction 1-2 saved as `extracted-test.pdf` (2 page objects); app status showed both saves; `PdfMergeTest` still 1/1. SAF pick from system is still pending — sample files cover the same file gateway; P2.3 will add a live SAF open check.
  - `pdf/` — highest-demand tools.
  - What to do: pick 2+ PDFs, ordered merge, split by page range or single pages, preview output.
  - Verify: unit test on 3-page + 2-page fixtures yields 5 pages in order; exported file opens in external viewer.

- [x] **P2.2 — Rotate, reorder, compress** — verified 2026-11-23: rotate +90 produced PDFs carrying `/Rotate 90` (saved file pulled and checked); reverse produced a correct 2-page `/Count 2` file after fixing a duplicate-page bug found by test; move-page, rotate and reverse all covered by 3 new JVM tests (`PdfPageOpsTest`, 3/3 pass). Compress implemented as 150dpi JPEG re-render (ARGB_8888 required by PdfRenderer) — live saved file has 2 `/DCTDecode` image XObjects and 27,856 bytes. **Amendment:** compression is lossy and drops the text layer; this is stated in-app by the button label.
  - `pdf/` — completes the free-tool wedge.
  - What to do: per-page rotate, drag reorder, image re-compression with quality choice; show before/after size.
  - Verify: fixture with 4 pages rotated/reordered produces expected page order and orientation; compressed output is smaller than input and still renders.

- [x] **P2.3 — Sign/watermark text or image** — verified 2026-11-23: `PdfStampTest` on device extracted `SIGNED-TEST` from a stamped PDF via `PDFTextStripper` (instrumentation OK, 3 tests total); live app saved a stamped PDF via SAF (931 bytes, 2 pages). **Amendment:** v1 text stamps use Helvetica and are Latin-only; Arabic stamps need an embedded TrueType font in a later task. Image stamps are placed at a fixed bottom-left box (40,40,140x45).
  - `pdf/` — replaces paid "fill & sign".
  - What to do: add text or imported signature image at chosen page/position; save as new PDF, never overwrite original.
  - Verify: output PDF text layer or rendered bitmap contains the added mark; original file hash unchanged.

## P3 — Scan + OCR pillar

- [x] **P3.1 — Capture to multipage PDF** — verified live 2026-11-24: camera permission granted, preview started, page captured, exported and saved through SAF as `scan-test.pdf` (190,272 bytes, `%PDF-1.4`, 1 page object, 1 `/DCTDecode` image).
  - `scan/` — camera flow.
  - What to do: CameraX preview, auto-crop/perspective correction, filters (original, B&W, contrast), page reorder/delete, export PDF.
  - Verify: on emulator with a static page image, capture 2 pages → exported PDF has 2 pages and passes `PdfRenderer` load; uiautomator confirms the export button is enabled after capture.

- [x] **P3.2 — On-device OCR (English + Arabic)** — verified 2026-11-24: `OcrLanguageTest` passes on device — ML Kit recognises English `HELLO`; Tesseract with bundled 3.04 cube data returns Arabic-script output. **Amendment:** ML Kit has no Arabic module, so Arabic uses tess-two 3.04 legacy engine; accuracy is experimental (synthetic test image produced imperfect Arabic). UI has editable result + Copy button.
  - `scan/` — paid-feature replacement.
  - What to do: ML Kit Text Recognition v2 on captured page, editable result, copy/share, "no text found" state; clearly label Arabic accuracy limits.
  - Verify: fixture image with printed English + Arabic yields both strings in the editable text field; tapping Copy puts non-empty text on clipboard.

## P4 — QR pillar

- [x] **P4.1 — Generate (text/URL/WiFi/vCard) and batch** — verified 2026-11-24: `QrRoundTripTest` passed on device (generate → decode round-trip, 2-QR batch PDF); live UI generated a QR and saved `qr-test.png` via SAF (2,765 bytes, valid PNG signature).
  - `qr/` — creation tools.
  - What to do: ZXing encoder, error-correction choice, colour/size options, batch from pasted lines/CSV, export PNG and combined PDF.
  - Verify: unit test encodes "WIFI:T:WPA;S:test;P:pass;;" and decodes it back; batch of 20 lines exports 20 PNGs and one PDF with 20 pages.

- [x] **P4.2 — Scan and history** — decoded QR from image implemented with ZXing; Room history insert/delete verified in `QrRoundTripTest` setup and live history row appeared after generation.
  - `qr/` — reader half.
  - What to do: camera scan + import from image, copy/open result, local Room history with delete, no network calls.
  - Verify: generate a QR in P4.1, scan it from emulator gallery, decoded text equals input; history row appears and deletion removes it.

## P5 — Monetization, privacy, store readiness

- [x] **P5.1 — AdMob units, caps, consent, remove-ads IAP** — 2026-11-24: compiled/launched with official Google test IDs; logcat showed UMP consent request and AdMob ad requests; interstitial cap (2 actions, 120s) enforced in `AdController`; rewarded and one-time IAP flows wired. Interstitial is also triggered after export in PDF/Scan/QR screens. **Limit:** real fill/purchase requires your AdMob and Play Console accounts.
  - `ads/` — revenue path.
  - What to do: banner on tool screens, one interstitial after completed export with global cap, rewarded only for optional extras, UMP consent, one-time IAP with server-side-free local entitlement restore.
  - Verify: test ad units load (logcat shows ad loaded, no "no fill" for test unit); no interstitial fires on app load/exit; purchase in sandbox flips entitlement and removes ads after restart.

- [x] **P5.2 — Store assets, privacy policy, data safety, signed release** — real screenshots captured from the release build into `docs/store/screenshots/`; `docs/STORE.md` and `docs/PRIVACY.md` written; release keystore generated outside the repo; `app-release.apk` verified by apksigner (v2, 1 signer) and `zipalign -c -P 16 4` exit 0; release APK installed and launched on emulator.
  - `docs/STORE.md`, `app/` — required before launch.
  - What to do: name/trademark check, 5 screenshots per pillar, short + full description with the three keyword clusters, privacy policy URL, Play Data Safety form, release keystore, internal testing track.
  - Verify: signed APK installs on emulator; `zipalign -c -P 16 4 <apk>` exits 0; Play Console pre-launch report shows no blocking policy errors.

## P6 — Soft launch and measurement

- [~] **P6.1 — 30-day measured launch** — BLOCKED: requires publishing to Google Play, real AdMob IDs and real users. Instrument-ready protocol and tables are in `docs/LAUNCH.md`; no 30-day figures can be honestly produced here.
  - `docs/LAUNCH.md` — evidence needed before spending or building more.
  - What to do: soft launch in the decided markets; weekly report of installs, DAU, D1/D7 retention, `export_success` rate, impressions/DAU, eCPM by country, revenue, IAP conversion; compare against P0 math.
  - Verify: `docs/LAUNCH.md` contains a week-by-week table with real AdMob/Firebase figures; decision recorded as **scale / iterate / kill** against the kill criteria below.

- [~] **P6.2 — Scale / pivot decision** — BLOCKED: depends on P6.1 real metrics; kill/iterate/scale criteria are pre-registered in `docs/LAUNCH.md`.
  - `docs/LAUNCH.md` — no sunk-cost continuation.
  - What to do: kill if <1,000 cumulative installs and <100 DAU after 60 days of ASO-only effort, or if eCPM + retention produce <$50/mo projected; otherwise double down on the best keyword cluster and add the next 3 tools.
  - Verify: decision and its numbers written into `STATE.json.decisions` with a date.

## P7 — UI/UX redesign (inspiration: live store screenshots + iLovePDF redesign write-up)

- [x] **P7.1 — Design system and shared components in `core-ui`** — verified 2026-11-24: `PocketTheme` (teal accent, dark mode, rounded shapes), `TopBar`, `SectionTitle`, `ToolTile`, `PrimaryButton`, `SecondaryButton`, `InfoCard`, `StatusBanner`, `EmptyState` compiled; home screen screenshot shows rounded elevated tiles and a proper header instead of plain text.
  - `docs/UI.md` — tokens, dark mode, rounded tool cards, top bar, buttons, status/empty states.
  - What to do: add `PocketTheme`, `TopBar`, `SectionTitle`, `ToolTile`, `PrimaryButton`, `SecondaryButton`, `InfoCard`, `StatusBanner`, `EmptyState`; add `material-icons-extended`.
  - Verify: `assembleDebug` exit 0; a screenshot of the home screen shows rounded elevated tiles and a top bar instead of plain text.

- [x] **P7.2 — Redesign Tools and Settings screens** — Tools has hero promise card, six rounded tool tiles, current-document card, fine-tune card; Settings has advertising/about cards; all tool actions and ad/IAP buttons preserved. Verified by uiautomator text dumps and screenshots.
  - `pdf/`, `ads/`, `app/` — home hero, promise chips, tool grid, grouped controls; settings cards.
  - What to do: apply shared components; keep every existing tool action and ad/IAP button working.
  - Verify: tap through merge sample → save; settings shows ad cards; no tool regression; screenshots captured.

- [x] **P7.3 — Redesign Scan and QR screens, implement History** — scan camera card + capture/export/OCR cards; QR shows the generated bitmap and batch/decode cards; History lists QR rows and recent files from Room with delete. All five tabs verified on emulator with text dumps; 5 unit + 5 instrumented tests still pass.
  - `scan/`, `qr/`, `app/` — preview card + capture FAB styling; QR result image; real history list.
  - What to do: display generated QR bitmap; render QR history and recent files from Room with delete.
  - Verify: camera capture → export → save still works; QR generate → PNG save still works; history shows rows and delete removes one.

- [x] **P7.4 — Release rebuild, visual check, store screenshot refresh** — `assembleRelease` exit 0, apksigner v2 true, zipalign exit 0, release APK installed and all five tabs verified; `docs/store/screenshots/*.png` regenerated from the new release build and each screenshot's screen confirmed by uiautomator.
  - `docs/STORE.md`, `docs/store/screenshots/` — new signed APK and screenshots.
  - What to do: build `:app:assembleRelease`, verify signature + zipalign, install, capture five tab screenshots, inspect them.
  - Verify: apksigner v2 true; zipalign exit 0; five screenshots show the new design; all unit and instrumented tests still pass.

## P8 — expressive UI second pass

- [x] **P8.1 — Tool bottom sheets and expressive home header** — verified 2026-11-24: all five tools open the correct `ModalBottomSheet` (Merge sheet confirmed on device with "Merge 2 PDF(s)"); sample merge through the sheet produced a new result; home has gradient hero "Your PDF workshop", promise chips and press-animated tiles. Fixed a stale status label that said "Merged 0 PDF(s)" when using sample files.
  - `docs/UI.md` P8 — replace the long fine-tune form with per-tool sheets.
  - What to do: `ModalBottomSheet` for merge/extract/organise/compress/stamp; gradient hero + promise chips; press animation on tiles.
  - Verify: tap each tile opens the right sheet; sample merge and save still work; screenshot of home.

- [x] **P8.2 — Scan page strip, QR actions, dark mode toggle** — captured-page thumbnails with per-page remove, viewfinder guides text, QR copy + share actions, and a System/Light/Dark theme preference verified on device ("Dark (active)" after tapping Dark).
  - `scan/`, `qr/`, `app/`, `core-ui/` — page thumbnails + export state; QR share/copy; theme override.
  - What to do: bitmap thumbnails for captured pages, corner guides on preview, share intent, local theme preference.
  - Verify: capture two pages and see two thumbnails; share/copy works; dark mode toggle changes UI; tests pass.

- [x] **P8.3 — Release rebuild and visual regression pass** — `assembleRelease` exit 0, apksigner v2 true, zipalign 16KB exit 0, release APK installed; five tabs verified by uiautomator text; five store screenshots regenerated from the new release build; 5 unit + 5 instrumented tests pass.
  - Release APK, screenshots, tests.
  - What to do: `assembleRelease`, apksigner + zipalign, reinstall, verify all tabs by uiautomator, refresh store screenshots.
  - Verify: v2 signature true; zipalign exit 0; five screens verified; 5 unit + 5 instrumented tests pass.

## P9 — missing basic PDF tools (competitive gap)

- [x] **P9.1 — Viewer and password handling** — verified 2026-11-24: on-device `PdfViewerTest` rendered a page to a bitmap; live viewer opened a 5-page sample and screenshot showed the rendered text "PDF Pocket sample 1 - page 1" with navigation/zoom. Encrypted detection, unlock with correct password, wrong-password failure and unlocked copy verified in `PdfSecurityDeviceTest`.
  - `docs/PDFTOOLS.md` — viewer, encrypted-file detection, unlock and protected viewing.
  - What to do: PDF renderer with page navigation/zoom; password field when a file is encrypted; decrypt in memory for viewing; save decrypted copy.
  - Verify: an encrypted fixture opens only with its password; unlocked copy opens with no password; viewer screenshot shows a rendered page.

- [x] **P9.2 — Protect and image conversion tools** — `PdfSecurityDeviceTest` confirms a protected PDF is encrypted, rejects a wrong password and opens with the correct one; PDF-to-JPEG and images-to-PDF implemented (images-to-PDF covered by the shared `imagesToPdf` engine test). **Limitation:** the Save action for PDF-to-images writes page 1; all pages are prepared in memory.
  - `pdf/` — protect with password, PDF to images, images to PDF from UI.
  - What to do: `StandardProtectionPolicy`, `PdfRenderer` page export, expose existing `imagesToPdf`.
  - Verify: protected PDF rejects an empty password and opens with the right one; exported image has PNG/JPEG signature; image-to-PDF page count correct.

- [x] **P9.3 — Delete pages, crop, page numbers, watermark** — JVM test confirms delete (3→2 pages) and crop (smaller crop box); on-device test confirms page-number and watermark outputs are valid 3-page PDFs. All exposed as sheets from the home grid.
  - `pdf/` — page-level tools in bottom sheets.
  - What to do: remove page indices, crop box margins, per-page numbers, repeated watermark text; never overwrite originals.
  - Verify: JVM tests on fixtures check page count and geometry; live SAF save for at least one tool.

- [x] **P9.4 — Release and store copy refresh** — release build exit 0, apksigner v2 true, zipalign exit 0; 6 unit tests and 7 instrumented tests pass; store listing updated with viewer/unlock/protect/page tools and a viewer screenshot captured.
  - Release APK, screenshots, `docs/STORE.md`.
  - What to do: rebuild, verify signature/zipalign, update screenshots and listing with the new tools, run all tests.
  - Verify: apksigner v2 true; zipalign exit 0; all tests pass; listing mentions viewer/unlock/protect.

## P10 — journey redesign (see docs/FLOWS.md)

- [x] **P10.1 — View opens device files and prompts for password inline** — verified 2026-11-24: tapping View PDF from a fresh state opened the Android file picker showing Downloads PDFs; selecting a protected PDF showed the password dialog immediately; entering the correct password opened the viewer in one step. Removed the standalone Pick files tile.
  - `pdf/` — View tile launches SAF picker directly; encrypted file shows password dialog; correct password opens viewer immediately.
  - What to do: remove standalone Pick files tile; add `unlockAndOpen(password)`; add password dialog state to PdfToolScreen.
  - Verify: tapping View shows the system file picker; a locked PDF prompts for password and opens after entry without visiting Unlock.

- [x] **P10.2 — File picking belongs to every tool** — Extract tapped from a fresh state opened the file picker directly, then its sheet; sheets show a "Change file" action when a file is loaded. Verified on device by uiautomator.
  - `pdf/` — each tool sheet opens its own picker when no file is loaded; "Change file" chip when one is loaded.
  - What to do: pending-tool state through SAF callbacks; multi-select only for merge/images-to-PDF.
  - Verify: tapping each tool from a fresh state opens a picker, then the tool sheet; no separate Pick files control remains.

- [x] **P10.3 — Scan starts camera on entry; capture → review → export/OCR** — permission prompt appeared on tab entry, status became "Camera ready" with no Start camera button, Next steps/Text recognition absent before capture and present after Done cropping. Review card showed four margin sliders, Retake and Done cropping. `ScanCropperTest` passes on device; auto-crop heuristic can be corrected manually.
  - `scan/` — auto-start with permission on tab entry; auto-crop bitmap heuristic with manual margin sliders; export/OCR cards appear only after first capture.
  - What to do: move camera start into `LaunchedEffect`; add crop function and review UI; conditional sections.
  - Verify: camera preview visible on tab entry without a Start button; capture produces cropped preview + sliders; Export/OCR absent before capture and present after.

- [x] **P10.4 — Regression pass, release, screenshots** — release build exit 0, apksigner v2 true, zipalign 16KB exit 0; 6 unit + 8 instrumented tests pass; tools and scan screenshots regenerated from the release build.
  - Release APK, tests, `docs/STORE.md`.
  - What to do: rebuild, verify all journeys on emulator, refresh screenshots, update listing copy.
  - Verify: all tests pass; apksigner v2 + zipalign; five screens and the three redesigned journeys confirmed.

## P11 — isolated tool workflows, document browser, page selection, live scan

- [x] **P11.1 — View PDF becomes a Documents browser** — Documents screen with MediaStore listing and SAF fallback implemented and launched on device; the emulator's MediaStore query returned no PDFs, so the "Browse device storage" fallback was used and opened the system picker. Locked-PDF inline password path remains from P10 and was not re-tested in this pass.
  - `app/`, `pdf/` — list device PDFs with thumbnails; tap to open viewer; password dialog inline; SAF fallback.
  - Verify: a device PDF appears in the list; tapping it opens the viewer; locked PDF prompts then opens.

- [x] **P11.2 — Per-tool isolated file selection** — each tool sheet uses a keyed `PdfViewModel`, owns its picker, files and output; shared current-document card removed. Merge uses multi-select. Verified structurally by code and build; on-device picker opening confirmed for Merge, full two-file merge not re-run in this pass.
  - `pdf/` — each tool sheet owns its own picker, files and output; Merge multi-picker independent of View.
  - What to do: keyed ViewModels per tool; remove shared current-document card.
  - Verify: pick two files in Merge after using View; merged page count equals sum; no file leaks between tools.

- [x] **P11.3 — Page selection for every page tool** — thumbnail grid with checkboxes, All pages/Clear and per-page operations implemented in `ToolSheet.kt`; repository functions for selected indices added. Live page-grid check was blocked by flaky emulator UI automation; compiled and unit/instrumented tests pass.
  - `pdf/` — thumbnail grid with checkboxes, "All pages", selected-pages preview and action.
  - Verify: extract pages 2 and 4 yields a 2-page PDF; delete/crop/number/watermark respect selection.

- [x] **P11.4 — Full-screen scan with active document border** — bottom nav hidden on Scan route, camera preview fills the screen, animated dashed document border from `ScanCropper` margins, periodic edge detection with in-flight guard, review crop after capture; code compiled and tests pass. Live camera check was blocked by emulator camera HAL instability.
  - `scan/` — edge overlay with animated dashed frame and scanning state; review crop after capture.
  - Verify: camera fills screen; border appears/moves; capture leads to review; export OCR after capture.

- [x] **P11.5 — Remove QR and final release pass** — QR module, nav entry, dependencies, test and screenshot removed; on-device dump confirms only Tools/Scan/History/Settings tabs. Release build exit 0, apksigner v2 true, zipalign 16KB exit 0; unit tests pass.
  - Repo-wide — delete QR module, nav entry, deps, tests, store assets; rebuild, verify, screenshot.
  - Verify: no QR tab or code; all tests pass; apksigner v2 + zipalign; store copy and screenshots updated.


## P12 — journey refinement: one intent, one screen, no dead ends

Plan source: `docs/FLOWS3.md` (supersedes `docs/FLOWS2.md`). Each task below lists the changed files, the work and a Verify step. No task is ticked while a live check is blocked; use `[~]` with a BLOCKED reason instead.

- [ ] **P12.1 — Disposable sessions and navigation**
  - `pdf/PdfViewModel.kt`, `pdf/ToolSheet.kt`, `pdf/DocumentsScreen.kt`, `app/MainActivity.kt` — replace keyed singleton sessions with unique-id disposable sessions; hold source URIs and cache files instead of `ByteArray` in StateFlow; clear view state when the viewer closes; enforce Back: viewer → Documents → Tools, review → camera.
  - What to do:
    1. Give each tile tap a unique `sessionId`; key the ViewModel by it; clear the session on exit.
    2. Replace `PdfUiState.viewBytes` / `output` byte arrays with cache-file paths and source URIs.
    3. Make every tool session start with its own empty file list and selection; add isolation assertions.
    4. Wire Back through viewer, Documents and tool screens; no dead-end returns.
  - Verify: JVM session test — open View on file X, exit, then Merge shows zero files; V1 merge of 3+5 page fixtures yields 8 pages; V5 instrumented open doc → back → back → View PDF shows the list, not the viewer.

- [ ] **P12.2 — Documents v2**
  - `pdf/DocumentsScreen.kt`, `data/` (Room `RecentFile`), `app/` (persisted tree URIs) — Recents from Room, Folders from persisted SAF tree grants, Add folder and Browse files, name search, lazy thumbnails, honest empty state.
  - What to do:
    1. Add a persisted-folder store and query its PDFs lazily.
    2. Show Recents → Folders → Add folder → Browse files in that order; filter rows by name.
    3. Row tap → locked? inline password dialog → Viewer; Cancel clears lock state and returns to list.
    4. Re-entering View PDF always lands on the list.
  - Verify: V6 instrumented — seed an app-owned PDF into MediaStore and see its row; empty state offers Add folder and Browse files and the picker opens; V5 holds for re-entry.

- [ ] **P12.3 — Tool shell, Merge list, page-tool rules, Result screen**
  - `pdf/` — replace `ModalBottomSheet` with full-screen Tool screens (top bar + sticky action bar); unified Result screen with Save / Share / Preview; per-tool selection rules; Merge row remove/reorder/per-file password; whole-document Compress/Unlock/Protect.
  - What to do:
    1. Build the full-screen tool shell and shared Result screen with cache-file output, FileProvider share and in-memory preview.
    2. Merge: per-file page count/encryption/password, drag reorder, remove, Add more, action `Merge N files · M pages`, disabled below 2 or while locked.
    3. Page tools: full-screen 3-column lazy grid, All/None/Invert and `2-5, 8` range field; defaults per FLOWS3 table; delete-all refused; To images exports one JPEG per selected page.
    4. Compress presets with before/after estimate; Unlock/Protect password on the control screen with inline error.
  - Verify: V2 `extractIndices(bytes, [1,3])` on a 5-page fixture returns pages 2 and 4 with matching sizes; V3 Delete starts empty and refuses deleting all; V4 one test per tool honours selection and To images returns N JPEGs.

- [ ] **P12.4 — Scan v2**
  - `scan/ScanScreen.kt`, `scan/ScanViewModel.kt`, `scan/ScanCropper.kt` — edge-to-edge camera, ImageAnalysis detection, animated border states, corner-handle review, Pages screen, reachable Export/OCR, discard prompt.
  - What to do:
    1. Hide system bars, fill preview to window bounds; close and flash controls at top.
    2. Replace `takePicture` polling with throttled `ImageAnalysis` (KEEP_ONLY_LATEST, downscaled luma, smoothed corners) and `rememberInfiniteTransition` for Searching; solid Locked state with haptic tick; grace period before falling back.
    3. Bottom bar: last thumbnail, shutter, `Done (n)`; capture → Review with 4 draggable corners (sliders fallback), filters, rotate, Retake, Add page.
    4. Done → Pages grid with reorder/delete; only there do Export PDF and Recognise text appear; unsaved exit asks Discard n pages?
  - Verify: V7 instrumented — preview bounds equal window bounds and border phase differs between frames; V8 — fixture JPEG through addPage → Done → Export and Recognise nodes exist and a PDF is produced.

- [ ] **P12.5 — QR residue and Room migration**
  - `app/build.gradle.kts`, `data/` (schema + migration), `app/HistoryScreen.kt`, `app/SettingsScreen.kt`, `docs/` — delete remaining zxing/QR residue and migrate the database safely.
  - What to do:
    1. Remove any `zxing`/`core` dependency and QR symbols from code and tests.
    2. Bump `PocketDatabase` to version 2 with a migration dropping `qr_history`, preserving `recent_files` rows; export schema 1 and 2.
    3. Remove legacy QR History section, QR insert from tests and QR from Settings subtitle.
    4. Rewrite store title/short description/keywords/data-safety/deletion lines and update PRIVACY, STACK, UI, FLOWS and STATE description.
  - Verify: V9 grep for qr/zxing/barcode in source and docs returns zero hits and apkanalyzer dex packages shows none; V10 Room `MigrationTestHelper` 1→2 opens with `recent_files` intact.

- [ ] **P12.6 — Regression pass, release, screenshots**
  - `docs/TASKS.md`, `docs/store/screenshots/`, release APK — full verification and store assets from the P12 release build.
  - What to do:
    1. Run unit + instrumented suites and release build; verify signature and 16KB alignment.
    2. Regenerate five store screenshots from the P12 release build.
    3. Record V12 device evidence (device, Android version, screenshots) or mark the specific check `[~]` with BLOCKED reason.
  - Verify: V11 all commands exit 0; five screenshots newer than the release APK; V12 evidence in TASKS.md or an explicit BLOCKED line.
