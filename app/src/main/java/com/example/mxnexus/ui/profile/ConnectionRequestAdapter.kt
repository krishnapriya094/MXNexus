package com.example.mxnexus.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mxnexus.R
import com.example.mxnexus.data.model.ConnectionRequest
import com.google.android.material.button.MaterialButton

class ConnectionRequestAdapter(
    private val requests: List<ConnectionRequest>,
    private val onAccept: (ConnectionRequest) -> Unit,
    private val onReject: (ConnectionRequest) -> Unit
) : RecyclerView.Adapter<ConnectionRequestAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val imgAvatar: ImageView = v.findViewById(R.id.imgRequestAvatar)
        val tvName: TextView = v.findViewById(R.id.tvRequestName)
        val tvRole: TextView = v.findViewById(R.id.tvRequestRole)
        val btnAccept: MaterialButton = v.findViewById(R.id.btnAcceptRequest)
        val btnReject: MaterialButton = v.findViewById(R.id.btnRejectRequest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_connection_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requests[position]
        holder.tvName.text = request.senderName
        holder.tvRole.text = request.senderRole

        if (request.senderProfileImageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(request.senderProfileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .into(holder.imgAvatar)
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_profile)
        }

        holder.btnAccept.setOnClickListener { onAccept(request) }
        holder.btnReject.setOnClickListener { onReject(request) }
    }

    override fun getItemCount() = requests.size
}
