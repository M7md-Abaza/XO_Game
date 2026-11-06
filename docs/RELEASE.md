# Releasing XO Game 3.0

Version **3.0**, versionCode **30** (2.0 used 20 and 21; v1.2 was 9). Play only accepts a versionCode
higher than every build ever uploaded, so check the highest code in Play Console → App bundle
explorer; if it is 30 or more, raise `versionCode` in `app/build.gradle.kts`.

What 3.0 adds (v3, `docs/UPGRADE_PLAN_V3.md`): **two-phone play** over Nearby Connections (host / join
with a code check, a full match with requests, rematch and stats, a new "Beat a friend" achievement),
a **"No ads" badge** that opens the Remove-ads dialog from Home, the game and the Daily challenge, and
an interstitial before the two-phone lobby (never between two-phone rounds).

Checked before release (2026-11-06, v3 milestone 6): all JVM tests (182 domain, 51 data, 93
presentation) and 71 Compose UI tests (with accessibility checks and a font scale 2.0 / right-to-left
check of the two-phone game); lint without errors; a minified release APK (5.4 MB) on two API 37
emulators: consent, the pre-game interstitial, banners, host / join with the code, a round, a
new-match request accepted, leaving, in Arabic at font scale 2.0, with no crash. Then, with the two
new AdMob units in the release build: the lobby banner and the pre-lobby interstitial load from them
(as test ads on the emulator).

## 1. Build the bundle

1. **App signing:** Play Console → Test and release → App integrity. If Play App Signing is on (it
   shows an "upload key"), sign with that upload key. If the upload key is lost, request an upload
   key reset there (it takes a few days).
2. Android Studio → **Build → Generate Signed App Bundle or APK → Android App Bundle**, build variant
   **release**, signed with the same upload key as 2.0.
3. The bundle already contains R8's `mapping.txt`, so Play Console shows readable crash reports.

## 2. Before the upload

**AdMob (done 2026-11-06):** 3.0 uses **one ad unit per placement, 6 in all** (v1/v2 had one per mode
and board). All six are in the `release` block of `app/build.gradle.kts`:

| Placement | Resource in the app | AdMob unit (rename to) | ID |
|---|---|---|---|
| Home + Daily challenge banner | `banner_main` | Banner Main (keep) | `.../8758794756` |
| Game screen banner, every mode and board | `banner_game` | Banner EasyVsComputer → **Banner Game** | `.../1754402706` |
| Interstitial between rounds | `interstitial_between_rounds` | IntEasyVsComputer → **Int BetweenRounds** | `.../8898874481` |
| Rewarded (hints, undos) | `rewarded` | Reward Hard1 → **Reward Assist** (optional) | `.../7531570468` |
| Two-phone lobby + game banner | `banner_two_phones` | banner_two_phones (keep) | `.../5545981810` |
| Interstitial before the two-phone lobby | `interstitial_two_phones` | pre_two_phones_looby (keep) | `.../2905349916` |

(`...` is `ca-app-pub-4454440016331822`.) Renaming a unit in AdMob changes only its label; the ID and
its history stay. The new names are optional but make the reports readable. The two-phone
interstitial's frequency capping can stay at "No cap": the app shows it at most every 3 minutes.

**Play Console:**
- **Data safety:** review the form. Still true from 2.0: the app stores everything on the phone
  (settings, stats, daily challenge, achievements), the daily reminder is a local notification, the
  ads SDK (AdMob + UMP consent) and Google Play Billing are unchanged. New in 3.0: two-phone play
  sends only the game (moves, the match choices) and the player's chosen name, directly between the
  two phones over Nearby Connections; nothing goes to a server, so no new data type is collected or
  shared. Android 7–12 calls the Bluetooth scan permission "location"; the app never reads, uses or
  stores the location.
- **Permissions:** the app now asks for nearby devices (Android 12+: Bluetooth scan / advertise /
  connect, Nearby Wi-Fi devices) and, on Android 7–12 only, location. It is foreground use only, so
  no background-location declaration is needed. If the console asks about them, the purpose is
  "finding a friend's phone nearby for two-phone play".
- **Store listing:** add the two-phone feature to the description (texts below) and new screenshots
  of the two-phone lobby and game.
- Still valid from 2.0, check once more: content rating (the app sells Remove ads), Advertising ID →
  "Yes, for advertising", "Contains ads" Yes, target audience 13+ (the ad requests have no
  child-directed settings), the privacy policy URL
  (`https://m7md-abaza.github.io/xo-game-privacy-policy.html`; the source is
  `docs/xo-game-privacy-policy.html`). The privacy policy should mention that two-phone play sends
  the game and the name only to the friend's phone; update the page if it doesn't.
- **Target API:** the app targets API 36, which meets Play's current requirement.

## 3. Test on a testing track (internal testing)

Installs from Play are the only way to test purchases and updates:

1. Upload the bundle to **Internal testing**, join as a tester and install from the Play link
   (uninstall any local build first; the signing key differs).
2. **Two phones (two real phones, both with the testing-track build):**
   - Ideally one Android 7–12 phone (the location permission and "Location must be on" up to
     Android 11) and one Android 12+ phone (the nearby-devices permission).
   - Home → Two phones: the interstitial shows first (not again within 3 minutes, never after Remove
     ads), then the permission screen, then the lobby.
   - One phone hosts (board, match length, who starts), the other joins; both show the same 4-digit
     code; accept.
   - Play a best-of-3 match: turns, "Waiting for…" between rounds, the other player starts the next
     round, Rematch. Ask for a restart (accept and decline) and a new match. Stats → Two phones and
     the "Beat a friend" achievement on the winner's phone.
   - Leave from one phone ("… left the game." on the other), and lose the connection on purpose
     (Bluetooth off, walk away, swipe the app away): "The connection was lost." within a few seconds.
   - Once in Arabic, and once with Remove ads bought on one phone (that phone shows no banners and no
     pre-game ad; the friend's phone still does).
3. **"No ads" badge:** with Play available it shows on Home, the game and the Daily challenge; it
   opens the Remove-ads dialog (Buy for {price}, Restore purchases). After buying, the badge,
   banners and interstitials disappear.
4. **Remove ads:** buy with a license tester account; then uninstall, reinstall and use **Restore
   purchases**.
5. **In-app update:** upload a second bundle with a higher versionCode (e.g. 31) to the same track;
   Play offers the update, it downloads in the background, and a "Restart" message installs it.
6. Quick pass in English and Arabic: every board at every level, two players, the daily challenge
   and its reminder, achievements, a rewarded hint, an interstitial between rounds (after round 2).
7. **Update from 2.0:** install 2.0 from production, then update to 3.0 from the testing track; the
   settings, stats, achievements and Remove-ads purchase stay.
8. **Pre-launch report** (Test and release → Pre-launch report): check it for crashes and serious
   warnings before promoting.

## 4. Release

- Promote to **Production** with a staged rollout (for example 20 %, then 100 % after a few days)
  and watch Android vitals (crashes, ANRs).

## 5. AdMob housekeeping

3.0 requests only the 6 units in section 2. The other **12 units** of the app can go, but most are
still requested by phones on 1.x or 2.0: deleting a unit doesn't crash those versions, they just stop
showing (and earning from) that ad. So:

**Delete any time** (no released version uses it; first make sure no other app of yours does):

| AdMob unit | ID |
|---|---|
| banner more Apps | `.../2770868222` |

**Delete (or archive) once few users are left on 1.x / 2.0** (Play Console → Statistics, by app
version):

| AdMob unit | ID | Used by |
|---|---|---|
| Banner PopUPXO | `.../5478764952` | 1.x only ("who plays first" popup) |
| Banner MeduimVsComputer | `.../1558961582` | 1.x and 2.0 (4×4 against the phone) |
| Banner HardVsComputer | `.../1368931886` | 1.x and 2.0 (8×8 against the phone) |
| Banner Easy | `.../4895022629` | 1.x and 2.0 (3×3 two players) |
| Banner Meduim | `.../9832682202` | 1.x and 2.0 (4×4 two players) |
| Banner Hard | `.../9625153867` | 1.x and 2.0 (8×8 two players) |
| IntMeduimVsComputer | `.../6500297073` | 1.x and 2.0 |
| IntHardVsComputer | `.../8097991769` | 1.x and 2.0 |
| IntEasy | `.../3000035763` | 1.x and 2.0 |
| IntMeduim | `.../2369480375` | 1.x and 2.0 |
| IntHard | `.../5845690957` | 1.x and 2.0 |

(`...` is `ca-app-pub-4454440016331822`.) After that, the app has exactly the 6 units of section 2.


## Store texts

**What's new 3.0 (English)**

```
XO Game 3.0: play with a friend on two phones!
• Two phones: each player on their own phone, connected nearby over Bluetooth, no internet needed
• Host a game or join a friend's, with a code check so you never join a stranger
• Best of 3 or 5, rematches and your own two-phone stats
• New achievement: Beat a friend
• Remove ads from anywhere with the new "No ads" button
• Clearer layout at large text sizes
```

**What's new 3.0 (Arabic)**

```
لعبة XO 3.0: العب مع صديقك على هاتفين!
• هاتفان: كل لاعب على هاتفه، متصلان عن قرب عبر البلوتوث دون الحاجة إلى الإنترنت
• استضف لعبة أو انضم إلى لعبة صديقك، مع رمز للتأكد حتى لا تنضم إلى غريب
• مباريات من 3 أو 5 جولات، مباراة إعادة، وإحصائياتك الخاصة باللعب على هاتفين
• إنجاز جديد: تغلّب على صديق
• أزل الإعلانات من أي مكان بزر "بلا إعلانات" الجديد
• عرض أوضح مع أحجام الخط الكبيرة
```

**Store description, paragraph to add (English)**

```
Play on two phones: sit with a friend, each on your own phone, and play over Bluetooth without
internet. One of you hosts a game, the other joins, and both phones show the same code so you
always join the right friend. Play single games or best of 3 or 5, ask for a restart or a new match,
and keep your own two-phone stats.
```

**Store description, paragraph to add (Arabic)**

```
العب على هاتفين: اجلس مع صديقك، كلٌّ على هاتفه، والعبا عبر البلوتوث دون إنترنت. يستضيف أحدكما
لعبة وينضم الآخر، ويعرض الهاتفان الرمز نفسه حتى تنضم دائمًا إلى صديقك الصحيح. العب جولة واحدة أو
الأفضل من 3 أو 5، واطلب إعادة الجولة أو مباراة جديدة، واحتفظ بإحصائياتك الخاصة باللعب على هاتفين.
```

The 2.0 texts (for reference, if a store listing still needs the general feature list) are in this
file's history (`git log -p docs/RELEASE.md`).
