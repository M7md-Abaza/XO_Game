package com.m7mdabaza.xogame.presentation.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import javax.inject.Inject

/** The app language choice. [tag] is the BCP 47 language tag, or null to follow the system. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    ARABIC("ar"),
}

/**
 * Reads and changes the per-app language. An interface so ViewModels can be
 * tested without Android.
 */
interface AppLanguageController {
    val current: AppLanguage

    /** Applies [language]; Android recreates the activities to show it. */
    fun set(language: AppLanguage)
}

/**
 * Uses AppCompat's per-app locales: the system per-app language setting on
 * Android 13+, and AppCompat's own storage below that (enabled with
 * `autoStoreLocales` in the app manifest). Android keeps the choice, so it is
 * not part of the DataStore settings.
 */
class AndroidAppLanguageController @Inject constructor() : AppLanguageController {

    override val current: AppLanguage
        get() {
            val locales = AppCompatDelegate.getApplicationLocales()
            if (locales.isEmpty) return AppLanguage.SYSTEM
            return when (locales[0]?.language) {
                "ar" -> AppLanguage.ARABIC
                "en" -> AppLanguage.ENGLISH
                else -> AppLanguage.SYSTEM
            }
        }

    override fun set(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            language.tag?.let { LocaleListCompat.forLanguageTags(it) } ?: LocaleListCompat.getEmptyLocaleList(),
        )
    }
}
