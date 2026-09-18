package com.agriloop.repository;

import com.agriloop.model.Notification;

import java.util.List;

/**
 * Data access contract for persistent user-specific Notifications.
 */
public interface NotificationRepository extends BaseRepository<Notification, Long> {
    List<Notification> findByRecipientUserId(Long recipientUserId);
    List<Notification> findUnreadByRecipientUserId(Long recipientUserId);
    int countUnreadByRecipientUserId(Long recipientUserId);
    boolean markAsRead(Long notificationId);
    boolean markAllAsReadForUser(Long recipientUserId);
    boolean deleteAllForUser(Long recipientUserId);
}
