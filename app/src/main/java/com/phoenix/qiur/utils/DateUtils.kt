package com.phoenix.qiur.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


data class ParsedDate(val millis: Long, val allDay: Boolean)

// Handles "20261022" (all-day), "20261022T140000Z" (UTC) and "20261022T140000" (local)
fun parseIcalDate(valueString: String?): ParsedDate? {
    val value = valueString?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val utc = TimeZone.getTimeZone("UTC")
    return runCatching {
        when {
            value.length == 8 -> ParsedDate(
                SimpleDateFormat(DateFormatConstants.DATE_ALL_DAY, Locale.getDefault()).apply {
                    timeZone = utc
                }.parse(value)!!.time,
                allDay = true
            )

            value.endsWith("Z", ignoreCase = true) -> ParsedDate(
                SimpleDateFormat(DateFormatConstants.DATE_UTC, Locale.getDefault()).apply { timeZone = utc }
                    .parse(value.uppercase())!!.time,
                allDay = false
            )

            else -> ParsedDate(
                SimpleDateFormat(DateFormatConstants.DATE_LOCAL, Locale.getDefault()).parse(value)!!.time,
                allDay = false
            )
        }
    }.getOrNull()
}
