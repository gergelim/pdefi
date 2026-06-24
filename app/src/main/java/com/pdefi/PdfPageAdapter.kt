package com.pdefi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfPageAdapter(
    private val context: Context,
    private val renderer: PdfRenderer,
    private val pageCount: Int,
    private val onPageTap: () -> Unit
) : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    // Cache rendered bitmaps (max 5 pages in memory)
    private val bitmapCache = LinkedHashMap<Int, Bitmap>(8, 0.75f, true)
    private val MAX_CACHE = 5

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPage: ZoomableImageView = view.findViewById(R.id.ivPage)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        var currentPage: Int = -1
        var renderJob: Job? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_pdf_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.currentPage = position
        holder.renderJob?.cancel()

        holder.ivPage.setOnClickListener { onPageTap() }

        // Check cache first
        val cached = bitmapCache[position]
        if (cached != null && !cached.isRecycled) {
            holder.progressBar.visibility = View.GONE
            holder.ivPage.setImageBitmap(cached)
            return
        }

        // Show loading, render async
        holder.progressBar.visibility = View.VISIBLE
        holder.ivPage.setImageBitmap(null)

        holder.renderJob = scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                renderPage(position)
            }
            if (holder.currentPage == position && bitmap != null) {
                holder.progressBar.visibility = View.GONE
                holder.ivPage.setImageBitmap(bitmap)
                addToCache(position, bitmap)
            }
        }
    }

    override fun onViewRecycled(holder: PageViewHolder) {
        super.onViewRecycled(holder)
        holder.renderJob?.cancel()
        holder.ivPage.setImageBitmap(null)
    }

    override fun getItemCount(): Int = pageCount

    private fun renderPage(pageIndex: Int): Bitmap? {
        return try {
            synchronized(renderer) {
                renderer.openPage(pageIndex).use { page ->
                    // Scale to screen width (high quality)
                    val displayMetrics = context.resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels
                    val screenHeight = displayMetrics.heightPixels

                    val pageWidth = page.width
                    val pageHeight = page.height

                    val scale = minOf(
                        screenWidth.toFloat() / pageWidth,
                        screenHeight.toFloat() / pageHeight
                    ) * 1.5f  // 1.5x for crispness

                    val bitmapWidth = (pageWidth * scale).toInt().coerceAtLeast(1)
                    val bitmapHeight = (pageHeight * scale).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    val matrix = Matrix()
                    matrix.setScale(scale, scale)

                    page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun addToCache(page: Int, bitmap: Bitmap) {
        if (bitmapCache.size >= MAX_CACHE) {
            val oldest = bitmapCache.entries.first()
            bitmapCache.remove(oldest.key)
            if (!oldest.value.isRecycled) oldest.value.recycle()
        }
        bitmapCache[page] = bitmap
    }

    fun cleanup() {
        scope.cancel()
        bitmapCache.values.forEach { if (!it.isRecycled) it.recycle() }
        bitmapCache.clear()
    }
}
