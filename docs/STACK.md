# Verified stack — P0.1 spike (2026-11-23)

All versions were resolved and built locally on this machine; nothing here is a guess.

| Component | Version | Licence / terms | Notes |
|---|---|---|---|
| Gradle | 8.13 | Apache-2.0 | Extracted to `C:/tmp/gradle/gradle-8.13` (durable install preferred later) |
| JDK | Temurin 17.0.20.1 | GPL+CE | `C:/Users/munaa/jdk17` (no system JDK) |
| Android Gradle Plugin | 8.9.2 | Apache-2.0 | Pinned in root `build.gradle.kts` |
| Kotlin | 2.1.20 | Apache-2.0 | `org.jetbrains.kotlin.plugin.compose` required for Kotlin 2.x |
| Compose BOM | 2025.02.00 | Apache-2.0 | Resolves Compose UI 1.7.8, Material3 1.3.1 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 24 | — | platform android-35 installed |
| buildToolsVersion | 36.1.0 | — | 34.0.0 is corrupt on this machine; 36.1.0 verified |
| PDF engine | `com.tom-roush:pdfbox-android:2.0.27.0` | Apache-2.0 | Package namespace is `com.tom_roush.pdfbox.*`, **not** `org.apache.pdfbox` |
| QR | `com.google.zxing:core:3.5.3` | Apache-2.0 | Dependency resolved, runtime encode/decode still to be tested in P4 |
| OCR | `com.google.android.gms:play-services-mlkit-text-recognition:19.0.1` | Google SDK terms | Dependency resolved; on-device,Play-services delivered |
| JUnit | 4.13.2 | EPL-1.0 | JVM unit test |

## Spike results

- `gradle --no-daemon :app:assembleDebug` → **BUILD SUCCESSFUL** (35 tasks). Shell scaffolding is a Compose `MaterialTheme` + `Scaffold` + text ("PDF Pocket").
- `gradle --no-daemon :app:testDebugUnitTest --tests '*PdfMergeTest*'` → **BUILD SUCCESSFUL**, one test passed: a 2-page fixture + a 3-page fixture merged into a 5-page document that reloads through `PDDocument.load`.

## Integration notes discovered

1. `gradle.properties` must set `android.useAndroidX=true` or AGP fails `checkDebugAarMetadata`.
2. `local.properties` uses `sdk.dir=C:/Users/munaa/AppData/Local/Android/Sdk`; escaped backslashes are unnecessary and risky here.
3. PDFBox-Android text rendering (fonts) requires `PDFBoxResourceLoader.init(context)` on device; the JVM unit test uses blank pages and cannot exercise font assets. Text/signature tools in P2.3 must verify on an emulator.
4. The final APK must pass `zipalign -c -P 16 4 <apk>` (16KB-page devices).

## Still to verify in later tasks

- CameraX + document crop path (P3), ML Kit on-device OCR quality English/Arabic (P3.2)
- ZXing encode/decode round-trip and batch export (P4)
- AdMob SDK + UMP + IAP (P5)
