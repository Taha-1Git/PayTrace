package com.paytrace.ui.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

public class EmailSimulator {

    /** Shows a popup that "simulates" sending an email. */
    public static void send(String to, String subject, String body) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("📧 Simulated Email Sent");
        a.setHeaderText("To: " + to + "\nSubject: " + subject);
        TextArea ta = new TextArea(body);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(8);
        a.getDialogPane().setContent(ta);
        a.showAndWait();
    }
}