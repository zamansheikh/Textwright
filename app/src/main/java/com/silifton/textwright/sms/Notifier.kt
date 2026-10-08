package com.silifton.textwright.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.silifton.textwright.MainActivity
import com.silifton.textwright.R
import com.silifton.textwright.data.SmsRepository

object Notifier {

    private const val CHANNEL_ID = "messages"

    fun notifyIncoming(context: Context, threadId: Long, address: String, body: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_messages),
                NotificationManager.IMPORTANCE_HIGH,
            )
        )

        val open = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_THREAD_ID, threadId)
            .putExtra(MainActivity.EXTRA_ADDRESS, address)
        val contentIntent = PendingIntent.getActivity(
            context,
            threadId.toInt(),
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(SmsRepository(context).contactName(address) ?: address)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(threadId.toInt(), notification)
        } catch (e: SecurityException) {
            // Notification permission was revoked between the check and the post.
        }
    }

    fun cancel(context: Context, threadId: Long) {
        NotificationManagerCompat.from(context).cancel(threadId.toInt())
    }
}
