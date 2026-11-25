# Missing basic PDF tools — competitive gap and P9 plan

Research source: live tool catalogues read from the three sites on 2026-11-24 (Sejda, Smallpdf, iLovePDF — see `docs/UI.md` style for evidence labelling). Measured: all three list core "basic" tools that PDF Pocket lacks.

| Tool | Sejda | Smallpdf | iLovePDF | PDF Pocket today | Feasible on-device? |
|---|---|---|---|---|---|
| PDF viewer / reader | PDF Editor | PDF Reader | — | **Missing** | Yes — `PdfRenderer` |
| Unlock / remove password | Unlock | Unlock | Unlock | **Missing** | Yes — PDFBox decrypt with user password |
| Open/view password-protected PDF | implied | implied | implied | **Missing** | Yes — decrypt in memory, then render |
| Protect with password | Protect | Protect | Protect PDF | **Missing** | Yes — `StandardProtectionPolicy` |
| PDF to JPG/PNG | PDF To JPG | PDF to JPG/PNG | PDF to JPG | **Missing** | Yes — `PdfRenderer` + JPEG |
| JPG/PNG to PDF | JPG To PDF | JPG to PDF | JPG to PDF | Engine exists, no UI | Yes |
| Delete pages | Delete Pages | Delete PDF Pages | Organize PDF | **Missing** | Yes |
| Crop | Crop | Crop PDF | Crop PDF | **Missing** | Yes — `PDPage.setCropBox` |
| Page numbers | Page Numbers | Number Pages | Page numbers | **Missing** | Yes — text stamp per page |
| Watermark | Watermark | Watermark PDF | Watermark | Partial (text stamp) | Yes |

## Scope for P9 (offline equivalents, not cloud conversions)

- **P9.1 Viewer + password handling:** PDF viewer with page navigation/zoom; detect encrypted files; enter password to view; unlock (save decrypted copy).
- **P9.2 Protect and export:** password-protect a PDF; export pages as images; images-to-PDF from the UI.
- **P9.3 Page basics:** delete pages, crop margins, add page numbers, text watermark.
- **P9.4 Verification:** JVM tests for decrypt/protect/delete/crop/watermark; on-device viewer test; release rebuild, screenshots, store copy update.

Non-goals for this pass: PDF↔Word/Excel/PPT (layout fidelity needs cloud/heavy engines), redaction, OCR searchable-layer writeback, PDF/A.

Each task must be backed by real output: a decrypted PDF that opens without a password, an encrypted PDF that rejects an empty password, page counts before/after, and viewport text/screenshots for the viewer.
