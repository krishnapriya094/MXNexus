const functions = require("firebase-functions");
const admin     = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Triggered whenever a new document is written to the "alerts" collection.
 * Reads the receiver's FCM token from Firestore and sends a push notification.
 *
 * Notification types handled:
 *   message            → 💬 New Message
 *   comment            → 💬 New Comment
 *   mention            → 🔔 You were mentioned
 *   query_reply        → 💡 Query Answered
 *   connection_request → 🤝 Connection Request
 *   connection_accepted→ ✅ Connection Accepted
 */
exports.sendPushNotification = functions.firestore
  .document("alerts/{alertId}")
  .onCreate(async (snap, context) => {
    const alert = snap.data();
    if (!alert) return null;

    const receiverId = alert.receiverId;
    if (!receiverId) return null;

    // Get receiver's FCM token
    const receiverDoc = await db.collection("users").document(receiverId).get();
    if (!receiverDoc.exists) return null;

    const fcmToken = receiverDoc.data().fcmToken;
    if (!fcmToken) {
      console.log(`No FCM token for user ${receiverId}`);
      return null;
    }

    // Build notification title based on type
    const titles = {
      message:             "💬 New Message",
      comment:             "💬 New Comment",
      mention:             "🔔 You were mentioned",
      query_reply:         "💡 Query Answered",
      connection_request:  "🤝 Connection Request",
      connection_accepted: "✅ Connection Accepted",
    };

    const title = titles[alert.type] || "MXNexus";
    const body  = alert.message || "You have a new notification";

    const message = {
      token: fcmToken,
      notification: { title, body },
      android: {
        priority: "high",
        notification: {
          channelId: "mxnexus_notifications",
          sound:     "default",
          clickAction: "OPEN_ALERTS",
        },
      },
      data: {
        type:     alert.type    || "",
        postId:   alert.postId  || "",
        queryId:  alert.queryId || "",
        senderId: alert.senderId || "",
      },
    };

    try {
      const response = await admin.messaging().send(message);
      console.log(`Push sent to ${receiverId}: ${response}`);
      return response;
    } catch (error) {
      console.error(`Push failed for ${receiverId}:`, error);
      return null;
    }
  });
