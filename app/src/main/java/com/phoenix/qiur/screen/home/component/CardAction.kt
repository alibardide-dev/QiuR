package com.phoenix.qiur.screen.home.component

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import androidx.core.net.toUri
import com.phoenix.qiur.utils.CodeContent
import com.phoenix.qiur.utils.ParsedDate

class CardAction(val label: String, val onClick: () -> Unit)

fun actionSearch(text: String, onSearch: (String) -> Unit) =
    CardAction("Search") { onSearch(text) }

fun actionCopy(text: String, onCopy: (String) -> Unit) =
    CardAction("Copy") { onCopy(text) }

fun actionShare(text: String, onShare: (String) -> Unit) =
    CardAction("Share") { onShare(text) }

fun actionOpen(label: String = "Open", uri: Uri, launch: (Intent) -> Unit) =
    CardAction(label) { launch(Intent(Intent.ACTION_VIEW, uri)) }

fun actionDial(number: String, launch: (Intent) -> Unit) =
    CardAction("Dial") {
        launch(
            Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
        )
    }

fun actionSendSMS(number: String, message: String?, launch: (Intent) -> Unit) =
    CardAction("Send SMS") {
        launch(
            Intent(
                Intent.ACTION_SENDTO,
                Uri.fromParts("smsto", number, null)
            ).apply {
                putExtra("sms_body", message.orEmpty())
            }
        )
    }

fun actionComposeEmail(
    to: List<String>,
    cc: List<String>,
    bcc: List<String>,
    subject: String?,
    body: String?,
    launch: (Intent) -> Unit
) =
    CardAction("Compose E-Mail") {
        launch(
            Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).apply {
                putExtra(Intent.EXTRA_EMAIL, to.toTypedArray())
                putExtra(Intent.EXTRA_CC, cc.toTypedArray())
                putExtra(Intent.EXTRA_BCC, bcc.toTypedArray())
                putExtra(Intent.EXTRA_SUBJECT, subject.orEmpty())
                putExtra(Intent.EXTRA_TEXT, body.orEmpty())
            }
        )
    }

fun actionMakeContact(content: CodeContent.Contact, launch: (Intent) -> Unit) =
    CardAction("Add contact") {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            content.name?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
            content.organization?.let {
                putExtra(
                    ContactsContract.Intents.Insert.COMPANY,
                    it
                )
            }
            content.jobTitle?.let {
                putExtra(
                    ContactsContract.Intents.Insert.JOB_TITLE,
                    it
                )
            }
            content.phones.getOrNull(0)
                ?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
            content.phones.getOrNull(1)?.let {
                putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, it)
            }
            content.emails.getOrNull(0)
                ?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
            content.emails.getOrNull(1)?.let {
                putExtra(ContactsContract.Intents.Insert.SECONDARY_EMAIL, it)
            }
            content.address?.let { putExtra(ContactsContract.Intents.Insert.POSTAL, it) }
            content.note?.let { putExtra(ContactsContract.Intents.Insert.NOTES, it) }
        }
        launch(intent)
    }

fun actionAddCalendar(
    content: CodeContent.CalendarEvent,
    start: ParsedDate?,
    end: ParsedDate?,
    launch: (Intent) -> Unit) =
    CardAction("Add to calendar") {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            content.summary?.let { putExtra(CalendarContract.Events.TITLE, it) }
            content.description?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
            content.location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
            start?.let {
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it.millis)
                if (it.allDay) putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
            }
            end?.let { putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it.millis) }
        }
        launch(intent)
    }
