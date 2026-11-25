# Ad flow rules — verified against live policy (2026-11-23)

Sources (read directly, not from memory):
- [AdMob: Disallowed interstitial implementations](https://support.google.com/admob/answer/6201362?hl=en)
- [AdMob: Interstitial ad guidance](https://support.google.com/admob/answer/6066980?hl=en)
- [Google Play: Better Ads Experiences policy](https://support.google.com/googleplay/android-developer/answer/12271244?hl=en)

## Hard rules

1. **No interstitial on app load or app exit.** App-open ad is the only format allowed at load/resume, and only after consent.
2. **No more than one interstitial after every two user actions**, including Back. Our design: show interstitial after a *completed export* only, and only if at least two user actions have happened since the last one.
3. **Never immediately after another interstitial**, and never mid-task (during form entry, camera capture, page reordering, OCR reading).
4. **Logical breaks only**: between `export_success` and the result/share screen. Preload the ad in advance so carrier latency cannot make it pop unexpectedly.
5. AdMob explicitly warns that pure utility apps with no clear start/stop point may be better with banners. Our app has a clear unit of work (export), but interstitials stay capped and banners carry the baseline.
6. **Rewarded ads are strictly opt-in** with a stated reward shown before the ad; never required for a core promised feature ("no watermark, no daily limit" stays free).
7. **UMP consent** for EEA/UK traffic before any ad SDK initialisation; no ads to users who withhold consent where consent is required.
8. Frequency cap across the whole app: max 1 interstitial per 2 minutes AND max 1 per 2 actions, whichever is stricter.
9. Banner only at the bottom of tool screens, never covering a button or the export result.
10. Data Safety declaration and privacy policy must match actual SDK behaviour before release (P5.2).

## Enforcement in code (P5.1)

`AdController` owns the action counter and last-show timestamp; every tool calls `onUserAction()`; only the export-success path calls `maybeShowInterstitial()`. No other call site may instantiate an interstitial. Assert in an instrumented test that launching the app and pressing Back never shows an interstitial.
