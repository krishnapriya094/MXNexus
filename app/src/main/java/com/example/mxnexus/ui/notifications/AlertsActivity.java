package com.example.mxnexus.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mxnexus.R;
import com.example.mxnexus.data.model.Notification;
import com.example.mxnexus.ui.home.CommentsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity to display dynamic user alerts from Firebase in MX Nexus.
 */
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

        // 1. Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 2. Setup Toolbar with Back Button
        Toolbar toolbar = findViewById(R.id.toolbarNotifications);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // 3. Initialize RecyclerView
        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        
        notificationList = new ArrayList<>();
        // Pass 'this' as the click listener to handle item clicks
        adapter = new NotificationAdapter(notificationList, this);
        rvNotifications.setAdapter(adapter);

        // 4. Load Alerts from Firestore
        loadAlerts();
    }

    private void loadAlerts() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Toast.makeText(this, "Please log in to see alerts", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("AlertsActivity", "Loading alerts for user: " + currentUserId);

        // Query the "alerts" collection for the current user's ID
        db.collection("alerts")
            .whereEqualTo("receiverId", currentUserId)
            .addSnapshotListener((snapshot, error) -> {
                if (error != null) {
                    android.util.Log.e("AlertsActivity", "Error loading alerts", error);
                    Toast.makeText(this, "Error loading alerts", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (snapshot != null) {
                    List<Notification> alerts = snapshot.toObjects(Notification.class);
                    android.util.Log.d("AlertsActivity", "Found " + alerts.size() + " alerts");
                    for (Notification alert : alerts) {
                        android.util.Log.d("AlertsActivity", "Alert: " + alert.getMessage() + ", type: " + alert.getType() + ", postId: " + alert.getPostId());
                    }
                    // Manual sorting to avoid index requirements
                    java.util.Collections.sort(alerts, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    adapter.updateList(alerts);
                } else {
                    android.util.Log.d("AlertsActivity", "Snapshot is null");
                }
            });
    }

    @Override
    public void onNotificationClick(Notification notification) {
        String postId = notification.getPostId();
        String type = notification.getType();

        android.util.Log.d("AlertsActivity", "Notification clicked: type=" + type + ", postId=" + postId);

        // If the alert is related to a post (Like, Comment, Mention), navigate to that post
        if (postId != null && !postId.isEmpty()) {
            if ("like".equalsIgnoreCase(type) || "comment".equalsIgnoreCase(type) || "mention".equalsIgnoreCase(type)) {
                Intent intent = new Intent(this, com.example.mxnexus.ui.home.CommentsActivity.class);
                intent.putExtra("postId", postId);
                startActivity(intent);
            }
        } else if ("follow".equalsIgnoreCase(type)) {
            // Optional: Redirect to the user's profile who followed you
            Toast.makeText(this, "Follower details opening...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Details for this alert are unavailable", Toast.LENGTH_SHORT).show();
        }
    }
}
