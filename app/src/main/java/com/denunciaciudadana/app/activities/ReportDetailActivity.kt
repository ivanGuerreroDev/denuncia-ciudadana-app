package com.denunciaciudadana.app.activities

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import com.denunciaciudadana.app.R
import com.denunciaciudadana.app.database.DBHelper
import com.denunciaciudadana.app.models.AccusationDataItem
import com.denunciaciudadana.app.models.Report


/**
 * Activity that displays detailed information about a specific report.
 * Shows report metadata, key-value data, and provides access to attachments.
 */
class ReportDetailActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "ReportDetailActivity"
        private const val REPORT_ID_KEY = "REPORT_ID"
    }
    
    private lateinit var dbHelper: DBHelper
    private lateinit var reportDataRecyclerView: RecyclerView
    private lateinit var attachmentsRecyclerView: RecyclerView
    
    // UI elements
    private lateinit var reportIdTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var attachmentsTitleTextView: TextView
    private lateinit var backButton: Button
    
    private var reportId: String? = null
    private var report: Report? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_detail)
        
        initializeDatabase()
        initializeViews()
        
        if (!loadReportData()) {
            return  // Exit if report loading failed
        }
        
        displayReportDetails()
        setupAttachments()
        setupBackButton()
    }
    
    /**
     * Initialize the database helper.
     */
    private fun initializeDatabase() {
        dbHelper = DBHelper(this)
    }
    
    /**
     * Initialize views from the layout.
     */
    private fun initializeViews() {
        try {
            reportIdTextView = findViewById(R.id.detailReportIdTextView)
            dateTextView = findViewById(R.id.detailDateTextView)
            statusTextView = findViewById(R.id.detailStatusTextView)
            attachmentsTitleTextView = findViewById(R.id.attachmentsTitleTextView)
            backButton = findViewById(R.id.backToListButton)
            
            reportDataRecyclerView = findViewById(R.id.reportDataRecyclerView)
            reportDataRecyclerView.layoutManager = LinearLayoutManager(this)
            
            attachmentsRecyclerView = findViewById(R.id.attachmentsRecyclerView)
            attachmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
            showErrorAndFinish("Error al inicializar la vista")
        }
    }
    
    /**
     * Load report data from the database using the ID from the intent.
     * 
     * @return true if report loaded successfully, false otherwise
     */
    private fun loadReportData(): Boolean {
        try {
            // Get report ID from intent
            reportId = intent.getStringExtra(REPORT_ID_KEY)
            if (reportId == null) {
                showErrorAndFinish("Error: ID de reporte no encontrado")
                return false
            }
            
            // Load report from database
            report = dbHelper.getReportById(reportId!!)
            if (report == null) {
                showErrorAndFinish("Error: Reporte no encontrado")
                return false
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading report data", e)
            showErrorAndFinish("Error cargando datos del reporte")
            return false
        }
    }
    
    /**
     * Display the report details in the UI.
     */
    private fun displayReportDetails() {
        report?.let { report ->
            reportIdTextView.text = "ID: ${report.id}"
            // Use the correct property name from your Report class
            dateTextView.text = "Fecha: ${formatDate(report.timestamp)}"
            statusTextView.text = report.status
            
            // Setup the data RecyclerView
            reportDataRecyclerView.adapter = ReportDataAdapter(report.accusationData)
        }
    }

    /**
     * Format a date string to a more readable format
     * 
     * @param dateStr The original date string
     * @return The formatted date string
     */
    private fun formatDate(dateStr: String): String {
        try {
            // Handle ISO 8601 format (e.g., "2025-04-10T13:45:30.634Z")
            val inputFormat = if (dateStr.contains("T") && (dateStr.contains("Z") || dateStr.contains("+"))) {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
            } else {
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            }
            
            val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            
            val parsedDate = inputFormat.parse(dateStr)
            return parsedDate?.let { outputFormat.format(it) } ?: dateStr
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: $dateStr", e)
            // Try alternative ISO format if first attempt fails
            try {
                val alternativeFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.getDefault())
                val parsedDate = alternativeFormat.parse(dateStr)
                val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                return parsedDate?.let { outputFormat.format(it) } ?: dateStr
            } catch (e2: Exception) {
                Log.e(TAG, "Error with alternative date format", e2)
                return dateStr
            }
        }
    }
    
    /**
     * Set up attachments section and RecyclerView.
     */
    private fun setupAttachments() {
        report?.let { report ->
            if (report.attachments.isEmpty()) {
                hideAttachmentsSection()
            } else {
                attachmentsRecyclerView.adapter = AttachmentsAdapter(report.attachments)
            }
        }
    }
    
    /**
     * Hide attachments section if there are no attachments.
     */
    private fun hideAttachmentsSection() {
        attachmentsTitleTextView.visibility = View.GONE
        attachmentsRecyclerView.visibility = View.GONE
    }
    
    /**
     * Set up back button click listener.
     */
    private fun setupBackButton() {
        backButton.setOnClickListener { finish() }
    }
    
    /**
     * Show an error message and finish the activity.
     * 
     * @param message Error message to display
     */
    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
    
    /**
     * Adapter for displaying report data items.
     */
    private class ReportDataAdapter(private val dataItems: List<AccusationDataItem>) :
        RecyclerView.Adapter<ReportDataAdapter.DataViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_report_data, parent, false)
            return DataViewHolder(view)
        }
        
        override fun getItemCount(): Int = dataItems.size
        
        override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
            holder.bind(dataItems[position])
        }
        
        /**
         * ViewHolder for report data items.
         */
        class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val keyTextView: TextView = itemView.findViewById(R.id.dataKeyTextView)
            private val valueTextView: TextView = itemView.findViewById(R.id.dataValueTextView)
            
            fun bind(dataItem: AccusationDataItem) {
                keyTextView.text = dataItem.key
                valueTextView.text = dataItem.value
            }
        }
    }
    
    /**
     * Adapter for displaying attachment items.
     */
    private inner class AttachmentsAdapter(private val attachments: List<String>) :
        RecyclerView.Adapter<AttachmentsAdapter.AttachmentViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttachmentViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_attachment, parent, false)
            return AttachmentViewHolder(view)
        }
        
        override fun getItemCount(): Int = attachments.size
        
        override fun onBindViewHolder(holder: AttachmentViewHolder, position: Int) {
            holder.bind(attachments[position])
        }
        
        /**
         * ViewHolder for attachment items.
         */
        inner class AttachmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val attachmentIcon: ImageView = itemView.findViewById(R.id.attachmentIcon)
            private val attachmentNameTextView: TextView = itemView.findViewById(R.id.attachmentNameTextView)
            private val viewAttachmentButton: ImageButton = itemView.findViewById(R.id.viewAttachmentButton)
            
            /**
             * Bind attachment data to the view.
             * 
             * @param attachmentPath Path to the attachment file
             */
            fun bind(attachmentPath: String) {
                try {
                    val file = File(attachmentPath)
                    attachmentNameTextView.text = file.name
                    
                    setAttachmentTypeIcon(attachmentPath)
                    setupViewAttachmentButton(attachmentPath)
                } catch (e: Exception) {
                    Log.e(TAG, "Error binding attachment: $attachmentPath", e)
                    attachmentNameTextView.text = "Archivo no disponible"
                    attachmentIcon.setImageResource(R.drawable.ic_attachment)
                }
            }
            
            /**
             * Set the appropriate icon based on file type.
             * 
             * @param filePath Path to the file
             */
            private fun setAttachmentTypeIcon(filePath: String) {
                val iconResId = when {
                    isImageFile(filePath) -> android.R.drawable.ic_menu_gallery
                    isAudioFile(filePath) -> android.R.drawable.ic_media_play
                    isVideoFile(filePath) -> android.R.drawable.ic_media_play
                    else -> R.drawable.ic_attachment
                }
                attachmentIcon.setImageResource(iconResId)
            }
            
            /**
             * Set up the view attachment button click listener.
             * 
             * @param filePath Path to the file
             */
            private fun setupViewAttachmentButton(filePath: String) {
                viewAttachmentButton.setOnClickListener {
                    openAttachmentFile(filePath)
                }
            }
        }
        
        /**
         * Check if the file is an image.
         * 
         * @param filePath Path to the file
         * @return True if file is an image
         */
        private fun isImageFile(filePath: String): Boolean {
            return filePath.endsWith(".jpg", true) || 
                   filePath.endsWith(".jpeg", true) || 
                   filePath.endsWith(".png", true)
        }
        
        /**
         * Check if the file is an audio file.
         * 
         * @param filePath Path to the file
         * @return True if file is an audio file
         */
        private fun isAudioFile(filePath: String): Boolean {
            return filePath.endsWith(".mp3", true) || 
                   filePath.endsWith(".wav", true) || 
                   filePath.endsWith(".aac", true) ||
                   filePath.endsWith(".3gp", true)
        }
        
        /**
         * Check if the file is a video file.
         * 
         * @param filePath Path to the file
         * @return True if file is a video file
         */
        private fun isVideoFile(filePath: String): Boolean {
            return filePath.endsWith(".mp4", true) || 
                   filePath.endsWith(".3gp", true)
        }
        
        /**
         * Get the MIME type for a file based on its extension.
         * 
         * @param filePath Path to the file
         * @return MIME type string
         */
        private fun getMimeType(filePath: String): String {
            return when {
                filePath.endsWith(".jpg", true) || filePath.endsWith(".jpeg", true) -> "image/jpeg"
                filePath.endsWith(".png", true) -> "image/png"
                filePath.endsWith(".mp3", true) -> "audio/mp3"
                filePath.endsWith(".wav", true) -> "audio/wav"
                filePath.endsWith(".aac", true) -> "audio/aac"
                filePath.endsWith(".mp4", true) -> "video/mp4"
                filePath.endsWith(".3gp", true) -> "video/3gpp"
                else -> "*/*"
            }
        }
        
        /**
         * Open the attachment file with the appropriate app.
         * 
         * @param filePath Path to the file
         */
        private fun openAttachmentFile(filePath: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                val uri = Uri.parse(filePath)
                intent.setDataAndType(uri, getMimeType(filePath))
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    this@ReportDetailActivity,
                    "No se encontró aplicación para abrir este archivo",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error opening file: $filePath", e)
                Toast.makeText(
                    this@ReportDetailActivity,
                    "Error al abrir el archivo: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}