package com.majkeylab.qrscannercreator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonetizationPolicyTest {
    @Test
    fun debugUsesProductionAppIdWithGoogleTestBanner() {
        val ads = MonetizationConfig.adsForDebuggable(debuggable = true)

        assertEquals("ca-app-pub-6991329209066655~5561017627", ads.appId)
        assertEquals("ca-app-pub-3940256099942544/9214589741", ads.bannerAdUnitId)
    }

    @Test
    fun releaseUsesProductionBanner() {
        val ads = MonetizationConfig.adsForDebuggable(debuggable = false)
        assertEquals("ca-app-pub-6991329209066655~5561017627", ads.appId)
        assertEquals("ca-app-pub-6991329209066655/6914512227", ads.bannerAdUnitId)
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

    @Test
    fun changedPrivacyChoicesInvalidateAdsEvenWhenRequestsStayAllowed() {
        val gate = ConsentGate()
        gate.update(canRequestAds = true, privacyOptionsRequired = true)
        val previous = gate.revision
        gate.update(canRequestAds = true, privacyOptionsRequired = true, invalidateAds = true)
        assertTrue(gate.canRequestAds)
        assertEquals(previous + 1, gate.revision)
    }

    @Test
    fun consentRevocationInvalidatesInFlightLoads() {
        val gate = ConsentGate()
        gate.update(canRequestAds = true, privacyOptionsRequired = true)
        val previous = gate.revision
        gate.update(canRequestAds = false, privacyOptionsRequired = true)
        assertFalse(gate.canRequestAds)
        assertEquals(previous + 1, gate.revision)
    }
}
