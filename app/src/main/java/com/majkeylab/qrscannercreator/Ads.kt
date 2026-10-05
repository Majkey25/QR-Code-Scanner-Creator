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
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
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
)

internal object MonetizationConfig {
    fun adsForDebuggable(debuggable: Boolean): AdIds =
        if (debuggable) {
            AdIds(
                appId = "ca-app-pub-6991329209066655~5561017627",
                bannerAdUnitId = "ca-app-pub-3940256099942544/9214589741",
            )
        } else {
            AdIds(
                appId = "ca-app-pub-6991329209066655~5561017627",
                bannerAdUnitId = "ca-app-pub-6991329209066655/6914512227",
            )
        }
}

internal class ConsentGate {
    var canRequestAds by mutableStateOf(false)
        private set
    var privacyOptionsRequired by mutableStateOf(false)
        private set
    var revision by mutableStateOf(0L)
        private set
    private var requestStarted = false

    fun beginRequest(): Boolean {
        if (requestStarted) return false
        requestStarted = true
        return true
    }

    fun update(canRequestAds: Boolean, privacyOptionsRequired: Boolean, invalidateAds: Boolean = false) {
        if (this.canRequestAds != canRequestAds || invalidateAds) revision++
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
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            updateGate(UserMessagingPlatform.getConsentInformation(activity), invalidateAds = error == null)
        }
    }

    private fun updateGate(consentInformation: ConsentInformation, invalidateAds: Boolean = false) {
        gate.update(
            canRequestAds = consentInformation.canRequestAds(),
            privacyOptionsRequired =
                consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
            invalidateAds = invalidateAds,
        )
    }
}

private fun mayRequestAds(revision: Long): Boolean {
    val state = PremiumController.state
    val gate = ConsentCoordinator.gate
    return revision == gate.revision && shouldShowAds(state.premium, state.entitlementVerified, gate.canRequestAds)
}

private suspend fun ensureAdsReady(context: Context, ads: AdIds, revision: Long): Boolean {
    if (!mayRequestAds(revision)) return false
    withContext(Dispatchers.IO) {
        if (!MobileAds.isInitialized) {
            MobileAds.initialize(context, InitializationConfig.Builder(ads.appId).build())
        }
    }
    return mayRequestAds(revision) && MobileAds.isInitialized
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
    val revision = ConsentCoordinator.gate.revision
    val description = stringResource(R.string.advertisement)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(1)
        val adSize = remember(widthDp) { AdSize.getInlineAdaptiveBannerAdSize(widthDp, 120) }
        val adView =
            remember(activity, widthDp, description, revision) {
                AdView(activity).apply {
                    contentDescription = description
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                }
            }
        var visible by remember(adView) { mutableStateOf(true) }
        var loaded by remember(adView) { mutableStateOf(false) }
        var active by remember(adView) { mutableStateOf(true) }

        LaunchedEffect(adView, adSize) {
            try {
                if (!ensureAdsReady(activity.applicationContext, ads, revision) || !active) return@LaunchedEffect
                adView.loadAd(
                    BannerAdRequest.Builder(ads.bannerAdUnitId, adSize).build(),
                    object : AdLoadCallback<BannerAd> {
                        override fun onAdLoaded(ad: BannerAd) {
                            if (active && mayRequestAds(revision)) loaded = true
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            if (active) visible = false
                        }
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RuntimeException) {
                visible = false
            }
        }
        DisposableEffect(adView) {
            onDispose {
                active = false
                adView.destroy()
            }
        }

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
