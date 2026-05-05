package com.example.mxnexus.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MentionAdapter(
    private val users: MutableList<Pair<String, String>>, // userId, userName
    private val onMentionClick: (String) -> Unit
) : RecyclerView.Adapter<MentionAdapter.MentionViewHolder>() {

    inner class MentionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMentionName: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MentionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return MentionViewHolder(view)
    }

    override fun onBindViewHolder(holder: MentionViewHolder, position: Int) {
        val (_, userName) = users[position]
        holder.tvMentionName.text = "@$userName"
        holder.tvMentionName.setTextColor(
            android.graphics.Color.parseColor("#2D3A8C")
        )
        holder.itemView.setOnClickListener {
            onMentionClick(userName)
        }
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<Pair<String, String>>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }
}