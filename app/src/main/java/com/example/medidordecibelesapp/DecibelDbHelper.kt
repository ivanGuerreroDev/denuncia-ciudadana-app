package com.example.medidordecibelesapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DecibelDbHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "DecibelDB"
        private const val DATABASE_VERSION = 2
        private const val TABLE_NAME = "decibel_measurements"
        private const val COLUMN_ID = "id"
        private const val COLUMN_DECIBEL = "decibel"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DECIBEL REAL NOT NULL,
                $COLUMN_TIMESTAMP TEXT NOT NULL,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Add latitude and longitude columns if they don't exist
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_LATITUDE REAL")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_LONGITUDE REAL")
        }
    }

    fun addDecibelMeasurement(decibel: Double, timestamp: String, latitude: Double? = null, longitude: Double? = null) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_DECIBEL, decibel)
            put(COLUMN_TIMESTAMP, timestamp)
            latitude?.let { put(COLUMN_LATITUDE, it) }
            longitude?.let { put(COLUMN_LONGITUDE, it) }
        }
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun getAllMeasurements(): List<DecibelMeasurement> {
        val measurements = mutableListOf<DecibelMeasurement>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_DECIBEL, COLUMN_TIMESTAMP, COLUMN_LATITUDE, COLUMN_LONGITUDE),
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow(COLUMN_ID))
                val decibel = getDouble(getColumnIndexOrThrow(COLUMN_DECIBEL))
                val timestamp = getString(getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                
                val latIndex = getColumnIndex(COLUMN_LATITUDE)
                val longIndex = getColumnIndex(COLUMN_LONGITUDE)
                
                val latitude = if (latIndex != -1 && !isNull(latIndex)) getDouble(latIndex) else null
                val longitude = if (longIndex != -1 && !isNull(longIndex)) getDouble(longIndex) else null
                
                measurements.add(DecibelMeasurement(id, decibel, timestamp, latitude, longitude))
            }
        }
        cursor.close()
        db.close()
        return measurements
    }

    fun getDatabasePath(): String {
        return context.getDatabasePath(DATABASE_NAME).absolutePath
    }
}