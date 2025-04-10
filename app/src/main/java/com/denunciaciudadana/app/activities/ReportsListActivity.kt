package com.denunciaciudadana.app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.denunciaciudadana.app.R
import com.denunciaciudadana.app.database.DBHelper
import com.denunciaciudadana.app.models.Report

/**
 * Activity that displays a list of all reports submitted by the user.
 * Shows reports in a RecyclerView with basic information and allows
 * navigation to detailed view.
 */
class ReportsListActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "ReportsListActivity"
    }
    
    private lateinit var dbHelper: DBHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReportAdapter
    private lateinit var emptyStateView: TextView
    private lateinit var backButton: ImageButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports_list)
        
        initializeViews()
        setupBackButton()
        loadAndDisplayReports()
    }
    
    /**
     * Initialize views from the layout.
     */
    private fun initializeViews() {
        try {
            emptyStateView = findViewById(R.id.emptyStateTextView)
            recyclerView = findViewById(R.id.reportsRecyclerView)
            backButton = findViewById(R.id.backButton)
            
            recyclerView.layoutManager = LinearLayoutManager(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views", e)
        }
    }
    
    /**
     * Set up the back button click listener.
     */
    private fun setupBackButton() {
        backButton.setOnClickListener { finish() }
    }
    
    /**
     * Load reports from database and display them.
     */
    private fun loadAndDisplayReports() {
        try {
            dbHelper = DBHelper(this)
            val reports = dbHelper.getAllReports()
            
            if (reports.isEmpty()) {
                showEmptyState()
            } else {
                showReportsList(reports)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading reports", e)
            showEmptyState()
        }
    }
    
    /**
     * Show empty state message when no reports exist.
     */
    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
    }
    
    /**
     * Show the list of reports in the RecyclerView.
     * 
     * @param reports List of reports to display
     */
    private fun showReportsList(reports: List<Report>) {
        recyclerView.visibility = View.VISIBLE
        emptyStateView.visibility = View.GONE
        
        adapter = ReportAdapter(reports, this::onReportClicked)
        recyclerView.adapter = adapter
    }
    
    /**
     * Handle report item click by navigating to the detail activity.
     * 
     * @param report The report that was clicked
     */
    private fun onReportClicked(report: Report) {
        val intent = Intent(this, ReportDetailActivity::class.java).apply {
            putExtra("REPORT_ID", report.id)
        }
        startActivity(intent)
    }
    
    /**
     * RecyclerView adapter for displaying report items.
     * 
     * @property reports List of reports to display
     * @property onReportClick Callback function for report item clicks
     */
    private class ReportAdapter(
        private val reports: List<Report>,
        private val onReportClick: (Report) -> Unit
    ) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_report, parent, false)
            return ReportViewHolder(view)
        }
        
        override fun getItemCount(): Int = reports.size
        
        override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
            val report = reports[position]
            holder.bind(report)
            holder.itemView.setOnClickListener { onReportClick(report) }
        }
        
        /**
         * ViewHolder for report items in the RecyclerView.
         */
        class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val idTextView: TextView = itemView.findViewById(R.id.reportIdTextView)
            private val dateTextView: TextView = itemView.findViewById(R.id.reportDateTextView)
            private val statusTextView: TextView = itemView.findViewById(R.id.reportStatusTextView)
            
            /**
             * Bind report data to the view.
             * 
             * @param report The report to display
             */
            fun bind(report: Report) {
                idTextView.text = "ID: ${report.id}"
                dateTextView.text = "Fecha: ${report.getFormattedDate()}"
                statusTextView.text = report.status
            }
        }
    }
}