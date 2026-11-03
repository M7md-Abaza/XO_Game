# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Single-module Android app (`:app`): a Kotlin XO (tic-tac-toe style) game with AdMob ads. Source package is `com.m7mdabaza.xogame`, but the published `applicationId` is `com.m7mdabaza.x_ogame`. Don't "fix" this mismatch, because changing the applicationId breaks Play Store updates.

## Build / test

The toolchain is AGP 7.4.2, Gradle 7.6.4 wrapper, Kotlin 1.7.22 (the last Kotlin with kotlin-android-extensions; do not go to Kotlin 1.8+ or AGP 8+ without migrating off synthetics), compileSdk/targetSdk 29, minSdk 16. Gradle 7.6.4 cannot run on JDK 20+ (the Android Studio JBR is JDK 25), so in Studio set the Gradle JDK to JDK 11 or 17. Point `JAVA_HOME` at JDK 11 first, e.g. in Git Bash: `export JAVA_HOME=~/.jdks/jbr_dcevm-11.0.16`.

```sh
./gradlew assembleDebug                 # build debug APK
./gradlew installDebug                  # install on connected device/emulator
./gradlew test                          # JVM unit tests (app/src/test)
./gradlew test --tests "com.m7mdabaza.xogame.ExampleUnitTest"   # single test class
./gradlew connectedAndroidTest          # instrumented tests (needs device)
./gradlew lint
```

The only tests are the template `Example*Test` files. No game logic is covered.

`app/build.gradle` mixes `com.android.support:*` with AndroidX and declares an odd `cardview-v7:280.0`. All of it still resolves and builds, so leave it unless you're deliberately upgrading.

AdMob IDs are set per build type in `app/build.gradle`. The app ID goes into the manifest as `${admobAppId}`. Every screen has its own unit IDs as `resValue` strings: `banner_<screen>` (used by the layout XML), `interstitial_<screen>`, and one shared `rewarded_vs_computer`. Screens are `main`, `easy/medium/hard_vs_computer`, `easy/medium/hard_level`, and `x_or_o_dialog`. Release has the real IDs (from Release_1.1). Debug uses Google's public test IDs for all of them. A blank ID crashes the app at startup. Release lint currently fails on `ExpiredTargetSdkVersion` (targetSdk 29 < 31).

Installing on a device that has the Play Store build fails with `INSTALL_FAILED_VERSION_DOWNGRADE`: the Play build has a higher versionCode and a different signing key. Uninstall it first. Only `startActivities.splash` is exported, so launch with `adb shell am start -n com.m7mdabaza.x_ogame/com.m7mdabaza.xogame.startActivities.splash`.

## Architecture

- Views are bound through **kotlin-android-extensions synthetics** (`kotlinx.android.synthetic.main.<layout>.*`), so view IDs in the XML layouts are referenced directly as Kotlin properties. Renaming an ID in a layout breaks the activity that uses it.
- Flow: `startActivities/splash` (launcher) → `MainActivity` → `OnePlayerBottomSheetDialog` / `TwoPlayerBottomSheetDialog` → one of six game activities.
- Game activities come in two parallel sets. Each activity is fully self-contained, with no shared base class or game engine:
  - `twoPlayers/EasyLevel`, `MediumLevel`, `HardLevel`: local 2-player games. A start dialog (`x_or_o_dialog.xml`) chooses who plays first (`xPlayFirst`).
  - `onePlayer/*VsComputer`: the human is always X and the computer is O, moving after a `Handler.postDelayed`.
  - Board sizes: Easy is 3×3, Medium is 4×4 (4 in a row), Hard is 8×8 (5 in a row). Each activity has a `buttons: Array<Array<Button?>>` filled from layout IDs `btn_<row><col>` in `getButtonPosition()`.
  - Board state is the Button **text** (`"x"`, `"o"`, `""`). The background drawable (`R.drawable.x`/`o`) is set alongside it. `checkForWin()` copies the texts into a `field` array and checks hard-coded line combinations.
  - `roundCount` counts moves and is used for draw detection. `playTimeCount` counts games and drives both interstitial ad frequency and, in vs-computer modes, which fallback move order the AI uses.
- **Computer AI is hand-written, hard-coded pattern matching** with no search algorithm. `computerTurn()` calls a chain of pattern functions (e.g. `oMainPattern`, `xMainPattern`, `xTwoInRaw`, `ifNothingToDo1..N`). Each one checks specific cell combinations, places an O, and flips `player1Turn`. Later functions are guarded by `if (player1Turn)`, so the first function to move "wins". `HardLevelVsComputer.kt` is about 8.6k lines of this. Edit it with targeted searches, not full reads.
- Because logic is copy-pasted across all six activities (win check, dialogs, sounds, ads, save/restore state), a fix usually needs to be applied to every relevant variant.
- Ads (Google Mobile Ads SDK 19): every game screen has a banner, interstitials, and in vs-computer modes a rewarded video. Watching it (`showAdsDialog` → `onRewarded`, `adWatched*` flags) changes the AI's behavior as a help mechanic. The `adView.adUnitId = ...` lines in each `bannerAds()` set the ID on an unused local `AdView`; the real banner ID comes from the layout XML.
- Shared resources: custom font `app/src/assets/sukar.ttf` (an extra assets srcDir in `app/build.gradle`), loaded per-screen via `Typeface.createFromAsset`. Sounds are in `res/raw`. Strings in `res/values/strings.xml` are used for win/draw messages.

## Known inconsistencies

Some draw checks use the wrong `roundCount` threshold for the board size. Examples: in `twoPlayers/HardLevel` one branch checks `== 16` instead of `== 64`, and `MediumLevelVsComputer` uses `9` in `onClick` but `8` in `computerTurn`. In vs-computer modes `roundCount` counts only the human's moves, so 5 on Easy means a full board. Check these before assuming draw behavior is intentional.
