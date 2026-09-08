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
    private val purchaseQueries = PurchaseQueries<Purchase> {
        it.purchaseState == Purchase.PurchaseState.PURCHASED && it.products.any(PREMIUM_PRODUCT_IDS::contains)
    }
    private var productGeneration = 0L
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
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> Unit
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> queryPurchases(client)
            else -> updateState { it.copy(error = true) }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        onMain {
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchaseQueries.invalidate()
                    val updated = purchases.orEmpty().filter { it.products.any(PREMIUM_PRODUCT_IDS::contains) }
                    if (updated.isNotEmpty()) processPurchases(updated, authoritative = false)
                    val client = billingClient
                    if (client != null && client.isReady) {
                        queryPurchases(client)
                    } else if (updated.isEmpty()) {
                        markBillingUnavailable()
                    }
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    val client = billingClient
                    if (client != null && client.isReady) queryPurchases(client) else markBillingUnavailable()
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> Unit
                else -> markBillingUnavailable()
            }
        }
    }

    private fun startConnection(client: BillingClient) {
        connectionStarted = true
        updateState { it.copy(checking = true, error = false) }
        client.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    onMain {
                        connectionStarted = false
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            queryProduct(client)
                            queryPurchases(client)
                        } else {
                            markBillingUnavailable()
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    onMain {
                        connectionStarted = false
                        markBillingUnavailable()
                    }
                }
            },
        )
    }

    private fun queryProduct(client: BillingClient) {
        val generation = ++productGeneration
        offers = emptyMap()
        updateState {
            it.copy(
                monthlyPrice = null,
                monthlyAvailable = false,
                lifetimePrice = null,
                lifetimeAvailable = false,
            )
        }
        queryProduct(client, PremiumPlan.Lifetime, BillingClient.ProductType.INAPP, generation)
        queryProduct(client, PremiumPlan.Monthly, BillingClient.ProductType.SUBS, generation)
    }

    private fun queryProduct(client: BillingClient, plan: PremiumPlan, productType: String, generation: Long) {
        val product =
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(plan.productId)
                .setProductType(productType)
                .build()
        client.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build(),
        ) { result, detailsResult ->
            onMain product@{
                if (generation != productGeneration) return@product
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    updateOffer(plan, offer = null, price = null)
                    return@product
                }
                val details = detailsResult.productDetailsList.firstOrNull { it.productId == plan.productId }
                val monthlyOffer = details?.subscriptionOfferDetails?.firstOrNull {
                    it.basePlanId == "monthly" && it.offerId == null
                }
                val token =
                    when (plan) {
                        PremiumPlan.Monthly -> monthlyOffer?.offerToken
                        PremiumPlan.Lifetime -> details?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
                    }
                val price =
                    when (plan) {
                        PremiumPlan.Monthly ->
                            monthlyOffer?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
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
    }

    private fun updateOffer(plan: PremiumPlan, offer: BillingOffer?, price: String?) {
        offers = if (offer == null) offers - plan else offers + (plan to offer)
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
        updateState { it.copy(checking = true, error = false) }
        purchaseQueries.query({ plan, callback ->
            val productType = when (plan) {
                PremiumPlan.Lifetime -> BillingClient.ProductType.INAPP
                PremiumPlan.Monthly -> BillingClient.ProductType.SUBS
            }
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(productType).build(),
            ) { result, purchases ->
                onMain { callback(result.responseCode == BillingClient.BillingResponseCode.OK, purchases) }
            }
        }) { purchases ->
            if (purchases != null) processPurchases(purchases) else markBillingUnavailable()
        }
    }

    private fun processPurchases(purchases: List<Purchase>, authoritative: Boolean = true) {
        val premiumPurchases = purchases.filter { purchase -> purchase.products.any(PREMIUM_PRODUCT_IDS::contains) }
        val records =
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
            }
        premiumPurchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach(::acknowledge)
        updateState { it.withPurchases(records, authoritative) }
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
        onMain {
            state = update(state)
            if (state.premium || !state.entitlementVerified) stopAdPreloading()
        }
    }

    private fun markBillingUnavailable() {
        updateState { it.withPurchaseQueryFailure() }
    }

    private fun onMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else mainHandler.post { action() }
    }
}
