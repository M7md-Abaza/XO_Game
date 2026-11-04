package com.m7mdabaza.xogame.domain.model

/** What the interstitial policy needs to know about past ads and rounds. */
data class AdPolicyState(
    /** When the last interstitial was shown (epoch millis), or null if never. Kept across app restarts. */
    val lastInterstitialAtMillis: Long? = null,
    /** Rounds finished since the last interstitial. Kept across app restarts. */
    val roundsSinceInterstitial: Int = 0,
    /** Rounds finished since the app process started. */
    val roundsThisSession: Int = 0,
)

/** The Remove-ads product as Google Play offers it. */
data class RemoveAdsOffer(
    /** Localized price with currency, e.g. "$1.99". */
    val formattedPrice: String,
)

enum class PurchaseResult {
    PURCHASED,

    /** Paid with a delayed method (e.g. cash); ads go away once the payment completes. */
    PENDING,
    CANCELLED,
    ALREADY_OWNED,

    /** Google Play or the product is not available (no Play Store, product not set up, ...). */
    UNAVAILABLE,
    ERROR,
}

enum class RestoreResult { RESTORED, NOTHING_TO_RESTORE, ERROR }

/** The two in-game helps that may cost a rewarded ad. */
enum class AssistKind { HINT, UNDO }

enum class AssistAccess {
    FREE,

    /** The player has to watch a rewarded ad first. */
    REWARDED_AD,
}
