# XO Game v3 Upgrade Plan

Branch: `feature/v3-upgrade` (from v2.0, versionCode 21)

Two features:
1. A tappable **"No ads" badge** that opens a Remove-ads purchase dialog from anywhere.
2. **Two phones**: two players, each on their own phone, connected over Bluetooth, with a pre-game interstitial until the player buys Remove ads.

## Progress

| Milestone | Status |
|---|---|
| 1. No-ads badge + dialog | **Done and committed** (534112a). `presentation/purchase`: `RemoveAdsBadge` (yellow pill with a crossed-out "AD"; icon only in title bars from font scale 1.5), `RemoveAdsDialog` (benefits, Buy for {price}, Restore purchases, Not now), `RemoveAdsViewModel` (shared; Settings' purchase/restore code moved there). Badge on Home, the game screen and the Daily challenge, hidden after Remove ads or when Play has no price. The hint/undo offer's "Remove ads" link opens the dialog instead of Settings. A result closes the dialog and shows a snackbar; a cancel keeps it open. Tests: 142 domain, 27 data, 70 presentation JVM (11 new), 41 Compose UI (7 new + 3 accessibility checks of the dialog; badge added to the Home / game / challenge checks); lint clean. Checked on the API 37 emulator with a temporary placeholder price: badge on all three screens, dialog from the badge and from the hint offer, billing-unavailable message, Settings restore message, simulated purchase removes badge + banner, Arabic + dark + font scale 2.0 (icon-only badge in the game title bar). The real build hides the badge on the emulator (no Play price). |
| 2. Two-phone domain | **Done and committed** (4eb395b). Accepted the open defaults (user, 2026-11-06). `GameMode.TWO_PHONES` (`humanSymbol` = this phone's mark; no undo). `StatsKey(TWO_PHONES, board)` from this phone's side, with streaks; the data layer stores them with no change (`TWO_PHONES_THREE_wins`, ...). `Stats.playedTotal` now drives "100 games" and the review prompt. New achievement `BEAT_A_FRIEND` (en + ar). `domain/multiplayer`: `PeerChannel`, `PeerMessage` (protocol v1), `TwoPhoneRules`, `TwoPhoneState`, `TwoPhoneSession` (see "How it's built"). Stats keeps its two tabs until milestone 4; two-phone ad units fall back to the two-player ones until milestone 5. Tests: 178 domain (36 new: the rules with controlled message delivery, the session over an in-memory channel pair, the mode in stats and achievements), 27 data, 70 presentation JVM, 41 Compose UI; lint clean. The randomized test plays 3,000 games with random actions and random delivery orders and checks both phones agree whenever nothing is in flight. It reaches about 3,400 rounds, 4,300 granted and 4,300 declined requests, 600 crossed requests and 190 lapsed restarts. Two deliberate bugs (no tie-break; moving while your own request waits) both made it fail at once. |
| 3. Nearby connection + lobby | **Done and committed** (a393b2e). Domain `PeerLobby` (host, search, join, answer, stop; `LobbyState`, `LobbyError`, `ConnectedPeer` with this player's name). `:data` `nearby/`: `NearbyPeerLobby` + `NearbyPeerChannel` behind the internal `NearbyGateway` (`PlayNearbyGateway` on play-services-nearby 19.5.1: point to point, `ConnectionType.NON_DISRUPTIVE`, service ID `com.m7mdabaza.x_ogame.nearby`), `PeerCodec` (JSON with a "type" field, unknown fields ignored) and the host `Announcement` (`1\|THREE\|BEST_OF_3\|Ahmed`). The guest stops searching to join and accepts its own side; a refusal or failure goes back to searching. `close()` waits up to 1.5 s for the goodbye to arrive. `TwoPhoneSession` now sends the goodbye even when the screen closes right after leaving. Presentation: "Two phones" on Home (Home's buttons now scroll on short screens), lobby (permission / Bluetooth / Location gate, name saved in `Settings.myName`, host form, waiting, host list with other versions greyed out, join with code, accept / decline dialog), and a simple connected screen (names, match, your mark, Leave, end reasons); milestone 4 puts the board there. English + Arabic. Found on the emulator: Nearby needs `ACCESS_WIFI_STATE` on Android 17 too (now declared for every version). Tests: 179 domain, 51 data (24 new: codec, lobby, channel), 82 presentation JVM (12 new), 55 Compose UI (14 new, 8 of them accessibility checks); lint clean in presentation and data (app: no errors, only older warnings). Checked on **two API 37 emulators** (two `-read-only` instances of one AVD; Nearby works between them over emulated Bluetooth): host, find, code 4650 on both, accept → connected, names and setup over the protocol, Leave → "Sara left the game.", decline → "Your friend said no." and back to the list, rehost after a game, guest app killed → "The connection was lost." in about 1 s, Arabic lobby, and the same connect / leave as a minified release build (5.4 MB). Not checked yet: real phones, Android 7–12 permission paths. |
| 4. Two-phone game screen | **Done and committed.** The two-phone game reuses `GameScreen`: `GameUiState.twoPhones` (`TwoPhonesInfo`: can play / continue / restart / new match, waiting for the answer or the friend, friend ready) switches it to "You" vs the friend's name, "Your turn" / "Sara's turn", "Waiting for Sara's answer…", a Restart + Leave row instead of Undo / Hint / Restart, and after a round Next round / Rematch that waits for the friend ("Waiting for Sara…" on one phone, "Sara is ready" on the other). `TwoPhonesGameScreen` wraps it with the friend's request dialog (Accept / Decline; Back declines), "Leave the game?" (Back asks too), a new-match request form (board, match, who starts: me / my friend / random) and the end dialog over the last board ("Sara left the game." etc. + Back to Home). `TwoPhonesGameViewModel`: `TwoPhoneState.toUiState()`, sounds (the friend's moves don't vibrate), stats + achievements per finished round, "Sara said no." snackbar; no review prompt (the friend would wait). Stats screen: third tab "Two phones", a card per board with own results and streaks. New Leave icon (`ic_logout`, mirrored in RTL). English + Arabic. Banner on this screen comes with milestone 5. Tests: 179 domain, 51 data, 91 presentation JVM (14 new in `TwoPhonesGameViewModelTest`: two real ViewModels playing each other over `PeerChannelPair`), 68 Compose UI (14 new two-phone screen tests incl. 5 accessibility checks, plus the Stats two-phone tab check; a flaky lobby name test fixed); lint clean in presentation. Checked on **two API 37 emulators**: host/join, round 1 won ("You win!" + "Beat a friend" / "Ahmed wins!"), Next round from both with the other player starting round 2, restart declined ("Sara said no."), new 4×4 match requested and accepted (score reset, asker starts), Back → "Leave the game?" → Stay, Leave → "Ahmed left the game.", Stats tab (1 win, the unfinished round not counted), Arabic game screen and request dialog, guest app killed → "The connection was lost." in about 1 s. |
| 5. Two-phone ads | Not started |
| 6. Polish + release 3.0 | Not started |

How to resume: build with `export JAVA_HOME=~/.jdks/jbr-21.0.11 && ./gradlew assembleDebug test`. Work milestone by milestone. The user reviews each one, and nothing is committed or pushed until the user says so.

**Where we stopped (2026-11-06):** milestones 1–4 are committed; next is **milestone 5, two-phone ads**.
- To do in milestone 5:
  - `ShouldShowPreGameInterstitialUseCase`: false after Remove ads, and false if any interstitial was shown in the last 3 minutes (the same `AdPolicyRepository` clock as the between-round one).
  - Load the interstitial ahead of time on Home and show it when the player taps Two phones, before the lobby. If it isn't ready, open the lobby without it.
  - Banners on the lobby and the two-phone game screen. `TwoPhonesGameScreen` passes `showBanner = false` and `toUiState()` sets `bannerAdUnitId = ""` until then.
  - New units `interstitial_two_phones` / `banner_two_phones` in `AdUnitIds` / `AppAdUnitIds` (debug: Google's test IDs). The release IDs need the user to create the units in AdMob. Until then the two-phone units fall back to the two-player ones.
- Open questions from milestone 4 for the user: the lobby's "Who starts" default comes from the local game's saved starter (it can preselect "My friend"); the status line behind the end dialog still shows the last turn text.
- Still open from milestone 3: real phones, and the Android 7–12 permission paths (the API 29 phone).

## Decisions

| Topic | Decision |
|---|---|
| Badge placement | Game screen (every mode), Daily challenge and Home (user decision, 2026-11-06) |
| Connection | Google **Nearby Connections** (Play services), not classic Bluetooth: no pairing, a host/join list, works offline (user decision, 2026-11-06) |
| Pre-game interstitial | Shown when the player taps **Two phones** on Home, **before** the lobby, so the opponent never waits. Skipped if any interstitial was shown in the last 3 minutes, and never after Remove ads (user decision, 2026-11-06) |
| Ads between rounds | None in two-phone games: an ad on one phone would freeze the game on the other (user decision, 2026-11-06) |
| Hint / undo | **Off** in two-phone games (user decision, 2026-11-06) |

The proposed defaults below (marked **Default**) were accepted by the user on 2026-11-06 ("go ahead" with milestone 2).

Things that must not change (unchanged from v2):
- `applicationId` stays `com.m7mdabaza.x_ogame`.
- The existing per-screen AdMob unit IDs.
- UMP consent gates every ad request.

---

## 1. No-ads badge + Remove-ads dialog

### What the player sees
- A small pill in the top bar: a crossed-out "AD" icon + "No ads" (brand yellow, at least a 48 dp touch target, content description "Remove ads").
  - **Game screen:** next to the "New match" icon, in every mode (vs phone, two players, two phones).
  - **Daily challenge:** in its top bar.
  - **Home:** next to Stats / Achievements / Settings. Home's top bar is the tightest (Arabic at 2× font), so there the pill drops its text and shows only the icon when it doesn't fit.
- Tapping it opens the **Remove ads dialog**:
  - Title "Remove ads" and what you get: no banners, no interstitials (including the two-phone pre-game ad), plus one free hint and one free undo per round.
  - **Buy for {price}** (opens the Google Play purchase sheet), **Restore purchases**, **Not now**.
  - A spinner on the button while Play is busy. When it finishes, the dialog closes and a snackbar on that screen shows the result (purchased / pending / already owned / error). The ad-free flows (banner gone, badge gone) switch over live, as they do now.
- **Default:** the badge is hidden when Play can't sell the product (no Play account, product not set up). Settings keeps its "not available" text for that case. Once Remove ads is bought, the badge disappears everywhere.
- The "Remove ads" link in the existing "watch an ad?" hint/undo dialog opens this dialog instead of navigating to Settings, so the round stays on screen.

### How it's built
- `presentation/purchase/`:
  - `RemoveAdsBadge` (composable).
  - `RemoveAdsDialog` (composable).
  - `RemoveAdsViewModel` (`@HiltViewModel`): `adsRemoved`, offer price, busy flag, and a one-shot result message. It uses the existing `ObserveAdsRemovedUseCase`, `ObserveRemoveAdsOfferUseCase`, `PurchaseRemoveAdsUseCase` and `RestorePurchasesUseCase`.
- The badge owns its own ViewModel (`hiltViewModel()` scoped to the screen's nav entry), so Home, Game and Daily challenge only place the composable. Their ViewModels don't change.
- Settings' purchase/restore code (`SettingsViewModel.onRemoveAds` / `onRestorePurchases` and the result-to-message mapping) moves into the shared ViewModel, so the mapping lives in one place. The Settings section stays as it is.
- `GameRoute.onOpenSettings` goes away if nothing else uses it.
- No domain or data changes.

### Tests
- JVM: `RemoveAdsViewModel` (badge visibility for bought / not bought / no offer; every `PurchaseResult` and `RestoreResult` maps to the right message; busy state).
- Compose UI: the badge opens the dialog; after a simulated purchase the badge and banner are gone. `AccessibilityTest` covers the badge on all three screens and the dialog.
- Emulator: English, Arabic, dark theme, font scale 2.0, debug "Simulate Remove ads", and the billing-unavailable state.

---

## 2. Two phones (Nearby Connections)

### Flow
```
Home ── tap "Two phones" ──▶ [pre-game interstitial, if due] ──▶ Permissions ──▶ Lobby
                                                                                  │
                         ┌────────────────────────────────────────────────────────┤
                    Host a game                                             Join a game
          board + match length + who starts                    list of nearby hosts (by name)
              "Waiting for a friend…"                                tap one to connect
                         │                                                        │
        "Sara wants to join · code 4821"  ◀──── same 4-digit code on both ─────▶ "Waiting for Ahmed…"
               Accept / Decline                                                   │
                         └──────────────────────▶ Game (both phones) ◀────────────┘
```

### Lobby and connection
- A third button on Home: **Two phones** (Bluetooth icon), under "Vs phone" and "Two players".
- **Your name:** asked in the lobby the first time, remembered in a new `Settings.myName`, editable there. The other phone shows it.
- **Host a game:** pick the board (3×3 / 4×4 / 8×8), the match length (single / best of 3 / best of 5) and who starts (me / friend / random). **Default:** the host is X and the joiner is O.
- **Join a game:** a live list of nearby hosts by name. Tapping one asks to connect, the joiner sees the host's board and match length, and the host gets "Sara wants to join · code 4821, Accept / Decline". Both phones show the same code (Nearby's authentication digits), so nobody joins a stranger's game by mistake.
- Nearby setup: service ID `com.m7mdabaza.x_ogame.nearby`, strategy `P2P_POINT_TO_POINT` (one friend), and a **non-disruptive** connection type. Nearby then stays on Bluetooth and never switches the phone's Wi-Fi, so both phones keep their internet and ads keep loading.
- If the phone has no Google Play services, the Two phones button shows "Not available on this phone".

### Permissions (runtime, per Android version)
| Android | Permissions |
|---|---|
| 7–9 (API 24–28) | `ACCESS_COARSE_LOCATION` |
| 10–11 (API 29–30) | `ACCESS_FINE_LOCATION` |
| 12 (API 31–32) | `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT`, plus location (the guide still declares it up to API 32) |
| 13+ (API 33+) | the three Bluetooth ones + `NEARBY_WIFI_DEVICES` |

The manifest (in `:data`) also lists the install-time ones: `BLUETOOTH` / `BLUETOOTH_ADMIN` up to API 30, and `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE` on every version. Google's guide stops the Wi-Fi ones at API 32, but on the Android 17 emulator Nearby refused to advertise without `ACCESS_WIFI_STATE` (milestone 3). The Nearby library's Exposure Notifications `WakeUpService` is removed in the app manifest. `BLUETOOTH_SCAN` and `NEARBY_WIFI_DEVICES` are marked `neverForLocation`. `ACCESS_LOCAL_NETWORK` is needed only when targeting SDK 37 with Wi-Fi LAN, so it's not needed now (targetSdk 36, Bluetooth only).
- A short explanation screen comes before the system dialog. On Android 7–11 Android itself calls it "location", so the explanation says why: the phone needs it to find nearby phones, and the app never uses or stores the location.
- If the player refuses: the lobby explains and offers **Open settings**. If Bluetooth is off: ask to turn it on (`ACTION_REQUEST_ENABLE`). On Android 7–11 Location services must be on as well (Nearby reports `MISSING_SETTING_LOCATION_MUST_BE_ON`); the lobby checks and opens the location settings. Still to check on the API 29 phone.

### Game rules over two phones
- Same boards, win lengths and match rules as local two-player play (`MatchState`). The players take turns to start: each next round, and a rematch, is started by the player who didn't start the round before. This applies in single rounds too, unlike local play, where the chosen starter repeats; there's no random roll each round that both phones would have to agree on. A restart keeps the starter, and a new match uses the starter from its setup.
- Each phone shows its own player as "You". The board is enabled only on your turn, and the status line reads "Your turn" / "Sara's turn".
- **No hint and no undo** (decision). Restart round and New match send a request that the friend accepts or declines ("Ahmed wants to restart the round"). After the match: **Rematch** (both tap) or **Leave**.
  - While a request waits, its sender can't move or continue, and the friend must answer it before continuing. That way both phones apply it to the same board.
  - If both ask at the same moment, the host's request wins and the guest's is dropped on both phones.
  - A restart request lapses if the round ends first.
- **Disconnect** (Bluetooth off, out of range, app swiped away): "Connection lost" with "Back to Home". If the friend left on purpose: "Sara left the game". An unfinished round doesn't count.
- A two-phone game is **not** restored after process death, because the connection is gone. Rotation and short trips to another app keep the game while the process lives.
- Sounds and vibration as usual, also when the friend moves.

### Stats and achievements
- **Default:** new `StatsKey(TWO_PHONES, board)`: wins / losses / draws from this phone's side, with streaks (unlike local two-player, they are your own results). The Stats screen gets a "Two phones" section with a card per board.
- **Default:** two-phone games count towards "100 games". One new achievement, **"Beat a friend"** (win a two-phone game). New achievements already unlock silently for players who qualify.

### How it's built
- **`:domain`**
  - `GameMode.TWO_PHONES`. The compiler flags every exhaustive `when` (about 50 places use `GameMode`).
  - `multiplayer/` (built in milestone 2):
    - `PeerChannel`: an open connection, with `events: Flow<PeerEvent>`, `send` and `close`. Finding and accepting a friend (advertise, discover, connect, accept) is a separate lobby interface that comes with the Nearby code in milestone 3. The match only needs the open channel.
    - `PeerMessage`, `TwoPhoneRules`, `TwoPhoneState` and `TwoPhoneSession`. `TwoPhoneRules` is a pure state machine run by both phones: state + action or message → new state, messages to send, events. `TwoPhoneSession` runs it over a channel and applies inputs one at a time.
  - Messages: `Hello(protocolVersion, name)`, `Setup(board, matchLength, firstPlayer)`, `Ready`, `Move(stage, index, cell)`, `Continue(stage)` (next round or rematch, once both sent it), `Request(stage, kind)` / `Answer(stage, accepted)`, `Leave`.
  - `stage` is a counter both phones raise on every board reset, so each phone can check the friend is on the same board.
  - The host sends the setup and plays X. Both phones check every move with the existing `PlayMoveUseCase`. Anything that breaks the rules ends the game as "out of sync".
  - `protocolVersion` must be equal on both phones. Each side then says which phone to update. `Hello` must keep its shape forever.
- **`:data`**
  - `nearby/`: the lobby (advertise, discover, connect, accept) and `NearbyPeerChannel`, on `com.google.android.gms:play-services-nearby` **19.5.1+** (since 19.5.0 it no longer needs core-library desugaring). One `@Singleton`, so the connection survives the lobby → game navigation.
  - JSON codec (kotlinx.serialization) that ignores unknown fields, so newer versions can add fields.
  - Tested with a fake transport.
- **`:presentation`**
  - `twophones/`: `TwoPhonesLobbyScreen` + ViewModel, the permission/Bluetooth helpers, and `TwoPhonesGameViewModel`.
  - The game reuses the stateless `GameScreen` / `GameUiState` / `GameBoard`, with the Hint and Undo tiles hidden in this mode. It gets its own ViewModel rather than more branches in the 477-line `GameViewModel`, because it has no computer, no saved session and a remote opponent.
  - New destinations: `Destination.TwoPhonesLobby` and `Destination.TwoPhonesGame`.
- **`:app`**: the new manifest permissions and the new `AdUnitIds` entries.

### Ads in two-phone mode
- **Pre-game interstitial:** new `ShouldShowPreGameInterstitialUseCase`. It returns false after Remove ads, and false if any interstitial was shown in the last 3 minutes (the same `AdPolicyRepository` clock as the between-round one, so the two never stack). It's loaded ahead of time on Home. If it isn't ready when the player taps Two phones, the lobby opens without it, so the player never waits for an ad.
- **No interstitials between rounds** in two-phone games.
- **Banner:** on the lobby and the two-phone game screen, like the other game screens.
- **Default:** new AdMob units `interstitial_two_phones` and `banner_two_phones`, so AdMob reports show this mode separately. You create them in the AdMob console, and debug builds use Google's test IDs as usual. The alternative is to reuse the Home units.
- If one phone has Remove ads and the other doesn't, only the one without sees the pre-game ad, and nobody waits because the ad comes before the lobby.

### Tests
- Domain: `TwoPhoneMatch` with two in-memory peers wired together. Full single and best-of matches, alternating starts, requests accepted and declined, rematch, disconnect mid-round, version mismatch, out-of-turn and duplicate moves. Also the pre-game ad use case.
- Data: codec round trips and unknown fields; the Nearby lobby and channel state handling with a fake.
- Presentation (JVM): two `TwoPhonesGameViewModel`s playing each other through the fake pair; the lobby's permission and state handling.
- Compose UI + accessibility: lobby (host, join list, accept dialog, permission explanation), two-phone game screen, "Connection lost".
- **Real phones: two are needed.** Emulators can't run Nearby over Bluetooth reliably. Ideally the API 29 phone (location-permission path) plus an Android 12+ phone (Nearby-devices path). Cases: English / Arabic, one phone with simulated Remove ads, disconnects (Bluetooth off, walking out of range, swiping the app away), and a minified release build on both.

---

## 3. Milestones

Each milestone ends with a working build, tests passing and an emulator (or phone) check, and the user reviews it before anything is committed.

1. **No-ads badge + Remove-ads dialog.** Self-contained; it could ship on its own as 2.1 if two-phone play takes longer.
2. **Two-phone domain:** `GameMode.TWO_PHONES`, `PeerChannel`, messages, the `TwoPhoneRules` state machine + `TwoPhoneSession`, stats key, achievement and full JVM tests. No UI yet.
3. **Nearby connection + lobby:** `play-services-nearby`, the lobby interface (advertise, discover, connect, accept) and its Nearby implementation, `NearbyPeerChannel`, permissions + Bluetooth flow, Home button, lobby (name, host, join, code, accept). `close()` must deliver the last message (the goodbye) before disconnecting. Check on two phones: they find each other, connect and exchange names.
4. **Two-phone game screen:** ViewModel, turns, rounds, best-of, restart/new-match requests, rematch, disconnects, sounds, stats + Stats screen section.
5. **Two-phone ads:** pre-game interstitial before the lobby (use case + shared 3-min cap), banners, new ad units.
6. **Polish + release 3.0:** Arabic / RTL and font-scale-2.0 pass, `AccessibilityTest` additions, version **3.0 / versionCode 30**, minified release build on two phones, `CLAUDE.md` + `docs/RELEASE.md` updates, store texts (en + ar).

## 4. Tasks for you (outside the code)

- **Two real phones** for milestones 3–6. One of them can be the API 29 phone.
- **AdMob:** create `interstitial_two_phones` and `banner_two_phones` (if you keep that default) and send me the IDs for the release build. The old `banner_x_or_o_dialog` deletion from v2 is still pending.
- **Play Console:**
  - Data safety: Nearby sends only game moves and the player's name directly between the two phones (nothing to a server), so no new data type is collected. Re-check the form anyway, because the app now asks for location on Android 7–11 and Nearby devices on Android 12+.
  - Store listing: add the two-phone feature to the description (en + ar).

## 5. Risks

- **Discovery speed over Bluetooth only:** finding a host can take 5–15 seconds. This is fine for a turn-based game, and the lobby shows a searching animation. If it turns out too slow on real phones, we can allow Wi-Fi LAN as well, which needs `ACCESS_LOCAL_NETWORK` once targetSdk reaches 37.
- **Phones without Google Play services** (e.g. recent Huawei) can't use two-phone play. They already get no ads or billing.
- **Android 7–11 location prompt:** some players will refuse a "location" permission in a game. That's why the explanation screen comes first.
- **AdMob policy:** an interstitial at the start of an activity the player chose is a natural break. The 3-minute cap keeps it from repeating on every tap.

## 6. Defaults (accepted 2026-11-06)

1. Host is X and chooses board, match length and who starts.
2. Restart round / New match in two-phone games need the friend's OK.
3. New AdMob units for two-phone play, or reuse the Home units?
4. The badge is hidden when Play can't sell Remove ads.
5. Two-phone stats with streaks, counted towards "100 games", plus a new "Beat a friend" achievement.
