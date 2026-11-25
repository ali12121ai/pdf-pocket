# UX flow design — P10 (deep pass)

This is a **journey redesign**, not a defect list. Principle: one user intent = one entry point. File selection, authentication and review are contextual, never separate tools.

## Problems in the current app (measured from the UI and code)

| # | Problem | Root cause |
|---|---|---|
| 1 | "View PDF" does not open device files; it opens a sheet, then uses whatever file another tool picked | View action was added after the tool-picker pattern, so it inherited a two-step flow |
| 2 | Locked PDF cannot be viewed directly; user must first use "Unlock" | Viewer and unlock are separate state paths; viewer checks `needsPassword` but cannot collect the password |
| 3 | "Pick files" is a separate tile even though every tool needs a file | File selection was designed as a tool instead of a precondition |
| 4 | Scan tab opens with a "Start camera" button | Camera permission/start was wired to a click instead of tab entry |
| 5 | Export and OCR controls are visible before any page exists | Sections are always rendered with disabled buttons instead of appearing in the captured state |
| 6 | No crop adjustment for a captured page | Capture went straight into the page list |

## Designed journeys

### A. View a PDF (any file on the device)
`Tools → View PDF` → **system file picker immediately** → file selected:
- not encrypted → **viewer screen** (page 1 of N, next/prev, zoom).
- encrypted → **password dialog** inline → correct password → viewer opens immediately; wrong password → inline error, retry. No trip through the Unlock tile.

### B. Any PDF tool (merge, extract, organise, compress, unlock, protect, to-images, delete, crop, number, watermark)
`Tools → [tool tile]` → if no file loaded, **that tool's file picker opens immediately** (multi-select only for Merge/Images-to-PDF) → tool sheet with only that tool's inputs → action → result card → Save. Once a file is loaded, the sheet shows a "Change file" chip instead of reopening the picker.

### C. Unlock a PDF (save a decrypted copy)
`Tools → Unlock` → picker → password field in the sheet → "Remove password" → result card → Save. Same picker and same password path as journey A; no extra navigation.

### D. Scan a document
`Scan tab` → camera starts **automatically** (permission requested on entry) → viewfinder with **Auto-crop ON** toggle and edge guide → Capture → **Review state**:
- cropped preview + manual margin sliders (top/bottom/left/right) + Retake + Add page
- after at least one page: **Export to PDF** and **Recognise text** appear as the next actions, not before.
`Export` → PDF result card → Save. `Recognise text` → editable result + Copy/Share.

### E. QR create / decode
`QR tab` → type text → Generate → result image with Save/Copy/Share. Decode → pick image → decoded text + history. No changes needed beyond existing flow.

### F. Settings / History
`Settings` → ads, theme, about. `History` → QR rows and recent documents with delete. No changes needed.

## Implementation rules

- Remove the standalone "Pick files" tile; every tool picks its own file.
- Sheet `onDismiss` must not lose a pending action; the picker callback opens the intended sheet.
- Password is collected at the point of need; `unlockAndOpen(password)` decrypts in memory and opens the viewer.
- Camera starts on `ScanScreen` entry via `LaunchedEffect`; permission denial shows a clear "Open settings" state.
- Auto-crop is a real bitmap operation (detect bright page margins), with manual sliders to correct it.
- No new permissions; SAF only, no broad storage access.
- Every changed flow verified on the emulator with uiautomator text and screenshots; existing unit/instrumented tests must still pass.

## Verified outcomes (2026-11-24)

- View PDF from a fresh state → Android picker → locked PDF → inline password dialog → viewer, without visiting Unlock.
- Extract from a fresh state → picker → sheet. No standalone Pick files tile remains.
- Scan tab → automatic permission prompt → camera ready without a Start button; Export/OCR absent until after capture; review crop with four sliders, Retake and Done cropping; Next steps and Text recognition appear after Done.
- 6 unit + 8 instrumented tests pass; signed release APK verified (v2, zipalign 16KB).
- Known limitation: the auto-crop heuristic relies on bright page margins; low-contrast documents need the manual sliders.
