package com.majkeylab.qrscannercreator

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class AppTab(@StringRes val label: Int, val icon: Int) {
    SCAN(R.string.scan, R.drawable.ic_qr_code),
    CREATE(R.string.create, R.drawable.ic_qr_add),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrApp(
    scanResult: ScanResult?,
    scanError: String?,
    actionNotice: String?,
    actionError: String?,
    pickedPhone: PickedPhone?,
    pickedLogo: Bitmap?,
    onScan: () -> Unit,
    onPerformAction: (ScanResult) -> Unit,
    onCopy: (String) -> Unit,
    onShareText: (String) -> Unit,
    onPickPhone: () -> Unit,
    onPickLogo: () -> Unit,
    onClearLogo: () -> Unit,
    onShareImage: (QrImage) -> Unit,
) {
    QrTheme {
        var tab by remember { mutableStateOf(AppTab.SCAN) }
        var menuExpanded by remember { mutableStateOf(false) }
        var aboutVisible by remember { mutableStateOf(false) }

        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(tab.backgroundColors())),
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    CenterAlignedTopAppBar(
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent,
                            ),
                        title = {
                            Text(
                                stringResource(tab.label),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        actions = {
                            Box {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                ) {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(
                                            painterResource(R.drawable.ic_more_vert),
                                            contentDescription = stringResource(R.string.more_options),
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.about)) },
                                        onClick = {
                                            menuExpanded = false
                                            aboutVisible = true
                                        },
                                    )
                                }
                            }
                        },
                    )
                },
                bottomBar = {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            shape = RoundedCornerShape(32.dp),
                            shadowElevation = 2.dp,
                        ) {
                            NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                                AppTab.entries.forEach { option ->
                                    NavigationBarItem(
                                        selected = tab == option,
                                        onClick = { tab = option },
                                        colors =
                                            NavigationBarItemDefaults.colors(
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            ),
                                        icon = {
                                            Icon(
                                                painterResource(option.icon),
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        },
                                        label = { Text(stringResource(option.label)) },
                                    )
                                }
                            }
                        }
                    }
                },
            ) { innerPadding ->
                when (tab) {
                    AppTab.SCAN ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            ScanScreen(
                                result = scanResult,
                                scanError = scanError,
                                actionNotice = actionNotice,
                                actionError = actionError,
                                onScan = onScan,
                                onPerformAction = onPerformAction,
                                onCopy = onCopy,
                                onShare = onShareText,
                                contentPadding = innerPadding,
                                modifier = Modifier.fillMaxHeight().widthIn(max = 680.dp).fillMaxWidth(),
                            )
                        }
                    AppTab.CREATE ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            CreatorScreen(
                                pickedPhone = pickedPhone,
                                pickedLogo = pickedLogo,
                                onPickPhone = onPickPhone,
                                onPickLogo = onPickLogo,
                                onClearLogo = onClearLogo,
                                onShareImage = onShareImage,
                                contentPadding = innerPadding,
                                modifier = Modifier.fillMaxHeight().widthIn(max = 680.dp).fillMaxWidth(),
                            )
                        }
                }
            }
        }

        if (aboutVisible) AboutDialog(onDismiss = { aboutVisible = false })
    }
}

@Composable
private fun AppTab.backgroundColors(): List<Color> {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        AppTab.SCAN -> listOf(colors.primaryContainer, colors.surface, colors.background)
        AppTab.CREATE -> listOf(colors.secondaryContainer, colors.surface, colors.background)
    }
}

@Composable
private fun ScanScreen(
    result: ScanResult?,
    scanError: String?,
    actionNotice: String?,
    actionError: String?,
    onScan: () -> Unit,
    onPerformAction: (ScanResult) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier,
) {
    LazyColumn(
        modifier =
            modifier.padding(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
        contentPadding =
            PaddingValues(
                start = 24.dp,
                top = 28.dp,
                end = 24.dp,
                bottom = 28.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScanHero(
                onScan = onScan,
            )
        }
        item { MonetizationBanner() }
        scanError?.let { error -> item { StatusText(error, isError = true) } }
        actionNotice?.let { notice -> item { StatusText(notice, isError = false) } }
        actionError?.let { error -> item { StatusText(error, isError = true) } }
        result?.let { scanned ->
            item {
                ResultPanel(
                    result = scanned,
                    onPrimary = { onPerformAction(scanned) },
                    onCopy = { onCopy(scanned.raw) },
                    onShare = { onShare(scanned.raw) },
                )
            }
        }
    }
}

@Composable
private fun ScanHero(onScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_qr_code),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(
            stringResource(R.string.scan_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(R.string.scan_description),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onScan,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(stringResource(R.string.scan_button), fontWeight = FontWeight.SemiBold)
        }
        Text(
            stringResource(R.string.scan_privacy),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ResultPanel(
    result: ScanResult,
    onPrimary: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.scan_result),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                result.title(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                result.summary(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(result.action.label()))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (result.action != ScanAction.COPY) {
                    OutlinedButton(
                        onClick = onCopy,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.copy))
                    }
                }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.share))
                }
            }
        }
    }
}

@Composable
private fun StatusText(text: String, isError: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color =
            if (isError) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            },
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color =
                if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ScanResult.title(): String =
    stringResource(
        when (this) {
            is ScanResult.Web -> R.string.type_website
            is ScanResult.Wifi -> R.string.type_wifi
            is ScanResult.Contact -> R.string.type_contact
            is ScanResult.Phone -> R.string.type_phone
            is ScanResult.Email -> R.string.type_email
            is ScanResult.Sms -> R.string.type_sms
            is ScanResult.Geo -> R.string.type_location
            is ScanResult.Event -> R.string.type_event
            is ScanResult.Text -> R.string.type_text
        },
    )

@Composable
private fun ScanResult.summary(): String =
    when (this) {
        is ScanResult.Web -> url
        is ScanResult.Wifi -> ssid
        is ScanResult.Contact -> listOf(name, phone, email, organization).filter(String::isNotBlank).joinToString("\n")
        is ScanResult.Phone -> number
        is ScanResult.Email -> listOf(address, subject, body).filter(String::isNotBlank).joinToString("\n")
        is ScanResult.Sms -> listOf(number, message).filter(String::isNotBlank).joinToString("\n")
        is ScanResult.Geo -> "$latitude, $longitude"
        is ScanResult.Event -> listOf(title, location, description).filter(String::isNotBlank).joinToString("\n")
        is ScanResult.Text -> raw.ifBlank { stringResource(R.string.no_content) }
    }

@StringRes
private fun ScanAction.label(): Int =
    when (this) {
        ScanAction.OPEN_WEB -> R.string.open_link
        ScanAction.CONNECT_WIFI -> R.string.connect_wifi
        ScanAction.ADD_CONTACT -> R.string.add_contact
        ScanAction.DIAL -> R.string.call
        ScanAction.EMAIL -> R.string.write_email
        ScanAction.SMS -> R.string.write_sms
        ScanAction.MAPS -> R.string.open_map
        ScanAction.ADD_EVENT -> R.string.add_event
        ScanAction.COPY -> R.string.copy
    }

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.about_description))
                Text(
                    stringResource(R.string.version, BuildConfig.VERSION_NAME),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                PremiumPanel()
                AboutLink(R.string.privacy_policy) { uriHandler.openUri(PRIVACY_URL) }
                PrivacyOptionsLink()
                AboutLink(R.string.third_party_notices) { uriHandler.openUri(NOTICES_URL) }
                AboutLink(R.string.source_code) { uriHandler.openUri(SOURCE_URL) }
                Button(
                    onClick = { uriHandler.openUri(SUPPORT_URL) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    border = BorderStroke(1.dp, Color(0xFF111111)),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFDD00),
                            contentColor = Color(0xFF111111),
                        ),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_coffee),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(stringResource(R.string.support_coffee))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        shape = RoundedCornerShape(30.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    )
}

@Composable
private fun AboutLink(@StringRes label: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(label), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun QrTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
            dark ->
                darkColorScheme(
                    primary = Color(0xFFB8C4FF),
                    onPrimary = Color(0xFF16265F),
                    primaryContainer = Color(0xFF303F78),
                    tertiaryContainer = Color(0xFF324B4B),
                    background = Color(0xFF111318),
                    surface = Color(0xFF181A20),
                    surfaceVariant = Color(0xFF292B32),
                )
            else ->
                lightColorScheme(
                    primary = Color(0xFF3857A6),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFDDE4FF),
                    tertiary = Color(0xFF3F6361),
                    tertiaryContainer = Color(0xFFC1E8E4),
                    background = Color(0xFFFFFBFF),
                    surface = Color(0xFFFFFBFF),
                    surfaceVariant = Color(0xFFE8E8F0),
                )
        }
    val shapes =
        Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(26.dp),
            extraLarge = RoundedCornerShape(32.dp),
        )
    val typography =
        Typography(
            headlineLarge =
                MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp,
                ),
            headlineMedium =
                MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 29.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                ),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    MaterialTheme(
        colorScheme = colors,
        shapes = shapes,
        typography = typography,
        content = content,
    )
}

private const val SOURCE_URL = "https://github.com/Majkey25/QR-Code-Scanner-Creator"
private const val PRIVACY_URL = "https://majkey25.github.io/QR-Code-Scanner-Creator/privacy.html"
private const val NOTICES_URL = "$SOURCE_URL/blob/main/THIRD_PARTY_NOTICES.md"
private const val SUPPORT_URL = "https://www.buymeacoffee.com/majkey"
