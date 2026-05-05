package com.example.mxnexus.ui.queries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.example.mxnexus.data.model.Answer
import com.example.mxnexus.util.TimeUtils

class AnswerAdapter(
    private val answers: MutableList<Answer>
) : RecyclerView.Adapter<AnswerAdapter.AnswerViewHolder>() {

    class AnswerViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val userName: TextView = v.findViewById(R.id.tvAnswerUserName)
        val userRole: TextView = v.findViewById(R.id.tvAnswerUserRole)
        val timestamp: TextView = v.findViewById(R.id.tvAnswerTimestamp)
        val answerText: TextView = v.findViewById(R.id.tvAnswerText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnswerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_answer, parent, false)
        return AnswerViewHolder(view)
    }

    override fun onBindViewHolder(holder: AnswerViewHolder, position: Int) {
        val answer = answers[position]
        holder.userName.text = answer.userName
        holder.userRole.text = answer.userRole
        holder.timestamp.text = TimeUtils.getRelativeTime(answer.timestamp)
        holder.answerText.text = answer.answerText
    }

    override fun getItemCount() = answers.size

    fun updateAnswers(newAnswers: List<Answer>) {
        answers.clear()
        answers.addAll(newAnswers)
        notifyDataSetChanged()
    }
}
