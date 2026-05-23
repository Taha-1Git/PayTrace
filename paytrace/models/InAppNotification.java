package com.paytrace.models;

import com.paytrace.dao.NotificationDAO;

/**
 * Notification stored in the database — appears in the user's notification panel.
 */
public class InAppNotification extends Notification {

    private static final NotificationDAO dao = new NotificationDAO();

    @Override
    public void deliver() throws Exception {
        dao.save(this);
    }

    @Override
    public String getChannel() {
        return "IN_APP";
    }
}