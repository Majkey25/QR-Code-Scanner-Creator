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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun PremiumPanel() {
    val activity = LocalActivity.current ?: return
    val state = PremiumController.state
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
                when {
                    state.pending -> Text(stringResource(R.string.premium_pending))
                    state.error && state.purchaseAvailable -> Text(
                        stringResource(R.string.premium_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                    !state.checking && !state.purchaseAvailable ->
                        Text(stringResource(R.string.premium_unavailable))
                }
                Button(
                    onClick = { PremiumController.launchPurchase(activity) },
                    enabled = state.purchaseAvailable && !state.pending && !state.checking,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(
                        state.formattedPrice?.let {
                            stringResource(R.string.premium_buy_price, it)
                        } ?: stringResource(R.string.premium_buy),
                    )
                }
                TextButton(
                    onClick = { PremiumController.refresh(activity) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.premium_restore))
                }
            }
        }
    }
}
