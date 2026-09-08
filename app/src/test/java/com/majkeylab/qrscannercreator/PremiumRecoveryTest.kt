package com.majkeylab.qrscannercreator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumRecoveryTest {
    private val monthly = PremiumPurchase(setOf(PREMIUM_MONTHLY_PRODUCT_ID), PremiumPurchaseState.Purchased)
    private val lifetime = PremiumPurchase(setOf(PREMIUM_PRODUCT_ID), PremiumPurchaseState.Purchased)

    @Test
    fun failedLifetimeQueryStillRestoresMonthly() {
        val queries = PurchaseQueries<PremiumPurchase> { hasPremiumEntitlement(listOf(it)) }
        val requested = mutableListOf<PremiumPlan>()
        var restored: List<PremiumPurchase>? = null
        queries.query({ plan, callback ->
            requested += plan
            callback(plan == PremiumPlan.Monthly, if (plan == PremiumPlan.Monthly) listOf(monthly) else emptyList())
        }) { restored = it }
        assertEquals(listOf(PremiumPlan.Lifetime, PremiumPlan.Monthly), requested)
        assertEquals(listOf(monthly), restored)
    }

    @Test
    fun failedMonthlyQueryStillRestoresLifetime() {
        val queries = PurchaseQueries<PremiumPurchase> { hasPremiumEntitlement(listOf(it)) }
        var restored: List<PremiumPurchase>? = null
        queries.query({ plan, callback ->
            callback(plan == PremiumPlan.Lifetime, if (plan == PremiumPlan.Lifetime) listOf(lifetime) else emptyList())
        }) { restored = it }
        assertEquals(listOf(lifetime), restored)
    }

    @Test
    fun partialEmptyQueryCannotDeclareUserFree() {
        val queries = PurchaseQueries<PremiumPurchase> { hasPremiumEntitlement(listOf(it)) }
        var called = false
        queries.query({ plan, callback -> callback(plan == PremiumPlan.Monthly, emptyList()) }) {
            called = true
            assertNull(it)
        }
        assertTrue(called)
    }

    @Test
    fun bothSuccessfulEmptyQueriesCanRemoveExpiredPremium() {
        val queries = PurchaseQueries<PremiumPurchase> { hasPremiumEntitlement(listOf(it)) }
        var restored: List<PremiumPurchase>? = null
        queries.query({ _, callback -> callback(true, emptyList()) }) { restored = it }
        assertEquals(emptyList<PremiumPurchase>(), restored)
    }

    @Test
    fun newPurchaseInvalidatesAlreadyRunningQueries() {
        val queries = PurchaseQueries<PremiumPurchase> { hasPremiumEntitlement(listOf(it)) }
        val callbacks = mutableListOf<(Boolean, List<PremiumPurchase>) -> Unit>()
        var results = 0
        queries.query({ _, callback -> callbacks += callback }) { results++ }
        callbacks[0](true, emptyList())
        queries.invalidate()
        callbacks[1](true, emptyList())
        assertEquals(0, results)
    }

    @Test
    fun newerRefreshIgnoresOlderCallback() {
        val queries = PurchaseQueries<PremiumPurchase> { hasPremiumEntitlement(listOf(it)) }
        val oldCallbacks = mutableListOf<(Boolean, List<PremiumPurchase>) -> Unit>()
        var results = 0
        queries.query({ _, callback -> oldCallbacks += callback }) { results++ }
        queries.query({ _, callback -> callback(true, emptyList()) }) { results++ }
        oldCallbacks[0](true, listOf(lifetime))
        assertEquals(1, oldCallbacks.size)
        assertEquals(1, results)
    }

    @Test
    fun pendingCallbackDoesNotRemoveExistingLifetime() {
        val state = PremiumState(premium = true, entitlementVerified = true)
        val updated = state.withPurchases(listOf(monthly.copy(state = PremiumPurchaseState.Pending)), authoritative = false)
        assertTrue(updated.premium)
        assertTrue(updated.entitlementVerified)
        assertTrue(updated.pending)
    }

    @Test
    fun onlyAuthoritativeEmptyResultRemovesPremium() {
        val state = PremiumState(premium = true, entitlementVerified = true)
        assertTrue(state.withPurchases(emptyList(), authoritative = false).premium)
        assertFalse(state.withPurchases(emptyList(), authoritative = true).premium)
    }

    @Test
    fun failedQueryKeepsConfirmedPremiumButNotVerifiedFreeState() {
        assertTrue(PremiumState(premium = true, entitlementVerified = true).withPurchaseQueryFailure().premium)
        assertTrue(PremiumState(premium = true, entitlementVerified = true).withPurchaseQueryFailure().entitlementVerified)
        assertFalse(PremiumState(entitlementVerified = true).withPurchaseQueryFailure().entitlementVerified)
    }

    @Test
    fun managementOpensGooglePlaySubscriptions() {
        var opened = ""
        assertTrue(openPlaySubscriptions { opened = it })
        assertEquals("https://play.google.com/store/account/subscriptions", opened)
    }

    @Test
    fun missingOrBlockedManagementHandlerReturnsFailure() {
        assertFalse(openPlaySubscriptions { throw IllegalArgumentException("No handler") })
        assertFalse(openPlaySubscriptions { throw SecurityException("Blocked") })
    }
}
