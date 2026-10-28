package com.phoenix.qiur.screen.home.component

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.SizeResolver
import com.phoenix.qiur.R
import com.phoenix.qiur.utils.CodeContent
import com.phoenix.qiur.utils.CodeContentType
import com.phoenix.qiur.utils.CodeResult
import kotlinx.coroutines.launch

class CodeActions(
    val search: (String) -> Unit,
    val copy: (String) -> Unit,
    val share: (String) -> Unit,
    val launch: (Intent) -> Unit,
    val url: (String) -> Unit,
)

@Composable
internal fun ResultScreen(
    bitmap: Bitmap?,
    results: List<CodeResult>,
    onClickClose: () -> Unit,
    snackbar: (String) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val uriHandler = LocalUriHandler.current

    fun launchIntent(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No app can handle this", Toast.LENGTH_SHORT).show()
        }
    }

    val wifiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    val actions = CodeActions(
        search = {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, it)
            }
            launchIntent(intent)
        },
        copy = {
            coroutineScope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Copied!", it)))
                snackbar("Copied!")
            }
        },
        share = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                this.type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, it)
            }
            launchIntent(Intent.createChooser(intent, "Share"))
        },
        launch = { launchIntent(it) },
        url = { uriHandler.openUri(it) },
    )

    Box {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(results) { code ->
                Column(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "${code.format} - ${code.content?.type}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(Modifier.height(4.dp))
                    when (code.content) {
                        is CodeContent.Text -> TextContent(code.content, actions)
                        is CodeContent.Url -> UrlContent(code.content, actions)
                        is CodeContent.AppStoreLink -> AppStoreContent(code.content, actions)
                        is CodeContent.Phone -> PhoneContent(code.content, actions)
                        is CodeContent.Sms -> SmsContent(code.content, actions)
                        is CodeContent.Email -> EmailContent(code.content, actions)
                        is CodeContent.Contact -> ContactContent(code.content, actions)
                        is CodeContent.Wifi -> WifiContent(code.content, actions) {
                            connectWifi(
                                wifi = it,
                                wifiLauncher = { wifi -> wifiLauncher.launch(wifi) },
                                openWifiSetting = { launchIntent(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                            )
                        }

                        is CodeContent.Geo -> GeoContent(code.content, actions)
                        is CodeContent.CalendarEvent -> CalendarContent(code.content, actions)
                        is CodeContent.EpcPayment -> EpcContent(code.content, actions)
                        is CodeContent.UpiPayment -> UpiContent(code.content, actions)
                        is CodeContent.Crypto -> CryptoContent(code.content, actions)
                        null -> Text("Nothing readable in this code", fontSize = 18.sp)
                    }
                }
            }
            item {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(bitmap)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Selected Photo",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
        IconButton(
            onClick = { onClickClose() },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "Close popup",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val CodeContentType.label: String
    get() = when (this) {
        CodeContentType.TEXT -> "Text"
        CodeContentType.URL -> "Link"
        CodeContentType.APP_STORE -> "App store link"
        CodeContentType.PHONE -> "Phone number"
        CodeContentType.SMS -> "SMS"
        CodeContentType.EMAIL -> "Email"
        CodeContentType.CONTACT_VCARD, CodeContentType.CONTACT_MECARD -> "Contact"
        CodeContentType.WIFI -> "Wi-Fi"
        CodeContentType.GEO -> "Location"
        CodeContentType.CALENDAR_EVENT -> "Calendar event"
        CodeContentType.PAYMENT_EPC -> "SEPA payment"
        CodeContentType.PAYMENT_UPI -> "UPI payment"
        CodeContentType.CRYPTO -> "Crypto payment"
    }

@Composable
private fun Field(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Light)
        SelectionContainer {
            Text(text = value, fontSize = 18.sp)
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ActionRow(vararg actions: CardAction) {
    Spacer(Modifier.height(4.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEachIndexed { i, action ->
            if (i == 0) {
                Button(onClick = action.onClick) { Text(action.label) }
            } else {
                FilledTonalButton(onClick = action.onClick) { Text(action.label) }
            }
        }
    }
}

@Composable
private fun TextContent(content: CodeContent.Text, actions: CodeActions) {
    Field(content.type.label, content.raw)
    ActionRow(
        CardAction("Search") { actions.search(content.raw) },
        actionCopy(content.raw, actions.copy),
        actionShare(content.raw, actions.share)
    )
}

@Composable
private fun UrlContent(content: CodeContent.Url, actions: CodeActions) {
    Field(content.type.label, content.url)
    ActionRow(
        CardAction("Open") { actions.url(content.url) },
        actionCopy(content.url, actions.copy),
        actionShare(content.url, actions.share)
    )
}

@Composable
private fun AppStoreContent(content: CodeContent.AppStoreLink, actions: CodeActions) {
    Field(
        "Store", when (content.platform) {
            CodeContent.AppStorePlatform.GOOGLE_PLAY -> "Google Play"
            CodeContent.AppStorePlatform.APPLE_APP_STORE -> "App Store"
            CodeContent.AppStorePlatform.UNKNOWN -> null
        }
    )
    Field("App ID", content.appId)
    Field("Link", content.raw)
    ActionRow(
        actionOpen("Open", content.raw.toUri(), actions.launch),
        actionCopy(content.raw, actions.copy),
        actionShare(content.raw, actions.share)
    )
}

@Composable
private fun PhoneContent(content: CodeContent.Phone, actions: CodeActions) {
    Field("Phone number", content.number)
    ActionRow(
        actionDial(content.number, actions.launch),
        actionCopy(content.number, actions.copy),
        actionShare(content.number, actions.share)
    )
}

@Composable
private fun SmsContent(content: CodeContent.Sms, actions: CodeActions) {
    Field("To", content.number)
    Field("Message", content.message)
    ActionRow(
        actionSendSMS(content.number, content.message.orEmpty(), actions.launch),
        actionCopy(content.raw, actions.copy),
        actionShare(content.raw, actions.share)
    )
}

@Composable
private fun EmailContent(content: CodeContent.Email, actions: CodeActions) {
    Field("To", content.to.joinToString(", "))
    Field("Cc", content.cc.joinToString(", "))
    Field("Bcc", content.bcc.joinToString(", "))
    Field("Subject", content.subject)
    Field("Body", content.body)
    ActionRow(
        actionComposeEmail(
            content.to,
            content.cc,
            content.bcc,
            content.subject,
            content.body,
            actions.launch
        ),
        actionCopy(content.to.joinToString(", "), actions.copy),
        actionShare(content.to.joinToString(", "), actions.share)
    )
}

@Composable
private fun ContactContent(content: CodeContent.Contact, actions: CodeActions) {
    Field("Name", content.name)
    Field("Organization", content.organization)
    Field("Job title", content.jobTitle)
    Field("Phone", content.phones.joinToString("\n"))
    Field("Email", content.emails.joinToString("\n"))
    Field("Website", content.websites.joinToString("\n"))
    Field("Address", content.address)
    Field("Note", content.note)
    ActionRow(
        actionMakeContact(content, actions.launch),
        actionCopy(content.raw, actions.copy),
        actionShare(content.raw, actions.share)
    )
}

@Composable
private fun WifiContent(
    content: CodeContent.Wifi,
    actions: CodeActions,
    connectWifi: (CodeContent.Wifi) -> Unit
) {
    Field("Network", content.ssid)
    Field("Password", content.password)
    Field(
        "Security", when (content.security) {
            CodeContent.WifiSecurity.WPA -> "WPA / WPA2 / WPA3"
            CodeContent.WifiSecurity.WEP -> "WEP"
            CodeContent.WifiSecurity.NONE -> "Open"
            CodeContent.WifiSecurity.UNKNOWN -> "Unknown"
        }
    )
    if (content.hidden) Field("Hidden network", "Yes")
    if (content.password != null)
        ActionRow(
            CardAction("Connect") { connectWifi(content) },
            CardAction("Copy password") { actions.copy(content.password) }
        )
    else
        ActionRow(
            CardAction("Connect") { connectWifi(content) }
        )
}

@Composable
private fun GeoContent(content: CodeContent.Geo, actions: CodeActions) {
    val coordinates = "${content.latitude}, ${content.longitude}"
    Field("Coordinates", coordinates)
    Field("Altitude", content.altitude?.let { "$it m" })
    Field("Search query", content.query)
    ActionRow(
        actionOpen("Open in maps", content.raw.toUri(), actions.launch),
        actionCopy(coordinates, actions.copy),
        actionShare(coordinates, actions.share)
    )
}

@Composable
private fun CalendarContent(content: CodeContent.CalendarEvent, actions: CodeActions) {
    val context = LocalContext.current
    val start = parseIcalDate(content.start)
    val end = parseIcalDate(content.end)

    fun format(date: ParsedDate?, fallback: String?): String? {
        if (date == null) return fallback
        var flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
        flags =
            if (date.allDay) flags or DateUtils.FORMAT_UTC else flags or DateUtils.FORMAT_SHOW_TIME
        return DateUtils.formatDateTime(context, date.millis, flags)
    }

    Field("Title", content.summary)
    Field("Starts", format(start, content.start))
    Field("Ends", format(end, content.end))
    Field("Location", content.location)
    Field("Description", content.description)
    ActionRow(
        actionAddCalendar(content, start, end, actions.launch),
        actionCopy(content.raw, actions.copy),
        actionShare(content.raw, actions.share)
    )
}

@Composable
private fun EpcContent(content: CodeContent.EpcPayment, actions: CodeActions) {
    val amount = listOfNotNull(content.amount?.toPlainString(), content.currency).joinToString(" ")
    Field("Beneficiary", content.beneficiaryName)
    Field("IBAN", content.iban)
    Field("BIC", content.bic)
    Field("Amount", amount)
    Field("Reference", content.structuredReference)
    Field("Description", content.remittanceText)
    Field("Info", content.info)
    ActionRow(
        CardAction("Copy IBAN") { actions.copy(content.iban) },
        CardAction("Share") { actions.share(content.raw) })
}

@Composable
private fun UpiContent(content: CodeContent.UpiPayment, actions: CodeActions) {
    val amount = listOfNotNull(content.amount?.toPlainString(), content.currency).joinToString(" ")
    Field("Payee", content.payeeName)
    Field("UPI ID", content.payeeAddress)
    Field("Amount", amount)
    Field("Note", content.note)
    Field("Reference", content.transactionRef)
    ActionRow(
        actionOpen("Pay", content.raw.toUri(), actions.launch),
        CardAction("Copy UPI ID") { actions.copy(content.payeeAddress) }
    )
}

@Composable
private fun CryptoContent(content: CodeContent.Crypto, actions: CodeActions) {
    Field("Currency", content.currency.replaceFirstChar { it.uppercase() })
    Field("Address", content.address)
    Field("Amount", content.amount?.toPlainString())
    Field("Label", content.label)
    Field("Message", content.message)
    ActionRow(
        actionOpen("Open wallet", content.raw.toUri(), actions.launch),
        CardAction("Copy address") { actions.copy(content.address) }
    )
}

fun connectWifi(
    wifi: CodeContent.Wifi,
    wifiLauncher: (Intent) -> Unit,
    openWifiSetting: () -> Unit,
) {
    val canSuggest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            (wifi.security == CodeContent.WifiSecurity.NONE ||
                    (wifi.security == CodeContent.WifiSecurity.WPA && !wifi.password.isNullOrEmpty()))
    if (canSuggest) {
        val builder = WifiNetworkSuggestion.Builder()
            .setSsid(wifi.ssid)
            .setIsHiddenSsid(wifi.hidden)
        if (wifi.security == CodeContent.WifiSecurity.WPA) {
            builder.setWpa2Passphrase(wifi.password!!)
        }
        val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS).putParcelableArrayListExtra(
            Settings.EXTRA_WIFI_NETWORK_LIST,
            arrayListOf(builder.build())
        )
        try {
            wifiLauncher(intent)
        } catch (_: ActivityNotFoundException) {
            openWifiSetting()
        }
    } else {
        openWifiSetting()
    }
}