package com.paytrace.models;

import java.time.LocalDateTime;

/**
 * Abstract base for all notification types.
 * Subclasses: InAppNotification (database-backed), EmailNotification (real SMTP send).
 *
 * Demonstrates: inheritance, polymorphism (subclasses override deliver()),
 * abstraction (cannot instantiate Notification directly).
 */
public abstract class Notification {

    public enum Type {
        INVOICE_CREATED,
        PAYMENT_RECEIVED,
        PAYMENT_COMPLETED,
        CONNECTION_APPROVED,
        RECONCILIATION_RESULT,
        OVERDUE_WARNING,
        ACCOUNT_BLOCKED,
        REFUND_ISSUED,
        GENERIC
    }

    private String        notificationId;
    private String        recipientUserId;
    private Type          type;
    private String        title;
    private String        message;
    private String        relatedEntityType;
    private String        relatedEntityId;
    private boolean       read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    /** Polymorphic — InAppNotification persists, EmailNotification fires SMTP. */
    public abstract void deliver() throws Exception;

    /** Polymorphic — the channel name for logging/audit. */
    public abstract String getChannel();

    public String  getNotificationId()                 { return notificationId; }
    public void    setNotificationId(String v)         { this.notificationId = v; }
    public String  getRecipientUserId()                { return recipientUserId; }
    public void    setRecipientUserId(String v)        { this.recipientUserId = v; }
    public Type    getType()                           { return type; }
    public void    setType(Type v)                     { this.type = v; }
    public String  getTitle()                          { return title; }
    public void    setTitle(String v)                  { this.title = v; }
    public String  getMessage()                        { return message; }
    public void    setMessage(String v)                { this.message = v; }
    public String  getRelatedEntityType()              { return relatedEntityType; }
    public void    setRelatedEntityType(String v)      { this.relatedEntityType = v; }
    public String  getRelatedEntityId()                { return relatedEntityId; }
    public void    setRelatedEntityId(String v)        { this.relatedEntityId = v; }
    public boolean isRead()                            { return read; }
    public void    setRead(boolean v)                  { this.read = v; }
    public LocalDateTime getCreatedAt()                { return createdAt; }
    public void    setCreatedAt(LocalDateTime v)       { this.createdAt = v; }
    public LocalDateTime getReadAt()                   { return readAt; }
    public void    setReadAt(LocalDateTime v)          { this.readAt = v; }
}