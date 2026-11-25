# PDF Pocket (working name) — combined PDF + Scan/OCR + QR app

Status: **PLAN ONLY — no code written**. Created 2026-11-23 (AST).
Scope: one Android app merging ideas 1 (PDF Toolbox), 2 (Camera Scanner + OCR), 7 (QR maker/reader) from the research shortlist.

## 1. Product definition

One offline-first document utility: **every tool free, no account, no upload, no watermark, no daily limit**. Ads pay for it; an optional one-time purchase removes ads.

| Pillar | v1 tools | Why people return |
|---|---|---|
| PDF | merge, split, rotate, reorder pages, compress, add text/image signature, password-protect (stretch) | weekly document handling |
| Scan + OCR | CameraX capture, auto-crop, filters, multipage → PDF, on-device text extraction, copy/share | students, shops, receipts, contracts |
| QR | create text/URL/WiFi/vCard, batch generate, scan via camera or image, save PNG/PDF, history | weekly |

Shared pipeline: `pick/capture → on-device transform → preview → export/share → interstitial`. One install, three ASO keyword clusters, cross-links between pillars.

## 2. Evidence behind the scope

**Measured (iTunes Search API, AE/US/IN, 2026-11-23)** — rating counts proxy installs, not revenue:
- AE: CamScanner 122,444; iScanner 38,959; Adobe Scan 38,587; QR Code Reader 11,898.
- US: CamScanner 1,990,746; Adobe Scan 1,611,201; iScanner 1,412,696; QR Code Reader 670,256.
- IN: Adobe Scan 1,101,326; iScanner 203,317; QR Code Reader 64,353.

Conclusion: **red ocean**. Only a sharp promise wins: free, offline, unlimited, no watermark. If the promise cannot be delivered on-device, stop and pivot (P0 gate).

**Estimated/modelled (published benchmarks, not this app's results)**:
- AdMob Android eCPM: rewarded $12 / $5 / $1.40; interstitial $8 / $3.20 / $0.90; banner $0.90 / $0.40 / $0.10 for Tier 1 / 2 / 3 audiences ([source](https://www.revenuebyadapp.com/guides/how-much-do-apps-make-from-ads)).
- Utility category multiplier 0.9; iOS ~1.3x Android. Fill 78–90%. December 1.36 vs January 0.78 seasonality (same source).
- Real ad-only comparables: Reampo PDF Converter 1.5M installs / 676k MAU / $10k+ monthly, 100% ads ([AppGoblin](https://appgoblin.info/apps/com.pdf.reader.converter.tools)); PDF Viewer & Scanner 30.7M installs / 1.4M MAU / $10k+ monthly, 100% ads.
- Paid incumbents: Smallpdf ~$15/mo, iLovePDF $5–9/mo, Adobe $19.99+/mo — the price gap is the wedge.

## 3. Revenue model and explicit math

Ad mix assumption: **1 interstitial per completed export + 0.5 rewarded/user/day + 4 banner impressions/user/day**, utility multiplier 0.9.

| Audience | ARPDAU (est) | Revenue per 1,000 DAU/mo | DAU needed for $1,000/mo | Sustained installs/day needed (6-day active life) |
|---|---|---|---|---|
| Tier 1 (US/UK/CA/AU) | $0.01584 | $475 | 2,104 | 351 |
| Blended 30/30/40 | $0.00744 | $223 | 4,478 | 746 |
| Tier 3 (IN/Africa/others) | $0.00180 | $54 | 18,519 | 3,086 |

Formula: `DAU × impressions/user/day × eCPM / 1000 × 30`. All figures are **modelled**, not forecasts. Downloads do not earn; DAU does. Expect near-zero revenue for the first months and a ~43% January drop even with flat DAU.

Placement rules (verify current policy at P0.2): interstitials **between** completed job and result screen only — never on app load or exit; rewarded ads strictly opt-in with a stated reward; optional one-time "Remove ads" IAP (target $2.99–4.99 equivalent, final price after store pricing review); UMP consent for EEA/UK traffic.

## 4. Architecture (single repo, no backend)

- Kotlin + Jetpack Compose, single Activity, Material 3, Navigation Compose.
- Feature modules: `core-ui`, `pdf`, `scan`, `qr`, `ads`, `data`; no server, no account, no upload.
- PDF: on-device library (candidate PDFBox-Android, Apache-2.0) + Android `PdfRenderer`; candidate list must be build-verified in P0.1 before commitment.
- Scan: CameraX + on-device document detection/crop (ML Kit Document Scanner or CameraX + in-app edge detection) + ML Kit Text Recognition v2 (English + Arabic in v1); all on-device.
- QR: ZXing (Apache-2.0) generate + scan; Room for local history/recent files; export via Storage Access Framework (no broad storage permission).
- Ads: AdMob SDK + UMP; Firebase Analytics + Crashlytics; events `tool_used`, `export_success`, `interstitial_shown`, `rewarded_completed`.
- Local env constraints: JDK at `C:\Users\munaa\jdk17`, SDK `C:\Users\munaa\AppData\Local\Android\Sdk`, `buildToolsVersion = "36.1.0"` (34.0.0 is corrupt), Kotlin 2.x needs `org.jetbrains.kotlin.plugin.compose`, Gradle 8.13 distribution must be downloaded/extracted with native paths. Final APK must pass `zipalign -c -P 16 4` for 16KB-page devices.

## 5. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Incumbents (Adobe/CamScanner) already free with generous tiers | Wedge = offline + no account + no watermark + no daily limit; validate in P0.3; kill if not defensible |
| ASO discovery for a 3-in-1 app is weaker than for single-purpose apps | One clear store promise, three keyword clusters, in-app cross-links; measure installs/keyword in P6 |
| PDF library compatibility / licence | P0.1 spike builds and merges two real fixture PDFs before any UI work |
| OCR accuracy on Arabic/low light | v1 English + Arabic only; show confidence and allow manual correction; no silent wrong text |
| Ad policy violations / accidental clicks | Frequency cap, no load/exit interstitials, UMP consent, policy re-check at P0.2 |
| Revenue reality: thousands of DAU needed | Kill criteria in P6; no spend on paid UA before ASO evidence |
| 16KB-page device crash | `zipalign -c -P 16 4 <apk>` on the final artifact, per android-client skill |

## 6. Phase gates

- **G0** stack spike builds, policy checked, promise defensible, platform decided → else stop/pivot.
- **G1** app shell installs and exports a real file on emulator.
- **G2** 6 PDF tools pass fixture tests and open in an external viewer. **[MET 2026-11-23]** merge/extract/rotate/reverse/move/compress covered by JVM tests and live saved PDFs; stamps verified with an on-device text-extraction test.
- **G3** scan → multipage PDF → OCR text copy works on a real captured page.
- **G4** QR create/scan round-trip and batch export work.
- **G5** signed APK installs, ads and IAP verified in test mode, store assets ready.
- **G6** 30-day measured launch report; scale / iterate / kill decision.

Detailed executable tasks, files and Verify steps: `TASKS.md`. Current position and decisions: `STATE.json`.
