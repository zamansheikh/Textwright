package com.silifton.textwright.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Remembers the original text of every message edited in Textwright, so edits
 * can be marked in the UI and undone. The system SMS store has no such field.
 */
class EditStore private constructor(context: Context) :
    SQLiteOpenHelper(context, "edits.db", null, 5) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE edits (msg_id INTEGER PRIMARY KEY, original TEXT NOT NULL, edited_at INTEGER NOT NULL, original_date INTEGER)"
        )
        createPending(db)
        createAdded(db)
        createMoved(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createPending(db)
        if (oldVersion < 3) db.execSQL("ALTER TABLE edits ADD COLUMN original_date INTEGER")
        // Only a v2 database has a pending table without record_date; createPending above already includes it.
        if (oldVersion == 2) db.execSQL("ALTER TABLE pending ADD COLUMN record_date INTEGER")
        if (oldVersion < 4) createAdded(db)
        if (oldVersion < 5) createMoved(db)
    }

    private fun createPending(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS pending (old_id INTEGER PRIMARY KEY, thread_id INTEGER NOT NULL, " +
                "date INTEGER NOT NULL, new_body TEXT NOT NULL, record TEXT, record_date INTEGER)"
        )
    }

    private fun createAdded(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS added (msg_id INTEGER PRIMARY KEY)")
    }

    private fun createMoved(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS moved (old_id INTEGER PRIMARY KEY, new_id INTEGER NOT NULL, at INTEGER NOT NULL)")
    }

    /**
     * Remembers that an edit replaced row [oldId] with [newId] while a delivery report was still due,
     * so the report can find the message. Entries that never get a report are dropped after a week.
     */
    fun recordMove(oldId: Long, newId: Long) {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("old_id", oldId)
            put("new_id", newId)
            put("at", now)
        }
        val db = writableDatabase
        db.insertWithOnConflict("moved", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.delete("moved", "at < ?", arrayOf((now - 7 * 86_400_000L).toString()))
    }

    /** The row that now holds message [id], following any replacements, and forgetting them. */
    fun takeCurrentId(id: Long): Long {
        var current = id
        val db = writableDatabase
        while (true) {
            val next = db.rawQuery("SELECT new_id FROM moved WHERE old_id = ?", arrayOf(current.toString())).use { c ->
                if (c.moveToFirst()) c.getLong(0) else null
            } ?: return current
            db.delete("moved", "old_id = ?", arrayOf(current.toString()))
            current = next
        }
    }

    /** Marks a message as written into the store by hand in Textwright, not sent or received. */
    fun markAdded(messageId: Long) {
        val values = ContentValues().apply { put("msg_id", messageId) }
        writableDatabase.insertWithOnConflict("added", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    /** Returns true if the message carried the mark. */
    fun unmarkAdded(messageId: Long): Boolean =
        writableDatabase.delete("added", "msg_id = ?", arrayOf(messageId.toString())) > 0

    fun added(): Set<Long> {
        val result = HashSet<Long>()
        readableDatabase.rawQuery("SELECT msg_id FROM added", null).use { c ->
            while (c.moveToNext()) result += c.getLong(0)
        }
        return result
    }

    /** What a message looked like before its first edit in Textwright. */
    class Original(val body: String, val date: Long)

    /** Journal of row replacements in flight, so a crash between insert and delete can be repaired. [date] is the new date. */
    class Pending(val oldId: Long, val threadId: Long, val date: Long, val newBody: String, val record: Original?)

    fun beginPending(p: Pending) {
        val values = ContentValues().apply {
            put("old_id", p.oldId)
            put("thread_id", p.threadId)
            put("date", p.date)
            put("new_body", p.newBody)
            put("record", p.record?.body)
            put("record_date", p.record?.date)
        }
        writableDatabase.insertWithOnConflict("pending", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun endPending(oldId: Long) {
        writableDatabase.delete("pending", "old_id = ?", arrayOf(oldId.toString()))
    }

    fun pending(): List<Pending> {
        val result = ArrayList<Pending>()
        readableDatabase.rawQuery("SELECT old_id, thread_id, date, new_body, record, record_date FROM pending", null).use { c ->
            while (c.moveToNext()) {
                val record = if (c.isNull(4)) null else Original(c.getString(4), c.getLong(5))
                result += Pending(c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3), record)
            }
        }
        return result
    }

    /** Keeps the first original only, so repeated edits never overwrite it. */
    fun record(messageId: Long, original: Original) {
        val values = ContentValues().apply {
            put("msg_id", messageId)
            put("original", original.body)
            put("original_date", original.date)
            put("edited_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("edits", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun remove(messageId: Long) {
        writableDatabase.delete("edits", "msg_id = ?", arrayOf(messageId.toString()))
    }

    /** Rows edited before date editing existed have no stored date; [dateOf] supplies the current one. */
    fun originals(dateOf: (Long) -> Long = { 0L }): Map<Long, Original> {
        val result = HashMap<Long, Original>()
        readableDatabase.rawQuery("SELECT msg_id, original, original_date FROM edits", null).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                result[id] = Original(c.getString(1), if (c.isNull(2)) dateOf(id) else c.getLong(2))
            }
        }
        return result
    }

    companion object {
        @Volatile
        private var instance: EditStore? = null

        fun get(context: Context): EditStore =
            instance ?: synchronized(this) {
                instance ?: EditStore(context.applicationContext).also { instance = it }
            }
    }
}
