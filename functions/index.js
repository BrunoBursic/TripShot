const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

// ─── Helpers ─────────────────────────────────────────────────────────────────

function channelForType(type) {
  switch (type) {
    case "new_follower": return "followers";
    case "photo_prompt": return "photo_prompts";
    default: return "general";
  }
}

/**
 * Computes N random notification timestamps spread across the trip's days,
 * each falling in the 08:00–22:00 UTC window on the assigned day.
 *
 * Notifications are distributed round-robin across members so each member
 * receives approximately totalCount / memberIds.length prompts.
 *
 * @param {number} startMs  trip start (Unix ms)
 * @param {number} endMs    trip end   (Unix ms)
 * @param {number} totalCount  total notifications for the trip
 * @param {string[]} memberIds  all trip members (creator + invitees)
 * @returns {{ userId: string, scheduledAt: admin.firestore.Timestamp }[]}
 */
function buildSchedule(startMs, endMs, totalCount, memberIds) {
  const ONE_DAY_MS = 86_400_000;
  const actualDays = (endMs - startMs) / ONE_DAY_MS;
  const durationInDays = Math.max(1, Math.ceil(actualDays));
  const now = Date.now();
  const results = [];

  for (let i = 0; i < totalCount; i++) {
    const dayIndex = i % durationInDays;
    const userId = memberIds[i % memberIds.length];

    // Start of the target calendar day in UTC+2 (offset the UTC midnight back by 2 hours).
    const TIMEZONE_OFFSET_HOURS = 2;
    const tripStartDay = new Date(startMs);
    tripStartDay.setUTCHours(-TIMEZONE_OFFSET_HOURS, 0, 0, 0);
    const dayStartMs = tripStartDay.getTime() + dayIndex * ONE_DAY_MS;

    // Random time between 08:00 and 21:59 local (UTC+2).
    const randomHour = Math.floor(Math.random() * 14) + 8; // 8..21
    const randomMinute = Math.floor(Math.random() * 60);
    const triggerMs = dayStartMs + (randomHour * 3600 + randomMinute * 60) * 1000;

    // Skip slots that have already passed.
    if (triggerMs <= now) continue;

    results.push({
      userId,
      scheduledAt: admin.firestore.Timestamp.fromMillis(triggerMs),
    });
  }

  return results;
}

// ─── Functions ────────────────────────────────────────────────────────────────

/**
 * 1. Triggered when a new trip document is created.
 *    Computes the photo-prompt schedule for all members and writes
 *    individual schedule docs to `photo_prompt_schedules/{id}`.
 */
exports.schedulePhotoPrompts = onDocumentCreated({ document: "trips/{tripId}", region: "europe-west1" }, async (event) => {
  const tripData = event.data.data();
  if (!tripData) return;

  const tripId = event.params.tripId;
  const tripName = tripData.name || "";
  const startMs = tripData.startDateTimeMillis;
  const endMs = tripData.endDateTimeMillis;
  const totalCount = tripData.totalPhotoNotifications || 0;
  const memberIds = tripData.memberIds || [];

  if (!startMs || !endMs || totalCount === 0 || memberIds.length === 0) return;

  const slots = buildSchedule(startMs, endMs, totalCount, memberIds);
  if (slots.length === 0) return;

  // Write all schedule docs in batches of 500 (Firestore batch limit).
  const BATCH_SIZE = 500;
  for (let i = 0; i < slots.length; i += BATCH_SIZE) {
    const batch = db.batch();
    slots.slice(i, i + BATCH_SIZE).forEach((slot) => {
      const ref = db.collection("photo_prompt_schedules").doc();
      batch.set(ref, {
        tripId,
        tripName,
        userId: slot.userId,
        scheduledAt: slot.scheduledAt,
      });
    });
    await batch.commit();
  }
});

/**
 * 2. Runs every 15 minutes.
 *    Fetches all due schedule docs (scheduledAt <= now), creates a
 *    notification doc for each member (which triggers sendPushNotification),
 *    and deletes the schedule doc — atomically in a batch.
 */
exports.dispatchDuePhotoPrompts = onSchedule({ schedule: "every 15 minutes", region: "europe-west1" }, async () => {
  const now = admin.firestore.Timestamp.now();

  const dueSnap = await db
    .collection("photo_prompt_schedules")
    .where("scheduledAt", "<=", now)
    .limit(100)
    .get();

  if (dueSnap.empty) return;

  // Each pair (notification write + schedule delete) = 2 ops; 100 pairs = 200 ops < 500 limit.
  const batch = db.batch();

  dueSnap.docs.forEach((scheduleDoc) => {
    const { tripId, tripName, userId } = scheduleDoc.data();

    const notifRef = db.collection("users").doc(userId).collection("notifications").doc();
    batch.set(notifRef, {
      type: "photo_prompt",
      title: "Time for a TripShot!",
      message: `Capture a moment from ${tripName}`,
      tripId: tripId || "",
      tripName: tripName || "",
      fromUserId: "",
      fromUserName: "",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Delete the schedule doc so it is never dispatched again.
    batch.delete(scheduleDoc.ref);
  });

  await batch.commit();
});

/**
 * 3. Triggered when any notification doc is created in users/{userId}/notifications.
 *    Looks up the user's FCM token and sends a push notification.
 *    Passes tripId in the data payload so the app can deep-link to the prompt screen.
 */
exports.sendPushNotification = onDocumentCreated(
  { document: "users/{userId}/notifications/{notifId}", region: "europe-west1" },
  async (event) => {
    const notifData = event.data.data();
    if (!notifData) return;

    const userId = event.params.userId;

    const userDoc = await db.collection("users").doc(userId).get();
    const fcmToken = userDoc.get("fcmToken");
    if (!fcmToken) return;

    const data = {
      type: notifData.type || "general",
      userId,
    };
    if (notifData.tripId) data.tripId = notifData.tripId;

    const message = {
      token: fcmToken,
      notification: {
        title: notifData.title || "New notification",
        body: notifData.message || "",
      },
      data,
      android: {
        priority: "high",
        notification: {
          channelId: channelForType(notifData.type),
        },
      },
    };

    try {
      await messaging.send(message);
    } catch (error) {
      if (error.code === "messaging/registration-token-not-registered") {
        await db.collection("users").doc(userId).update({
          fcmToken: admin.firestore.FieldValue.delete(),
        });
      }
    }
  }
);
