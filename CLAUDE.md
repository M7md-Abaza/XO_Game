# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android XO (tic-tac-toe style) game in Kotlin with AdMob ads. Source packages are `com.m7mdabaza.xogame.*`, but the published `applicationId` is `com.m7mdabaza.x_ogame`. Don't "fix" this mismatch, because changing the applicationId breaks Play Store updates.

A v2 rewrite is in progress on branch `feature/v2-upgrade`; the agreed plan and milestones are in `docs/UPGRADE_PLAN.md`. Do not commit or push unless the user explicitly asks.

## Build / test

The toolchain is AGP 9.5.0 (built-in Kotlin, so there is no `kotlin-android` plugin), Gradle 9.8.2 wrapper, Kotlin 2.4.21, KSP + Hilt, compileSdk 37 (needed by current Compose/AndroidX), targetSdk 36, and minSdk 24 (required by Google Mobile Ads 25). All versions live in `gradle/libs.versions.toml`; build scripts are Kotlin DSL. AGP 9 needs JDK 17+. In Git Bash: `export JAVA_HOME=~/.jdks/jbr-21.0.11`. In Studio, set the Gradle JDK to 17 or 21. `buildFeatures.resValues` must stay on in `:app` because the AdMob IDs are resValues.

```sh
./gradlew assembleDebug                 # build debug APK
./gradlew installDebug                  # install on connected device/emulator
./gradlew test                          # all JVM unit tests (every module)
./gradlew :domain:test                  # pure-Kotlin domain tests only (fast)
./gradlew :domain:test --tests "com.m7mdabaza.xogame.domain.model.LevelTest"   # single test class
./gradlew connectedAndroidTest          # instrumented tests (needs device)
./gradlew :presentation:connectedDebugAndroidTest   # Compose UI tests of the game screen (needs device)
./gradlew lint
```

## Modules

- `:domain`: pure Kotlin/JVM (no Android). `model/` (`Player`, `Cell`, `Board`, `GameConfig`, `GameState`, `GameResult`, `MatchState`, `Level`, `GameMode`), `engine/WinChecker`, `ai/` (`SearchAi`, `DifficultyPolicy`) and `usecase/` (start, play, undo, computer move, hint). Use cases have `@Inject` constructors so Hilt can build them. Tests in `domain/src/test`.
- `:data`: Android library with repository implementations (DataStore, Play Billing) bound in `data/di/DataModule` (empty until milestone 5).
- `:presentation`: Compose UI (Material 3), ViewModels (`@HiltViewModel`), type-safe Navigation Compose (`navigation/XoNavHost.kt`), theme (`theme/`, brand yellow `#D6BD17` / black, `sukar` font in `res/font`), English + Arabic strings (`values`, `values-ar`), the game screen (`game/`), sounds and vibration (`feedback/`, sounds in `res/raw`), its Hilt module (`di/`), and the ad/consent code (`ads/`). JVM tests in `src/test`, Compose UI tests in `src/androidTest`.
- `:app`: the application. `XoApplication` (`@HiltAndroidApp`), the single Compose `MainActivity`, `di/` (binds `AdUnitIds` to `AppAdUnitIds`, which reads the resValue IDs), and the legacy XML splash screen.
- Dependency rule: `presentation → domain ← data`; only `:app` sees everything. Each module has its own `.gitignore` for `/build`.
- Compose-drawn brand graphics live in `presentation/components` (`drawX`/`drawO`, `XoLogo`, `BrandBackground`). The "XO" logo and the game board are forced LTR so they never mirror in Arabic.

AdMob IDs are set per build type in `app/build.gradle.kts`. The app ID goes into the manifest as `${admobAppId}`. Every placement has its own unit ID as a `resValue` string: `banner_<screen>`, `interstitial_<screen>`, and one shared `rewarded_vs_computer`. Screens are `main`, `easy/medium/hard_vs_computer` and `easy/medium/hard_level`; `AppAdUnitIds` maps mode + level to them so AdMob reports stay comparable with v1. (`banner_x_or_o_dialog` was removed with its dialog in milestone 4; the unit still has to be deleted in the AdMob console, see `docs/UPGRADE_PLAN.md`.) Release has the real IDs (from Release_1.1). Debug uses Google's public test IDs for all of them. A blank ID crashes the app at startup. The `:presentation` UI test APK declares Google's sample app ID in `src/androidTest/AndroidManifest.xml` for the same reason.

Installing on a device that has the Play Store build fails with `INSTALL_FAILED_VERSION_DOWNGRADE`: the Play build has a higher versionCode and a different signing key. Uninstall it first. Only `startActivities.splash` is exported, so launch with `adb shell am start -n com.m7mdabaza.x_ogame/com.m7mdabaza.xogame.startActivities.splash`.

## Architecture

- The app is portrait only (`screenOrientation="portrait"` on both activities, a user decision). Android 16+ may ignore that on large screens, so `GameScreen` keeps a side-by-side layout for wide windows.
- Flow: `startActivities/splash` (legacy XML launcher, runs UMP consent) → Compose `MainActivity` → `XoApp` → `XoNavHost`: Home (+ level picker sheet, `presentation/home`) → `Destination.Game(mode, level)` (`presentation/game`). The splash finishes itself; when the launcher reopens an existing task it closes again at once (`isTaskRoot` check) so the restored screens show instead of a second `MainActivity`.
- The splash is the only XML screen left. It calls `setupEdgeToEdge()` (`ui/EdgeToEdge.kt`) before `setContentView`, finds its views through a file-private extension property (`private val splash.textView ...`, the old synthetics replacement), and loads the font from `app/src/assets/sukar.ttf` (an extra assets srcDir in `app/build.gradle.kts`).
- **Game screen** (one screen for every mode and level):
  - `GameViewModel` owns the match: setup, rounds, the computer's moves (`viewModelScope`, a short `COMPUTER_DELAY_MS` pause, then the search on the injected `@DefaultDispatcher`), undo, hints and the score. It reads `mode`/`level` (enum names) from `SavedStateHandle`, so the `Game` route's properties must stay plain strings named like `GameViewModel.ARG_MODE` / `ARG_LEVEL`.
  - After every change the whole session is written to `SavedStateHandle` as JSON (`SavedSession`: settings, match score, the round's first player and its moves), so it survives rotation and process death; a round is rebuilt by replaying its moves.
  - `GameUiState.phase` is SETUP / PLAYING / ROUND_OVER / MATCH_OVER. Undo works only in PLAYING: a finished round (won, lost or drawn) is never undone (user decision). `GameScreen` is stateless (`GameUiState` + `GameActions`); `GameRoute` connects it to the ViewModel. `GameBoard` draws the grid, animated marks, the hint preview and the win line; every square is a clickable node with a content description. `MatchSetupDialog` picks symbol / names, who starts and the match length.
  - Matches (`domain` `MatchState`): Single is a running tally of independent rounds; best of 3/5 lasts at most N rounds (draws count) and ends early when the leader can't be caught. In a best-of match the players take turns to start.
  - Sounds and vibration go through the `GameFeedback` interface (fake in tests); `AndroidGameFeedback` is a singleton `SoundPool` + `Vibrator`. Milestone 5 gates them on settings.
- **AI** (`domain/ai`): one alpha-beta engine (`SearchAi`) for all sizes over a window-counting `SearchBoard`; it always wins/blocks in one, is perfect on 3×3 and searches only the top pattern-ranked cells near existing marks on 8×8. `DifficultyPolicy` sets depth, candidate limit and mistake chance per level for the computer and for hints.
- Ads (Google Mobile Ads SDK 25): Home and the game screen show the Compose `BannerAd` (large anchored adaptive). Interstitial and rewarded ads come back in milestone 6 through `presentation/ads/AdManagers.kt` (`InterstitialAdManager`, `RewardedAdManager`); until then hint and undo are free.
- EU consent uses Google UMP (`presentation/ads/ConsentManager.kt`). `splash` calls `gatherConsent()` and waits for it before opening `MainActivity`. Every ad request is gated on `canRequestAds()`: banners go through `AdView.loadAdIfAllowed()` (the Compose `BannerAd` uses it too), and the AdManagers check it in `load()`. Don't call `MobileAds.initialize` or `adView.loadAd` directly. The Compose Home shows a "Privacy settings" link only when UMP says privacy options are required. Debug builds force EEA geography, so the form shows on emulators. The real message text is configured in AdMob → Privacy & messaging.
- Compose UI tests need Espresso 3.7 pinned in `:presentation` (Compose testing pulls in 3.5, which crashes on API 34+).
