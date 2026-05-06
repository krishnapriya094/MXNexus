package com.example.mxnexus.ui.notifications;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mxnexus.R;
import com.example.mxnexus.data.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;


public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Back button (Toolbar replaced by ImageView in updated layout)
        findViewById(R.id.btnNotifBack).setOnClickListener(v -> onBackPressed());

        // 2. Initialize RecyclerView
        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        notificationList = new ArrayList<>();

        // 3. Set Adapter
        adapter = new NotificationAdapter(notificationList, notification -> {
            // Handle notification click if needed
        });
        rvNotifications.setAdapter(adapter);

        // 4. Load real-time notifications
        loadNotifications();
    }


    private void loadNotifications() {
        String userId = auth.getCurrentUser().getUid();

        // Query to fetch notifications for the current user, ordered by timestamp descending
        db.collection("alerts")
                .whereEqualTo("receiverId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        // Handle error
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        notificationList.clear();
                        for (var doc : snapshots.getDocuments()) {
                            Notification notification = doc.toObject(Notification.class);
                            if (notification != null) {
                                notification.setNotificationId(doc.getId());
                                notificationList.add(notification);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
