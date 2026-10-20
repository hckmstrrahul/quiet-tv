package dev.rahul.quiettv

import android.service.notification.NotificationListenerService

/** Android requires enabled notification-listener access to observe other apps'
 * MediaSessions. We do not read, store, forward or act on notification contents. */
class MediaAccessService : NotificationListenerService()
