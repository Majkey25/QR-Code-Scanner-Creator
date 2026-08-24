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

internal object PremiumController : PurchasesUpdatedListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null
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

    fun launchPurchase(activity: Activity) {
        val client = billingClient
        val details = productDetails
        val offerToken = details?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
        if (client == null || !client.isReady || details == null || offerToken == null) {
            updateState { it.copy(error = true) }
            return
        }
        val productParams =
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
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
            BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty())
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
        val product =
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        client.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build(),
        ) { result, detailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                productDetails = null
                updateState {
                    it.copy(formattedPrice = null, purchaseAvailable = false, error = true)
                }
                return@queryProductDetailsAsync
            }
            val details = detailsResult.productDetailsList.firstOrNull { it.productId == PREMIUM_PRODUCT_ID }
            val offer = details?.oneTimePurchaseOfferDetailsList?.firstOrNull()
            productDetails = details
            updateState {
                it.copy(
                    formattedPrice = offer?.formattedPrice,
                    purchaseAvailable = offer?.offerToken != null,
                )
            }
        }
    }

    private fun queryPurchases(client: BillingClient) {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            } else {
                markBillingUnavailable()
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val premiumPurchases = purchases.filter { PREMIUM_PRODUCT_ID in it.products }
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
