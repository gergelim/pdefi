package com.pdefi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var rvRecent: RecyclerView
    private lateinit var tvNoRecent: TextView
    private lateinit var recentAdapter: RecentFilesAdapter

    // SAF file picker launcher
    private val openPdfLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Persist read permission across reboots
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                saveRecentFile(uri)
                openReader(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("pdefi_prefs", Context.MODE_PRIVATE)

        rvRecent = findViewById(R.id.rvRecentFiles)
        tvNoRecent = findViewById(R.id.tvNoRecent)

        setupRecentFiles()

        findViewById<Button>(R.id.btnOpenPdf).setOnClickListener {
            launchFilePicker()
        }

        // Handle PDF opened from external app (e.g., file manager)
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            intent.data?.let { uri ->
                saveRecentFile(uri)
                openReader(uri)
            }
        }
    }

    private fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        openPdfLauncher.launch(Intent.createChooser(intent, getString(R.string.file_picker_title)))
    }

    private fun openReader(uri: Uri) {
        val intent = Intent(this, ReaderActivity::class.java).apply {
            putExtra(ReaderActivity.EXTRA_PDF_URI, uri.toString())
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    // ─── Recent Files ───────────────────────────────────────────────────────────

    private fun setupRecentFiles() {
        recentAdapter = RecentFilesAdapter(
            context = this,
            files = loadRecentFiles().toMutableList(),
            onItemClick = { uri ->
                openReader(uri)
            },
            onItemLongClick = { uri ->
                removeRecentFile(uri)
                refreshRecentFiles()
            }
        )
        rvRecent.layoutManager = LinearLayoutManager(this)
        rvRecent.adapter = recentAdapter
        refreshRecentFiles()
    }

    private fun refreshRecentFiles() {
        val list = loadRecentFiles()
        recentAdapter.updateList(list.toMutableList())
        tvNoRecent.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadRecentFiles(): List<RecentFile> {
        val json = prefs.getString("recent_files", "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = mutableListOf<RecentFile>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                RecentFile(
                    uri = Uri.parse(obj.getString("uri")),
                    name = obj.getString("name"),
                    dateAdded = obj.getLong("date")
                )
            )
        }
        return list
    }

    private fun saveRecentFile(uri: Uri) {
        val list = loadRecentFiles().toMutableList()
        // Remove if already present
        list.removeAll { it.uri == uri }
        // Get display name
        val name = getFileName(uri)
        // Add to front
        list.add(0, RecentFile(uri, name, System.currentTimeMillis()))
        // Keep max 20
        val trimmed = list.take(20)
        val arr = JSONArray()
        for (f in trimmed) {
            val obj = org.json.JSONObject()
            obj.put("uri", f.uri.toString())
            obj.put("name", f.name)
            obj.put("date", f.dateAdded)
            arr.put(obj)
        }
        prefs.edit().putString("recent_files", arr.toString()).apply()
    }

    private fun removeRecentFile(uri: Uri) {
        val list = loadRecentFiles().toMutableList()
        list.removeAll { it.uri == uri }
        val arr = JSONArray()
        for (f in list) {
            val obj = org.json.JSONObject()
            obj.put("uri", f.uri.toString())
            obj.put("name", f.name)
            obj.put("date", f.dateAdded)
            arr.put(obj)
        }
        prefs.edit().putString("recent_files", arr.toString()).apply()
    }

    private fun getFileName(uri: Uri): String {
        var name = "documento.pdf"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx) ?: name
            }
        }
        return name
    }
}

// ─── Data class ─────────────────────────────────────────────────────────────────

data class RecentFile(
    val uri: Uri,
    val name: String,
    val dateAdded: Long
)
