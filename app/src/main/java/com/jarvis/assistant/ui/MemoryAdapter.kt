package com.jarvis.assistant.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jarvis.assistant.R
import com.jarvis.assistant.memory.MemoryEntity

class MemoryAdapter(
    private var items: List<MemoryEntity>,
    private val onDeleteClicked: (MemoryEntity) -> Unit
) : RecyclerView.Adapter<MemoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategory: TextView = view.findViewById(R.id.tvMemoryCategory)
        val tvValue: TextView = view.findViewById(R.id.tvMemoryValue)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteMemory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_memory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvCategory.text = "[${item.category}] ${item.key}"
        holder.tvValue.text = item.value
        holder.btnDelete.setOnClickListener {
            onDeleteClicked(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<MemoryEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}
