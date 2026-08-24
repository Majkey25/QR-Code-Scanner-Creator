package com.majkeylab.qrscannercreator

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.View
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class AdIds(
    val appId: String,
    val bannerAdUnitId: String,
    val interstitialAdUnitId: String,
)

internal object MonetizationConfig {
    fun adsForDebuggable(debuggable: Boolean): AdIds =
        if (debuggable) {
            AdIds(
                appId = "ca-app-pub-6991329209066655~5561017627",
                bannerAdUnitId = "ca-app-pub-3940256099942544/9214589741",
                interstitialAdUnitId = "ca-app-pub-3940256099942544/1033173712",
            )
        } else {
            AdIds(
                appId = "ca-app-pub-6991329209066655~5561017627",
                bannerAdUnitId = "ca-app-pub-6991329209066655/6914512227",
                interstitialAdUnitId = "ca-app-pub-6991329209066655/1662185545",
            )
        }
}

internal class ConsentGate {
    var canRequestAds by mutableStateOf(false)
        private set
    var privacyOptionsRequired by mutableStateOf(false)
        private set
    private var requestStarted = false

    fun beginRequest(): Boolean {
        if (requestStarted) return false
        requestStarted = true
        return true
    }

    fun update(canRequestAds: Boolean, privacyOptionsRequired: Boolean) {
        this.canRequestAds = canRequestAds
        this.privacyOptionsRequired = privacyOptionsRequired
    }
}

internal object ConsentCoordinator {
    val gate = ConsentGate()

    fun ensureRequested(activity: Activity) {
        if (!gate.beginRequest()) return
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val parameters =
            ConsentRequestParameters.Builder().apply {
                if (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                    setConsentDebugSettings(
                        ConsentDebugSettings.Builder(activity)
                            .setDebugGeography(
                                ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA,
                            ).build(),
                    )
                }
            }.build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            parameters,
            {
                updateGate(consentInformation)
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    updateGate(consentInformation)
                }
            },
            { updateGate(consentInformation) },
        )
        updateGate(consentInformation)
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            updateGate(UserMessagingPlatform.getConsentInformation(activity))
        }
    }

    private fun updateGate(consentInformation: ConsentInformation) {
        gate.update(
            canRequestAds = consentInformation.canRequestAds(),
            privacyOptionsRequired =
                consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
        )
    }
}

private object AdRuntime {
    private const val PREFS_NAME = "ads"
    private const val COMPLETED_SCANS_KEY = "completed_scans"
    private const val LAST_INTERSTITIAL_KEY = "last_interstitial"

    private var dueScan: Int? = null

    fun recordCompletedScan(context: Context) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = preferences.getInt(COMPLETED_SCANS_KEY, 0)
        val next = if (current == Int.MAX_VALUE) 1 else current + 1
        preferences.edit { putInt(COMPLETED_SCANS_KEY, next) }
        dueScan = next.takeIf { it % 5 == 0 }
    }

    fun isDue(context: Context, nowMillis: Long): Boolean {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val completed = preferences.getInt(COMPLETED_SCANS_KEY, 0)
        val lastShown = preferences.getLong(LAST_INTERSTITIAL_KEY, 0L).takeIf { it > 0L }
        return dueScan == completed && isInterstitialDue(completed, nowMillis, lastShown)
    }

    fun consume() {
        dueScan = null
    }

    fun markShown(context: Context, nowMillis: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putLong(LAST_INTERSTITIAL_KEY, nowMillis) }
        dueScan = null
    }
}

private suspend fun ensureAdsReady(context: Context, ads: AdIds) {
    withContext(Dispatchers.IO) {
        if (!MobileAds.isInitialized) {
            MobileAds.initialize(context, InitializationConfig.Builder(ads.appId).build())
        }
    }
    if (InterstitialAdPreloader.getConfiguration(ads.interstitialAdUnitId) == null) {
        InterstitialAdPreloader.start(
            ads.interstitialAdUnitId,
            PreloadConfiguration(AdRequest.Builder(ads.interstitialAdUnitId).build()),
        )
    }
}

@Composable
internal fun MonetizationBanner() {
    val activity = LocalActivity.current ?: return
    val premiumState = PremiumController.state
    LaunchedEffect(activity, premiumState.premium, premiumState.entitlementVerified) {
        if (premiumState.entitlementVerified && !premiumState.premium) {
            ConsentCoordinator.ensureRequested(activity)
        }
    }
    if (
        !shouldShowAds(
            premium = premiumState.premium,
            entitlementVerified = premiumState.entitlementVerified,
            canRequestAds = ConsentCoordinator.gate.canRequestAds,
        )
    ) return

    val ads = remember(activity) { activity.adIds() }
    val description = stringResource(R.string.advertisement)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(1)
        val adSize = remember(widthDp) { AdSize.getInlineAdaptiveBannerAdSize(widthDp, 120) }
        val adView =
            remember(activity, widthDp, description) {
                AdView(activity).apply {
                    contentDescription = description
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                }
            }
        var visible by remember(adView) { mutableStateOf(true) }
        var loaded by remember(adView) { mutableStateOf(false) }

        LaunchedEffect(adView, adSize) {
            try {
                ensureAdsReady(activity.applicationContext, ads)
                adView.loadAd(
                    BannerAdRequest.Builder(ads.bannerAdUnitId, adSize).build(),
                    object : AdLoadCallback<BannerAd> {
                        override fun onAdLoaded(ad: BannerAd) {
                            loaded = true
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            visible = false
                        }
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RuntimeException) {
                visible = false
            }
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }

        if (visible && loaded) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
            ) {
                Column {
                    Text(
                        description,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider()
                    AndroidView(
                        factory = { adView },
                        modifier = Modifier.fillMaxWidth().height(adSize.height.coerceIn(50, 120).dp),
                    )
                }
            }
        }
    }
}

internal fun recordCompletedScan(context: Context) {
    AdRuntime.recordCompletedScan(context.applicationContext)
}

internal fun showScanInterstitial(activity: Activity, onComplete: () -> Unit) {
    val state = PremiumController.state
    if (
        activity.isFinishing || activity.isDestroyed || state.premium ||
        !state.entitlementVerified || !ConsentCoordinator.gate.canRequestAds
    ) {
        onComplete()
        return
    }
    val now = System.currentTimeMillis()
    if (!AdRuntime.isDue(activity, now)) {
        onComplete()
        return
    }
    AdRuntime.consume()
    val ad = InterstitialAdPreloader.pollAd(activity.adIds().interstitialAdUnitId)
    if (ad == null) {
        onComplete()
        return
    }
    var completed = false
    fun finish() {
        if (completed) return
        completed = true
        onComplete()
    }
    AdRuntime.markShown(activity, now)
    ad.adEventCallback =
        object : InterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() = finish()

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) = finish()
        }
    try {
        ad.show(activity)
    } catch (_: RuntimeException) {
        finish()
    }
}

@Composable
internal fun PrivacyOptionsLink() {
    val activity = LocalActivity.current
    if (
        activity != null && ConsentCoordinator.gate.privacyOptionsRequired
    ) {
        TextButton(
            onClick = { ConsentCoordinator.showPrivacyOptions(activity) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.privacy_options))
        }
    }
}

private fun Activity.adIds(): AdIds =
    MonetizationConfig.adsForDebuggable(
        applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
    )
