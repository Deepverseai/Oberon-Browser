package com.antigravity.oberon.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class BookmarkItem(
    val id: Long,
    val title: String,
    val url: String,
    val createdAt: Long
)

data class HistoryItem(
    val id: Long,
    val title: String,
    val url: String,
    val visitTime: Long,
    val visitCount: Int
)

class BookmarkDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "oberon_browser.db"
        const val DATABASE_VERSION = 1

        const val TABLE_BOOKMARKS = "bookmarks"
        const val TABLE_HISTORY = "history"

        const val COL_ID = "id"
        const val COL_TITLE = "title"
        const val COL_URL = "url"
        const val COL_CREATED_AT = "created_at"
        const val COL_VISIT_TIME = "visit_time"
        const val COL_VISIT_COUNT = "visit_count"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_BOOKMARKS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_URL TEXT NOT NULL UNIQUE,
                $COL_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_HISTORY (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_URL TEXT NOT NULL UNIQUE,
                $COL_VISIT_TIME INTEGER NOT NULL,
                $COL_VISIT_COUNT INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKMARKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        onCreate(db)
    }

    fun addBookmark(title: String, url: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, title.ifBlank { url })
            put(COL_URL, url)
            put(COL_CREATED_AT, System.currentTimeMillis())
        }
        return db.insertWithOnConflict(TABLE_BOOKMARKS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteBookmark(url: String): Int {
        val db = writableDatabase
        return db.delete(TABLE_BOOKMARKS, "$COL_URL = ?", arrayOf(url))
    }

    fun getAllBookmarks(): List<BookmarkItem> {
        val list = mutableListOf<BookmarkItem>()
        val db = readableDatabase
        val cursor = db.query(TABLE_BOOKMARKS, null, null, null, null, null, "$COL_CREATED_AT DESC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    BookmarkItem(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)),
                        url = it.getString(it.getColumnIndexOrThrow(COL_URL)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(COL_CREATED_AT))
                    )
                )
            }
        }
        return list
    }

    fun addHistory(title: String, url: String) {
        if (url.startsWith("about:") || url.isBlank()) return
        val db = writableDatabase
        db.execSQL("""
            INSERT INTO $TABLE_HISTORY ($COL_TITLE, $COL_URL, $COL_VISIT_TIME, $COL_VISIT_COUNT)
            VALUES (?, ?, ?, 1)
            ON CONFLICT($COL_URL) DO UPDATE SET
                $COL_TITLE = excluded.$COL_TITLE,
                $COL_VISIT_TIME = excluded.$COL_VISIT_TIME,
                $COL_VISIT_COUNT = $TABLE_HISTORY.$COL_VISIT_COUNT + 1
        """.trimIndent(), arrayOf(title.ifBlank { url }, url, System.currentTimeMillis()))
    }

    fun getTopVisited(limit: Int = 6): List<HistoryItem> {
        val list = mutableListOf<HistoryItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_HISTORY,
            null,
            null,
            null,
            null,
            null,
            "$COL_VISIT_COUNT DESC, $COL_VISIT_TIME DESC",
            limit.toString()
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    HistoryItem(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)),
                        url = it.getString(it.getColumnIndexOrThrow(COL_URL)),
                        visitTime = it.getLong(it.getColumnIndexOrThrow(COL_VISIT_TIME)),
                        visitCount = it.getInt(it.getColumnIndexOrThrow(COL_VISIT_COUNT))
                    )
                )
            }
        }
        return list
    }

    fun clearAllData() {
        val db = writableDatabase
        db.delete(TABLE_BOOKMARKS, null, null)
        db.delete(TABLE_HISTORY, null, null)
    }
}
