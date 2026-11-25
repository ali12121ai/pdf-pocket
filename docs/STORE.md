# Store readiness — PDF Pocket (working title)

Status: **signed release APK built and verified** (`app-release.apk`, v2 signature, zipalign 16KB). Store assets captured from the running release build on emulator-5554. Publishing still requires your Play Console account and real IDs.

## Listing copy (draft)

**Title:** PDF Pocket: Scan, OCR & QR
**Short description:** Free offline PDF tools, document scanner, OCR and QR codes. No account, no watermark, no limits.

**Full description:**
PDF Pocket puts the tools you use every day in one offline app.


- View PDFs from device storage page by page with zoom — locked files prompt for their password on the spot
- Unlock password-protected PDFs and protect PDFs with a password
- Scan documents with automatic camera start and adjustable auto-crop before export
- PDF: merge, split/extract pages, rotate, reorder, move, delete pages, crop, page numbers, watermark, text and image stamps
- Convert: images to PDF, PDF pages to JPEG; camera scan to PDF; text recognition (English + Arabic)
- Scan: camera capture, multipage export, editable text recognition (English + Arabic)

Everything runs on your device. No sign-in, no upload, no watermark, no daily limit. Ads keep it free; remove them with a one-time purchase.

**Keyword clusters:** pdf editor / merge pdf / split pdf; document scanner / scan to pdf / ocr; qr code generator / qr scanner / qr batch.

## Screenshots

Real captures from the release build (regenerated 2026-11-24 after adding viewer, unlock/protect and page tools): `docs/store/screenshots/tools.png`, `viewer.png`, `scan.png`, `history.png`, `settings.png`.

## Privacy policy

See `docs/PRIVACY.md`. It must be published at a public URL before the Play listing goes live; the URL must be entered in Play Console **and** in the UMP consent form configuration (currently a placeholder in `AdController.requestConsent`).

## Data safety answers (draft)

- **Data collected:** purchase/entitlement state (local Preferences), QR history and recent files (local Room DB), ad interaction events (AdMob SDK), approximate consent state (UMP). No file contents, names, or OCR text leave the device.
- **Purposes:** app functionality, advertising, monetisation.
- **Shared with:** Google AdMob (ads), Google Play Billing (purchase).
- **Encryption in transit:** standard SDK HTTPS.
- **Deletion:** in-app History → X removes QR rows; app uninstall removes all local data. No account means no server-side data to delete.
- **Children:** app is not directed at children; Families policy not applied.

## Placeholders you must replace before release

| Item | Current | Action |
|---|---|---|
| AdMob app ID | `ca-app-pub-3940256099942544~3347511713` (Google test) | Create AdMob app, put real ID in `AndroidManifest.xml` |
| Ad unit IDs | Google test banner/interstitial/rewarded IDs | Replace constants in `AdController` |
| IAP product | `remove_ads` | Create one-time product in Play Console; price it (suggested AED 12–19) |
| Privacy URL | none | Publish `docs/PRIVACY.md` and wire URL into UMP + Play Console |
| Release keystore | `C:\Users\munaa\paperkit-release\pdfpocket.jks` | Back it up securely; losing it means you cannot update the app |
| Store listing | draft above | Play Console: title, screenshots, descriptions, data safety, target countries |

## Release verification performed

- `gradle :app:assembleRelease` → BUILD SUCCESSFUL
- `apksigner verify --verbose`: v2 scheme true, 1 signer, Verified
- `zipalign -c -P 16 4 app-release.apk` → exit 0
- Release APK installed on emulator and launched; all five tabs rendered.
