package com.paytrace.ui.controllers;

import com.paytrace.dao.UserConnectionDAO;
import com.paytrace.dao.VendorDAO;
import com.paytrace.models.UserConnection;
import com.paytrace.models.Vendor;
import com.paytrace.models.enums.TargetType;
import com.paytrace.services.AuthService;
import com.paytrace.services.ConnectionService;
import com.paytrace.ui.utils.SceneNavigator;
import com.paytrace.ui.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lets a logged-in User pick which Vendor's data they want to view this session,
 * either by entering a 4-digit access code (already approved) or by submitting
 * a connection-request for an administrator to approve.
 */
public class ChooseAccessController {

    @FXML private Label          userLabel;
    @FXML private TextField      codeInput;
    @FXML private Label          codeErrorLabel;

    @FXML private ComboBox<String> targetBox;
    @FXML private TextField        addressField;
    @FXML private TextField        jobField;
    @FXML private TextField        phoneField;
    @FXML private TextField        extraField;
    @FXML private Label            requestStatusLabel;

    @FXML private ListView<String> connectionsList;
    @FXML private TextField        accountNumberField;

    private final VendorDAO          vendorDAO = new VendorDAO();
    private final UserConnectionDAO  connDAO   = new UserConnectionDAO();
    private final AuthService        auth      = new AuthService();
    private final ConnectionService  connSvc   = new ConnectionService();

    private final Map<String, String> targetNameToId = new HashMap<>();

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();
        userLabel.setText(sm.getCurrentUserId() == null ? "" : sm.getCurrentUserId());

        reloadTargets();
        loadMyConnections();

        // If user came here via Login Option 2 — pre-select their picked vendor
        if (com.paytrace.ui.utils.PendingRequestContext.isSet()) {
            targetBox.setValue(
                    com.paytrace.ui.utils.PendingRequestContext.getTargetName());
            com.paytrace.ui.utils.PendingRequestContext.clear();
        }
    }

    private void reloadTargets() {
        targetBox.getItems().clear();
        targetNameToId.clear();
        try {
            for (Vendor v : vendorDAO.findAll()) {
                targetBox.getItems().add(v.getVendorName());
                targetNameToId.put(v.getVendorName(), v.getVendorId());
            }
        } catch (Exception e) {
            requestStatusLabel.setText("Could not load list: " + e.getMessage());
        }
    }

    private void loadMyConnections() {
        try {
            ObservableList<String> items = FXCollections.observableArrayList();
            List<UserConnection> my =
                    connDAO.findByUser(SessionManager.getInstance().getCurrentUserId());
            for (UserConnection uc : my) {
                String name = vendorDAO.findById(uc.getTargetId())
                        .map(Vendor::getVendorName).orElse("Vendor");
                items.add("Vendor — " + name + "   (code: " + uc.getAccessCode() + ")");
            }
            connectionsList.setItems(items);
        } catch (Exception e) {
            System.err.println("Load connections error: " + e.getMessage());
        }
    }

    @FXML
    private void enterWithCode() {
        codeErrorLabel.setText("");
        String code = codeInput.getText() == null ? "" : codeInput.getText().trim();
        if (code.length() != 4) {
            codeErrorLabel.setText("Code must be 4 digits.");
            return;
        }
        SessionManager sm = SessionManager.getInstance();
        try {
            var connOpt = connDAO.findByUserAndCode(sm.getCurrentUserId(), code);
            if (connOpt.isEmpty()) {
                codeErrorLabel.setText(
                        "Code not recognised. Request access to this vendor first.");
                return;
            }
            var uc = connOpt.get();
            String name = vendorDAO.findById(uc.getTargetId())
                    .map(Vendor::getVendorName).orElse("Vendor");
            sm.getCurrentSession().setContextType(uc.getTargetType());
            sm.getCurrentSession().setContextId(uc.getTargetId());
            sm.getCurrentSession().setContextName(name);
            SceneNavigator.navigateTo(SceneNavigator.DASHBOARD, 1280, 800);
        } catch (Exception e) {
            codeErrorLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void submitRequest() {
        requestStatusLabel.setText("");
        String pickedName = targetBox.getValue();
        if (pickedName == null) {
            requestStatusLabel.setText("Pick a vendor first.");
            return;
        }
        String targetId = targetNameToId.get(pickedName);

        String addr = addressField.getText();
        String job  = jobField.getText();
        String phn  = phoneField.getText();
        String acct = accountNumberField.getText();
        String ext  = extraField.getText();

        if (addr == null || addr.isBlank() || job == null || job.isBlank()
                || phn == null || phn.isBlank()
                || acct == null || acct.isBlank()) {
            requestStatusLabel.setText(
                    "Address, Job, Phone, and Account Number are required.");
            return;
        }

        try {
            connSvc.submitRequest(
                    SessionManager.getInstance().getCurrentUserId(),
                    TargetType.VENDOR, targetId, addr, job, phn, ext, acct);

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Request Submitted");
            a.setHeaderText("Your request has been submitted.");
            a.setContentText("You'll receive an email with your access code "
                    + "once the administrator approves.\n\n"
                    + "Your registered account: " + acct);
            a.showAndWait();

            addressField.clear(); jobField.clear(); phoneField.clear();
            accountNumberField.clear(); extraField.clear();
            requestStatusLabel.setText("✓ Request submitted. Awaiting approval.");
            requestStatusLabel.setStyle("-fx-text-fill: #16a34a;");
        } catch (Exception e) {
            requestStatusLabel.setText("Failed: " + e.getMessage());
            requestStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            auth.logout(SessionManager.getInstance().getCurrentSessionId());
            SessionManager.getInstance().clearSession();
            SceneNavigator.navigateTo(SceneNavigator.LOGIN, 900, 600);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
