package com.example.mxnexus.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.mxnexus.R
import com.example.mxnexus.data.model.Post
import com.example.mxnexus.util.TimeUtils

class PostAdapter(
    private var posts: MutableList<Post>,
    private val currentUserId: String,
    private val currentUserRole: String,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onShareClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit,
    private val onMessageClick: ((Post) -> Unit)? = null,
    private val onImageClick: ((String) -> Unit)? = null,
    private val onProfileClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView     = itemView.findViewById(R.id.imgPostAvatar)
        val tvUserName: TextView     = itemView.findViewById(R.id.tvPostUserName)
        val tvUserRole: TextView     = itemView.findViewById(R.id.tvPostRole)
        val tvTimestamp: TextView    = itemView.findViewById(R.id.tvPostTime)
        val tvContent: TextView      = itemView.findViewById(R.id.tvPostContent)
        val imgPostPhoto: ImageView  = itemView.findViewById(R.id.imgPostPhoto)
        val btnLike: LinearLayout    = itemView.findViewById(R.id.btnPostLike)
        val icLike: ImageView        = itemView.findViewById(R.id.icLike)
        val tvLikeCount: TextView    = itemView.findViewById(R.id.tvPostLikeCount)
        val btnComment: LinearLayout = itemView.findViewById(R.id.btnPostComment)
        val tvCommentCount: TextView = itemView.findViewById(R.id.tvPostCommentCount)
        val btnMessage: ImageView    = itemView.findViewById(R.id.btnPostMessage)
        val btnShare: ImageView      = itemView.findViewById(R.id.btnPostShare)
        val btnMore: ImageView       = itemView.findViewById(R.id.btnPostMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post      = posts[position]
        val ctx       = holder.itemView.context
        val isOwnPost = post.userId == currentUserId

        // ── User info ──────────────────────────────────────────────────────────
        holder.tvUserName.text  = post.userName.ifBlank { "Unknown User" }
        holder.tvUserRole.text  = post.userRole
        holder.tvTimestamp.text = TimeUtils.getRelativeTime(post.timestamp)
        holder.tvContent.text   = post.content

        // Profile click (avatar + name)
        holder.imgAvatar.setOnClickListener  { onProfileClick?.invoke(post.userId) }
        holder.tvUserName.setOnClickListener { onProfileClick?.invoke(post.userId) }

        // Avatar
        if (post.profileImageUrl.isNotBlank()) {
            Glide.with(ctx)
                .load(post.profileImageUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.ic_profile)
                .into(holder.imgAvatar)
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_profile)
        }

        // ── Post image ─────────────────────────────────────────────────────────
        if (post.imageUrl.isNotBlank()) {
            holder.imgPostPhoto.visibility = View.VISIBLE
            Glide.with(ctx)
                .load(post.imageUrl)
                .apply(RequestOptions().transform(RoundedCorners(16)))
                .into(holder.imgPostPhoto)
            holder.imgPostPhoto.setOnClickListener { onImageClick?.invoke(post.imageUrl) }
        } else {
            holder.imgPostPhoto.visibility = View.GONE
            Glide.with(ctx).clear(holder.imgPostPhoto)
        }

        // ── Reactions ──────────────────────────────────────────────────────────
        val isLiked = post.likedBy.contains(currentUserId)
        holder.icLike.setImageResource(
            if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        holder.tvLikeCount.text    = post.likeCount.toString()
        holder.btnLike.setOnClickListener    { onLikeClick(post) }
        holder.tvCommentCount.text = post.commentCount.toString()
        holder.btnComment.setOnClickListener { onCommentClick(post) }
        holder.btnShare.setOnClickListener   { onShareClick(post) }

        // ── Message button — only for other users when callback is set ─────────
        if (!isOwnPost && onMessageClick != null) {
            holder.btnMessage.visibility = View.VISIBLE
            holder.btnMessage.setOnClickListener { onMessageClick.invoke(post) }
        } else {
            holder.btnMessage.visibility = View.GONE
        }

        // ── Delete / more — own posts or Admin ────────────────────────────────
        if (isOwnPost || currentUserRole == "Admin") {
            holder.btnMore.visibility = View.VISIBLE
            holder.btnMore.setOnClickListener { onDeleteClick(post) }
        } else {
            holder.btnMore.visibility = View.GONE
        }
    }

    override fun getItemCount() = posts.size

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
}
