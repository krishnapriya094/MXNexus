package com.example.mxnexus.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mxnexus.R;
import com.example.mxnexus.data.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;

public class AlertsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Back button (new layout uses ImageView, not Toolbar)
        findViewById(R.id.btnNotifBack).setOnClickListener(v -> onBackPressed());

        // Mark all read button
        TextView btnMarkAll = findViewById(R.id.btnMarkAllRead);
        if (btnMarkAll != null) btnMarkAll.setOnClickListener(v -> markAllAsRead());

        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList, this);
        rvNotifications.setAdapter(adapter);

        loadAlerts();
        markAllAsRead();
    }

    private void loadAlerts() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) return;

        db.collection("alerts")
            .whereEqualTo("receiverId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshot, error) -> {
                if (error != null) return;
                if (snapshot != null) {
                    notificationList.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Notification n = doc.toObject(Notification.class);
                        if (n != null) {
                            n.setNotificationId(doc.getId());
                            notificationList.add(n);
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            });
    }

    private void markAllAsRead() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) return;

        db.collection("alerts")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener(snapshots -> {
                WriteBatch batch = db.batch();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    batch.update(doc.getReference(), "isRead", true);
                }
                batch.commit();
            });
    }

    @Override
    public void onNotificationClick(Notification notification) {
        String postId = notification.getPostId();
        String type = notification.getType();

        if (postId != null && !postId.isEmpty()) {
            Intent intent = new Intent(this, com.example.mxnexus.ui.home.CommentsActivity.class);
            intent.putExtra("postId", postId);
            startActivity(intent);
        } else if ("connection_request".equalsIgnoreCase(type)) {
            startActivity(new Intent(this, com.example.mxnexus.ui.profile.PendingRequestsActivity.class));
        }
    }
}
