package com.example.mxnexus.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.mxnexus.R;
import com.example.mxnexus.data.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import com.example.mxnexus.data.model.Notification;
import java.util.List;

/**
 * Adapter for the Notifications RecyclerView.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notificationList;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notificationList, OnNotificationClickListener listener) {
        this.notificationList = notificationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        holder.tvMessage.setText(notification.getMessage());
        holder.tvTime.setText(getRelativeTime(notification.getTimestamp()));

        // Default to initial
        String senderName = notification.getSenderName();
        if (senderName != null && !senderName.isEmpty()) {
            holder.tvInitial.setText(String.valueOf(senderName.charAt(0)).toUpperCase());
        } else {
            holder.tvInitial.setText("U");
        }
        holder.imgAvatar.setVisibility(View.GONE);
        holder.tvInitial.setVisibility(View.VISIBLE);

        // Fetch and load profile image
        String senderId = notification.getSenderId();
        if (senderId != null && !senderId.isEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(senderId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            if (holder.itemView.getContext() != null) {
                                holder.imgAvatar.setVisibility(View.VISIBLE);
                                holder.tvInitial.setVisibility(View.GONE);
                                Glide.with(holder.itemView.getContext())
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .into(holder.imgAvatar);
                            }
                        }
                    }
                });
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public void updateList(List<Notification> newList) {
        this.notificationList = newList;
        notifyDataSetChanged();
    }

    private String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        if (diff < 0) return "Just now";
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        if (seconds < 60) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        if (hours < 24) return hours + "h ago";
        return days + "d ago";
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvInitial, tvMessage, tvTime;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgNotifAvatar);
            tvInitial = itemView.findViewById(R.id.tvNotifInitial);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime    = itemView.findViewById(R.id.tvNotificationTime);
        }
    }
}
