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
    SQLiteOpenHelper(context, "edits.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE edits (msg_id INTEGER PRIMARY KEY, original TEXT NOT NULL, edited_at INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

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
