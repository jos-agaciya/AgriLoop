package com.agriloop.model;

import java.time.LocalDateTime;

/**
 * Model representing a persistent user-specific notification.
 */
public class Notification extends BaseEntity {
    private Long recipientUserId;
    private String notificationType;
    private String title;
    private String message;
    private Long relatedOrderId;
    private boolean read;

    public Notification() {
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    public Notification(Long recipientUserId, String notificationType, String title, String message, Long relatedOrderId) {
        this();
        this.recipientUserId = recipientUserId;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.relatedOrderId = relatedOrderId;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Long recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getRelatedOrderId() {
        return relatedOrderId;
    }

    public void setRelatedOrderId(Long relatedOrderId) {
        this.relatedOrderId = relatedOrderId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
