# Free-vs-paid promise — validated 2026-11-23

The wedge must survive contact with free incumbents. Sources below are incumbent-owned help/pricing pages or the live store listing; rating counts are from the iTunes Search API check recorded in `PLAN.md` and proxy installs, not revenue.

| Incumbent | Measured App Store ratings (AE / US) | What free users actually get | Their paywall / limit | Our promise |
|---|---|---|---|---|
| CamScanner | 122,444 / 1,990,746 | Scan, crop, enhance, share | FAQ: removing the "Created by CamScanner" QR/watermark requires the paid version; OCR and collage are paid | Export with **no watermark**, no account, offline |
| Adobe Scan | 38,587 / 1,611,201 | Scan to PDF, auto-crop, no watermark on free files per third-party review | Scans saved to Adobe Document Cloud (account); free text recognition limited; subscription for extra scanning power | **No account**, no cloud upload, full text export in v1 languages |
| iScanner | 38,959 / 1,412,696 | Scanning and some PDF editing free | Pro paywall: "OCR in 30+ languages, no ads, no scan limits" | **No scan limit**, no ads blocking the tool, on-device OCR |
| iLovePDF | 11,761 (AE) | Essential tools, limited processing | Premium $5/mo annual, $9 monthly; full tool access and unlimited processing paid | Merge/split/compress/sign **unlimited and offline** |
| QR Code Reader · | 11,898 / 670,256 | Scan and basic generate | Pro/Premium packs $2.99–$29.99 (paywall library), ads in free tier | Batch QR generation and export free, ads confined to result screens |

Sources: [CamScanner FAQ](https://www.camscanner.com/faq), [Adobe Scan FAQ](https://helpx.adobe.com/mobile-apps/help/adobe-scan-faq.html), [Adobe Scan Play listing](https://play.google.com/store/apps/details?id=com.adobe.scan.android), [iScanner paywall](https://promo.iscanner.com/paywall), [iLovePDF pricing](https://www.ilovepdf.com/pricing), [QR paywall library](https://adapty.io/paywall-library/qr-code-reader/).

## Defensible promise (v1)

1. No account, no sign-in, no upload: all processing on-device, files stay on the phone.
2. No watermark on any export, ever.
3. No daily scan/export limit.
4. All three pillars in one install: PDF tools, scan→PDF→OCR, QR create/scan.
5. Ads pay for the app; a one-time purchase removes them. No forced subscription.

## What is NOT claimed

- Same OCR accuracy as paid cloud OCR; v1 is English + Arabic on-device with editable output.
- Same PDF feature depth as Acrobat (no text reflow in v1).
- No fake "faster/better than Adobe" marketing claims in store copy — only the five concrete promises above.

Gate result: **defensible**. The five promises are all deliverable on-device with the P0.1 verified stack; each maps to a specific incumbent limit.
