// Top-level build file. Plugin versions live in gradle/libs.versions.toml.
// AGP 9 compiles Kotlin in Android modules itself ("built-in Kotlin"), so
// there is no org.jetbrains.kotlin.android plugin anywhere.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}