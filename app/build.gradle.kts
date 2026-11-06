plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Google's public test AdMob IDs: debug builds never request real ads.
private val testBanner = "ca-app-pub-3940256099942544/6300978111"
private val testInterstitial = "ca-app-pub-3940256099942544/1033173712"
private val testRewarded = "ca-app-pub-3940256099942544/5224354917"

// One AdMob unit per placement (6 in all, user decision 2026-11-06; read by AppAdUnitIds).
private val bannerPlacements = listOf("main", "game", "two_phones")
private val interstitialPlacements = listOf("between_rounds", "two_phones")

androidComponents {
    beforeVariants { variantBuilder ->
        (variantBuilder as? com.android.build.api.variant.HasUnitTestBuilder)?.enableUnitTest = true
    }
}

android {
    namespace = "com.m7mdabaza.xogame"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // Published package name. Never change it: Play Store updates depend on it.
        applicationId = "com.m7mdabaza.x_ogame"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        // Play needs a higher code than any upload (v1.2 was 9; 2.0 used 20 and 21).
        versionCode = 30
        versionName = "3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            bannerPlacements.forEach { resValue("string", "banner_$it", testBanner) }
            interstitialPlacements.forEach { resValue("string", "interstitial_$it", testInterstitial) }
            resValue("string", "rewarded", testRewarded)
        }
        release {
            // Real AdMob IDs, one unit per placement. The game banner and the between-round
            // interstitial reuse v1's 3×3-against-the-phone units (renamed in AdMob); the other
            // per-board units of v1/v2 are no longer requested (docs/RELEASE.md, section 5).
            manifestPlaceholders["admobAppId"] = "ca-app-pub-4454440016331822~9464823022"
            resValue("string", "banner_main", "ca-app-pub-4454440016331822/8758794756") // Home + Daily challenge
            resValue("string", "banner_game", "ca-app-pub-4454440016331822/1754402706") // game screen, every board
            resValue("string", "interstitial_between_rounds", "ca-app-pub-4454440016331822/8898874481")
            resValue("string", "rewarded", "ca-app-pub-4454440016331822/7531570468") // hints and undos
            resValue("string", "banner_two_phones", "ca-app-pub-4454440016331822/5545981810") // two-phone lobby + game
            // Before the two-phone lobby; named "pre_two_phones_looby" in AdMob.
            resValue("string", "interstitial_two_phones", "ca-app-pub-4454440016331822/2905349916")
            // R8: shrinks, optimizes and obfuscates. Libraries bring their own keep rules;
            // app-specific ones go in proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        resValues = true // the AdMob IDs above are resValues (off by default in AGP 9)
    }

    // The app switches language itself (Settings, and the reminder notification's texts), so
    // every install needs both English and Arabic: Play would otherwise install only the
    // device language's resources. Two languages of strings cost almost nothing.
    bundle {
        language {
            enableSplit = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":presentation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.play.services.ads)
    implementation(libs.ump)
    // :presentation uses it for the daily-challenge reminder. Keep it here too: the ads SDK pulls
    // in WorkManager 2.7.0 (Room 2.2.5), whose old keep rules let R8 strip the database
    // constructor, so the release build crashed at startup.
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso)
}
