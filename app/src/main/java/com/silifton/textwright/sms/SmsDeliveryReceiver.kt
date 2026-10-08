package com.silifton.textwright.sms

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.silifton.textwright.data.EditStore

/** Receives the network's delivery report for a message sent by [SmsSender] and stores the outcome on the message. */
class SmsDeliveryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val uri = intent.data ?: return
        val pdu = intent.getByteArrayExtra("pdu") ?: return
        val format = intent.getStringExtra("format")
        val report = runCatching { SmsMessage.createFromPdu(pdu, format) }.getOrNull() ?: return

        // An edit made since sending moved the message to a new row.
        val sentId = runCatching { ContentUris.parseId(uri) }.getOrNull() ?: return
        val id = EditStore.get(context).takeCurrentId(sentId)
        val values = ContentValues().apply { put(Telephony.Sms.STATUS, outcome(report.status, format)) }
        context.contentResolver.update(ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, id), values, null, null)
    }

    /** Maps the report's raw status to one of the Telephony.Sms STATUS_ values. */
    private fun outcome(status: Int, format: String?): Int {
        if (format == "3gpp2") {
            // CDMA packs an error class and a status code into the upper bytes.
            val errorClass = (status shr 24) and 0x03
            val code = (status shr 16) and 0x3f
            return when {
                errorClass == 0 && code == 2 -> Telephony.Sms.STATUS_COMPLETE
                errorClass == 0 -> Telephony.Sms.STATUS_PENDING
                else -> Telephony.Sms.STATUS_FAILED
            }
        }
        // GSM: 0 delivered, 64 and above permanent or temporary failure, anything else still trying.
        return when {
            status == 0 -> Telephony.Sms.STATUS_COMPLETE
            status >= Telephony.Sms.STATUS_FAILED -> Telephony.Sms.STATUS_FAILED
            else -> Telephony.Sms.STATUS_PENDING
        }
    }
}
