package com.majkeylab.qrscannercreator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonetizationPolicyTest {
    @Test
    fun debugUsesProductionAppIdWithGoogleTestAdUnits() {
        val ads = MonetizationConfig.adsForDebuggable(debuggable = true)

        assertEquals("ca-app-pub-6991329209066655~5561017627", ads.appId)
        assertEquals("ca-app-pub-3940256099942544/9214589741", ads.bannerAdUnitId)
        assertEquals("ca-app-pub-3940256099942544/1033173712", ads.interstitialAdUnitId)
    }

    @Test
    fun interstitialRequiresFifthScanAndFiveMinuteCooldown() {
        assertFalse(isInterstitialDue(4, 300_000, null))
        assertTrue(isInterstitialDue(5, 300_000, null))
        assertFalse(isInterstitialDue(10, 299_999, 0))
        assertTrue(isInterstitialDue(10, 300_000, 0))
    }

    @Test
    fun adsRequireVerifiedFreeEntitlementAndConsent() {
        assertTrue(shouldShowAds(premium = false, entitlementVerified = true, canRequestAds = true))
        assertFalse(shouldShowAds(premium = true, entitlementVerified = true, canRequestAds = true))
        assertFalse(shouldShowAds(premium = false, entitlementVerified = false, canRequestAds = true))
        assertFalse(shouldShowAds(premium = false, entitlementVerified = true, canRequestAds = false))
    }

    @Test
    fun onlyPurchasedMatchingProductGrantsPremium() {
        assertTrue(
            hasPremiumEntitlement(
                listOf(PremiumPurchase(setOf(PREMIUM_PRODUCT_ID), PremiumPurchaseState.Purchased)),
            ),
        )
        assertTrue(
            hasPremiumEntitlement(
                listOf(PremiumPurchase(setOf(PREMIUM_MONTHLY_PRODUCT_ID), PremiumPurchaseState.Purchased)),
            ),
        )
        assertFalse(
            hasPremiumEntitlement(
                listOf(PremiumPurchase(setOf(PREMIUM_PRODUCT_ID), PremiumPurchaseState.Pending)),
            ),
        )
        assertFalse(
            hasPremiumEntitlement(
                listOf(PremiumPurchase(setOf("other"), PremiumPurchaseState.Purchased)),
            ),
        )
    }

    @Test
    fun pendingPurchaseStaysLocked() {
        val entitlement =
            resolvePremiumEntitlement(
                listOf(PremiumPurchase(setOf(PREMIUM_MONTHLY_PRODUCT_ID), PremiumPurchaseState.Pending)),
            )

        assertFalse(entitlement.premium)
        assertTrue(entitlement.pending)
    }
}
