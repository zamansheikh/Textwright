package com.silifton.textwright.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/** Receives the send result for a message written by [SmsSender]. */
class SmsStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val uri = intent.data ?: return
        if (resultCode == Activity.RESULT_OK) {
            // Only promote from "sending", so one failed part of a long message keeps it failed.
            val values = ContentValues().apply { put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT) }
            context.contentResolver.update(
                uri,
                values,
                "${Telephony.Sms.TYPE} = ?",
                arrayOf(Telephony.Sms.MESSAGE_TYPE_OUTBOX.toString()),
            )
        } else {
            SmsSender.markFailed(context, uri)
        }
    }
}
