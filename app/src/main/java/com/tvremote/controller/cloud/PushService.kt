package com.tvremote.controller.cloud

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives alerts from the TV agent (usage report ready, screen time finished,
 * blocked app attempt) and "TV online/offline" presence changes.
 */
class PushService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: return
        when (type) {
            "screen_time_finished" -> Unit // notify parent
            "blocked_attempt" -> Unit      // child tried a blocked app
            "device_online" -> Unit
            "device_offline" -> Unit
            "usage_report" -> Unit
        }
    }

    override fun onNewToken(token: String) {
        // save to Firestore: parents/{uid}/fcmToken
    }
}
