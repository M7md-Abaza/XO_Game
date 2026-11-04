# XO Game v2 Upgrade Plan

Branch: `feature/v2-upgrade`

## Progress

| Milestone | Status |
|---|---|
| 1. Project setup | **Done and committed.** Modules, version catalog, Kotlin DSL, Hilt, Compose Home + level picker, ads/consent moved to `:presentation`, English + Arabic. Verified on emulator API 37 (en + ar) and phone API 29. |
| 2. Domain core | **Done and committed** (971aff4). `model/` (`Player`, `Cell`, `Move`, immutable `Board`, `GameConfig`, `GameState`, `GameResult`), `engine/WinChecker` (generic N×N / k-in-row; returns the whole winning run; draw = full board), `usecase/` `StartGameUseCase`, `PlayMoveUseCase` (returns `MoveResult`: Played / GameOver / OutOfBounds / CellOccupied), `UndoMoveUseCase` (vs computer: back to and including the human's last move; null when there is nothing to undo). 51 JVM tests in `domain/src/test`. `MatchState`, `PlayerStats` and `Settings` arrive with the milestones that use them (4 and 5). |
| 3. AI | **Done and committed** (c82fcbd). One alpha-beta engine for all sizes, `ai/SearchAi` over a fast window-counting `ai/SearchBoard`: always wins/blocks in one, detects double threats, perfect on 3×3, top-N pattern-ranked candidates near marks on 8×8, random among equal moves. `ai/DifficultyPolicy` (computer + hint per level, calibrated head to head). `ComputerMoveUseCase`, `GetHintUseCase`. Hard computer looks 5 moves ahead (user decision). 77 domain tests. On the emulator Hilt builds the use cases and the slowest move is about 50 ms. |
| 4. Game screen | **Done and committed** (4c9cfd9). One Compose `GameScreen` + `GameViewModel` for every mode and level (route `Game(mode, level)`): setup dialog (symbol, names, who starts, single / best of 3 / best of 5), scoreboard, animated marks, win line, hint preview, undo / hint / restart (free until milestone 6), match-over dialog, banner, sounds + vibration (`GameFeedback`), portrait only (wide-window layout kept for split screen and large screens), no undo after a round ends, survives rotation and process death (`SavedSession`). Domain `MatchState`. The six legacy activities, their layouts, drawables, strings and the Material/CardView dependencies are gone; the splash closes itself when reopened into an existing task. 21 ViewModel tests (JVM) + 4 Compose UI tests (emulator). Checked on the API 37 emulator in English and Arabic. |
| 5. Persistence | **Done and committed.** Domain `Settings` (sound, vibration, theme + the last setup choices as defaults), `LevelStats` / `Stats` (W/L/D, current and best win streak), repository interfaces and use cases. `:data`: `SettingsRepositoryImpl` + `StatsRepositoryImpl` on Preferences DataStore. Settings screen (sound, vibration, theme system/light/dark, language system/English/Arabic via AppCompat per-app locales, default match, privacy settings), Stats screen (per mode and level, reset), dark theme with theme-aware marks, sounds/vibration follow the settings, rounds recorded, setup remembers the last choices. Tests: 94 domain, 7 data (DataStore on temp files), 33 presentation JVM tests, 4 Compose UI tests. Checked on the API 37 emulator (dark theme, Arabic switch, persistence across restarts). |
| 6. Ads + billing | **In progress.** |
| 7–9 | Not started (see section 5). |

How to resume: build with `export JAVA_HOME=~/.jdks/jbr-21.0.11 && ./gradlew assembleDebug test`. Work milestone by milestone; the user reviews each one, and nothing is committed or pushed until the user says so.

## Decisions

| Topic | Decision |
|---|---|
| UI | Jetpack Compose, single activity, Navigation Compose |
| Architecture | Clean architecture split into modules: `:domain`, `:data`, `:presentation`, `:app` |
| Gameplay | All Phase 3 features (see below) |
| Rewarded ad | Gives a **hint** or an **undo** (replaces the hidden "make the phone easier" help) |
| Languages | English + Arabic (full RTL) |
| Monetization | Banner + time-capped interstitial + rewarded hint/undo + one-time **Remove ads** purchase |
| Banner size | Google's recommended size for the current SDK: large anchored adaptive (`AdSize.getLargeAnchoredAdaptiveBannerAdSize`), on every screen |

Things that must not change:
- `applicationId` stays `com.m7mdabaza.x_ogame`. Changing it would break Play Store updates.
- The existing per-screen AdMob unit IDs are kept. They are mapped by mode + level, so AdMob reports stay comparable. (Only `banner_x_or_o_dialog` was dropped in milestone 4, because its dialog no longer exists.)
- UMP consent gates every ad request, as it does today.

---

## 1. Module structure

```
:app            Android application. Wires everything together.
 ├─ :presentation  Compose UI, ViewModels, navigation, theme, strings (en + ar), ads UI
 │    └─ :domain
 ├─ :data          Repository implementations: DataStore, Play Billing, ad-frequency storage
 │    └─ :domain
 └─ :domain        Pure Kotlin (no Android). Models, game engine, AI, repository interfaces, use cases
```

Dependency rule: `presentation → domain ← data`. `:app` depends on all three and is the only module that knows about all of them (dependency injection).

- **`:domain`** is a `kotlin("jvm")` module, so its tests run on the JVM in milliseconds.
- **`:data`** and **`:presentation`** are Android library modules.
- Library versions live in one place: `gradle/libs.versions.toml`.
- **DI:** Hilt. Its code generator must work with AGP 9's built-in Kotlin. Milestone 1 checks this first, and we fall back to Koin if it doesn't.

### 1.1 `:domain`

**Models**
- `Player` (X, O)
- `Cell(row, col)`
- `Board` (immutable; size N, cells)
- `GameConfig(mode, level, humanSymbol, firstPlayer)`
- `Level` (EASY 3×3 / 3-in-row, MEDIUM 4×4 / 4-in-row, HARD 8×8 / 5-in-row)
- `GameMode` (VS_COMPUTER, TWO_PLAYERS)
- `GameState` (board, turn, move history, result)
- `GameResult` (Win(player, winningLine), Draw, InProgress)
- `MatchState` (best-of-N score)
- `LevelStats` / `Stats` (per mode/level: wins, losses, draws, current/best win streak; in two-player mode wins/losses are X/O wins)
- `Settings` (sound, vibration, theme, and the last setup choices: match length, symbol, who starts, player names). The language is not stored here: Android keeps the per-app language (AppCompat per-app locales).

**Engine**
- `WinChecker`: generic N×N, k-in-row. Returns the winning line. It replaces all six hard-coded `checkForWin()` copies.
- **Draw** means the board is full. This fixes every `roundCount` bug at once.

**AI** (as built in milestone 3: one engine instead of the planned `MinimaxAi` + `ThreatSpaceAi` pair)
- `SearchAi.chooseMove(state, difficulty)` plays for `state.turn`. It is an alpha-beta (negamax) search that always takes a win in one, always blocks a loss in one and treats an opponent double threat as lost.
  - 3×3 is searched to the end (perfect). 4×4 is depth-limited, with moves ordered by a pattern score.
  - On 8×8 it searches only the top-N cells within 2 of existing marks, ranked by the open lines they extend or block (the planned "threat" scoring).
  - Positions it cannot search to the end are scored by counting marks in every 5-cell (k-cell) window still open to one player.
- `DifficultyPolicy`: search depth, candidate limit and mistake chance per level, for the computer and for hints. The AI picks randomly among equally good moves.
  - Computer: Easy depth 9 with a 35% mistake chance, Medium depth 4 with 15%, Hard depth 5 / 10 candidates with 5%.
  - Hints: Easy perfect, Medium depth 6, Hard depth 5 / 12 candidates.
  - Mistakes never skip a win or a block.

**Repository interfaces**
- `StatsRepository`: observe and record results.
- `SettingsRepository`: observe and update settings.
- `PurchaseRepository`: `adsRemoved: Flow<Boolean>`, launch purchase, restore.
- `AdPolicyRepository`: when the last interstitial was shown and how many games have been played since.

**Use cases** (one public `operator fun invoke` each)

| Use case | Purpose |
|---|---|
| `StartGameUseCase` | new `GameState` from `GameConfig` |
| `PlayMoveUseCase` | validate + apply a move, compute result |
| `ComputerMoveUseCase` | ask the AI for the computer's move at the config's difficulty |
| `UndoMoveUseCase` | revert last move (vs computer: human move + computer reply) |
| `GetHintUseCase` | best move for the human (AI at full strength) |
| `ConsumeAssistUseCase` | decides if a hint/undo is free (buyer, first use this game) or needs a rewarded ad |
| `RecordGameResultUseCase` | update stats + streaks |
| `ObserveStatsUseCase` | stats screen |
| `ObserveSettingsUseCase` / `UpdateSettingsUseCase` | settings screen |
| `ObserveAdsRemovedUseCase` / `PurchaseRemoveAdsUseCase` / `RestorePurchasesUseCase` | billing |
| `ShouldShowInterstitialUseCase` | time cap (≥ 3 min) + at least 2 games since last ad + not purchased + consent allows |
| `GetDailyChallengeUseCase` | deterministic puzzle of the day (win in N) |
| `ObserveAchievementsUseCase` / `EvaluateAchievementsUseCase` | unlock achievements from stats |

### 1.2 `:data`

| Implementation | Backed by |
|---|---|
| `SettingsRepositoryImpl` | Preferences DataStore |
| `StatsRepositoryImpl` | Preferences DataStore (switch to Room only if we later want full game history) |
| `PurchaseRepositoryImpl` | Google Play Billing (one-time product `remove_ads`), with the result cached in DataStore so it works offline |
| `AdPolicyRepositoryImpl` | DataStore (timestamps, counters) |
| `AchievementsRepositoryImpl` | DataStore |

### 1.3 `:presentation`

- One `MainActivity` (in `:app`) hosting `XoNavHost`.
- **Screens:** Splash/Consent → Home → Mode & Level picker → Game → Stats → Settings → Daily challenge → Achievements.
- **One `GameScreen` + `GameViewModel`** replaces the 6 activities.
  - State is a `StateFlow<GameUiState>` that survives rotation and process death via `SavedStateHandle`.
  - The computer's move runs in `viewModelScope` (it is cancelled automatically when you leave) and on `Dispatchers.Default` for the 8×8 search.
- **Board:** a Compose grid, with an animated piece placement and a winning-line highlight drawn on a `Canvas` overlay.
- **Ads UI:**
  - `BannerAd` composable (an `AndroidView` wrapping `AdView`).
  - `InterstitialController` and `RewardedController`, based on the existing `AdManagers.kt`.
  - `ConsentManager` is kept as-is.
  - Ad unit IDs come from `:app` through an `AdUnitIds` provider, so `:presentation` holds no IDs.
- **Theme:** Material 3, light + dark. The custom `sukar` font is a `FontFamily`.
- **Strings:** all moved to resources. `values-ar/strings.xml` with full RTL (Compose mirrors the layout automatically; the board logic is unaffected).
- **Sound and vibration:** `SoundPool`-based `SoundPlayer` (one instance, sounds preloaded, released properly) plus `Haptics`, both gated by settings.

---

## 2. Features (Phase 3)

1. **Winning line highlight + piece animations.**
2. **Choose your symbol (X/O) and who starts** (you / phone / random), in both modes.
3. **Undo:** in vs-computer it costs a rewarded ad (Remove-ads buyers get 1 free per game); in two-player mode it is free. Only while a round is being played: once a round has ended (win, loss or draw) it can't be undone.
4. **Hint:** costs a rewarded ad (Remove-ads buyers get 1 free per game); shows the best cell with a pulse.
5. **Match mode:** single round, or best of 3 / 5, with a match-winner dialog.
6. **Player names** in two-player mode.
7. **Stats screen** per mode/level: W/L/D, win %, current and best streak. Saved permanently.
8. **Settings:** sound, vibration, theme (system/light/dark), language (system/English/Arabic), default match length, privacy settings (UMP), restore purchases.
9. **Arabic translation + RTL.**
10. **Daily challenge:** one puzzle per day ("win in 2 moves as X"), generated from a fixed seed so it's the same for everyone, with a streak counter.
11. **Achievements** (local), e.g. first win, beat Hard, 5-win streak, 7-day challenge streak, 100 games.

## 3. Ads and monetization (Phase 4)

- **Interstitial:** only between rounds, at most one every 3 minutes, and never before the 2nd game of a session.
- **Rewarded:** used only for hint and undo. Always optional.
- **Remove ads** (one-time purchase, product ID `remove_ads`): hides the banner and the interstitial.
  - Buyers also get **one free hint and one free undo per game**. Any extra hint or undo costs a rewarded ad, the same as for everyone else.
  - Non-buyers: every hint costs a rewarded ad, and so does every undo in vs-computer mode. Undo is free in two-player mode.
- **Fix:** the cut-off banner in the old who-plays-first dialog goes away with the new UI.
- **Store-rating prompt** (Play In-App Review) after the 5th completed game, and Play In-App Updates (flexible).

**Play Console tasks for you:**
- Create the in-app product `remove_ads`.
- Update the Data safety form if needed (purchase history is handled by Google Play).
- Add Arabic store listing text.

**AdMob tasks for you:**
- Delete (or archive) the banner unit that belonged to the old "who plays first" dialog: `x_or_o_dialog`, `ca-app-pub-4454440016331822/5478764952`. The app stopped using it in milestone 4. Do it once v2 is live, because v1 still requests it until users update.

## 4. Testing

- **`:domain` unit tests:**
  - `WinChecker` (all sizes, edges, diagonals, draw).
  - AI: 3×3 full strength never loses; it always takes a win in 1 and blocks a loss in 1 on every size.
  - Use cases with fake repositories.
- **`:data` tests:** DataStore repositories (JVM/Robolectric), billing state handling with a fake `BillingClient` wrapper.
- **`:presentation` tests:** `GameViewModel` tests (turn flow, undo, hint, rotation/process-death restore); a few Compose UI tests for the game screen.
- **Manual pass on the emulator (API 37) and on the phone (API 29)** at the end of each milestone, in both English and Arabic.

## 5. Milestones

Each milestone ends with a working build, tests passing and an emulator check. You review before anything is committed.

1. **Project setup** (done and committed; Hilt confirmed working with AGP 9, so no Koin fallback; compileSdk 37 because current Compose/AndroidX require it)
   - Version catalog; `:domain`, `:data`, `:presentation` modules.
   - Compose + Hilt (or Koin) + Navigation working with an empty Home screen.
   - Existing XML screens still reachable.
2. **Domain core:** models, `WinChecker`, `GameState`, play/undo use cases, full tests.
3. **AI:** minimax (3×3, 4×4) + threat AI (8×8) + difficulty policy + hint, with tests and a quick strength check.
4. **Game screen in Compose:** both modes, all levels, symbol/first-player choice, win-line animation, match mode, names, sounds/haptics. Old activities removed.
5. **Persistence:** DataStore repositories, stats, settings, theme, language switch.
6. **Ads + billing:** banner/interstitial/rewarded in Compose with the new frequency policy, hint/undo rewards, Remove ads purchase + restore.
7. **Arabic + RTL pass,** string audit, accessibility (content descriptions, touch targets).
8. **Daily challenge + achievements.**
9. **Release prep:**
   - In-app review/updates, R8 minify on, versionCode/versionName bump.
   - Update `CLAUDE.md`, final regression on emulator and phone.

## 6. Resolved questions

1. Remove-ads buyers: no unlimited free assists, but 1 free hint **and** 1 free undo per game (two separate free uses, confirmed by the user on 2026-11-04); extras need a rewarded ad.
2. You set the Remove-ads price in Play Console.
3. Keep the brand colors (yellow `#d6bd17` / black); the UI can be refreshed freely to fit the new features (Material 3 components, Compose-drawn X/O and board).
4. No undo after a round ends, whether it was won, lost or drawn (user decision, 2026-11-04). Undo works only while a round is being played.
5. The app is portrait only (user decision, 2026-11-04): both activities use `screenOrientation="portrait"`. Android 16+ may ignore this on large screens (600 dp and wider), so the game screen keeps a side-by-side layout for wide windows.
6. `banner_x_or_o_dialog` was removed from the app (user decision, 2026-11-04). Remind the user to delete the unit in AdMob (see "AdMob tasks for you").
