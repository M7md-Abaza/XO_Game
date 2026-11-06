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

private val bannerPlacements = listOf(
    "main", "easy_vs_computer", "medium_vs_computer", "hard_vs_computer",
    "easy_level", "medium_level", "hard_level", "two_phones",
)
private val interstitialPlacements = listOf(
    "easy_vs_computer", "medium_vs_computer", "hard_vs_computer",
    "easy_level", "medium_level", "hard_level", "two_phones",
)

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
            resValue("string", "rewarded_vs_computer", testRewarded)
        }
        release {
            // Real per-screen AdMob IDs (restored from Release_1.1, commit e69ce27)
            manifestPlaceholders["admobAppId"] = "ca-app-pub-4454440016331822~9464823022"
            resValue("string", "banner_main", "ca-app-pub-4454440016331822/8758794756")
            resValue("string", "banner_easy_vs_computer", "ca-app-pub-4454440016331822/1754402706")
            resValue("string", "banner_medium_vs_computer", "ca-app-pub-4454440016331822/1558961582")
            resValue("string", "banner_hard_vs_computer", "ca-app-pub-4454440016331822/1368931886")
            resValue("string", "banner_easy_level", "ca-app-pub-4454440016331822/4895022629")
            resValue("string", "banner_medium_level", "ca-app-pub-4454440016331822/9832682202")
            resValue("string", "banner_hard_level", "ca-app-pub-4454440016331822/9625153867")
            resValue("string", "interstitial_easy_vs_computer", "ca-app-pub-4454440016331822/8898874481")
            resValue("string", "interstitial_medium_vs_computer", "ca-app-pub-4454440016331822/6500297073")
            resValue("string", "interstitial_hard_vs_computer", "ca-app-pub-4454440016331822/8097991769")
            resValue("string", "interstitial_easy_level", "ca-app-pub-4454440016331822/3000035763")
            resValue("string", "interstitial_medium_level", "ca-app-pub-4454440016331822/2369480375")
            resValue("string", "interstitial_hard_level", "ca-app-pub-4454440016331822/5845690957")
            resValue("string", "rewarded_vs_computer", "ca-app-pub-4454440016331822/7531570468")
            // Two-phone play (v3): the lobby + game banner, and the interstitial before the lobby
            // (named "pre_two_phones_looby" in AdMob).
            resValue("string", "banner_two_phones", "ca-app-pub-4454440016331822/5545981810")
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
