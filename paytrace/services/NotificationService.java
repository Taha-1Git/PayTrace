package com.paytrace.services;

import com.paytrace.models.EmailNotification;
import com.paytrace.models.InAppNotification;
import com.paytrace.models.Notification;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-level helper to fire notifications.
 * Demonstrates polymorphism — both InAppNotification and EmailNotification
 * are treated as Notification, but each runs its own deliver() implementation.
 */
public class NotificationService {

    // FIX B — single-thread executor (one email at a time, no Gmail throttling)
    private static final ExecutorService MAIL_POOL =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "paytrace-mail");
                t.setDaemon(true);
                return t;
            });

    /** Send both an in-app notification AND an email. */
    public static void notify(String recipientUserId,
                              Notification.Type type,
                              String title,
                              String message,
                              String relatedEntityType,
                              String relatedEntityId,
                              boolean alsoEmail) {

        // In-app — runs synchronously on calling thread
        Notification inApp = new InAppNotification();
        inApp.setRecipientUserId(recipientUserId);
        inApp.setType(type);
        inApp.setTitle(title);
        inApp.setMessage(message);
        inApp.setRelatedEntityType(relatedEntityType);
        inApp.setRelatedEntityId(relatedEntityId);
        deliverSafely(inApp);

        if (alsoEmail) {
            Notification email = new EmailNotification();
            email.setRecipientUserId(recipientUserId);
            email.setType(type);
            email.setTitle(title);
            email.setMessage(message);
            email.setRelatedEntityType(relatedEntityType);
            email.setRelatedEntityId(relatedEntityId);
            // FIX B — submit to pool instead of spawning a raw thread
            MAIL_POOL.submit(() -> deliverSafely(email));
        }
    }

    /** Shorthand: in-app only, no email. */
    public static void notifyInApp(String recipientUserId,
                                   Notification.Type type,
                                   String title,
                                   String message,
                                   String relatedEntityType,
                                   String relatedEntityId) {
        notify(recipientUserId, type, title, message,
                relatedEntityType, relatedEntityId, false);
    }

    /** Polymorphic dispatch — calls deliver() on whatever subclass we got. */
    private static void deliverSafely(Notification n) {
        try {
            n.deliver();
        } catch (Exception e) {
            // FIX A — print full stack trace so SMTP errors are visible in console
            System.err.println("Notification delivery failed ("
                    + n.getChannel() + "): " + e.getMessage());
            e.printStackTrace();
        }
    }
}