package com.example.medidordecibelesapp.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.medidordecibelesapp.models.AccusationDataItem
import com.example.medidordecibelesapp.models.Report
import org.json.JSONArray
import org.json.JSONObject

/**
 * SQLite database helper for managing local storage of reports and related data.
 * Provides methods to save, retrieve and manage reports in the local database.
 */
class DBHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "DBHelper"
        private const val DATABASE_NAME = "AppDB"
        private const val DATABASE_VERSION = 4
        
        // Reports table
        private const val REPORTS_TABLE = "reports"
        private const val REPORT_ID = "id"
        private const val REPORT_TIMESTAMP = "timestamp"
        private const val REPORT_STATUS = "status"
        private const val REPORT_DATA = "accusation_data"
        private const val REPORT_ATTACHMENTS = "attachments"
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            // Create reports table
            val createReportsTable = """
                CREATE TABLE $REPORTS_TABLE (
                    $REPORT_ID TEXT PRIMARY KEY,
                    $REPORT_TIMESTAMP TEXT NOT NULL,
                    $REPORT_STATUS TEXT NOT NULL,
                    $REPORT_DATA TEXT NOT NULL,
                    $REPORT_ATTACHMENTS TEXT
                )
            """.trimIndent()
            db.execSQL(createReportsTable)
            Log.d(TAG, "Database created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating database: ${e.message}", e)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            if (oldVersion < 4) {
                // Drop the old decibel_measurements table
                db.execSQL("DROP TABLE IF EXISTS decibel_measurements")
                
                // Recreate reports table to ensure it has the correct structure
                db.execSQL("DROP TABLE IF EXISTS $REPORTS_TABLE")
                onCreate(db)
                Log.d(TAG, "Database upgraded from $oldVersion to $newVersion")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error upgrading database: ${e.message}", e)
        }
    }
    
    /**
     * Saves a report to the database. If a report with the same ID already exists,
     * it will be replaced.
     * 
     * @param report The Report object to save
     */
    fun saveReport(report: Report) {
        try {
            val db = this.writableDatabase
            val values = createContentValuesFromReport(report)
            
            // Insert or update the report
            db.insertWithOnConflict(REPORTS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            db.close()
            Log.d(TAG, "Report saved successfully: ${report.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving report: ${e.message}", e)
        }
    }
    
    /**
     * Creates ContentValues from a Report object for database operations.
     * 
     * @param report The Report object to convert
     * @return ContentValues containing the report data
     */
    private fun createContentValuesFromReport(report: Report): ContentValues {
        // Convert accusation data to JSON string
        val dataJson = JSONArray()
        report.accusationData.forEach { item ->
            val itemJson = JSONObject()
            itemJson.put("key", item.key)
            itemJson.put("value", item.value)
            dataJson.put(itemJson)
        }
        
        // Convert attachments to JSON string
        val attachmentsJson = JSONArray()
        report.attachments.forEach { attachmentsJson.put(it) }
        
        return ContentValues().apply {
            put(REPORT_ID, report.id)
            put(REPORT_TIMESTAMP, report.timestamp)
            put(REPORT_STATUS, report.status)
            put(REPORT_DATA, dataJson.toString())
            put(REPORT_ATTACHMENTS, attachmentsJson.toString())
        }
    }
    
    /**
     * Retrieves all reports from the database, ordered by timestamp (newest first).
     * 
     * @return List of Report objects
     */
    fun getAllReports(): List<Report> {
        val reports = mutableListOf<Report>()
        try {
            val db = this.readableDatabase
            val cursor = db.query(
                REPORTS_TABLE,
                arrayOf(REPORT_ID, REPORT_TIMESTAMP, REPORT_STATUS, REPORT_DATA, REPORT_ATTACHMENTS),
                null,
                null,
                null,
                null,
                "$REPORT_TIMESTAMP DESC"
            )
            
            reports.addAll(extractReportsFromCursor(cursor))
            cursor.close()
            db.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving reports: ${e.message}", e)
        }
        return reports
    }
    
    /**
     * Retrieves a specific report by its ID.
     * 
     * @param reportId The ID of the report to retrieve
     * @return The Report object, or null if not found
     */
    fun getReportById(reportId: String): Report? {
        var report: Report? = null
        try {
            val db = this.readableDatabase
            val cursor = db.query(
                REPORTS_TABLE,
                arrayOf(REPORT_ID, REPORT_TIMESTAMP, REPORT_STATUS, REPORT_DATA, REPORT_ATTACHMENTS),
                "$REPORT_ID = ?",
                arrayOf(reportId),
                null,
                null,
                null
            )
            
            val reports = extractReportsFromCursor(cursor)
            report = reports.firstOrNull()
            cursor.close()
            db.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving report $reportId: ${e.message}", e)
        }
        return report
    }
    
    /**
     * Extracts Report objects from a database cursor.
     * 
     * @param cursor The cursor containing report data
     * @return List of Report objects
     */
    private fun extractReportsFromCursor(cursor: Cursor): List<Report> {
        val reports = mutableListOf<Report>()
        
        with(cursor) {
            while (moveToNext()) {
                try {
                    val id = getString(getColumnIndexOrThrow(REPORT_ID))
                    val timestamp = getString(getColumnIndexOrThrow(REPORT_TIMESTAMP))
                    val status = getString(getColumnIndexOrThrow(REPORT_STATUS))
                    
                    // Parse accusation data from JSON
                    val accusationDataJson = getString(getColumnIndexOrThrow(REPORT_DATA))
                    val accusationDataItems = parseAccusationData(accusationDataJson)
                    
                    // Parse attachments from JSON
                    val attachments = parseAttachments(this)
                    
                    reports.add(Report(id, timestamp, status, accusationDataItems, attachments))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing report from database: ${e.message}", e)
                }
            }
        }
        
        return reports
    }
    
    /**
     * Parses accusation data from JSON string.
     * 
     * @param accusationDataJson JSON string containing accusation data
     * @return List of AccusationDataItem objects
     */
    private fun parseAccusationData(accusationDataJson: String): List<AccusationDataItem> {
        val accusationDataItems = mutableListOf<AccusationDataItem>()
        try {
            val jsonArray = JSONArray(accusationDataJson)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val key = jsonObject.getString("key")
                val value = jsonObject.getString("value")
                accusationDataItems.add(AccusationDataItem(key, value))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing accusation data: ${e.message}", e)
        }
        return accusationDataItems
    }
    
    /**
     * Parses attachments from the cursor.
     * 
     * @param cursor The cursor containing attachment data
     * @return List of attachment paths
     */
    private fun parseAttachments(cursor: Cursor): List<String> {
        val attachments = mutableListOf<String>()
        try {
            val attachmentsIndex = cursor.getColumnIndex(REPORT_ATTACHMENTS)
            if (attachmentsIndex != -1 && !cursor.isNull(attachmentsIndex)) {
                val attachmentsJson = cursor.getString(attachmentsIndex)
                val attachmentsArray = JSONArray(attachmentsJson)
                for (i in 0 until attachmentsArray.length()) {
                    attachments.add(attachmentsArray.getString(i))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing attachments: ${e.message}", e)
        }
        return attachments
    }

    /**
     * Gets the absolute path of the database file.
     * 
     * @return Absolute path to the database file
     */
    fun getDatabasePath(): String {
        return context.getDatabasePath(DATABASE_NAME).absolutePath
    }
}