package com.majkeylab.qrscannercreator

internal const val PREMIUM_PRODUCT_ID = "qr_scanner_creator_premium"
internal const val PREMIUM_MONTHLY_PRODUCT_ID = "qr_scanner_creator_premium_monthly"
internal val PREMIUM_PRODUCT_IDS = setOf(PREMIUM_PRODUCT_ID, PREMIUM_MONTHLY_PRODUCT_ID)
private const val INTERSTITIAL_COOLDOWN_MILLIS = 300_000L

internal enum class PremiumPlan(val productId: String) {
    Monthly(PREMIUM_MONTHLY_PRODUCT_ID),
    Lifetime(PREMIUM_PRODUCT_ID),
}

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
        it.state == PremiumPurchaseState.Purchased && it.productIds.any(PREMIUM_PRODUCT_IDS::contains)
    }

internal fun resolvePremiumEntitlement(purchases: List<PremiumPurchase>): PremiumEntitlement =
    PremiumEntitlement(
        premium = hasPremiumEntitlement(purchases),
        pending =
            purchases.any {
                it.state == PremiumPurchaseState.Pending && it.productIds.any(PREMIUM_PRODUCT_IDS::contains)
            },
    )

internal data class PremiumState(
    val premium: Boolean = false,
    val entitlementVerified: Boolean = false,
    val checking: Boolean = true,
    val pending: Boolean = false,
    val monthlyPrice: String? = null,
    val monthlyAvailable: Boolean = false,
    val lifetimePrice: String? = null,
    val lifetimeAvailable: Boolean = false,
    val error: Boolean = false,
) {
    val purchaseAvailable: Boolean
        get() = monthlyAvailable || lifetimeAvailable
}

internal fun PremiumState.withPurchaseQueryFailure(): PremiumState =
    copy(entitlementVerified = entitlementVerified && premium, checking = false, error = true)

internal fun PremiumState.withPurchases(
    purchases: List<PremiumPurchase>,
    authoritative: Boolean,
): PremiumState {
    val entitlement = resolvePremiumEntitlement(purchases)
    return copy(
        premium = entitlement.premium || (!authoritative && premium),
        entitlementVerified = authoritative || entitlement.premium || entitlementVerified,
        pending = entitlement.pending || (!authoritative && pending),
        checking = false,
        error = false,
    )
}

// SDK purchase objects stay intact; callback ordering needs no Android runtime.
internal class PurchaseQueries<T>(private val isPremium: (T) -> Boolean) {
    private var generation = 0L

    fun invalidate() {
        generation++
    }

    fun query(
        queryPurchases: (PremiumPlan, (Boolean, List<T>) -> Unit) -> Unit,
        onResult: (List<T>?) -> Unit,
    ) {
        val current = ++generation
        queryPurchases(PremiumPlan.Lifetime) lifetime@{ lifetimeAvailable, lifetime ->
            if (current != generation) return@lifetime
            queryPurchases(PremiumPlan.Monthly) monthly@{ monthlyAvailable, monthly ->
                if (current != generation) return@monthly
                val purchases =
                    (if (lifetimeAvailable) lifetime else emptyList()) +
                        (if (monthlyAvailable) monthly else emptyList())
                onResult(purchases.takeIf { (lifetimeAvailable && monthlyAvailable) || it.any(isPremium) })
            }
        }
    }
}

internal fun openPlaySubscriptions(openUri: (String) -> Unit): Boolean =
    try {
        openUri("https://play.google.com/store/account/subscriptions")
        true
    } catch (_: IllegalArgumentException) {
        false
    } catch (_: SecurityException) {
        false
    }
