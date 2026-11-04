package com.m7mdabaza.xogame.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import com.m7mdabaza.xogame.domain.repository.TimeProvider
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

/** A calendar day (days since 1970-01-01) as a localized date, e.g. "Nov 4, 2026". */
@Composable
fun formatEpochDay(epochDay: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, LocalLocale.current.platformLocale)
        // The day number is already a local calendar day; format it as UTC so it isn't shifted.
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(epochDay * TimeProvider.MILLIS_PER_DAY))

/** A moment (epoch millis) as a localized date in the device time zone. */
@Composable
fun formatMillisDate(millis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, LocalLocale.current.platformLocale).format(Date(millis))
