package com.silifton.textwright.sms

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import com.silifton.textwright.data.AppSettings

object SmsSender {

    /**
     * Stores the message as "sending" and hands it to the radio. [SmsStatusReceiver]
     * flips it to sent or failed, and [SmsDeliveryReceiver] records the delivery report when
     * those are turned on. The default SMS app must write its own sent messages.
     * Without a valid [subId] the system's default SMS SIM is used.
     */
    fun send(
        context: Context,
        address: String,
        body: String,
        subId: Int = SubscriptionManager.INVALID_SUBSCRIPTION_ID,
    ): Uri? {
        val app = context.applicationContext
        val sub = if (subId >= 0) subId else Sims.choose(Sims.active(app))
        val wantReport = AppSettings.deliveryReports(app)
        val values = ContentValues().apply {
            put(Telephony.Sms.THREAD_ID, Telephony.Threads.getOrCreateThreadId(app, address))
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_OUTBOX)
            if (wantReport) put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_PENDING)
            if (sub >= 0) put(Telephony.Sms.SUBSCRIPTION_ID, sub)
        }
        val uri = app.contentResolver.insert(Telephony.Sms.CONTENT_URI, values) ?: return null

        val sentIntent = PendingIntent.getBroadcast(
            app,
            0,
            Intent(app, SmsStatusReceiver::class.java).setData(uri),
            PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            val manager = smsManager(app, sub)
            val parts = manager.divideMessage(body)
            // A long message goes out in parts; the report for the last part stands for the whole message.
            val deliveryIntents = if (wantReport) {
                val delivered = PendingIntent.getBroadcast(
                    app,
                    0,
                    Intent(app, SmsDeliveryReceiver::class.java).setData(uri),
                    PendingIntent.FLAG_MUTABLE,
                )
                ArrayList(parts.indices.map { if (it == parts.lastIndex) delivered else null })
            } else {
                null
            }
            manager.sendMultipartTextMessage(address, null, parts, ArrayList(parts.map { sentIntent }), deliveryIntents)
        } catch (e: Exception) {
            markFailed(app, uri)
        }
        return uri
    }

    fun markFailed(context: Context, uri: Uri) {
        val values = ContentValues().apply { put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_FAILED) }
        context.contentResolver.update(uri, values, null, null)
    }

    private fun smsManager(context: Context, subId: Int): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(SmsManager::class.java)
            if (subId >= 0) manager.createForSubscriptionId(subId) else manager
        } else {
            @Suppress("DEPRECATION")
            if (subId >= 0) SmsManager.getSmsManagerForSubscriptionId(subId) else SmsManager.getDefault()
        }
}
