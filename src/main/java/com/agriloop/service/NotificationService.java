package com.agriloop.service;

import com.agriloop.model.Notification;
import com.agriloop.model.User;
import com.agriloop.repository.NotificationRepository;
import com.agriloop.repository.impl.JdbcNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service managing user-specific, database-backed alert and workflow notifications.
 */
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static NotificationService instance;

    private final NotificationRepository notificationRepo;
    private final List<Consumer<List<Notification>>> listeners = new ArrayList<>();

    private NotificationService() {
        this.notificationRepo = new JdbcNotificationRepository();
    }

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    /**
     * Creates and persists a user-specific notification in MySQL.
     */
    public synchronized Notification pushNotification(Long recipientUserId, String notificationType, String title, String message, Long relatedOrderId) {
        if (recipientUserId == null) {
            logger.warn("Cannot create notification without recipientUserId");
            return null;
        }

        Notification notification = new Notification(recipientUserId, notificationType, title, message, relatedOrderId);
        try {
            Notification saved = notificationRepo.save(notification);
            logger.info("Notification created for user {}: {} - {}", recipientUserId, title, message);
            notifyListeners();
            return saved;
        } catch (Exception e) {
            logger.error("Failed to persist notification for user " + recipientUserId, e);
            return notification;
        }
    }

    /**
     * Convenience method for current authenticated user notification push.
     */
    public synchronized void pushNotification(String title, String message, String type) {
        User currentUser = ProfileService.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() != null) {
            pushNotification(currentUser.getId(), type, title, message, null);
        } else {
            logger.info("Local notification: {} - {}", title, message);
            notifyListeners();
        }
    }

    public synchronized List<Notification> getNotificationsForCurrentUser() {
        User currentUser = ProfileService.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            return Collections.emptyList();
        }
        try {
            return notificationRepo.findByRecipientUserId(currentUser.getId());
        } catch (Exception e) {
            logger.error("Failed to load notifications for current user", e);
            return Collections.emptyList();
        }
    }

    public synchronized List<Notification> getNotifications() {
        return getNotificationsForCurrentUser();
    }

    public synchronized int getUnreadCount() {
        User currentUser = ProfileService.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            return 0;
        }
        try {
            return notificationRepo.countUnreadByRecipientUserId(currentUser.getId());
        } catch (Exception e) {
            logger.warn("Failed to count unread notifications: {}", e.getMessage());
            return 0;
        }
    }

    public synchronized void markAllAsRead() {
        User currentUser = ProfileService.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() != null) {
            try {
                notificationRepo.markAllAsReadForUser(currentUser.getId());
                notifyListeners();
            } catch (Exception e) {
                logger.error("Failed to mark notifications as read", e);
            }
        }
    }

    public synchronized void markAsRead(Long notificationId) {
        if (notificationId != null) {
            try {
                notificationRepo.markAsRead(notificationId);
                notifyListeners();
            } catch (Exception e) {
                logger.error("Failed to mark notification as read", e);
            }
        }
    }

    public synchronized void clearAll() {
        User currentUser = ProfileService.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getId() != null) {
            try {
                notificationRepo.deleteAllForUser(currentUser.getId());
                notifyListeners();
            } catch (Exception e) {
                logger.error("Failed to clear notifications", e);
            }
        }
    }

    public synchronized void addListener(Consumer<List<Notification>> listener) {
        if (listener != null) {
            listeners.add(listener);
            listener.accept(getNotificationsForCurrentUser());
        }
    }

    private void notifyListeners() {
        List<Notification> list = getNotificationsForCurrentUser();
        for (Consumer<List<Notification>> l : listeners) {
            try {
                l.accept(list);
            } catch (Exception ignored) {}
        }
    }
}
