package com.silifton.textwright.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Android requires a default SMS app to declare a WAP push receiver.
 * MMS is not supported yet, so incoming MMS notifications are dropped here.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
