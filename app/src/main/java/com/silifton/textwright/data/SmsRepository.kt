package com.silifton.textwright.data

import android.Manifest
import android.app.role.RoleManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
        recoverPending()
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
        val originals = edits.originals(dateOf = { id -> dateOfRow(id) })
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
                    original = originals[id],
                )
            }
        }
        return result
    }

    fun threadIdFor(address: String): Long = Telephony.Threads.getOrCreateThreadId(context, address)

    /**
     * Changes the stored text and/or date on this device only; the other party's copy is untouched.
     * Restores the original instead when both values match what the message first had.
     */
    fun editMessage(message: Message, newBody: String, newDate: Long) {
        if (newBody == message.body && newDate == message.date) return
        val original = message.original ?: EditStore.Original(message.body, message.date)
        if (newBody == original.body && newDate == original.date) {
            restore(message)
            return
        }
        replaceBody(message, newBody, newDate, record = original)
    }

    fun restore(message: Message) {
        val original = message.original ?: return
        replaceBody(message, original.body, original.date, record = null)
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

    /**
     * Other messaging apps keep their own copy and ignore in-place changes, so the
     * edit is written as a fresh row (same metadata, new body) and the old row is removed.
     * [record] is the original text and date to remember for the new row, or null to clear the edit marker.
     * The step is journaled in [EditStore] so [recoverPending] can finish it after a crash.
     * Returns false if anything failed (the old row is left intact then).
     */
    private fun replaceBody(message: Message, body: String, date: Long, record: EditStore.Original?): Boolean = synchronized(replaceLock) {
        val columns = arrayOf(
            Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.DATE_SENT,
            Telephony.Sms.READ, Telephony.Sms.SEEN, Telephony.Sms.TYPE, Telephony.Sms.STATUS,
            Telephony.Sms.SUBSCRIPTION_ID, Telephony.Sms.SERVICE_CENTER, Telephony.Sms.PROTOCOL,
            Telephony.Sms.LOCKED,
        )
        val values = ContentValues()
        val found = resolver.query(messageUri(message.id), columns, null, null, null)?.use { c ->
            if (!c.moveToFirst()) return@use false
            for ((i, name) in columns.withIndex()) {
                if (c.isNull(i)) continue
                if (c.getType(i) == android.database.Cursor.FIELD_TYPE_INTEGER) values.put(name, c.getLong(i))
                else values.put(name, c.getString(i))
            }
            true
        } ?: false
        if (!found) return@synchronized false
        values.put(Telephony.Sms.BODY, body)
        values.put(Telephony.Sms.DATE, date)
        val pending = EditStore.Pending(
            message.id,
            values.getAsLong(Telephony.Sms.THREAD_ID),
            date,
            body,
            record,
        )
        edits.beginPending(pending)
        val newUri = resolver.insert(Telephony.Sms.CONTENT_URI, values)
        val newId = newUri?.let { ContentUris.parseId(it) } ?: 0L
        if (newUri == null || newId <= 0) {
            edits.endPending(message.id)
            return@synchronized false
        }
        if (resolver.delete(messageUri(message.id), null, null) <= 0) {
            resolver.delete(newUri, null, null)
            edits.endPending(message.id)
            return@synchronized false
        }
        finishReplace(pending, newId)
        true
    }

    private fun dateOfRow(messageId: Long): Long =
        resolver.query(messageUri(messageId), arrayOf(Telephony.Sms.DATE), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getLong(0) else 0L
        } ?: 0L

    private fun isDefaultSmsApp(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }

    private fun finishReplace(p: EditStore.Pending, newId: Long) {
        edits.remove(p.oldId)
        p.record?.let { edits.record(newId, it) }
        edits.endPending(p.oldId)
    }

    /** Completes or cancels replacements that were interrupted by a crash or kill. */
    fun recoverPending() = synchronized(replaceLock) {
        if (!isDefaultSmsApp()) return@synchronized
        for (p in edits.pending()) {
            runCatching {
                var newId = 0L
                resolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(Telephony.Sms._ID),
                    "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.DATE} = ? AND ${Telephony.Sms.BODY} = ? AND ${Telephony.Sms._ID} != ?",
                    arrayOf(p.threadId.toString(), p.date.toString(), p.newBody, p.oldId.toString()),
                    null,
                )?.use { c -> if (c.moveToFirst()) newId = c.getLong(0) }
                if (newId > 0) {
                    resolver.delete(messageUri(p.oldId), null, null)
                    finishReplace(p, newId)
                } else {
                    edits.endPending(p.oldId)
                }
            }
        }
    }

    private fun messageUri(messageId: Long): Uri =
        ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, messageId)

    private companion object {
        val replaceLock = Any()
    }
}
