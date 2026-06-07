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
    case "trip_invite": return "trips";
    default: return "general";
  }
}

/**
 * Schedules photo-prompt notifications across the trip's valid time windows.
 *
 * Uses dailyRate (from TripNotificationCalculator) to determine how many slots
 * to place per day, and totalCount (also from the calculator) as the hard cap.
 * Valid slots are 08:00–22:00 UTC+2, intersected with [startMs, endMs], so no
 * notification can ever fire outside the trip window.
 *
 * Members are assigned round-robin across all slots.
 *
 * @param {number}   startMs    trip start (Unix ms)
 * @param {number}   endMs      trip end   (Unix ms)
 * @param {number}   dailyRate  notifications per day (dailyPhotoNotificationRate from Kotlin)
 * @param {number}   totalCount hard cap on total notifications (totalPhotoNotifications from Kotlin)
 * @param {string[]} memberIds  all trip members (creator + invitees)
 * @returns {{ userId: string, scheduledAt: admin.firestore.Timestamp }[]}
 */
function buildSchedule(startMs, endMs, dailyRate, totalCount, memberIds) {
  const ONE_DAY_MS = 86_400_000;
  const TIMEZONE_OFFSET_HOURS = 2;
  const WINDOW_START_HOUR = 8;
  const WINDOW_END_HOUR = 22;

  // Midnight of the trip-start day in UTC+2.
  const tripStartDay = new Date(startMs);
  tripStartDay.setUTCHours(-TIMEZONE_OFFSET_HOURS, 0, 0, 0);
  const dayZeroStartMs = tripStartDay.getTime();

  // Build one valid [windowStart, windowEnd] per calendar day, clamped to trip bounds.
  // Days where the trip has already ended before 08:00 or hasn't started by 22:00 are skipped.
  const totalDaysSpanned = Math.ceil((endMs - dayZeroStartMs) / ONE_DAY_MS);
  const validWindows = [];
  for (let d = 0; d < totalDaysSpanned; d++) {
    const dayStartMs = dayZeroStartMs + d * ONE_DAY_MS;
    const windowStart = Math.max(dayStartMs + WINDOW_START_HOUR * 3_600_000, startMs);
    const windowEnd   = Math.min(dayStartMs + WINDOW_END_HOUR   * 3_600_000, endMs);
    if (windowStart < windowEnd) validWindows.push({ windowStart, windowEnd });
  }

  if (validWindows.length === 0) return [];

  const now = Date.now();
  const results = [];
  let memberIndex = 0;
  let remaining = totalCount;

  for (const { windowStart, windowEnd } of validWindows) {
    if (remaining <= 0) break;

    // Stochastic rounding: floor(rate) slots guaranteed, +1 with probability equal to
    // the fractional part. Over many days the expected total equals dailyRate * days = totalCount.
    const base = Math.floor(dailyRate);
    const countThisDay = Math.min(
      base + (Math.random() < (dailyRate - base) ? 1 : 0),
      remaining
    );

    for (let j = 0; j < countThisDay; j++) {
      const userId = memberIds[memberIndex % memberIds.length];
      memberIndex++;
      const triggerMs = Math.floor(windowStart + Math.random() * (windowEnd - windowStart));
      if (triggerMs <= now) continue; // trip started in the past; skip elapsed slots
      results.push({ userId, scheduledAt: admin.firestore.Timestamp.fromMillis(triggerMs) });
    }

    remaining -= countThisDay;
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
  const dailyRate  = tripData.dailyPhotoNotificationRate || 0;
  const memberIds  = tripData.memberIds || [];

  if (!startMs || !endMs || totalCount === 0 || dailyRate === 0 || memberIds.length === 0) return;

  const slots = buildSchedule(startMs, endMs, dailyRate, totalCount, memberIds);
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
 * 2. Triggered when a new trip document is created.
 *    Sends a "you've been added to a trip" notification to every invited user.
 */
exports.notifyTripInvitees = onDocumentCreated({ document: "trips/{tripId}", region: "europe-west1" }, async (event) => {
  const tripData = event.data.data();
  if (!tripData) return;

  const tripId = event.params.tripId;
  const tripName = tripData.name || "";
  const creatorId = tripData.creatorId || "";
  const creatorName = tripData.creatorName || "";
  const invitedUserIds = tripData.invitedUserIds || [];

  if (invitedUserIds.length === 0) return;

  const BATCH_SIZE = 500;
  for (let i = 0; i < invitedUserIds.length; i += BATCH_SIZE) {
    const batch = db.batch();
    invitedUserIds.slice(i, i + BATCH_SIZE).forEach((userId) => {
      const notifRef = db.collection("users").doc(userId).collection("notifications").doc();
      batch.set(notifRef, {
        type: "trip_invite",
        title: "You've been added to a trip!",
        message: `${creatorName} added you to ${tripName}`,
        tripId,
        tripName,
        fromUserId: creatorId,
        fromUserName: creatorName,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });
    await batch.commit();
  }
});

/**
 * 3. Runs every 15 minutes.
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
 * 4. Triggered when any notification doc is created in users/{userId}/notifications.
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
