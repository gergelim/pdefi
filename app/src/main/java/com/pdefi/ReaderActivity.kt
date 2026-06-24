package com.pdefi

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.WindowInsetsController
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class ReaderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PDF_URI = "extra_pdf_uri"
        private const val BARS_HIDE_DELAY = 3500L
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var topBar: View
    private lateinit var bottomBar: View
    private lateinit var tvFileName: TextView
    private lateinit var tvPageBadge: TextView
    private lateinit var tvPageInfo: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var btnClose: ImageButton

    private var pdfRenderer: PdfRenderer? = null
    private var parcelFd: ParcelFileDescriptor? = null
    private var pageAdapter: PdfPageAdapter? = null
    private var totalPages = 0
    private var barsVisible = true

    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideBars() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        bindViews()
        enableImmersiveMode()

        val uriString = intent.getStringExtra(EXTRA_PDF_URI)
        if (uriString == null) {
            finish()
            return
        }

        val uri = Uri.parse(uriString)
        val fileName = getFileName(uri)
        tvFileName.text = fileName

        if (!openPdf(uri)) {
            Toast.makeText(this, getString(R.string.error_open), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupViewPager()
        setupControls()
        scheduleHideBars()
    }

    // ─── View binding ─────────────────────────────────────────────────────────

    private fun bindViews() {
        viewPager = findViewById(R.id.viewPager)
        topBar = findViewById(R.id.topBar)
        bottomBar = findViewById(R.id.bottomBar)
        tvFileName = findViewById(R.id.tvFileName)
        tvPageBadge = findViewById(R.id.tvPageBadge)
        tvPageInfo = findViewById(R.id.tvPageInfo)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        seekBar = findViewById(R.id.seekBar)
        btnClose = findViewById(R.id.btnClose)
    }

    // ─── PDF open ─────────────────────────────────────────────────────────────

    private fun openPdf(uri: Uri): Boolean {
        return try {
            parcelFd = contentResolver.openFileDescriptor(uri, "r") ?: return false
            pdfRenderer = PdfRenderer(parcelFd!!)
            totalPages = pdfRenderer!!.pageCount
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ─── ViewPager setup ──────────────────────────────────────────────────────

    private fun setupViewPager() {
        pageAdapter = PdfPageAdapter(this, pdfRenderer!!, totalPages) {
            toggleBars()
        }
        viewPager.adapter = pageAdapter
        viewPager.offscreenPageLimit = 1

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicators(position)
            }
        })

        seekBar.max = (totalPages - 1).coerceAtLeast(0)
        updatePageIndicators(0)
    }

    // ─── Controls ─────────────────────────────────────────────────────────────

    private fun setupControls() {
        btnClose.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        btnPrev.setOnClickListener {
            val cur = viewPager.currentItem
            if (cur > 0) viewPager.currentItem = cur - 1
            scheduleHideBars()
        }

        btnNext.setOnClickListener {
            val cur = viewPager.currentItem
            if (cur < totalPages - 1) viewPager.currentItem = cur + 1
            scheduleHideBars()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewPager.setCurrentItem(progress, false)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {
                hideHandler.removeCallbacks(hideRunnable)
            }
            override fun onStopTrackingTouch(sb: SeekBar) {
                scheduleHideBars()
            }
        })
    }

    private fun updatePageIndicators(page: Int) {
        val display = page + 1
        tvPageBadge.text = "$display / $totalPages"
        tvPageInfo.text = getString(R.string.page_of, display, totalPages)
        seekBar.progress = page
        btnPrev.isEnabled = page > 0
        btnNext.isEnabled = page < totalPages - 1
    }

    // ─── Bar toggle (immersive) ───────────────────────────────────────────────

    private fun toggleBars() {
        if (barsVisible) hideBars() else showBars()
    }

    private fun showBars() {
        barsVisible = true
        topBar.animate().alpha(1f).translationY(0f).setDuration(220).start()
        bottomBar.animate().alpha(1f).translationY(0f).setDuration(220).start()
        scheduleHideBars()
    }

    private fun hideBars() {
        barsVisible = false
        topBar.animate().alpha(0f).translationY(-topBar.height.toFloat()).setDuration(300).start()
        bottomBar.animate().alpha(0f).translationY(bottomBar.height.toFloat()).setDuration(300).start()
    }

    private fun scheduleHideBars() {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, BARS_HIDE_DELAY)
    }

    // ─── Immersive / full screen ───────────────────────────────────────────────

    private fun enableImmersiveMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { ctrl ->
                ctrl.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                ctrl.hide(
                    android.view.WindowInsets.Type.statusBars() or
                    android.view.WindowInsets.Type.navigationBars()
                )
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

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

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        hideHandler.removeCallbacksAndMessages(null)
        pageAdapter?.cleanup()
        pdfRenderer?.close()
        parcelFd?.close()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }
}
