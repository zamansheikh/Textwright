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
    SQLiteOpenHelper(context, "edits.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE edits (msg_id INTEGER PRIMARY KEY, original TEXT NOT NULL, edited_at INTEGER NOT NULL)"
        )
        createPending(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) createPending(db)
    }

    private fun createPending(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS pending (old_id INTEGER PRIMARY KEY, thread_id INTEGER NOT NULL, " +
                "date INTEGER NOT NULL, new_body TEXT NOT NULL, record TEXT)"
        )
    }

    /** Journal of row replacements in flight, so a crash between insert and delete can be repaired. */
    class Pending(val oldId: Long, val threadId: Long, val date: Long, val newBody: String, val record: String?)

    fun beginPending(p: Pending) {
        val values = ContentValues().apply {
            put("old_id", p.oldId)
            put("thread_id", p.threadId)
            put("date", p.date)
            put("new_body", p.newBody)
            put("record", p.record)
        }
        writableDatabase.insertWithOnConflict("pending", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun endPending(oldId: Long) {
        writableDatabase.delete("pending", "old_id = ?", arrayOf(oldId.toString()))
    }

    fun pending(): List<Pending> {
        val result = ArrayList<Pending>()
        readableDatabase.rawQuery("SELECT old_id, thread_id, date, new_body, record FROM pending", null).use { c ->
            while (c.moveToNext()) {
                result += Pending(c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3), c.getString(4))
            }
        }
        return result
    }

    /** Keeps the first original only, so repeated edits never overwrite it. */
    fun record(messageId: Long, original: String) {
        val values = ContentValues().apply {
            put("msg_id", messageId)
            put("original", original)
            put("edited_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("edits", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun remove(messageId: Long) {
        writableDatabase.delete("edits", "msg_id = ?", arrayOf(messageId.toString()))
    }

    fun originals(): Map<Long, String> {
        val result = HashMap<Long, String>()
        readableDatabase.rawQuery("SELECT msg_id, original FROM edits", null).use { c ->
            while (c.moveToNext()) result[c.getLong(0)] = c.getString(1)
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
