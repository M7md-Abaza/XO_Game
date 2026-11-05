# Releasing XO Game 2.0

Version **2.0**, versionCode **20**. The current Play build is 1.2 (versionCode 9). Play only accepts a
versionCode higher than every build ever uploaded, so check the highest code in Play Console →
App bundle explorer; if it is 20 or more, raise `versionCode` in `app/build.gradle.kts`.

Checked before release (2026-11-05, commit a92c718): all JVM tests in debug and release, lint (no
errors), minified release APK and AAB built; release manifest not debuggable, real AdMob IDs, debug
switches (EEA consent, "Simulate Remove ads") off; native libraries 16 KB aligned; AAB carries the R8
mapping; purchases acknowledged on every start; update from v1.2 (versionCode 6 build of `master`)
to 2.0 works on the emulator, with the v1 consent kept. Added after that check: the daily-challenge
reminder (notification permission, a WorkManager job), checked on the API 37 emulator in debug and as
a minified release build; rebuild the bundle and re-run the checks before uploading.

## 1. Build the bundle

1. **App signing:** Play Console → Test and release → App integrity. If Play App Signing is on (it
   shows an "upload key"), sign with that upload key. If the upload key is lost, request an upload
   key reset there (it takes a few days). If the app still uses the old self-managed key (no Play
   App Signing), either enroll in Play App Signing or upload a signed APK instead of the AAB.
2. Android Studio → **Build → Generate Signed App Bundle or APK → Android App Bundle**, build variant
   **release**, signed with the same upload key as v1.
3. The bundle already contains R8's `mapping.txt`, so Play Console shows readable crash reports.

## 2. Play Console, before the first upload

- **Payments profile:** selling anything needs a payments (merchant) profile linked to the developer
  account (Settings → Payments profile), with tax and bank details for payouts.
- **In-app product:** Monetize → Products → In-app products → create `remove_ads` (one-time), set the
  price, activate it. Until it exists, Settings shows "Not available right now" for Remove ads.
- **License testers:** add your Google account under Settings → License testing, so test purchases
  cost nothing.
- **Data safety:** review the form. The app itself stores everything on the phone (settings, stats,
  daily challenge, achievements); the daily reminder is a local notification and sends nothing. The ads SDK (AdMob + UMP consent) is unchanged from v1; the purchase
  is handled by Google Play Billing (the app sends no purchase data anywhere itself; check the form's
  help on whether Play Billing needs a "Purchase history" entry).
- **Content rating:** answer the questionnaire again; the app now sells a digital item (Remove ads).
- **Advertising ID:** App content → Advertising ID → "Yes", for advertising (the ads SDK adds the
  `AD_ID` permission).
- **Ads and target audience:** "Contains ads" stays Yes. If the target audience includes children
  under 13, the Families policy applies and the ad requests need child-directed settings, which the
  app doesn't set yet; keep the audience 13+ or change the app first.
- **Privacy policy:** the listing needs a privacy policy URL that covers ads (AdMob, advertising ID,
  consent) and in-app purchases. The text (English + Arabic) is `docs/xo-game-privacy-policy.html`.
  Upload it unchanged to the `M7md-Abaza.github.io` repo (next to Fragile Freight's
  `privacy-policy.html`, which must stay), so it is served at
  `https://m7md-abaza.github.io/xo-game-privacy-policy.html`, and enter that URL under App content →
  Privacy policy. Don't publish this repo's `docs` folder, which holds internal notes.
- **Store listing:** add the Arabic listing and new screenshots (the whole UI changed). Store texts
  are below.
- **Target API:** the app targets API 36, which meets Play's current requirement.

## 3. Test on a testing track (internal testing)

Installs from Play are the only way to test these:

1. Upload the bundle to **Internal testing**, join as a tester and install from the Play link
   (uninstall any local build first; the signing key differs).
2. **Remove ads:** buy with a license tester account. Banners and interstitials disappear, and you
   get one free hint and one free undo per round. Then uninstall, reinstall and use
   **Restore purchases**.
3. **In-app update:** upload a second bundle with a higher versionCode (e.g. 21) to the same track.
   Open the app: Play offers the update, it downloads in the background, and a "Restart" message
   installs it. The offer comes back at most every 3 days after a "No thanks".
4. **Review prompt:** shows after 5 finished games, when you beat the phone or solve the daily
   puzzle, and at most every 90 days. Play decides whether the sheet really appears, and you can't
   submit a review from a testing-track install.
5. Quick pass on a real phone in English and Arabic: every board at every level (3×3 Hard never
   loses), two players on each board, the daily challenge,
   achievements, a rewarded hint and an interstitial (after round 2 of a session).
   **Daily reminder:** on Android 13+ the first start asks for notifications. The reminder comes
   24 hours after the app was last opened; to see it at once, put the app in the background and
   force the job with adb (the job id is in `adb shell dumpsys jobscheduler | grep ChallengeReminder`):
   `adb shell cmd jobscheduler run -f -n androidx.work.systemjobscheduler com.m7mdabaza.x_ogame <id>`.
   Tapping it opens the Daily challenge.
6. **Pre-launch report** (Test and release → Pre-launch report): Play runs the uploaded build on
   real devices; check it for crashes and serious warnings before promoting.

AdMob, before production: the GDPR consent message is published in AdMob → Privacy & messaging
(the form real EEA/UK users see), and `app-ads.txt` on the developer website shown in the store
listing is verified in AdMob → Apps.

## 4. Release

- Promote to **Production** with a staged rollout (for example 20 %, then 100 % after a few days)
  and watch Android vitals (crashes, ANRs).

## 5. AdMob, after v2 is live

- Delete (or archive) the banner unit of the old "who plays first" dialog: `x_or_o_dialog`,
  `ca-app-pub-4454440016331822/5478764952`. Wait until most users have updated, because v1 still
  requests it.
- Optional: the Daily challenge screen reuses the Home banner unit (`banner_main`). Create a separate
  unit if you want it reported on its own, and add it to `app/build.gradle.kts` and `AppAdUnitIds`.

## Store texts

**What's new (English)**

```
XO Game 2.0, completely redesigned!
• New look with a dark theme and full Arabic support
• Choose your board (3×3, 4×4 or 8×8) and how hard the phone plays: Easy, Medium or Hard
• A smarter computer, unbeatable on 3×3 Hard
• Daily challenge: a new puzzle every day; keep your streak going, with a friendly reminder so you never miss one
• Achievements and detailed statistics
• Best of 3 or best of 5 matches, player names, choose who starts
• Hints and undo
• Remove ads with a one-time purchase
```

**What's new (Arabic)**

```
لعبة XO 2.0 بتصميم جديد بالكامل!
• شكل جديد مع الوضع الداكن ودعم كامل للعربية
• اختر اللوحة (3×3 أو 4×4 أو 8×8) ومستوى الهاتف: سهل أو متوسط أو صعب
• كمبيوتر أذكى، لا يُهزم في المستوى الصعب على لوحة 3×3
• التحدي اليومي: لغز جديد كل يوم، حافظ على سلسلتك، مع تذكير لطيف حتى لا يفوتك أي لغز
• إنجازات وإحصائيات مفصّلة
• مباريات من 3 أو 5 جولات، أسماء اللاعبين، واختيار من يبدأ
• تلميحات وتراجع عن الحركة
• أزل الإعلانات بعملية شراء واحدة
```
