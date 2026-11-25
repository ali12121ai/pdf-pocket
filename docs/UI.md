# UI/UX redesign spec — P7

Status: **PLAN ONLY for this phase**. Inspiration reviewed 2026-11-24 from live App Store screenshots and first-party design write-ups.

## Evidence reviewed (measured)

- Downloaded current US App Store screenshots for CamScanner (1.99M ratings), Adobe Scan (1.61M), iScanner (1.41M), QR Reader (1.45M), iLovePDF (52.7k) and Smallpdf. Downloaded files: `C:/tmp/ui-inspo/` (working scratch, not committed).
- **CamScanner screenshot (viewed):** large bold headline with a single teal accent, app screen inside a white card, and a horizontal row of four *rounded filter cards* with selected state shown by a thick teal border and teal label. Accent used only for active/high-priority elements; everything else white/light grey/dark grey.
- **iLovePDF first-party redesign write-up** ([Lessons learned](https://www.ilovepdf.com/blog/lessons-learned-ios-makeover), [app redesign](https://www.ilovepdf.com/blog/iphone-app-version)): replaced stuffed hamburger with a bottom tab bar; settings/account moved to a top-right avatar; switched to white space and lighter colours; added dark mode; found flat square grid tiles did not look tappable and fixed it with **grey negative space, rounded edges and subtle 3D/elevation**; adopted a mobile-optimised type scale; applied the same language to cards, dialogs and buttons. Their closing advice: avoid overcomplicating.

## Design system for PDF Pocket

| Token | Value |
|---|---|
| Accent | Teal `#0B9A8A` (CamScanner-like, used only for active/primary) |
| Accent container | `#D9F2EE` light / `#134F49` dark |
| Surface | White light / `#12181B` dark; cards `#F4F7F7` / `#1C2528` |
| Text | `#10191C` / `#E9F1F0`; secondary `#5A6B6E` / `#A7B8B8` |
| Radius | Cards 20dp, buttons 16dp, tiles 18dp |
| Elevation | Cards 2dp, primary action 4dp |
| Spacing | 16dp screen padding, 12dp between cards, 8dp inside |
| Touch targets | >= 56dp primary button, >= 48dp icon tiles |

Components in `core-ui`: `PocketTheme` (Material 3 + dark mode), `TopBar`, `SectionTitle`, `ToolTile`, `PrimaryButton`, `SecondaryButton`, `InfoCard`, `StatusBanner`, `EmptyState`.

## Screen journeys

1. **Tools (home):** top bar with app name + settings icon; hero card "Everything you need for documents — offline" with three promise chips (No account / No watermark / No limits); "PDF tools" section with a 2-column grid of rounded `ToolTile`s (Merge, Extract, Rotate, Reverse, Move, Compress, Text stamp, Image stamp), each with icon, short subtitle and chevron; below it a "Fine-tune" card containing the existing inputs and buttons, now labelled and grouped; prominent Save result button when output exists.
2. **Scan:** preview inside a rounded card with a document frame overlay; large teal capture button with page-count badge; "Export to PDF" primary card; separate "Text recognition" card with editable result and Copy action.
3. **QR:** input card; result card showing the generated QR bitmap (not just text) with Save PNG; batch card with Save PDF; decode card with history list.
4. **History:** real list from Room (QR history + recent files) with delete actions and an empty state.
5. **Settings:** monetisation card with icon, ads status, rewarded ad, remove-ads, and banner; about/promise card.

## Non-negotiables

- No functionality regressions: every existing tool and SAF export stays wired.
- No new third-party UI framework; Material 3 Compose only, `material-icons-extended` for icons.
- Keep ad policy: no interstitial on load/exit; existing export-triggered caps remain.
- Verify with real screenshots from the release build and all existing tests.

## P8 — second pass: expressive + progressive disclosure (planned 2026-11-24)

Research: [Material 3 Expressive](https://m3.material.io/blog/building-with-m3-expressive/) and [Compose M3 1.5.0](https://m3.material.io/blog/compose-material-3-is-stable) (expressive theming, typography, motion, more shapes/emphasis); [NN/g bottom sheets](https://www.nngroup.com/articles/bottom-sheet/) and [M3 bottom sheets](https://m3.material.io/components/bottom-sheets/guidelines) (contextual controls in a sheet instead of a long scrolling form); [document scanner state checklist](https://dpopstudio.co/blog/document-scanner-app-ui-checklist-capture-crop-export-scanee) (the important screens are capture, page review and export states, not just the viewfinder).

1. **Tool sheets:** tapping a tool tile opens a `ModalBottomSheet` with only that tool's inputs and its primary action. The home screen becomes a calm tool grid + current-document card instead of a long form.
2. **Expressive hero:** gradient hero card, display typography, promise chips, shaped FAB-style capture, animated press states for tiles.
3. **Scan page strip:** captured pages appear as thumbnails with remove buttons and an export state card; viewfinder gets corner guides.
4. **Manual dark mode** toggle in Settings, stored locally; theme follows system by default.
5. **QR result actions:** save PNG, copy and share the decoded value.

Gates: no tool regression, all tests pass, screenshots regenerated from the signed release build.
