package com.majkeylab.qrscannercreator

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun PremiumPanel() {
    val activity = LocalActivity.current ?: return
    val state = PremiumController.state
    val uriHandler = LocalUriHandler.current
    var managementError by remember { mutableStateOf(false) }
    LaunchedEffect(activity) { PremiumController.refresh(activity) }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.premium_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(
                    if (state.premium) R.string.premium_active else R.string.premium_description,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!state.premium) {
                Text(
                    stringResource(R.string.premium_terms),
                    style = MaterialTheme.typography.bodySmall,
                )
                when {
                    state.pending -> Text(stringResource(R.string.premium_pending))
                    state.error && state.purchaseAvailable -> Text(
                        stringResource(R.string.premium_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                    !state.checking && !state.purchaseAvailable ->
                        Text(stringResource(R.string.premium_unavailable))
                }
                OutlinedButton(
                    onClick = { PremiumController.launchPurchase(activity, PremiumPlan.Monthly) },
                    enabled = state.monthlyAvailable && state.canStartPurchase,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(
                        state.monthlyPrice?.let {
                            stringResource(R.string.premium_monthly_price, it)
                        } ?: stringResource(R.string.premium_monthly),
                    )
                }
                Button(
                    onClick = { PremiumController.launchPurchase(activity, PremiumPlan.Lifetime) },
                    enabled = state.lifetimeAvailable && state.canStartPurchase,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(
                        state.lifetimePrice?.let {
                            stringResource(R.string.premium_lifetime_price, it)
                        } ?: stringResource(R.string.premium_lifetime),
                    )
                }
            }
            TextButton(
                onClick = { PremiumController.refresh(activity) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.premium_restore))
            }
            TextButton(
                onClick = { managementError = !openPlaySubscriptions(uriHandler::openUri) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.premium_manage))
            }
            if (managementError) {
                Text(stringResource(R.string.premium_manage_error), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
