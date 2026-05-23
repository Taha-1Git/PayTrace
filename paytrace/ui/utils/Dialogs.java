package com.paytrace.ui.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import java.util.Optional;

/**
 * Centralized dialog builders that apply the PayTrace stylesheet
 * so they look consistent with the rest of the app.
 */
public class Dialogs {

    public static Optional<ButtonType> confirm(String title, String header, String body) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(body);
        styleDialog(a);
        return a.showAndWait();
    }

    public static void info(String title, String body) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(body);
        styleDialog(a);
        a.showAndWait();
    }

    public static void warn(String title, String body) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(body);
        styleDialog(a);
        a.showAndWait();
    }

    public static void error(String title, String body) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(body);
        styleDialog(a);
        a.showAndWait();
    }

    private static void styleDialog(Alert a) {
        try {
            DialogPane pane = a.getDialogPane();
            pane.getStylesheets().add(
                    Dialogs.class.getResource("/css/paytrace.css").toExternalForm());
            pane.getStyleClass().add("paytrace-dialog");
        } catch (Exception ignored) {}
    }
}