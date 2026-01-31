# Push Notifications (FCM)

The Grocent customer app uses **Firebase Cloud Messaging (FCM)** for push notifications. After a user logs in with phone number, their FCM token is stored in Firestore so you can send push messages from the Firebase Console or from your backend.

## Where the token is stored

- **Collection:** `customers`
- **Document ID:** Firebase Auth UID (same as the logged-in user)
- **Field:** `fcmToken` (string)

The token is saved when the user logs in and when it refreshes (`onNewToken`). It is removed when the user logs out.

---

## 1. Send a test notification from Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/) and select your project.
2. In the left menu, open **Engage** → **Messaging** (or **Build** → **Cloud Messaging**).
3. Click **Create your first campaign** or **New campaign** → **Firebase Notification messages**.
4. Enter **Notification title** and **Notification text**.
5. Under **Target**, choose **Send to single device**.
6. To get a device token for testing:
   - Run the app on a device or emulator and log in with phone number.
   - In Firestore, open `customers/{userId}` and copy the value of `fcmToken`.
   - Paste that token in the “FCM registration token” field in the campaign.
7. Click **Review** → **Publish**.

You can also target **Topic** or **User segment** if you set those up later.

---

## 2. Send push from your backend or admin panel

To send push messages from your own server or admin panel, use the **FCM HTTP v1 API** with the user’s `fcmToken` from Firestore.

- **Docs:** [FCM HTTP v1 API](https://firebase.google.com/docs/cloud-messaging/http-server-ref)
- **Flow:**
  1. Look up the user (e.g. by order or customer ID).
  2. Read `fcmToken` from Firestore `customers/{userId}`.
  3. Call the FCM HTTP v1 API with that token (and optional `data` / `notification` payload).

**Using Firebase Admin SDK (Node.js example):**

```js
const admin = require('firebase-admin');
// Initialize with your service account

const token = '...'; // from Firestore customers/{userId}.fcmToken
await admin.messaging().send({
  token,
  notification: { title: 'Order update', body: 'Your order is on the way.' },
  data: { orderId: '123' }, // optional
});
```

**Using Cloud Functions:**  
Trigger a function on order status change (or other event), read the customer’s `fcmToken` from `customers/{userId}`, and call `admin.messaging().send()` as above.

---

## 3. Android 13+ (Tiramisu) notification permission

On Android 13 and above, the app needs the **POST_NOTIFICATIONS** runtime permission to show notifications. The permission is declared in the manifest. For the best experience, request it when the user is logged in (e.g. after first login or when entering the home screen). If you do not request it, the system may still show some notifications depending on device and channel settings.

---

## Summary

| Goal | Action |
|------|--------|
| Test push quickly | Use Firebase Console → Messaging → New campaign → Send to single device (paste token from Firestore `customers/{userId}.fcmToken`). |
| Send from backend/admin | Use FCM HTTP v1 API or Firebase Admin SDK with `fcmToken` from Firestore. |
| Token location | Firestore `customers/{userId}`, field `fcmToken`. |
