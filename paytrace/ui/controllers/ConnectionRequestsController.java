package com.paytrace.ui.controllers;

import com.paytrace.dao.UserDAO;
import com.paytrace.models.ConnectionRequest;
import com.paytrace.models.User;
import com.paytrace.services.ConnectionService;

import com.paytrace.ui.utils.SceneNavigator;
import com.paytrace.ui.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ConnectionRequestsController {



    @FXML private TableView<ConnectionRequest>             requestsTable;
    @FXML private TableColumn<ConnectionRequest, String>   userCol;
    @FXML private TableColumn<ConnectionRequest, String>   addressCol;
    @FXML private TableColumn<ConnectionRequest, String>   jobCol;
    @FXML private TableColumn<ConnectionRequest, String>   phoneCol;
    @FXML private TableColumn<ConnectionRequest, String>   dateCol;
    @FXML private Label                                    statusLabel;
    @FXML private TableColumn<ConnectionRequest, String>   acctCol;

    private final ConnectionService connSvc = new ConnectionService();
    private final UserDAO           userDAO = new UserDAO();

    private final Map<String, User> userCache = new HashMap<>();
    private final ObservableList<ConnectionRequest> rows = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();

        userCol.setCellValueFactory(d -> new SimpleStringProperty(
                resolveUserLabel(d.getValue().getUserId())));
        addressCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUserAddress() == null ? "" : d.getValue().getUserAddress()));
        jobCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUserJob() == null ? "" : d.getValue().getUserJob()));
        phoneCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUserPhone() == null ? "" : d.getValue().getUserPhone()));
        acctCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUserAccountNumber() == null
                        ? "" : d.getValue().getUserAccountNumber()));
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getRequestedAt() == null
                        ? "" : d.getValue().getRequestedAt().toString()));

        requestsTable.setItems(rows);
        refresh();
    }

    private String resolveUserLabel(String userId) {
        if (userCache.containsKey(userId)) {
            User u = userCache.get(userId);
            return u.getFullName() + " (" + u.getEmail() + ")";
        }
        try {
            Optional<User> u = userDAO.findById(userId);
            if (u.isPresent()) {
                userCache.put(userId, u.get());
                return u.get().getFullName() + " (" + u.get().getEmail() + ")";
            }
        } catch (Exception ignored) {}
        return userId;
    }

    @FXML
    private void refresh() {
        try {
            List<ConnectionRequest> pending = connSvc.findPendingForAdministrator(
                    SessionManager.getInstance().getCurrentUserId());
            rows.setAll(pending);
            statusLabel.setText(pending.size() + " pending request(s).");
            statusLabel.setStyle(null);
        } catch (Exception e) {
            statusLabel.setText("Load failed: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void approveSelected() {
        ConnectionRequest sel = requestsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Select a request first."); return; }

        try {
            // Approve in DB (creates user_connections row, updates request status)
            String code = connSvc.approveRequest(sel.getRequestId(),
                    SessionManager.getInstance().getCurrentUserId());

            // Look up the requesting user
            var userOpt = userDAO.findById(sel.getUserId());
            if (userOpt.isEmpty()) {
                statusLabel.setText("Approved, but user not found — code not emailed.");
                statusLabel.setStyle("-fx-text-fill: #d97706;");
                refresh();
                return;
            }
            String toEmail    = userOpt.get().getEmail();
            String userName   = userOpt.get().getFullName();
            String adminName  = SessionManager.getInstance().getContextName();
            String adminEmail = getAdministratorEmail();

            String subject = "PayTrace — Your access to " + adminName + " has been approved";
            String body =
                    "Hello " + userName + ",\n\n" +
                            "Good news — your request to access " + adminName + " has been approved" +
                            (adminEmail != null ? " by " + adminEmail : "") + ".\n\n" +
                            "Your 4-digit access code is: " + code + "\n\n" +
                            "To log in:\n" +
                            "  1. Open PayTrace and select 'User'\n" +
                            "  2. Enter your email and password\n" +
                            "  3. In the 'Option 1 — I have a code' field, type: " + code + "\n" +
                            "  4. Click Login\n\n" +
                            "Keep this code private. If you have questions, contact " +
                            (adminEmail != null ? adminEmail : adminName) + ".\n\n" +
                            "— PayTrace";

            // Send real email in a background thread so UI doesn't freeze
            new Thread(() -> {
                try {
                    com.paytrace.services.EmailService.send(toEmail, subject, body);
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("✓ Email sent to " + toEmail);
                        statusLabel.setStyle("-fx-text-fill: #16a34a;");
                        com.paytrace.ui.utils.ToastNotification.show(
                                requestsTable.getScene().getWindow(),
                                "✓ Connection approved — code emailed to " + toEmail,
                                com.paytrace.ui.utils.ToastNotification.ToastType.SUCCESS);
                    });
                } catch (Exception mailEx) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText(
                                "Approved, but email failed: " + mailEx.getMessage());
                        statusLabel.setStyle("-fx-text-fill: #dc2626;");
                    });
                    mailEx.printStackTrace();
                }
            }, "email-sender").start();

            // Refresh list immediately (email still sending in background)
            refresh();
            statusLabel.setText("Approved. Sending email to " + toEmail + "...");
            statusLabel.setStyle("-fx-text-fill: #2563eb;");
        } catch (Exception e) {
            statusLabel.setText("Failed: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    /** Looks up the current administrator's own email for the email body. */
    private String getAdministratorEmail() {
        try {
            return userDAO.findById(
                            SessionManager.getInstance().getCurrentUserId())
                    .map(u -> u.getEmail()).orElse(null);
        } catch (Exception e) { return null; }
    }

    @FXML
    private void rejectSelected() {
        ConnectionRequest sel = requestsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Select a request first."); return; }

        try {
            connSvc.rejectRequest(sel.getRequestId(),
                    SessionManager.getInstance().getCurrentUserId());
            statusLabel.setText("Request rejected.");
            statusLabel.setStyle("-fx-text-fill: #d97706;");
            refresh();
        } catch (Exception e) {
            statusLabel.setText("Failed: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private void showInfo(String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    @FXML
    private void goBack() {
        try { SceneNavigator.navigateTo(SceneNavigator.DASHBOARD, 1280, 800); }
        catch (Exception e) { e.printStackTrace(); }
    }
}