# P11 journey redesign — isolated tools, document browser, page selection, live scan

User feedback (2026-11-25): View PDF behaved like a file picker; Merge reused View's file and could not select several; Extract/other page tools gave no page selection; Scan was not a full-screen scanning experience; QR is unrelated to PDF and must be removed.

## Design decisions

1. **View PDF = Documents screen.** Tap View PDF → in-app browser listing PDFs from device storage (thumbnails + name + size). Tap a row → viewer. If the PDF is locked → password dialog → viewer. A "Browse device" button opens SAF as a fallback when MediaStore returns nothing.
2. **Every tool owns its own file selection.** No shared "current document" state. Each tool sheet launches its own picker (multi-select for Merge) and keeps its own output. A file chosen in View/Extract is never reused by Merge.
3. **Page tools show the whole PDF first.** Extract, Delete pages, Organise, Crop, Number pages, Watermark and To images all open a page grid with thumbnails and checkboxes; the operation applies to the selected pages. "All pages" is a single tap.
4. **Merge opens a multi-file picker first**, showing the chosen files with remove buttons and reorder, then merges exactly those.
5. **Scan = full-screen camera.** No cards around the preview. Active document bordering: dashed/animated frame following detected bright margins, "Scanning for edges…" state, then captured-page review with crop sliders. Export/OCR only after capture.
6. **QR removed** from navigation, dependencies, tests, store copy and screenshots.

## Journeys

- View: View PDF → Documents list → PDF row → [password if locked] → viewer pages with zoom.
- Merge: tile → multi-picker → chosen-files list → Merge → Save.
- Page tool: tile → picker → page grid (all selected by default) → adjust selection → action → Save.
- Unlock/Protect/Images-to-PDF: tile → picker → password/image inputs → action → Save.
- Scan: tab → full-screen camera + edge border → Capture → review crop → Add page / Retake → Export or OCR.

## Verification required

- Selecting two files in Merge produces a merged PDF with the sum of their page counts, independent of any View selection.
- Extract with pages 2 and 4 selected produces a 2-page PDF.
- Documents list shows at least one device PDF (or the SAF fallback opens).
- Scan screen fills the display and shows the animated border/scanning state.
- QR tab and module are absent from the built app.
- All unit/instrumented tests pass; signed release verified; screenshots refreshed.
