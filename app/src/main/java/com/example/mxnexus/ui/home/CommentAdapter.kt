package com.example.mxnexus.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mxnexus.R
import com.example.mxnexus.data.model.Comment
import com.google.firebase.auth.FirebaseAuth

class CommentAdapter(
    private val comments: MutableList<Comment>,
    private val currentUserRole: String, // Added role for Admin check
    private val onDeleteClick: (Comment) -> Unit = {}
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCommentUser: TextView = itemView.findViewById(R.id.tvCommentUser)
        val tvCommentText: TextView = itemView.findViewById(R.id.tvCommentText)
        val btnDeleteComment: ImageButton = itemView.findViewById(R.id.btnDeleteComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.tvCommentUser.text = comment.userName
        holder.tvCommentText.text = comment.commentText

        // ADMIN POWER: Show delete for OWN comments OR if user is ADMIN
        if (comment.userId == currentUserId || currentUserRole == "Admin") {
            holder.btnDeleteComment.visibility = View.VISIBLE
            holder.btnDeleteComment.setOnClickListener {
                onDeleteClick(comment)
            }
        } else {
            holder.btnDeleteComment.visibility = View.GONE
        }
    }

    override fun getItemCount() = comments.size

    fun updateComments(newComments: List<Comment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }
}
