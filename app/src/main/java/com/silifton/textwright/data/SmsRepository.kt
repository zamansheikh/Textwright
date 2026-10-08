package com.silifton.textwright.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import java.util.concurrent.ConcurrentHashMap

/** Reads and modifies the system SMS store. Writes only succeed while Textwright is the default SMS app. */
class SmsRepository(context: Context) {

    private val context = context.applicationContext
    private val resolver = this.context.contentResolver
    private val edits = EditStore.get(this.context)
    private val names = ConcurrentHashMap<String, String>()

    fun conversations(): List<Conversation> {
        val byThread = LinkedHashMap<Long, Conversation>()
        val projection = arrayOf(
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE,
        )
        resolver.query(Telephony.Sms.CONTENT_URI, projection, null, null, "${Telephony.Sms.DATE} DESC")?.use { c ->
            while (c.moveToNext()) {
                val threadId = c.getLong(0)
                val unread = c.getInt(4) == 0 && c.getInt(5) == Telephony.Sms.MESSAGE_TYPE_INBOX
                val existing = byThread[threadId]
                if (existing == null) {
                    val address = c.getString(1).orEmpty()
                    byThread[threadId] = Conversation(
                        threadId = threadId,
                        address = address,
                        name = contactName(address),
                        snippet = c.getString(2).orEmpty(),
                        date = c.getLong(3),
                        unread = if (unread) 1 else 0,
                    )
                } else if (unread) {
                    byThread[threadId] = existing.copy(unread = existing.unread + 1)
                }
            }
        }
        return byThread.values.toList()
    }

    /** Newest first. */
    fun messages(threadId: Long): List<Message> {
        val originals = edits.originals()
        val result = ArrayList<Message>()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.SUBSCRIPTION_ID,
        )
        resolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                result += Message(
                    id = id,
                    threadId = threadId,
                    address = c.getString(1).orEmpty(),
                    body = c.getString(2).orEmpty(),
                    date = c.getLong(3),
                    type = c.getInt(4),
                    subId = if (c.isNull(5)) -1 else c.getInt(5),
                    originalBody = originals[id],
                )
            }
        }
        return result
    }

    fun threadIdFor(address: String): Long = Telephony.Threads.getOrCreateThreadId(context, address)

    /** Changes the stored text on this device only; the other party's copy is untouched. */
    fun editBody(message: Message, newBody: String) {
        if (newBody == message.body) return
        if (newBody == message.originalBody) {
            restore(message)
            return
        }
        if (updateBody(message.id, newBody)) edits.record(message.id, message.body)
    }

    fun restore(message: Message) {
        val original = message.originalBody ?: return
        if (updateBody(message.id, original)) edits.remove(message.id)
    }

    fun delete(messageId: Long) {
        resolver.delete(messageUri(messageId), null, null)
        edits.remove(messageId)
    }

    fun markRead(threadId: Long) {
        val values = ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
        resolver.update(
            Telephony.Sms.CONTENT_URI,
            values,
            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
            arrayOf(threadId.toString()),
        )
    }

    fun contactName(address: String): String? {
        if (address.isBlank()) return null
        names[address]?.let { return it.ifEmpty { null } }
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address))
        val name = runCatching {
            resolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
        names[address] = name.orEmpty()
        return name
    }

    private fun updateBody(messageId: Long, body: String): Boolean {
        val values = ContentValues().apply { put(Telephony.Sms.BODY, body) }
        return resolver.update(messageUri(messageId), values, null, null) > 0
    }

    private fun messageUri(messageId: Long): Uri =
        ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, messageId)
}
