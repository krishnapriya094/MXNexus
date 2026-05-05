package com.example.mxnexus.ui.queries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.example.mxnexus.data.model.Query
import com.example.mxnexus.util.TimeUtils

class QueryAdapter(
    private val queries: MutableList<Query>,
    private val onQueryClick: (Query) -> Unit
) : RecyclerView.Adapter<QueryAdapter.QueryViewHolder>() {

    class QueryViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val userName: TextView = v.findViewById(R.id.tvQueryUserName)
        val timestamp: TextView = v.findViewById(R.id.tvQueryTimestamp)
        val question: TextView = v.findViewById(R.id.tvQueryQuestion)
        val answerCount: TextView = v.findViewById(R.id.tvAnswerCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_query, parent, false)
        return QueryViewHolder(view)
    }

    override fun onBindViewHolder(holder: QueryViewHolder, position: Int) {
        val query = queries[position]
        holder.userName.text = query.userName
        holder.timestamp.text = TimeUtils.getRelativeTime(query.timestamp)
        holder.question.text = query.question
        holder.answerCount.text = "${query.answerCount} Answers"

        holder.itemView.setOnClickListener { onQueryClick(query) }
    }

    override fun getItemCount() = queries.size

    fun updateQueries(newQueries: List<Query>) {
        queries.clear()
        queries.addAll(newQueries)
        notifyDataSetChanged()
    }
}
