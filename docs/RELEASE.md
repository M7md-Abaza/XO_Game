# Releasing XO Game 2.0

Version **2.0**, versionCode **20**. The current Play build is 1.2 (versionCode 9). Play only accepts a
versionCode higher than every build ever uploaded, so check the highest code in Play Console →
App bundle explorer; if it is 20 or more, raise `versionCode` in `app/build.gradle.kts`.

## 1. Build the bundle

1. Android Studio → **Build → Generate Signed App Bundle or APK → Android App Bundle**, build variant
   **release**, signed with the same upload key as v1.
2. The bundle already contains R8's `mapping.txt`, so Play Console shows readable crash reports.

## 2. Play Console, before the first upload

- **In-app product:** Monetize → Products → In-app products → create `remove_ads` (one-time), set the
  price, activate it. Until it exists, Settings shows "Not available right now" for Remove ads.
- **License testers:** add your Google account under Settings → License testing, so test purchases
  cost nothing.
- **Data safety:** review the form. The app itself stores everything on the phone (settings, stats,
  daily challenge, achievements). The ads SDK (AdMob + UMP consent) is unchanged from v1; the purchase
  is handled by Google Play Billing.
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
5. Quick pass on a real phone in English and Arabic: every level, two players, the daily challenge,
   achievements, a rewarded hint and an interstitial (after round 2 of a session).

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
• A smarter computer on every level
• Daily challenge: a new puzzle every day; keep your streak going
• Achievements and detailed statistics
• Best of 3 or best of 5 matches, player names, choose who starts
• Hints and undo
• Remove ads with a one-time purchase
```

**What's new (Arabic)**

```
لعبة XO 2.0 بتصميم جديد بالكامل!
• شكل جديد مع الوضع الداكن ودعم كامل للعربية
• كمبيوتر أذكى في كل المستويات
• التحدي اليومي: لغز جديد كل يوم، حافظ على سلسلتك
• إنجازات وإحصائيات مفصّلة
• مباريات من 3 أو 5 جولات، أسماء اللاعبين، واختيار من يبدأ
• تلميحات وتراجع عن الحركة
• أزل الإعلانات بعملية شراء واحدة
```
