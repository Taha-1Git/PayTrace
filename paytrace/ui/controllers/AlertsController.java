package com.paytrace.ui.controllers;

import com.paytrace.models.Alert;
import com.paytrace.models.enums.AlertStatus;
import com.paytrace.models.enums.Severity;
import com.paytrace.services.AlertService;
import com.paytrace.ui.utils.SceneNavigator;
import com.paytrace.ui.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;

public class AlertsController {


    @FXML private Label criticalCountLabel;
    @FXML private Label warningCountLabel;
    @FXML private Label infoCountLabel;
    @FXML private Label resolvedCountLabel;

    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> severityFilter;

    @FXML private TableView<Alert> alertsTable;
    @FXML private TableColumn<Alert, String> severityCol;
    @FXML private TableColumn<Alert, String> typeCol;
    @FXML private TableColumn<Alert, String> descCol;
    @FXML private TableColumn<Alert, String> ageCol;
    @FXML private TableColumn<Alert, String> statusAlertCol;

    @FXML private Label alertStatusLabel;

    /** UC-09 Subject. Encapsulates persistence + Observer broadcast. */
    private final AlertService alertService = AlertService.getInstance();

    private final ObservableList<Alert> tableData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();


        statusFilter.getItems().addAll("Active", "Reviewed", "Auto Resolved", "All");
        statusFilter.setValue("Active");

        severityFilter.getItems().addAll("All Severities", "Critical", "Warning", "Info");
        severityFilter.setValue("All Severities");

        configureColumns();
        loadAlerts();
    }

    private void configureColumns() {
        severityCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getSeverity() == null ? "—"
                        : d.getValue().getSeverity().name()));
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAlertType() == null ? "—"
                        : d.getValue().getAlertType().name().replace('_', ' ')));
        descCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDescription() == null ? "" : d.getValue().getDescription()));
        ageCol.setCellValueFactory(d -> new SimpleStringProperty(
                String.valueOf(d.getValue().getAgeInDays())));
        statusAlertCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getStatus() == null ? "—"
                        : d.getValue().getStatus().name().replace('_', ' ')));

        alertsTable.setItems(tableData);
    }

    @FXML
    private void loadAlerts() {
        try {
            List<Alert> list;
            String stChoice = statusFilter.getValue();
            if (stChoice == null) stChoice = "Active";

            switch (stChoice) {
                case "Active":
                    list = alertService.findByStatus(AlertStatus.ACTIVE); break;
                case "Reviewed":
                    list = alertService.findByStatus(AlertStatus.REVIEWED); break;
                case "Auto Resolved":
                    list = alertService.findByStatus(AlertStatus.AUTO_RESOLVED); break;
                case "All":
                default:
                    list = alertService.findAll();
            }

            // Severity filter
            String sev = severityFilter.getValue();
            if (sev != null && !"All Severities".equals(sev)) {
                List<Alert> filtered = new ArrayList<>();
                Severity target = Severity.valueOf(sev.toUpperCase());
                for (Alert a : list) if (a.getSeverity() == target) filtered.add(a);
                list = filtered;
            }

            tableData.setAll(list);
            updateKPIs();
            alertStatusLabel.setText(list.size() + " alert(s) shown.");
        } catch (Exception e) {
            alertStatusLabel.setText("Load failed: " + e.getMessage());
        }
    }

    @FXML
    private void applyFilter() { loadAlerts(); }

    private void updateKPIs() {
        try {
            List<Alert> active = alertService.findByStatus(AlertStatus.ACTIVE);
            int crit = 0, warn = 0, info = 0;
            for (Alert a : active) {
                if (a.getSeverity() == Severity.CRITICAL) crit++;
                else if (a.getSeverity() == Severity.WARNING) warn++;
                else if (a.getSeverity() == Severity.INFO) info++;
            }
            criticalCountLabel.setText(String.valueOf(crit));
            warningCountLabel.setText(String.valueOf(warn));
            infoCountLabel.setText(String.valueOf(info));

            int resolved = alertService.findByStatus(AlertStatus.REVIEWED).size()
                    + alertService.findByStatus(AlertStatus.AUTO_RESOLVED).size();
            resolvedCountLabel.setText(String.valueOf(resolved));
        } catch (Exception e) {
            System.err.println("KPI error: " + e.getMessage());
        }
    }

    @FXML
    private void markReviewed() {
        Alert sel = alertsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Select an alert to mark reviewed."); return; }

        try {
            String userId = SessionManager.getInstance().getCurrentUserId();
            // Through the service: status update + Observer broadcast.
            alertService.markReviewed(sel.getAlertId(), userId);
            alertStatusLabel.setText("Alert marked reviewed.");
            alertStatusLabel.setStyle("-fx-text-fill: #16a34a;");
            loadAlerts();
        } catch (Exception e) {
            alertStatusLabel.setText("Failed: " + e.getMessage());
            alertStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void dismissAlert() {
        Alert sel = alertsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showInfo("Select an alert to dismiss."); return; }

        // UC-09: only Info-level alerts can be dismissed
        if (sel.getSeverity() != Severity.INFO) {
            showInfo("Only Info-level alerts can be dismissed. "
                    + "Use 'Mark Reviewed' for warnings and critical alerts.");
            return;
        }

        try {
            String userId = SessionManager.getInstance().getCurrentUserId();
            alertService.dismiss(sel.getAlertId(), userId);
            alertStatusLabel.setText("Alert dismissed.");
            alertStatusLabel.setStyle("-fx-text-fill: #16a34a;");
            loadAlerts();
        } catch (Exception e) {
            alertStatusLabel.setText("Failed: " + e.getMessage());
            alertStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    // Audit logging is owned by AlertService now — reviewing/dismissing
    // an alert flows through markReviewed() / dismiss().

    private void showInfo(String msg) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    @FXML
    private void goBack() {
        try { SceneNavigator.navigateTo(SceneNavigator.DASHBOARD, 1280, 800); }
        catch (Exception e) { e.printStackTrace(); }
    }
}