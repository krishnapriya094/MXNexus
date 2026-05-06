package com.example.mxnexus.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mxnexus.R
import com.example.mxnexus.data.model.User
import com.google.android.material.button.MaterialButton

class UserListAdapter(
    private val userList: List<User>,
    private val onUserClick: (String) -> Unit,
    private val onActionClick: ((User) -> Unit)? = null,
    private val actionText: String? = null
) : RecyclerView.Adapter<UserListAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imgAvatar: ImageView = v.findViewById(R.id.imgUserAvatar)
        val tvName: TextView = v.findViewById(R.id.tvUserName)
        val tvRole: TextView = v.findViewById(R.id.tvUserRole)
        val btnAction: MaterialButton = v.findViewById(R.id.btnUserAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList[position]
        holder.tvName.text = user.name
        holder.tvRole.text = user.role

        if (user.profileImageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(user.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .into(holder.imgAvatar)
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_profile)
        }

        if (actionText != null) {
            holder.btnAction.visibility = View.VISIBLE
            holder.btnAction.text = actionText
            holder.btnAction.setOnClickListener { onActionClick?.invoke(user) }
        } else {
            holder.btnAction.text = "View"
            holder.btnAction.setOnClickListener { onUserClick(user.userId) }
        }

        holder.itemView.setOnClickListener { onUserClick(user.userId) }
    }

    override fun getItemCount() = userList.size
}
