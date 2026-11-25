# P12 — journey refinement: one intent, one screen, no dead ends

Supersedes `docs/FLOWS2.md` wherever they differ. Written 2026-11-26 after reading the P11 build against FLOWS2. Drop it into `docs/` and add the P12 tasks (bottom) to `docs/TASKS.md` before any code edit.

## Why a second pass

FLOWS2 set the right direction (isolated tools, Documents browser, page grid, full-screen scan, no QR). The P11 build only partly delivers it:

| # | What the user hits | Cause in code |
|---|---|---|
| 1 | Scan has no way to Export or run OCR | Export/OCR sit inside `if (reviewIndex == null)` nested in the branch that only renders when `reviewIndex != null` (`ScanScreen.kt`) |
| 2 | Scan border never moves | `animateFloatAsState` starts at its target; no animation runs |
| 3 | Tapping View PDF a second time can jump straight into the last document | The `"viewer"` ViewModel keeps `viewBytes`; `DocumentsScreen` opens the viewer on first composition |
| 4 | Documents list is probably empty on Android 11+ | `MediaStore.Files` query with no storage permission only returns files the app owns |
| 5 | Delete pages deletes everything in one tap | Page grid selects all pages by default, including for Delete |
| 6 | Merge shows file names but no remove/reorder; only the first file is checked for a password | `addFiles` reads page count, encryption and bytes from `newUris.first()` only |
| 7 | Compress, Reverse ignore the selection; To images saves only one JPEG | `compress()`/`reverse()` take no indices; `exportSelectedImages` sets `output = list.firstOrNull()` |
| 8 | QR still in the build | zxing dependency, `QrHistory` entity/DAO, History section, store and privacy copy |

## Principles

1. **One intent, one linear journey:** Pick → Review → Act → Result. No tool starts with a wall of controls.
2. **Every journey ends on the same Result screen:** Save, Share, Preview.
3. **A tool session is disposable.** It starts when the tile is tapped and is discarded on exit. The only thing that survives is the Recents list.
4. **Never offer an action that cannot succeed, or that destroys everything by default.**
5. **Back always closes the top layer** (viewer → Documents → Tools; review → camera).
6. **No new permissions.** SAF only, including persisted folder grants.

## Navigation map

```
Tools tab ─ View PDF ──▶ Documents ──▶ Viewer
          ─ any tool tile ──▶ Tool screen (full screen) ──▶ Result
Scan tab  ─ Camera ──▶ Review ──▶ Pages ──▶ Result
History tab · Settings tab
```

Tool sheets (`ModalBottomSheet`) are replaced by full-screen Tool screens with a top bar (title, back) and one sticky bottom action bar. The page grid needs the room; a 260 dp grid inside a scrolling sheet does not.

## Journeys

### A. View a PDF
`Tools → View PDF` → **Documents**:
- **Recents** (from Room `RecentFile`), then **Folders** (persisted tree grants), then **Add folder** and **Browse files** (system picker).
- Rows: thumbnail, name, size, modified date. Search box filters by name. Thumbnails load only for visible rows.
- Empty state explains why and offers Add folder / Browse files. The list is never presented as "all PDFs on the device" when it cannot be.
- Row tap → locked? **password dialog** (wrong password = inline error, retry; Cancel returns to the list and clears the lock state) → **Viewer** (page N of M, zoom, jump to page).
- Closing the viewer returns to Documents. Re-entering View PDF always lands on the list, never on the last document.

### B. Merge
`Tools → Merge` → multi-file picker → **file list**:
- Each row: thumbnail, name, page count, drag handle, remove. Locked rows show a lock and an inline "Enter password".
- **Add more files** appends. Order is exactly what the list shows, not picker order.
- Action bar: `Merge 3 files · 12 pages`. Disabled below 2 files or while any file is locked.
- → Result.

### C. Page tools
`Tools → tile` → single-file picker → **page grid** (full screen, 3 columns, lazy thumbnails, page numbers) → action → Result.

Selection helpers in the top bar: **All · None · Invert**, and a "Pages" field that accepts `2-5, 8`. Tap toggles; the count is always visible in the action bar.

| Tool | Default selection | Action bar | Behaviour | Empty selection |
|---|---|---|---|---|
| Extract | none | `Extract 2 pages` | New PDF of selected pages, ascending order | Disabled |
| Delete pages | none | `Delete 2 pages` | Removes selected | Disabled; also disabled if it would remove every page |
| Organise | all | `Rotate right` · `Rotate left` · `Reverse all` | Rotation applies to selected; Reverse is labelled whole-document | Rotate disabled |
| Crop | all | `Crop 6 pages` | Margin slider with live preview on the first selected page | Disabled |
| Page numbers | all | `Number 6 pages` | Position chips (bottom centre/right), format `1` or `1 / N`, numbered by original page index | Disabled |
| Watermark | all | `Watermark 6 pages` | Text field, preview on first selected page | Disabled |
| To images | all | `Export 6 images` | One JPEG per selected page, saved into a chosen folder (one page → single file) | Disabled |

**Compress, Unlock and Protect are whole-document tools**, not page tools: picker → one control screen → Result.
- Compress uses presets (Small / Balanced / Best) instead of a raw "JPEG quality" number, and shows estimated before/after size. The lossy, text-layer-dropping behaviour is stated on the screen.
- Unlock and Protect ask for the password on this screen; a wrong password is an inline error.

### D. Images to PDF
`Tools → Images to PDF` → multi-image picker → thumbnail grid with drag reorder and remove → page size (Fit image / A4) → Create → Result.

### E. Scan
`Scan tab` → permission asked on entry (denied: clear state with **Open settings**) → **full-screen camera, edge to edge** (system bars hidden, nothing between the preview and the display edges):
- **Top:** close, flash toggle.
- **Border states:** *Searching* — dashed, animated, label "Scanning for edges…"; *Locked* — solid, brighter, one haptic tick, label "Document found"; lost edges fall back to Searching after a short grace period. Auto-capture is an optional toggle, off by default.
- **Bottom:** thumbnail of the last page, shutter, `Done (n)` once at least one page exists.
- **Capture → Review:** captured page with four draggable corner handles (sliders remain as the fallback), filters (Original / Enhanced / B&W), rotate, **Retake**, **Add page**.
- **Done → Pages:** grid with reorder and delete. Only here and after do **Export PDF** and **Recognise text** appear.
- **Export** → Result with a default name `scan-YYYYMMDD-HHmm.pdf`. **Recognise text** → editable text per page, Copy, Share.
- Leaving with unsaved pages asks "Discard n pages?".

### F. Result screen (shared)
Shown after every action. Shows what was made (name, size, page count), then **Save** (system save dialog), **Share** (cache copy through a `FileProvider`; no new permissions), **Preview** (opens the in-memory result in the viewer).

Default output names derive from the input: `report-merged.pdf`, `report-extracted.pdf`, `report-pages-removed.pdf`, `report-rotated.pdf`, `report-compressed.pdf`, `report-cropped.pdf`, `report-numbered.pdf`, `report-watermarked.pdf`, `report-unlocked.pdf`, `report-protected.pdf`, `report-page-3.jpg`.

Ads follow `docs/ADS.md`: at most one interstitial per two completed actions, shown on the Result screen after a successful Save or Share. Never during capture, on Documents, on app load or on exit.

## State and lifecycle rules

- **Sessions, not keyed singletons.** Each tile tap creates a fresh session (`viewModel(key = <unique id>)`) and discards it on exit. Today's `viewModel(key = "tool-MERGE")` survives dismissal, so the old files and output reappear.
- **Isolation is a test, not a convention.** Opening View on file X and then Merge must show an empty Merge session.
- **Hold URIs and cache files, not `ByteArray`s.** `PdfUiState.viewBytes` and `output` hold whole PDFs in a `StateFlow`; large files will OOM. Keep the source `Uri`, render through `PdfRenderer` on a file descriptor, and write results to a cache file.
- **Per-file, not first-file.** Page count, encryption and password are tracked per file.
- **Closing the viewer clears view state** (`viewBytes`, `needsPassword`).
- **Scan edge detection uses `ImageAnalysis`** (`STRATEGY_KEEP_ONLY_LATEST`, downscaled luma plane, throttled to roughly 5 fps, corners smoothed between frames). The overlay is mapped through the `PreviewView` transform and `rotationDegrees`. No `takePicture` polling.
- **The border animation uses `rememberInfiniteTransition`**, not `animateFloatAsState`.

## QR removal — finish the job

- Delete `com.google.zxing:core` from `app/build.gradle.kts`.
- Remove `QrHistory`, `insertQr`, `qrHistory`, `deleteQr`. **Bump `PocketDatabase` to version 2 with a migration that drops `qr_history`** (or reset the schema if no build with the old database was ever installed). Keeping `version = 1` with the entity removed crashes on devices holding the old file.
- Remove the "Saved QR codes (legacy)" section from History, the QR insert from `FilePipelineTest`, and "QR codes" from the Settings subtitle.
- Rewrite store title, short description, keywords, data-safety and deletion lines in `docs/STORE.md`; update `docs/PRIVACY.md`, `docs/STACK.md`, `docs/UI.md`, `docs/FLOWS.md` and the `STATE.json` description.
- Regenerate all five store screenshots from the P12 release build.

## Verification

Each item is automated unless marked *device*. Fixtures use **different page sizes per page** so a test can tell which page came out; blank identical pages cannot (that is how the earlier duplicate-page bug hid).

| # | Requirement | How | Pass |
|---|---|---|---|
| V1 | Merge sums page counts, independent of View | JVM test on `PdfRepository.merge` with 3- and 5-page fixtures; session test: open View on X, then Merge | 8 pages; Merge session starts empty |
| V2 | Extract pages 2 and 4 | `extractIndices(bytes, listOf(1, 3))` on a 5-page fixture | 2 pages, page sizes match source pages 2 and 4 |
| V3 | Destructive defaults are safe | Unit test on selection rules | Delete starts with none selected; deleting all pages is refused |
| V4 | Every page tool honours the selection | One test per tool against a fixture | Compress/Reverse labelled whole-document; To images returns N JPEGs for N pages |
| V5 | View always lands on the list | Instrumented UI test: open doc → back → back → View PDF | List visible, viewer not |
| V6 | Documents shows a device PDF or offers a working fallback | Instrumented: seed an app-owned PDF into MediaStore → row appears; empty state shows Add folder and Browse files. *Device:* grant a folder on Android 13+ and see its PDFs | Row appears; picker opens |
| V7 | Scan fills the display and animates | Instrumented: preview bounds equal window bounds; border phase differs between two frames | Equal bounds; phase changes |
| V8 | Scan can export | Instrumented: feed a fixture JPEG through `addPage` → Done → assert Export and Recognise nodes exist and a PDF is produced | Both reachable |
| V9 | QR is gone | `grep -rniE "qr\|zxing\|barcode" --exclude-dir=.git --exclude-dir=build app core-ui data pdf scan ads *.kts docs/STORE.md docs/PRIVACY.md` returns nothing; `apkanalyzer dex packages app-release.apk \| grep -iE "zxing\|barcode"` returns nothing | Zero hits |
| V10 | Old database upgrades | Room `MigrationTestHelper` from schema 1 to 2 | Opens, `recent_files` rows intact |
| V11 | Release | `./gradlew test connectedDebugAndroidTest assembleRelease`; `apksigner verify`; `zipalign -c -P 16 4` | All exit 0 |
| V12 | *Device:* live camera and picker checks | Physical phone or a stable emulator image. Record the device, Android version and a screenshot per check in `TASKS.md` | Evidence attached |

A task is not marked done while any of its live checks is "blocked". Mark it `[~]` with the reason, as the legend in `TASKS.md` already allows.

## P12 task entries for `docs/TASKS.md`

- [ ] **P12.1 — Disposable sessions and navigation** — unique-id sessions, per-file state, URIs instead of byte arrays, viewer clears state on close, back behaviour. Verify V1, V5.
- [ ] **P12.2 — Documents v2** — Recents, persisted folder grants, search, lazy thumbnails, honest empty state. Verify V6.
- [ ] **P12.3 — Tool shell, Merge list, page-tool rules, Result screen** — full-screen Tool screens, selection helpers, per-tool defaults, drag-reorder merge list, Compress/Unlock/Protect as whole-document tools, Save/Share/Preview. Verify V2, V3, V4.
- [ ] **P12.4 — Scan v2** — edge-to-edge camera, `ImageAnalysis` detection, animated border states, corner-handle review, Pages screen, reachable Export/OCR, discard prompt. Verify V7, V8.
- [ ] **P12.5 — QR residue and Room migration** — everything in the QR section above. Verify V9, V10.
- [ ] **P12.6 — Regression pass, release, screenshots** — Verify V11, V12.
