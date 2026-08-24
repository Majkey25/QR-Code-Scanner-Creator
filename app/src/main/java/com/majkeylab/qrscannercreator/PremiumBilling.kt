package com.majkeylab.qrscannercreator

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

private data class BillingOffer(
    val details: ProductDetails,
    val token: String,
)

internal object PremiumController : PurchasesUpdatedListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var billingClient: BillingClient? = null
    private var offers: Map<PremiumPlan, BillingOffer> = emptyMap()
    private var purchasesByType: Map<String, List<Purchase>> = emptyMap()
    private var connectionStarted = false

    var state by mutableStateOf(PremiumState())
        private set

    fun refresh(context: Context) {
        val client =
            billingClient ?: BillingClient.newBuilder(context.applicationContext)
                .setListener(this)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
                ).enableAutoServiceReconnection()
                .build()
                .also { billingClient = it }
        if (client.isReady) {
            queryProduct(client)
            queryPurchases(client)
        } else if (!connectionStarted) {
            startConnection(client)
        }
    }

    fun launchPurchase(activity: Activity, plan: PremiumPlan) {
        val client = billingClient
        val offer = offers[plan]
        if (client == null || !client.isReady || offer == null) {
            updateState { it.copy(error = true) }
            return
        }
        val productParams =
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(offer.details)
                .setOfferToken(offer.token)
                .build()
        val result =
            client.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productParams))
                    .build(),
            )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            updateState { it.copy(error = true) }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.takeIf { it.isNotEmpty() }?.let(::processPurchases)
                billingClient?.takeIf { it.isReady }?.let(::queryPurchases)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> markBillingUnavailable()
        }
    }

    private fun startConnection(client: BillingClient) {
        connectionStarted = true
        updateState { it.copy(checking = true, error = false) }
        client.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connectionStarted = false
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryProduct(client)
                        queryPurchases(client)
                    } else {
                        markBillingUnavailable()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    connectionStarted = false
                    markBillingUnavailable()
                }
            },
        )
    }

    private fun queryProduct(client: BillingClient) {
        synchronized(this) { offers = emptyMap() }
        updateState {
            it.copy(
                monthlyPrice = null,
                monthlyAvailable = false,
                lifetimePrice = null,
                lifetimeAvailable = false,
            )
        }
        queryProduct(client, PremiumPlan.Lifetime, BillingClient.ProductType.INAPP)
        queryProduct(client, PremiumPlan.Monthly, BillingClient.ProductType.SUBS)
    }

    private fun queryProduct(client: BillingClient, plan: PremiumPlan, productType: String) {
        val product =
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(plan.productId)
                .setProductType(productType)
                .build()
        client.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build(),
        ) { result, detailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                updateOffer(plan, offer = null, price = null)
                return@queryProductDetailsAsync
            }
            val details = detailsResult.productDetailsList.firstOrNull { it.productId == plan.productId }
            val token =
                when (plan) {
                    PremiumPlan.Monthly -> details?.subscriptionOfferDetails?.firstOrNull()?.offerToken
                    PremiumPlan.Lifetime -> details?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
                }
            val price =
                when (plan) {
                    PremiumPlan.Monthly ->
                        details?.subscriptionOfferDetails?.firstOrNull()
                            ?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                    PremiumPlan.Lifetime ->
                        details?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
                }
            updateOffer(
                plan,
                offer = if (details != null && token != null) BillingOffer(details, token) else null,
                price = price,
            )
        }
    }

    private fun updateOffer(plan: PremiumPlan, offer: BillingOffer?, price: String?) {
        synchronized(this) {
            offers =
                if (offer == null) {
                    offers - plan
                } else {
                    offers + (plan to offer)
                }
        }
        updateState {
            when (plan) {
                PremiumPlan.Monthly ->
                    it.copy(monthlyPrice = price, monthlyAvailable = offer != null)
                PremiumPlan.Lifetime ->
                    it.copy(lifetimePrice = price, lifetimeAvailable = offer != null)
            }
        }
    }

    private fun queryPurchases(client: BillingClient) {
        synchronized(this) { purchasesByType = emptyMap() }
        queryPurchases(client, BillingClient.ProductType.INAPP)
        queryPurchases(client, BillingClient.ProductType.SUBS)
    }

    private fun queryPurchases(client: BillingClient, productType: String) {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(productType)
                .build(),
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val combined =
                    synchronized(this) {
                        purchasesByType = purchasesByType + (productType to purchases)
                        purchasesByType.takeIf { it.size == 2 }?.values?.flatten()
                    }
                combined?.let(::processPurchases)
            } else {
                markBillingUnavailable()
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val premiumPurchases = purchases.filter { purchase -> purchase.products.any(PREMIUM_PRODUCT_IDS::contains) }
        val entitlement =
            resolvePremiumEntitlement(
                premiumPurchases.map {
                    PremiumPurchase(
                        productIds = it.products.toSet(),
                        state =
                            when (it.purchaseState) {
                                Purchase.PurchaseState.PURCHASED -> PremiumPurchaseState.Purchased
                                Purchase.PurchaseState.PENDING -> PremiumPurchaseState.Pending
                                else -> PremiumPurchaseState.Unknown
                            },
                    )
                },
            )
        premiumPurchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach(::acknowledge)
        updateState {
            it.copy(
                premium = entitlement.premium,
                entitlementVerified = true,
                checking = false,
                pending = entitlement.pending,
                error = false,
            )
        }
    }

    private fun acknowledge(purchase: Purchase) {
        billingClient?.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build(),
        ) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                updateState { it.copy(error = true) }
            }
        }
    }

    private fun updateState(update: (PremiumState) -> PremiumState) {
        mainHandler.post { state = update(state) }
    }

    private fun markBillingUnavailable() {
        updateState {
            it.copy(
                entitlementVerified = it.entitlementVerified || BuildConfig.DEBUG,
                checking = false,
                error = true,
            )
        }
    }
}
