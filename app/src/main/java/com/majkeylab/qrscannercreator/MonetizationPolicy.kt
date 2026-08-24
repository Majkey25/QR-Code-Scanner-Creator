package com.majkeylab.qrscannercreator

internal const val PREMIUM_PRODUCT_ID = "qr_scanner_creator_premium"
private const val INTERSTITIAL_COOLDOWN_MILLIS = 300_000L

internal fun isInterstitialDue(
    completedScans: Int,
    nowMillis: Long,
    lastShownMillis: Long?,
): Boolean =
    completedScans > 0 &&
        completedScans % 5 == 0 &&
        (lastShownMillis == null || nowMillis - lastShownMillis >= INTERSTITIAL_COOLDOWN_MILLIS)

internal fun shouldShowAds(
    premium: Boolean,
    entitlementVerified: Boolean,
    canRequestAds: Boolean,
): Boolean = !premium && entitlementVerified && canRequestAds

internal enum class PremiumPurchaseState {
    Pending,
    Purchased,
    Unknown,
}

internal data class PremiumPurchase(
    val productIds: Set<String>,
    val state: PremiumPurchaseState,
)

internal data class PremiumEntitlement(
    val premium: Boolean,
    val pending: Boolean,
)

internal fun hasPremiumEntitlement(purchases: List<PremiumPurchase>): Boolean =
    purchases.any {
        it.state == PremiumPurchaseState.Purchased && PREMIUM_PRODUCT_ID in it.productIds
    }

internal fun resolvePremiumEntitlement(purchases: List<PremiumPurchase>): PremiumEntitlement =
    PremiumEntitlement(
        premium = hasPremiumEntitlement(purchases),
        pending =
            purchases.any {
                it.state == PremiumPurchaseState.Pending && PREMIUM_PRODUCT_ID in it.productIds
            },
    )

internal data class PremiumState(
    val premium: Boolean = false,
    val entitlementVerified: Boolean = false,
    val checking: Boolean = true,
    val pending: Boolean = false,
    val formattedPrice: String? = null,
    val purchaseAvailable: Boolean = false,
    val error: Boolean = false,
)
