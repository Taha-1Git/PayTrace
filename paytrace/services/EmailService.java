package com.paytrace.services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;
import java.util.Properties;

/**
 * Sends real emails via Gmail SMTP.
 * Reads credentials from backend/src/main/resources/db.properties.
 * The actual 'From:' address is always the configured SMTP user
 * (Gmail forbids spoofing). The email *body* can mention the administrator.
 */
public class EmailService {

    private static final Properties SMTP_PROPS = loadSmtpProps();

    public static void send(String toEmail,
                            String subject,
                            String body) throws Exception {

        String host     = SMTP_PROPS.getProperty("mail.smtp.host");
        String port     = SMTP_PROPS.getProperty("mail.smtp.port");
        String user     = SMTP_PROPS.getProperty("mail.smtp.username");
        String password = SMTP_PROPS.getProperty("mail.smtp.password");
        String fromName = SMTP_PROPS.getProperty("mail.smtp.from.name", "PayTrace");

        if (host == null || user == null || password == null)
            throw new RuntimeException(
                    "Mail config missing in db.properties (mail.smtp.host/username/password).");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(user, fromName));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject(subject);
        msg.setText(body);
        Transport.send(msg);
    }

    private static Properties loadSmtpProps() {
        Properties p = new Properties();
        try (InputStream in = EmailService.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in != null) p.load(in);
        } catch (Exception e) {
            System.err.println("Could not load db.properties for SMTP: " + e.getMessage());
        }
        return p;
    }
}