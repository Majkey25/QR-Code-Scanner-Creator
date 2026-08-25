package com.majkeylab.qrscannercreator

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private enum class CreatorType(@StringRes val label: Int) {
    TEXT(R.string.type_text),
    WEBSITE(R.string.type_website),
    WIFI(R.string.type_wifi),
    CONTACT(R.string.type_contact),
    EMAIL(R.string.type_email),
    PHONE(R.string.type_phone),
    SMS(R.string.type_sms),
    LOCATION(R.string.type_location),
    EVENT(R.string.type_event),
}

private data class CreatorForm(
    val type: CreatorType = CreatorType.TEXT,
    val text: String = "",
    val website: String = "",
    val ssid: String = "",
    val wifiPassword: String = "",
    val wifiSecurity: WifiSecurity = WifiSecurity.WPA,
    val wifiHidden: Boolean = false,
    val contactName: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val contactOrganization: String = "",
    val emailAddress: String = "",
    val emailSubject: String = "",
    val message: String = "",
    val phone: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val eventTitle: String = "",
    val eventStart: LocalDateTime,
    val eventEnd: LocalDateTime,
    val eventLocation: String = "",
    val eventDescription: String = "",
    val foreground: String = "#0B57F0",
    val finder: String = "#111111",
    val background: String = "#FFFFFF",
    val moduleShape: ModuleShape = ModuleShape.ROUNDED,
    val finderShape: ModuleShape = ModuleShape.SQUARE,
)

@Composable
fun CreatorScreen(
    pickedPhone: PickedPhone?,
    pickedLogo: Bitmap?,
    onPickPhone: () -> Unit,
    onPickLogo: () -> Unit,
    onClearLogo: () -> Unit,
    onShareImage: (QrImage) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier,
) {
    val focusManager = LocalFocusManager.current
    val defaultStart = remember { LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).plusHours(1) }
    var form by remember { mutableStateOf(CreatorForm(eventStart = defaultStart, eventEnd = defaultStart.plusHours(1))) }
    var appearanceExpanded by remember { mutableStateOf(false) }
    var generated by remember { mutableStateOf<QrImage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun update(next: CreatorForm) {
        form = next
        generated = null
        error = null
    }

    LaunchedEffect(pickedPhone?.selectionId) {
        pickedPhone?.let {
            update(
                form.copy(
                    type = CreatorType.CONTACT,
                    contactName = it.name,
                    contactPhone = it.number,
                ),
            )
        }
    }
    LaunchedEffect(pickedLogo) {
        generated = null
        error = null
    }

    LazyColumn(
        modifier = modifier,
        contentPadding =
            PaddingValues(
                start = 24.dp,
                top = contentPadding.calculateTopPadding() + 28.dp,
                end = 24.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            CreatorHero()
        }
        item {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        stringResource(R.string.content_type),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CreatorType.entries.size) { index ->
                            val option = CreatorType.entries[index]
                            FilterChip(
                                selected = form.type == option,
                                onClick = { update(form.copy(type = option)) },
                                shape = RoundedCornerShape(14.dp),
                                label = { Text(stringResource(option.label)) },
                            )
                        }
                    }
                    CreatorFields(form = form, onChange = ::update, onPickPhone = onPickPhone)
                }
            }
        }
        item {
            AppearanceFields(
                form = form,
                expanded = appearanceExpanded,
                hasLogo = pickedLogo != null,
                onExpandedChange = { appearanceExpanded = it },
                onChange = ::update,
                onPickLogo = onPickLogo,
                onClearLogo = onClearLogo,
            )
        }
        item {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    runCatching {
                        val payload = form.toDraft().payload().getOrThrow()
                        val style =
                            QrStyle(
                                foreground = form.foreground,
                                finder = form.finder,
                                background = form.background,
                                moduleShape = form.moduleShape,
                                finderShape = form.finderShape,
                            ).parse().getOrThrow()
                        val image = QrRenderer.renderPixels(payload, style)
                        pickedLogo?.let(image::withLogo) ?: image
                    }.onSuccess {
                        generated = it
                        error = null
                    }.onFailure {
                        generated = null
                        error = it.message
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(stringResource(R.string.generate_qr), fontWeight = FontWeight.SemiBold)
            }
        }
        error?.let { message ->
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        generated?.let { image ->
            item {
                val preview = remember(image) { image.toBitmap().asImageBitmap() }
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                    shadowElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            stringResource(R.string.preview),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .widthIn(max = 280.dp)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color.White)
                                    .padding(12.dp),
                        ) {
                            Image(
                                bitmap = preview,
                                contentDescription = stringResource(R.string.qr_preview),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        OutlinedButton(
                            onClick = { onShareImage(image) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(stringResource(R.string.share_qr_code))
                        }
                    }
                }
            }
        }
        item { MonetizationBanner() }
    }
}

@Composable
private fun CreatorHero() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_qr_add),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(
            stringResource(R.string.create_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(R.string.create_description),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CreatorFields(
    form: CreatorForm,
    onChange: (CreatorForm) -> Unit,
    onPickPhone: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (form.type) {
            CreatorType.TEXT ->
                QrField(
                    value = form.text,
                    label = R.string.text,
                    onValueChange = { onChange(form.copy(text = it)) },
                    singleLine = false,
                )
            CreatorType.WEBSITE ->
                QrField(
                    value = form.website,
                    label = R.string.website,
                    onValueChange = { onChange(form.copy(website = it)) },
                    keyboardType = KeyboardType.Uri,
                )
            CreatorType.WIFI -> {
                QrField(
                    value = form.ssid,
                    label = R.string.network_name,
                    onValueChange = { onChange(form.copy(ssid = it)) },
                )
                if (form.wifiSecurity == WifiSecurity.WPA) {
                    OutlinedTextField(
                        value = form.wifiPassword,
                        onValueChange = { onChange(form.copy(wifiPassword = it)) },
                        label = { Text(stringResource(R.string.password)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(18.dp),
                    )
                }
                Text(stringResource(R.string.security), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = form.wifiSecurity == WifiSecurity.WPA,
                        onClick = { onChange(form.copy(wifiSecurity = WifiSecurity.WPA)) },
                        label = { Text(stringResource(R.string.wpa_network)) },
                        shape = RoundedCornerShape(14.dp),
                    )
                    FilterChip(
                        selected = form.wifiSecurity == WifiSecurity.OPEN,
                        onClick = { onChange(form.copy(wifiSecurity = WifiSecurity.OPEN)) },
                        label = { Text(stringResource(R.string.open_network)) },
                        shape = RoundedCornerShape(14.dp),
                    )
                }
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .toggleable(
                                value = form.wifiHidden,
                                role = Role.Switch,
                                onValueChange = { onChange(form.copy(wifiHidden = it)) },
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.hidden_network))
                    Switch(
                        checked = form.wifiHidden,
                        onCheckedChange = null,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
            }
            CreatorType.CONTACT -> {
                QrField(
                    value = form.contactName,
                    label = R.string.name,
                    onValueChange = { onChange(form.copy(contactName = it)) },
                )
                QrField(
                    value = form.contactPhone,
                    label = R.string.phone_number,
                    onValueChange = { onChange(form.copy(contactPhone = it)) },
                    keyboardType = KeyboardType.Phone,
                )
                QrField(
                    value = form.contactEmail,
                    label = R.string.email_address,
                    onValueChange = { onChange(form.copy(contactEmail = it)) },
                    keyboardType = KeyboardType.Email,
                )
                QrField(
                    value = form.contactOrganization,
                    label = R.string.organization,
                    onValueChange = { onChange(form.copy(contactOrganization = it)) },
                )
                OutlinedButton(onClick = onPickPhone, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.pick_phone_contact))
                }
            }
            CreatorType.EMAIL -> {
                QrField(
                    value = form.emailAddress,
                    label = R.string.email_address,
                    onValueChange = { onChange(form.copy(emailAddress = it)) },
                    keyboardType = KeyboardType.Email,
                )
                QrField(
                    value = form.emailSubject,
                    label = R.string.email_subject,
                    onValueChange = { onChange(form.copy(emailSubject = it)) },
                )
                QrField(
                    value = form.message,
                    label = R.string.message,
                    onValueChange = { onChange(form.copy(message = it)) },
                    singleLine = false,
                )
            }
            CreatorType.PHONE ->
                QrField(
                    value = form.phone,
                    label = R.string.phone_number,
                    onValueChange = { onChange(form.copy(phone = it)) },
                    keyboardType = KeyboardType.Phone,
                )
            CreatorType.SMS -> {
                QrField(
                    value = form.phone,
                    label = R.string.phone_number,
                    onValueChange = { onChange(form.copy(phone = it)) },
                    keyboardType = KeyboardType.Phone,
                )
                QrField(
                    value = form.message,
                    label = R.string.message,
                    onValueChange = { onChange(form.copy(message = it)) },
                    singleLine = false,
                )
            }
            CreatorType.LOCATION -> {
                QrField(
                    value = form.latitude,
                    label = R.string.latitude,
                    onValueChange = { onChange(form.copy(latitude = it)) },
                    keyboardType = KeyboardType.Decimal,
                )
                QrField(
                    value = form.longitude,
                    label = R.string.longitude,
                    onValueChange = { onChange(form.copy(longitude = it)) },
                    keyboardType = KeyboardType.Decimal,
                )
            }
            CreatorType.EVENT -> {
                QrField(
                    value = form.eventTitle,
                    label = R.string.event_title,
                    onValueChange = { onChange(form.copy(eventTitle = it)) },
                )
                DateTimeButton(R.string.starts, form.eventStart) { onChange(form.copy(eventStart = it)) }
                DateTimeButton(R.string.ends, form.eventEnd) { onChange(form.copy(eventEnd = it)) }
                QrField(
                    value = form.eventLocation,
                    label = R.string.event_location,
                    onValueChange = { onChange(form.copy(eventLocation = it)) },
                )
                QrField(
                    value = form.eventDescription,
                    label = R.string.event_description,
                    onValueChange = { onChange(form.copy(eventDescription = it)) },
                    singleLine = false,
                )
            }
        }
    }
}

@Composable
private fun AppearanceFields(
    form: CreatorForm,
    expanded: Boolean,
    hasLogo: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onChange: (CreatorForm) -> Unit,
    onPickLogo: () -> Unit,
    onClearLogo: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = { onExpandedChange(!expanded) }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(if (expanded) R.string.hide_appearance else R.string.show_appearance),
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (expanded) {
                Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleMedium)
                ColorSwatches(
                    selected = form.foreground,
                    onSelected = { onChange(form.copy(foreground = it)) },
                )
                QrField(
                    value = form.foreground,
                    label = R.string.foreground_color,
                    onValueChange = { onChange(form.copy(foreground = it.uppercase())) },
                )
                QrField(
                    value = form.finder,
                    label = R.string.finder_color,
                    onValueChange = { onChange(form.copy(finder = it.uppercase())) },
                )
                QrField(
                    value = form.background,
                    label = R.string.background_color,
                    onValueChange = { onChange(form.copy(background = it.uppercase())) },
                )
                ShapePicker(
                    label = R.string.module_style,
                    selected = form.moduleShape,
                    onSelected = { onChange(form.copy(moduleShape = it)) },
                )
                ShapePicker(
                    label = R.string.corner_style,
                    selected = form.finderShape,
                    onSelected = { onChange(form.copy(finderShape = it)) },
                )
                Text(stringResource(R.string.logo), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPickLogo, modifier = Modifier.weight(1f)) {
                        Text(stringResource(if (hasLogo) R.string.logo_selected else R.string.choose_logo))
                    }
                    if (hasLogo) {
                        TextButton(onClick = onClearLogo) { Text(stringResource(R.string.remove_logo)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatches(
    selected: String,
    onSelected: (String) -> Unit,
) {
    Text(stringResource(R.string.quick_colors), style = MaterialTheme.typography.labelLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(PRESET_COLORS.size) { index ->
            val value = PRESET_COLORS[index]
            Surface(
                modifier =
                    Modifier.size(40.dp)
                        .semantics {
                            contentDescription = value
                            role = Role.Button
                        }
                        .clickable { onSelected(value) },
                shape = CircleShape,
                color = value.toComposeColor(),
                border =
                    BorderStroke(
                        if (value.equals(selected, ignoreCase = true)) 3.dp else 1.dp,
                        if (value.equals(selected, ignoreCase = true)) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            ) {}
        }
    }
}

@Composable
private fun ShapePicker(
    @StringRes label: Int,
    selected: ModuleShape,
    onSelected: (ModuleShape) -> Unit,
) {
    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ModuleShape.entries.size) { index ->
            val shape = ModuleShape.entries[index]
            FilterChip(
                selected = selected == shape,
                onClick = { onSelected(shape) },
                shape = RoundedCornerShape(14.dp),
                label = {
                    Text(
                        stringResource(
                            when (shape) {
                                ModuleShape.SQUARE -> R.string.square
                                ModuleShape.ROUNDED -> R.string.rounded
                                ModuleShape.DOTS -> R.string.dots
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun QrField(
    value: String,
    @StringRes label: Int,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(18.dp),
    )
}

@Composable
private fun DateTimeButton(
    @StringRes label: Int,
    value: LocalDateTime,
    onChange: (LocalDateTime) -> Unit,
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> onChange(LocalDateTime.of(year, month + 1, day, hour, minute)) },
                        value.hour,
                        value.minute,
                        true,
                    ).show()
                },
                value.year,
                value.monthValue - 1,
                value.dayOfMonth,
            ).show()
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text("${stringResource(label)}: ${value.format(DISPLAY_DATE_TIME)}")
    }
}

private fun CreatorForm.toDraft(): QrDraft =
    when (type) {
        CreatorType.TEXT -> QrDraft.Text(text)
        CreatorType.WEBSITE -> QrDraft.Website(website)
        CreatorType.WIFI -> QrDraft.Wifi(ssid, wifiPassword, wifiSecurity, wifiHidden)
        CreatorType.CONTACT -> QrDraft.Contact(contactName, contactPhone, contactEmail, contactOrganization)
        CreatorType.EMAIL -> QrDraft.Email(emailAddress, emailSubject, message)
        CreatorType.PHONE -> QrDraft.Phone(phone)
        CreatorType.SMS -> QrDraft.Sms(phone, message)
        CreatorType.LOCATION -> QrDraft.Location(latitude, longitude)
        CreatorType.EVENT ->
            QrDraft.Calendar(
                eventTitle,
                eventStart.toString(),
                eventEnd.toString(),
                eventLocation,
                eventDescription,
            )
    }

private val DISPLAY_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
private val PRESET_COLORS = listOf("#111111", "#3857A6", "#5B4BB7", "#0F766E", "#C2410C", "#B42318")

private fun String.toComposeColor(): Color = Color(("FF" + removePrefix("#")).toLong(16))
