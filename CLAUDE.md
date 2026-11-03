# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Single-module Android app (`:app`): a Kotlin XO (tic-tac-toe style) game with AdMob ads. Source package is `com.m7mdabaza.xogame`, but the published `applicationId` is `com.m7mdabaza.x_ogame`. Don't "fix" this mismatch, because changing the applicationId breaks Play Store updates.

## Build / test

The toolchain is AGP 9.5.0 (built-in Kotlin, so there is no `kotlin-android` plugin), Gradle 9.8.2 wrapper, compileSdk/targetSdk 36, and minSdk 24 (required by Google Mobile Ads 25). AGP 9 needs JDK 17+. In Git Bash: `export JAVA_HOME=~/.jdks/jbr-21.0.11`. In Studio, set the Gradle JDK to 17 or 21. `buildFeatures.resValues` must stay on because the AdMob IDs are resValues.

```sh
./gradlew assembleDebug                 # build debug APK
./gradlew installDebug                  # install on connected device/emulator
./gradlew test                          # JVM unit tests (app/src/test)
./gradlew test --tests "com.m7mdabaza.xogame.ExampleUnitTest"   # single test class
./gradlew connectedAndroidTest          # instrumented tests (needs device)
./gradlew lint
```

The only tests are the template `Example*Test` files. No game logic is covered.

Dependencies are all AndroidX/Material plus `play-services-ads`. Jetifier is off, so do not reintroduce `com.android.support:*`.

AdMob IDs are set per build type in `app/build.gradle`. The app ID goes into the manifest as `${admobAppId}`. Every screen has its own unit IDs as `resValue` strings: `banner_<screen>` (used by the layout XML), `interstitial_<screen>`, and one shared `rewarded_vs_computer`. Screens are `main`, `easy/medium/hard_vs_computer`, `easy/medium/hard_level`, and `x_or_o_dialog`. Release has the real IDs (from Release_1.1). Debug uses Google's public test IDs for all of them. A blank ID crashes the app at startup.

Installing on a device that has the Play Store build fails with `INSTALL_FAILED_VERSION_DOWNGRADE`: the Play build has a higher versionCode and a different signing key. Uninstall it first. Only `startActivities.splash` is exported, so launch with `adb shell am start -n com.m7mdabaza.x_ogame/com.m7mdabaza.xogame.startActivities.splash`.

## Architecture

- The old kotlin-android-extensions synthetics were replaced by generated **file-private extension properties** at the bottom of each Kotlin file. Examples: `private val EasyLevel.btn_00: Button get() = findViewById(R.id.btn_00)`, and `private val View.adView1: AdView ...` for inflated dialog views. View IDs are therefore still used directly as Kotlin properties. When you use a new layout ID, add a matching accessor. Renaming an ID in a layout breaks the activity that uses it.
- Every activity calls `setupEdgeToEdge()` (`ui/EdgeToEdge.kt`) right before `setContentView`. Android 15+ forces edge-to-edge, so this pads the root view by the system-bar insets and uses dark bar icons over the light background.
- Flow: `startActivities/splash` (launcher) → `MainActivity` → `OnePlayerBottomSheetDialog` / `TwoPlayerBottomSheetDialog` → one of six game activities.
- Game activities come in two parallel sets. Each activity is fully self-contained, with no shared base class or game engine:
  - `twoPlayers/EasyLevel`, `MediumLevel`, `HardLevel`: local 2-player games. A start dialog (`x_or_o_dialog.xml`) chooses who plays first (`xPlayFirst`).
  - `onePlayer/*VsComputer`: the human is always X and the computer is O, moving after a `Handler.postDelayed`.
  - Board sizes: Easy is 3×3, Medium is 4×4 (4 in a row), Hard is 8×8 (5 in a row). Each activity has a `buttons: Array<Array<Button?>>` filled from layout IDs `btn_<row><col>` in `getButtonPosition()`.
  - Board state is the Button **text** (`"x"`, `"o"`, `""`). The background drawable (`R.drawable.x`/`o`) is set alongside it. `checkForWin()` copies the texts into a `field` array and checks hard-coded line combinations.
  - `roundCount` counts moves and is used for draw detection. `playTimeCount` counts games and drives both interstitial ad frequency and, in vs-computer modes, which fallback move order the AI uses.
- **Computer AI is hand-written, hard-coded pattern matching** with no search algorithm. `computerTurn()` calls a chain of pattern functions (e.g. `oMainPattern`, `xMainPattern`, `xTwoInRaw`, `ifNothingToDo1..N`). Each one checks specific cell combinations, places an O, and flips `player1Turn`. Later functions are guarded by `if (player1Turn)`, so the first function to move "wins". `HardLevelVsComputer.kt` is about 8.6k lines of this. Edit it with targeted searches, not full reads.
- Because logic is copy-pasted across all six activities (win check, dialogs, sounds, ads, save/restore state), a fix usually needs to be applied to every relevant variant.
- Ads (Google Mobile Ads SDK 25): every game screen has a banner (layout XML) and interstitials, and vs-computer modes also have a rewarded ad. These go through `ads/AdManagers.kt` (`InterstitialAdManager`, `RewardedAdManager`), which keep the old `isLoaded`/`show()` shape and preload the next ad after each dismissal. The reward (`onRewarded` → `adWatched*` flags) changes the AI as a help mechanic, but the `txt_Help` button that opens it is `visibility="gone"` and never shown.
- EU consent uses Google UMP (`ads/ConsentManager.kt`). `splash` calls `gatherConsent()` and waits for it before opening `MainActivity`. Every ad request is gated on `canRequestAds()`: banners go through `AdView.loadAdIfAllowed()`, and the AdManagers check it in `load()`. Don't call `MobileAds.initialize` or `adView.loadAd` directly. MainActivity shows a `privacySettings` link only when UMP says privacy options are required. Debug builds force EEA geography, so the form shows on emulators. The real message text is configured in AdMob → Privacy & messaging.
- Shared resources: custom font `app/src/assets/sukar.ttf` (an extra assets srcDir in `app/build.gradle`), loaded per-screen via `Typeface.createFromAsset`. Sounds are in `res/raw`. Strings in `res/values/strings.xml` are used for win/draw messages.

## Known inconsistencies

Some draw checks use the wrong `roundCount` threshold for the board size. Examples: in `twoPlayers/HardLevel` one branch checks `== 16` instead of `== 64`, and `MediumLevelVsComputer` uses `9` in `onClick` but `8` in `computerTurn`. In vs-computer modes `roundCount` counts only the human's moves, so 5 on Easy means a full board. Check these before assuming draw behavior is intentional.
