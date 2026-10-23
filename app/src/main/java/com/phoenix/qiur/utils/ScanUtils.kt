package com.phoenix.qiur.utils

import zxingcpp.BarcodeReader
import java.math.BigDecimal
import java.net.URI
import java.net.URLDecoder

enum class CodeContentType {
    TEXT,
    URL,
    APP_STORE,
    PHONE,
    SMS,
    EMAIL,
    CONTACT_VCARD,
    CONTACT_MECARD,
    WIFI,
    GEO,
    CALENDAR_EVENT,
    PAYMENT_EPC,
    PAYMENT_UPI,
    CRYPTO;

    companion object {

        private val cryptoSchemes = listOf(
            "bitcoin:", "litecoin:", "ethereum:", "monero:", "dogecoin:", "bitcoincash:"
        )

        private val appStoreHosts = listOf(
            "play.google.com/store/apps",
            "apps.apple.com",
            "itunes.apple.com",
        )

        private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        fun detect(raw: String?): CodeContentType {
            val string = raw?.trim().orEmpty()
            if (string.isEmpty()) return TEXT
            val lower = string.lowercase()

            return when {
                lower.startsWith("begin:vcard") -> CONTACT_VCARD
                lower.startsWith("mecard:") -> CONTACT_MECARD
                lower.startsWith("begin:vevent") || lower.startsWith("begin:vcalendar") -> CALENDAR_EVENT
                lower.startsWith("wifi:") -> WIFI
                lower.startsWith("bcd\n") || lower.startsWith("bcd\r\n") -> PAYMENT_EPC
                lower.startsWith("upi://") -> PAYMENT_UPI

                lower.startsWith("tel:") -> PHONE
                lower.startsWith("sms:") || lower.startsWith("smsto:") || lower.startsWith("mms:") -> SMS
                lower.startsWith("mailto:") || lower.startsWith("matmsg:") -> EMAIL
                lower.startsWith("geo:") -> GEO
                cryptoSchemes.any { lower.startsWith(it) } -> CRYPTO

                lower.startsWith("market://") -> APP_STORE
                isUrl(lower) && appStoreHosts.any { lower.contains(it) } -> APP_STORE
                isUrl(lower) -> URL

                emailRegex.matches(string) -> EMAIL

                else -> TEXT
            }
        }

        private fun isUrl(lower: String): Boolean =
            lower.startsWith("http://") ||
                    lower.startsWith("https://") ||
                    lower.startsWith("www.")
    }
}

data class CodeResult(
    val format: BarcodeReader.Format,
    val content: CodeContent?
)

sealed interface CodeContent {
    val raw: String
    val type: CodeContentType

    data class Text(override val raw: String) : CodeContent {
        override val type: CodeContentType get() = CodeContentType.TEXT
    }

    data class Url(
        override val raw: String,
        val url: String,
        val scheme: String?,
        val host: String?
    ) : CodeContent {
        override val type: CodeContentType get() = CodeContentType.URL
    }

    enum class AppStorePlatform { GOOGLE_PLAY, APPLE_APP_STORE, UNKNOWN }

    data class AppStoreLink(
        override val raw: String,
        val platform: AppStorePlatform,
        val appId: String?
    ) : CodeContent {
        override val type: CodeContentType get() = CodeContentType.APP_STORE
    }

    data class Phone(override val raw: String, val number: String) : CodeContent {
        override val type: CodeContentType get() = CodeContentType.PHONE
    }

    data class Sms(
        override val raw: String,
        val number: String,
        val message: String?
    ) : CodeContent {
        override val type: CodeContentType get() = CodeContentType.SMS
    }

    data class Email(
        override val raw: String,
        val to: List<String>,
        val subject: String?,
        val body: String?,
        val cc: List<String>,
        val bcc: List<String>
    ) : CodeContent {
        override val type: CodeContentType get() = CodeContentType.EMAIL
    }

    data class Contact(
        override val raw: String,
        override val type: CodeContentType,
        val name: String?,
        val organization: String?,
        val jobTitle: String?,
        val phones: List<String>,
        val emails: List<String>,
        val websites: List<String>,
        val address: String?,
        val note: String?
    ) : CodeContent

    enum class WifiSecurity { WPA, WEP, NONE, UNKNOWN }

    data class Wifi(
        override val raw: String,
        val ssid: String,
        val password: String?,
        val security: WifiSecurity,
        val hidden: Boolean
    ) : CodeContent {
        override val type: CodeContentType get() = CodeContentType.WIFI
    }

    data class Geo(
        override val raw: String,
        val latitude: Double,
        val longitude: Double,
        val altitude: Double?,
        val query: String?
    ) : CodeContent {
        override val type: CodeContentType get() = CodeContentType.GEO
    }

    data class CalendarEvent(
        override val raw: String,
        val summary: String?,
        val description: String?,
        val location: String?,
        val start: String?,
        val end: String?
    ) : CodeContent {
        override val type: CodeContentType get() = CodeContentType.CALENDAR_EVENT
    }

    data class EpcPayment(
        override val raw: String,
        val beneficiaryName: String?,
        val iban: String,
        val bic: String?,
        val amount: BigDecimal?,
        val currency: String?,
        val purpose: String?,
        val structuredReference: String?,
        val remittanceText: String?,
        val info: String?
    ) : CodeContent {
        override val type: CodeContentType get() = CodeContentType.PAYMENT_EPC
    }

    data class UpiPayment(
        override val raw: String,
        val payeeAddress: String,
        val payeeName: String?,
        val amount: BigDecimal?,
        val currency: String?,
        val note: String?,
        val transactionRef: String?
    ) : CodeContent {
        override val type: CodeContentType get() = CodeContentType.PAYMENT_UPI
    }

    data class Crypto(
        override val raw: String,
        val currency: String,
        val address: String,
        val amount: BigDecimal?,
        val label: String?,
        val message: String?
    ) : CodeContent {
        override val type: CodeContentType get() = CodeContentType.CRYPTO
    }

    companion object {

        fun parse(raw: String?): CodeContent {
            val string = raw?.trim().orEmpty()
            return parse(string, CodeContentType.detect(string))
        }

        fun parse(raw: String?, type: CodeContentType): CodeContent {
            val string = raw?.trim().orEmpty()
            return when (type) {
                CodeContentType.TEXT -> Text(string)
                CodeContentType.URL -> parseUrl(string)
                CodeContentType.APP_STORE -> parseAppStore(string)
                CodeContentType.PHONE -> parsePhone(string)
                CodeContentType.SMS -> parseSms(string)
                CodeContentType.EMAIL -> parseEmail(string)
                CodeContentType.CONTACT_VCARD -> parseVCard(string)
                CodeContentType.CONTACT_MECARD -> parseMeCard(string)
                CodeContentType.WIFI -> parseWifi(string)
                CodeContentType.GEO -> parseGeo(string)
                CodeContentType.CALENDAR_EVENT -> parseCalendar(string)
                CodeContentType.PAYMENT_EPC -> parseEpc(string)
                CodeContentType.PAYMENT_UPI -> parseUpi(string)
                CodeContentType.CRYPTO -> parseCrypto(string)
            } ?: Text(string)
        }
    }
}

fun String.toCodeContent(): CodeContent = CodeContent.parse(this)

private fun parseUrl(raw: String): CodeContent.Url {
    val url = if (raw.startsWith("www.", ignoreCase = true)) "https://$raw" else raw
    val uri = runCatching { URI(url) }.getOrNull()
    val host = uri?.host ?: url.substringAfter("://", "")
        .substringBefore('/').substringBefore('?').substringBefore('#')
        .substringAfter('@').substringBefore(':')
        .takeIf { it.isNotEmpty() }
    return CodeContent.Url(raw, url, uri?.scheme?.lowercase(), host)
}

private fun parseAppStore(raw: String): CodeContent.AppStoreLink {
    val lower = raw.lowercase()
    val platform = when {
        lower.startsWith("market://") || "play.google.com" in lower ->
            CodeContent.AppStorePlatform.GOOGLE_PLAY
        "apps.apple.com" in lower || "itunes.apple.com" in lower ->
            CodeContent.AppStorePlatform.APPLE_APP_STORE
        else -> CodeContent.AppStorePlatform.UNKNOWN
    }
    val query = parseQuery(raw.substringAfter('?', "").substringBefore('#'))
    val appId = when (platform) {
        CodeContent.AppStorePlatform.GOOGLE_PLAY -> query["id"]
        CodeContent.AppStorePlatform.APPLE_APP_STORE ->
            Regex("""/id(\d+)""").find(raw)?.groupValues?.get(1)
        CodeContent.AppStorePlatform.UNKNOWN -> null
    }
    return CodeContent.AppStoreLink(raw, platform, appId)
}

private fun parsePhone(raw: String): CodeContent.Phone? {
    val number = urlDecode(afterScheme(raw)).trim()
    return if (number.isEmpty()) null else CodeContent.Phone(raw, number)
}

private fun parseSms(raw: String): CodeContent.Sms? {
    val body = afterScheme(raw)
    val number: String
    val message: String?
    if (raw.startsWith("smsto:", ignoreCase = true) && ':' in body) {
        // SMSTO:number:message
        number = body.substringBefore(':')
        message = body.substringAfter(':').ifEmpty { null }
    } else {
        // sms:number?body=message
        val (path, query) = splitUri(body)
        number = urlDecode(path)
        message = query["body"]
    }
    return if (number.isBlank()) null else CodeContent.Sms(raw, number.trim(), message)
}

private fun parseEmail(raw: String): CodeContent.Email? {
    val lower = raw.lowercase()
    return when {
        // MATMSG:TO:a@b.com;SUB:Hi;BODY:Text;;
        lower.startsWith("matmsg:") -> {
            val f = parseEscapedFields(afterScheme(raw)).toMap()
            val to = f["TO"]?.splitAddresses().orEmpty()
            if (to.isEmpty()) null
            else CodeContent.Email(raw, to, f["SUB"], f["BODY"], emptyList(), emptyList())
        }
        // mailto:a@b.com?subject=Hi&body=Text&cc=c@d.com
        lower.startsWith("mailto:") -> {
            val (path, q) = splitUri(afterScheme(raw))
            val to = urlDecode(path).splitAddresses() + q["to"]?.splitAddresses().orEmpty()
            if (to.isEmpty()) null
            else CodeContent.Email(
                raw, to, q["subject"], q["body"],
                q["cc"]?.splitAddresses().orEmpty(),
                q["bcc"]?.splitAddresses().orEmpty()
            )
        }
        else -> CodeContent.Email(raw, listOf(raw), null, null, emptyList(), emptyList())
    }
}

private fun parseVCard(raw: String): CodeContent.Contact {
    val props = parseIcalProps(raw)
    fun first(name: String) = props.firstOrNull { it.name == name }?.value
    fun all(name: String) = props.filter { it.name == name }.map { icalUnescape(it.value).trim() }
        .filter { it.isNotEmpty() }

    // N = Last;First;Middle;Prefix;Suffix
    val structuredName = first("N")?.split(';')?.let { p ->
        listOfNotNull(
            p.getOrNull(3),
            p.getOrNull(1),
            p.getOrNull(2),
            p.getOrNull(0),
            p.getOrNull(4)
        ).filter { it.isNotBlank() }.joinToString(" ")
    }
    // ADR = pobox;ext;street;city;region;postal;country
    val address = first("ADR")?.split(';')?.map { it.trim() }?.filter { it.isNotEmpty() }
        ?.joinToString(", ")

    return CodeContent.Contact(
        raw = raw,
        type = CodeContentType.CONTACT_VCARD,
        name = first("FN")?.let(::icalUnescape)?.trim()?.ifEmpty { null }
            ?: structuredName?.ifEmpty { null },
        organization = first("ORG")?.split(';')?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?.joinToString(", ")?.ifEmpty { null },
        jobTitle = first("TITLE")?.let(::icalUnescape)?.trim()?.ifEmpty { null },
        phones = all("TEL"),
        emails = all("EMAIL"),
        websites = all("URL"),
        address = address?.ifEmpty { null },
        note = first("NOTE")?.let(::icalUnescape)?.trim()?.ifEmpty { null }
    )
}

private fun parseMeCard(raw: String): CodeContent.Contact {
    val fields = parseEscapedFields(afterScheme(raw))
    fun first(key: String) = fields.firstOrNull { it.first == key }?.second?.ifBlank { null }
    fun all(key: String) = fields.filter { it.first == key }.map { it.second }.filter { it.isNotBlank() }

    // N = Last,First
    val name = first("N")?.let { n ->
        val p = n.split(',', limit = 2)
        if (p.size == 2) "${p[1].trim()} ${p[0].trim()}".trim() else n
    }
    return CodeContent.Contact(
        raw = raw,
        type = CodeContentType.CONTACT_MECARD,
        name = name,
        organization = first("ORG"),
        jobTitle = first("TITLE"),
        phones = all("TEL"),
        emails = all("EMAIL"),
        websites = all("URL"),
        address = first("ADR"),
        note = first("NOTE")
    )
}

private fun parseWifi(raw: String): CodeContent.Wifi? {
    // WIFI:T:WPA;S:MyNetwork;P:secret;H:true;;
    val f = parseEscapedFields(afterScheme(raw)).toMap()
    val ssid = f["S"] ?: return null
    val security = when {
        f["T"] == null -> CodeContent.WifiSecurity.NONE
        f["T"]!!.startsWith("WPA", ignoreCase = true) -> CodeContent.WifiSecurity.WPA
        f["T"].equals("WEP", ignoreCase = true) -> CodeContent.WifiSecurity.WEP
        f["T"].equals("nopass", ignoreCase = true) -> CodeContent.WifiSecurity.NONE
        else -> CodeContent.WifiSecurity.UNKNOWN
    }
    return CodeContent.Wifi(
        raw = raw,
        ssid = ssid,
        password = f["P"]?.ifEmpty { null },
        security = security,
        hidden = f["H"].equals("true", ignoreCase = true)
    )
}

private fun parseGeo(raw: String): CodeContent.Geo? {
    // geo:lat,lng[,alt][;crs=...;u=...][?q=...]
    val (path, query) = splitUri(afterScheme(raw))
    val coords = path.substringBefore(';').split(',')
    val lat = coords.getOrNull(0)?.trim()?.toDoubleOrNull() ?: return null
    val lng = coords.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return null
    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
    return CodeContent.Geo(raw, lat, lng, coords.getOrNull(2)?.trim()?.toDoubleOrNull(), query["q"])
}

private fun parseCalendar(raw: String): CodeContent.CalendarEvent {
    val props = parseIcalProps(raw)
    fun first(name: String) = props.firstOrNull { it.name == name }?.value
        ?.let(::icalUnescape)?.trim()?.ifEmpty { null }
    return CodeContent.CalendarEvent(
        raw = raw,
        summary = first("SUMMARY"),
        description = first("DESCRIPTION"),
        location = first("LOCATION"),
        start = first("DTSTART"),
        end = first("DTEND")
    )
}

private fun parseEpc(raw: String): CodeContent.EpcPayment? {
    // Line layout: BCD, version, charset, SCT, BIC, name, IBAN, amount, purpose, ref, text, info
    val lines = raw.replace("\r\n", "\n").split('\n')
    fun line(i: Int) = lines.getOrNull(i)?.trim()?.ifEmpty { null }
    val iban = line(6) ?: return null
    val amountField = line(7)
    return CodeContent.EpcPayment(
        raw = raw,
        beneficiaryName = line(5),
        iban = iban.replace(" ", ""),
        bic = line(4),
        amount = amountField?.drop(3)?.toBigDecimalOrNull(),
        currency = amountField?.take(3)?.takeIf { it.length == 3 },
        purpose = line(8),
        structuredReference = line(9),
        remittanceText = line(10),
        info = line(11)
    )
}

private fun parseUpi(raw: String): CodeContent.UpiPayment? {
    // upi://pay?pa=name@bank&pn=Name&am=10.00&cu=INR&tn=Note&tr=Ref
    val (_, q) = splitUri(afterScheme(raw).removePrefix("//"))
    val pa = q["pa"]?.ifBlank { null } ?: return null
    return CodeContent.UpiPayment(
        raw = raw,
        payeeAddress = pa,
        payeeName = q["pn"],
        amount = q["am"]?.toBigDecimalOrNull(),
        currency = q["cu"],
        note = q["tn"],
        transactionRef = q["tr"]
    )
}

private fun parseCrypto(raw: String): CodeContent.Crypto? {
    // bitcoin:address?amount=0.1&label=Shop&message=Order
    val scheme = raw.substringBefore(':').lowercase()
    val (path, q) = splitUri(afterScheme(raw).removePrefix("//"))
    if (path.isBlank()) return null
    return CodeContent.Crypto(
        raw = raw,
        currency = scheme,
        address = path,
        amount = q["amount"]?.toBigDecimalOrNull(),
        label = q["label"],
        message = q["message"]
    )
}

private fun afterScheme(raw: String): String = raw.substring(raw.indexOf(':') + 1)

private fun urlDecode(string: String): String =
    runCatching { URLDecoder.decode(string.replace("+", "%2B"), "UTF-8") }.getOrDefault(string)

private fun parseQuery(query: String?): Map<String, String> {
    if (query.isNullOrEmpty()) return emptyMap()
    return query.split('&').filter { it.isNotEmpty() }.associate { part ->
        val i = part.indexOf('=')
        if (i < 0) urlDecode(part).lowercase() to ""
        else urlDecode(part.substring(0, i)).lowercase() to urlDecode(part.substring(i + 1))
    }
}

private fun splitUri(body: String): Pair<String, Map<String, String>> {
    val i = body.indexOf('?')
    return if (i < 0) body to emptyMap()
    else body.substring(0, i) to parseQuery(body.substring(i + 1))
}

private fun String.splitAddresses(): List<String> =
    split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }

private fun splitUnescaped(string: String, delim: Char): List<String> {
    val out = mutableListOf<String>()
    val builder = StringBuilder()
    var i = 0
    while (i < string.length) {
        when (val char = string[i]) {
            '\\' if i + 1 < string.length -> { builder.append(char).append(string[i + 1]); i += 2 }
            delim -> { out.add(builder.toString()); builder.clear(); i++ }
            else -> { builder.append(char); i++ }
        }
    }
    out.add(builder.toString())
    return out
}

private fun indexOfUnescaped(string: String, char: Char): Int {
    var i = 0
    while (i < string.length) {
        when {
            string[i] == '\\' -> i += 2
            string[i] == char -> return i
            else -> i++
        }
    }
    return -1
}

private fun unescape(string: String): String = buildString {
    var i = 0
    while (i < string.length) {
        if (string[i] == '\\' && i + 1 < string.length) { append(string[i + 1]); i += 2 }
        else { append(string[i]); i++ }
    }
}

private fun parseEscapedFields(body: String): List<Pair<String, String>> =
    splitUnescaped(body, ';').mapNotNull { token ->
        val idx = indexOfUnescaped(token, ':')
        if (idx <= 0) null
        else token.substring(0, idx).trim().uppercase() to unescape(token.substring(idx + 1))
    }


private data class IcalProp(val name: String, val value: String)

private fun parseIcalProps(raw: String): List<IcalProp> {
    val unfolded = raw.replace("\r\n", "\n").replace(Regex("\n[ \t]"), "")
    return unfolded.split('\n').mapNotNull { line ->
        val colon = line.indexOf(':')
        if (colon <= 0) return@mapNotNull null
        val head = line.substring(0, colon)
        val name = head.substringBefore(';').substringAfterLast('.').trim().uppercase()
        IcalProp(name, line.substring(colon + 1))
    }
}

private fun icalUnescape(string: String): String = string
    .replace("\\n", "\n").replace("\\N", "\n")
    .replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")

