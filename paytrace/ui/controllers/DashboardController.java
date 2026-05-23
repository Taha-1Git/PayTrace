package com.paytrace.ui.controllers;

import com.paytrace.services.AuthService;
import com.paytrace.ui.utils.SceneNavigator;
import com.paytrace.ui.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.paytrace.ui.utils.KpiAnimator;
/**
 * Shell controller — manages the top bar, sidebar, and the swappable content pane.
 * Inner screen logic lives in their own controllers (DashboardHomeController, etc.).
 */
public class DashboardController {

    @FXML private Label welcomeLabel;

    @FXML private Button logoutButton;

    @FXML private VBox adminMenu;
    @FXML private VBox accountantMenu;
    @FXML private VBox userMenu;

    @FXML private StackPane contentPane;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();
        welcomeLabel.setText(sm.getCurrentUserId() == null ? "" : sm.getCurrentUserId());

        boolean isAdminLike = sm.isAdmin() || sm.isAdministrator();
        boolean isUser = sm.isUser();

        adminMenu.setVisible(isAdminLike);
        adminMenu.setManaged(isAdminLike);
        accountantMenu.setVisible(isAdminLike);
        accountantMenu.setManaged(isAdminLike);
        userMenu.setVisible(isUser);
        userMenu.setManaged(isUser);

        // Register the content pane so SceneNavigator.loadIntoShell() can swap content
        SceneNavigator.setShellContentPane(contentPane);

        // Load the home screen by default
        loadHome();
    }

    private void loadHome() {
        try {
            SceneNavigator.loadIntoShell(SceneNavigator.DASHBOARD_HOME);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Navigation ─ all use loadIntoShell, NOT navigateTo ──

    // ── Navigation Methods for the Sidebar ──

    @FXML
    public void goToDashboard() {
        swap(SceneNavigator.DASHBOARD_HOME);
    }

    @FXML
    public void goToReports() {
        swap(SceneNavigator.REPORTS);
    }

    @FXML
    public void goToReconciliation() {
        swap(SceneNavigator.RECON);
    }

    @FXML
    public void goToManualReconciliation() {
        swap(SceneNavigator.MANUAL_RECON);
    }

    @FXML
    public void goToAlerts() {
        swap(SceneNavigator.ALERTS);
    }

    @FXML
    public void goToConnectionRequests() {
        swap(SceneNavigator.CONN_REQUESTS);
    }

    @FXML
    public void goToMyInvoices() {
        swap(SceneNavigator.MY_INVOICES);
    }

    // This helper method uses your SceneNavigator to swap the middle part of the screen
    private void swap(String fxmlPath) {
        System.out.println("Button clicked! Loading: " + fxmlPath); // If this doesn't show up, the link is broken.
        try {

            SceneNavigator.loadIntoShell(fxmlPath);
        } catch (Exception e) {
            System.err.println("Error loading FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            authService.logout(SessionManager.getInstance().getCurrentSessionId());
            SessionManager.getInstance().clearSession();
            SceneNavigator.navigateTo(SceneNavigator.LOGIN, 1000, 720);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}