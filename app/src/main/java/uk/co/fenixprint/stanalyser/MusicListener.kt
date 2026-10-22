package uk.co.fenixprint.stanalyser

import android.service.notification.NotificationListenerService

class MusicListener : NotificationListenerService() {
    override fun onListenerConnected() { super.onListenerConnected(); MusicBridge.refresh(this) }
    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification?) {
        MusicBridge.refresh(this)
    }
    override fun onNotificationRemoved(sbn: android.service.notification.StatusBarNotification?) {
        MusicBridge.refresh(this)
    }
}
