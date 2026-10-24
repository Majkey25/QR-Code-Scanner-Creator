package com.majkeylab.qrscannercreator

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonetizationPolicyTest {
    @Test
    fun donationsAreUnavailableInPlayBuilds() {
        when (BuildConfig.BUILD_TYPE) {
            "release" -> assertFalse(BuildConfig.ENABLE_DONATIONS)
            "debug", "githubRelease" -> assertTrue(BuildConfig.ENABLE_DONATIONS)
            else -> error("Verify donation policy for ${BuildConfig.BUILD_TYPE}")
        }
    }

    @Test
    fun legalLinksUseTheMatchingDistributionPolicies() {
        val website = "https://majkey25.github.io/QR-Code-Scanner-Creator"
        if (BuildConfig.BUILD_TYPE == "release") {
            assertEquals("$website/play.html#privacy", PRIVACY_URL)
            assertEquals("$website/play.html#terms", TERMS_URL)
            assertEquals("$website/play.html#notices", NOTICES_URL)
        } else {
            assertEquals("$website/privacy.html", PRIVACY_URL)
            assertEquals("$website/terms.html", TERMS_URL)
            assertEquals("https://github.com/Majkey25/QR-Code-Scanner-Creator/blob/main/THIRD_PARTY_NOTICES.md", NOTICES_URL)
        }
    }

    @Test
    fun playLegalPageLinksOnlyToItsSectionsAndPolicySources() {
        val html = File("../docs/play.html").readText()
        val hrefs =
            Regex("""\bhref\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""", RegexOption.IGNORE_CASE)
                .findAll(html).map { it.groupValues.drop(1).first(String::isNotEmpty) }.toSet()

        assertEquals(
            setOf(
                "site.css", "#main", "#privacy", "#terms", "#refunds", "#cookies", "#notices", "#contact",
                "mailto:majkeylab@gmail.com",
                "https://policies.google.com/technologies/ads",
                "https://policies.google.com/privacy",
                "https://uoou.gov.cz/",
                "https://play.google.com/store/account/subscriptions",
                "https://support.google.com/googleplay/answer/15574897",
                "https://support.google.com/googleplay/answer/2479637?hl=en",
                "https://support.google.com/googleplay/answer/15576634",
                "https://coi.gov.cz/faq/3-odstoupeni-od-smlouvy-ve-lhute-14-dnu/",
                "https://play.google.com/about/play-terms/eea/index.html",
                "https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement",
                "https://github.com/Majkey25/QR-Code-Scanner-Creator/blob/main/LICENSE",
                "https://github.com/Majkey25/QR-Code-Scanner-Creator/blob/main/THIRD_PARTY_NOTICES.md",
            ),
            hrefs,
        )
        hrefs.filter { it.startsWith("#") }.forEach { href ->
            assertTrue("Missing policy section $href", html.contains("id=\"${href.drop(1)}\""))
        }
    }

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
