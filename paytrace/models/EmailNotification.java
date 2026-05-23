package com.paytrace.models;

import com.paytrace.dao.UserDAO;
import com.paytrace.services.EmailService;

import java.util.Optional;

/**
 * Notification sent via real SMTP email to the recipient's registered address.
 * Looks up the recipient's email from the users table at delivery time.
 */
public class EmailNotification extends Notification {

    private static final UserDAO userDAO = new UserDAO();

    @Override
    public void deliver() throws Exception {
        Optional<User> u = userDAO.findById(getRecipientUserId());
        if (u.isEmpty() || u.get().getEmail() == null) {
            throw new Exception("Recipient user/email not found");
        }
        EmailService.send(
                u.get().getEmail(),
                "PayTrace — " + getTitle(),
                getMessage() + "\n\n— PayTrace");
    }

    @Override
    public String getChannel() {
        return "EMAIL";
    }
}