package com.pdefi

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentFilesAdapter(
    private val context: Context,
    private var files: MutableList<RecentFile>,
    private val onItemClick: (Uri) -> Unit,
    private val onItemLongClick: (Uri) -> Unit
) : RecyclerView.Adapter<RecentFilesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvFileDate: TextView = view.findViewById(R.id.tvFileDate)
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_recent_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        holder.tvFileName.text = file.name
        holder.tvFileDate.text = dateFormat.format(Date(file.dateAdded))
        holder.itemView.setOnClickListener { onItemClick(file.uri) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(file.uri)
            true
        }
    }

    override fun getItemCount(): Int = files.size

    fun updateList(newList: MutableList<RecentFile>) {
        files = newList
        notifyDataSetChanged()
    }
}
