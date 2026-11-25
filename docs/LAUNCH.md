# Launch measurement plan — P6

The code, ads, IAP stub and signed release are ready. **30-day revenue data cannot be produced in this environment**: it requires publishing to Google Play, real AdMob IDs, a live product, and real users. This document is the instrument-ready measurement protocol; the tables must be filled from Firebase/AdMob after launch.

## Pre-launch checklist

- [x] Firebase Analytics/Crashlytics events defined (`tool_used`, `export_success`, `interstitial_shown`, `rewarded_completed`, `iap_remove_ads`) — currently logged to logcat in `Analytics.kt`; wire Firebase SDK before publishing.
- [x] AdMob SDK + UMP consent + one-time IAP flow compiled and launched on emulator with Google test IDs.
- [x] Signed release APK: v2 signature verified, zipalign 16KB verified.
- [ ] Replace test AdMob app ID and unit IDs with real ones.
- [ ] Create `remove_ads` one-time product in Play Console.
- [ ] Publish privacy policy, enter URL in Play Console and UMP.
- [ ] Upload listing and screenshots; submit for review.

## Measurement protocol (fill after go-live)

| Week | Installs | DAU | D1 ret. | D7 ret. | exports/DAU | impressions/DAU | eCPM by country | Ad revenue | IAP revenue |
|---|---|---|---|---|---|---|---|---|---|
| 1 | | | | | | | | | |
| 2 | | | | | | | | | |
| 3 | | | | | | | | | |
| 4 | | | | | | | | | |

Source of truth: Play Console (installs, ratings), Firebase (DAU, retention, events), AdMob (impressions, eCPM, revenue by country).

## Model to compare against (from PLAN.md)

Ad mix: 1 interstitial per completed export (capped at 1 per 2 actions / 2 minutes), 4 banners + 0.5 rewarded per user/day, utility multiplier 0.9.

| Audience | Est. ARPDAU | Est. revenue / 1,000 DAU / mo | DAU for $1,000/mo |
|---|---|---|---|
| Tier 1 | $0.01584 | $475 | 2,104 |
| Blended | $0.00744 | $223 | 4,478 |
| Tier 3 | $0.00180 | $54 | 18,519 |

These are **modelled**, not forecasts. Expect near-zero revenue for months and a ~43% January ad-eCPM drop.

## Decision gate (P6.2)

- **Kill:** <1,000 cumulative installs and <100 DAU after 60 days of ASO-only effort, or projected ad+IAP revenue <$50/mo.
- **Iterate:** DAU growing but exports/DAU <1 — fix tooling/onboarding before spending on acquisition.
- **Scale:** D7 retention >10%, exports/DAU >2, and blended ARPDAU within 50% of the model — then invest in ASO creative and consider the second app (UAE receipt/VAT scanner).

No paid user acquisition before the iterate/scale signal appears.
