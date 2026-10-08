package com.silifton.textwright.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Handles "reply with message" from the incoming-call screen. */
class HeadlessSmsSendService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val address = intent?.data?.schemeSpecificPart?.substringBefore('?').orEmpty()
        val body = intent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        if (address.isNotBlank() && body.isNotBlank()) {
            SmsSender.send(this, address, body)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
