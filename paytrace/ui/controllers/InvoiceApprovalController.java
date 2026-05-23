package com.paytrace.ui.controllers;

import com.paytrace.dao.InvoiceDAO;
import com.paytrace.dao.VendorDAO;
import com.paytrace.models.Invoice;
import com.paytrace.models.Vendor;
import com.paytrace.models.enums.InvoiceStatus;
import com.paytrace.services.InvoiceService;
import com.paytrace.services.InvoiceService;
import com.paytrace.ui.utils.SceneNavigator;
import com.paytrace.ui.utils.SessionManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InvoiceApprovalController {


    @FXML private Button backButton;

    @FXML private Label pendingCountLabel;
    @FXML private Label approvedTodayLabel;
    @FXML private Label onHoldCountLabel;
    @FXML private Label rejectedCountLabel;

    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField searchField;
    @FXML private Label resultCountLabel;

    @FXML private TableView<InvoiceRow> invoiceTable;
    @FXML private TableColumn<InvoiceRow, Boolean> selectCol;
    @FXML private TableColumn<InvoiceRow, String> invoiceNumCol;
    @FXML private TableColumn<InvoiceRow, String> vendorCol;
    @FXML private TableColumn<InvoiceRow, String> amountCol;
    @FXML private TableColumn<InvoiceRow, String> dueDateCol;
    @FXML private TableColumn<InvoiceRow, String> statusCol;
    @FXML private TableColumn<InvoiceRow, String> daysSinceCol;

    @FXML private Label statusMessageLabel;

    private final InvoiceDAO    invoiceDAO     = new InvoiceDAO();
    private final VendorDAO     vendorDAO      = new VendorDAO();
    /** Business-logic boundary — handles audit logging + Observer broadcast internally. */
    private final InvoiceService    invoiceService = new InvoiceService();

    private final Map<String, String> vendorNameCache = new HashMap<>();
    private final ObservableList<InvoiceRow> tableData =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();

        statusFilter.getItems().addAll(
                "Pending Approval", "Approved", "On Hold", "Rejected", "All");
        statusFilter.setValue("Pending Approval");

        configureTableColumns();
        refreshTable();
    }

    private void configureTableColumns() {
        selectCol.setCellValueFactory(d -> d.getValue().selectedProperty());
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setEditable(true);
        invoiceTable.setEditable(true);

        invoiceNumCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getInvoice().getInvoiceNumber()));
        vendorCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getVendorName()));
        amountCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getInvoice().getAmountDue() != null
                        ? d.getValue().getInvoice().getAmountDue().toPlainString()
                        : "0"));
        dueDateCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getInvoice().getDueDate() != null
                        ? d.getValue().getInvoice().getDueDate().toString() : "—"));
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getInvoice().getStatus() != null
                        ? d.getValue().getInvoice().getStatus().name().replace('_', ' ')
                        : "—"));
        daysSinceCol.setCellValueFactory(d -> new SimpleStringProperty(
                String.valueOf(d.getValue().getDaysSinceImport())));

        invoiceTable.setItems(tableData);
    }

    @FXML
    private void refreshTable() {
        try {
            List<Invoice> invoices = loadInvoicesByFilter();
            tableData.clear();
            for (Invoice inv : invoices) {
                tableData.add(new InvoiceRow(inv, resolveVendorName(inv)));
            }
            resultCountLabel.setText(tableData.size() + " record"
                    + (tableData.size() == 1 ? "" : "s"));
            updateKPIs();
            statusMessageLabel.setText("");
        } catch (Exception e) {
            showError("Failed to load invoices: " + e.getMessage());
        }
    }

    @FXML
    private void applyFilter() {
        refreshTable();
        String q = searchField.getText() == null
                ? "" : searchField.getText().trim().toLowerCase();
        if (q.isEmpty()) return;

        List<InvoiceRow> kept = new ArrayList<>();
        for (InvoiceRow r : tableData) {
            String inum = r.getInvoice().getInvoiceNumber() == null
                    ? "" : r.getInvoice().getInvoiceNumber().toLowerCase();
            String vname = r.getVendorName() == null
                    ? "" : r.getVendorName().toLowerCase();
            if (inum.contains(q) || vname.contains(q)) kept.add(r);
        }
        tableData.setAll(kept);
        resultCountLabel.setText(tableData.size() + " match"
                + (tableData.size() == 1 ? "" : "es"));
    }

    private List<Invoice> loadInvoicesByFilter() throws Exception {
        String choice = statusFilter.getValue();
        if (choice == null) choice = "Pending Approval";
        switch (choice) {
            case "Pending Approval":
                return invoiceDAO.findByStatus(InvoiceStatus.PENDING_APPROVAL);
            case "Approved":
                return invoiceDAO.findByStatus(InvoiceStatus.APPROVED);
            case "On Hold":
                return invoiceDAO.findByStatus(InvoiceStatus.ON_HOLD);
            case "Rejected":
                return invoiceDAO.findByStatus(InvoiceStatus.REJECTED);
            case "All":
            default:
                return invoiceDAO.findAll();
        }
    }

    private void updateKPIs() {
        try {
            pendingCountLabel.setText(String.valueOf(
                    invoiceDAO.findByStatus(InvoiceStatus.PENDING_APPROVAL).size()));
            onHoldCountLabel.setText(String.valueOf(
                    invoiceDAO.findByStatus(InvoiceStatus.ON_HOLD).size()));
            rejectedCountLabel.setText(String.valueOf(
                    invoiceDAO.findByStatus(InvoiceStatus.REJECTED).size()));

            List<Invoice> approved =
                    invoiceDAO.findByStatus(InvoiceStatus.APPROVED);
            long today = approved.stream()
                    .filter(i -> i.getCreatedAt() != null
                            && i.getCreatedAt().toLocalDate()
                            .equals(LocalDate.now()))
                    .count();
            approvedTodayLabel.setText(String.valueOf(today));
        } catch (Exception e) {
            System.err.println("KPI load error: " + e.getMessage());
        }
    }

    private String resolveVendorName(Invoice inv) {
        if (inv.getVendorId() == null) return "—";
        if (vendorNameCache.containsKey(inv.getVendorId()))
            return vendorNameCache.get(inv.getVendorId());
        try {
            Optional<Vendor> v = vendorDAO.findById(inv.getVendorId());
            String name = v.map(Vendor::getVendorName).orElse("—");
            vendorNameCache.put(inv.getVendorId(), name);
            return name;
        } catch (Exception e) {
            return "—";
        }
    }

    @FXML
    private void batchApprove() {
        List<InvoiceRow> selected = selectedRows();
        if (selected.isEmpty()) { showInfo("Select at least one invoice to approve."); return; }

        List<InvoiceRow> eligible = new ArrayList<>();
        int skipped = 0;
        for (InvoiceRow r : selected) {
            if (r.getInvoice().getStatus() == InvoiceStatus.PENDING_APPROVAL)
                eligible.add(r);
            else skipped++;
        }
        if (eligible.isEmpty()) {
            showError("No eligible invoices. Only 'Pending Approval' invoices can be approved.");
            return;
        }

        if (!confirm("Approve " + eligible.size() + " invoice(s)?",
                "This will mark them Approved and log the action. Continue?")) return;

        int ok = 0, fail = 0;
        String adminId = SessionManager.getInstance().getCurrentUserId();

        for (InvoiceRow r : eligible) {
            try {
                // Service handles status update + audit log atomically.
                invoiceService.approve(r.getInvoice().getInvoiceId(), adminId);
                ok++;
            } catch (Exception e) { fail++; }
        }

        String msg = "Approved " + ok + " of " + eligible.size();
        if (skipped > 0) msg += " (" + skipped + " skipped)";
        if (fail > 0)    msg += " — " + fail + " failed";
        statusMessageLabel.setText(msg);
        statusMessageLabel.setStyle(fail == 0
                ? "-fx-text-fill: #16a34a;" : "-fx-text-fill: #d97706;");
        refreshTable();
    }

    @FXML
    private void holdSelected() {
        List<InvoiceRow> selected = selectedRows();
        if (selected.isEmpty()) { showInfo("Select at least one invoice to hold."); return; }

        String reason = promptForReason("Hold Invoice(s)",
                "Enter a hold reason (applies to all " + selected.size() + " selected):");
        if (reason == null || reason.trim().isEmpty()) {
            showInfo("Hold cancelled — a reason is required."); return;
        }

        int ok = 0, fail = 0;
        String adminId = SessionManager.getInstance().getCurrentUserId();

        for (InvoiceRow r : selected) {
            try {
                // Service: status update + audit log + Observer-broadcast alert.
                invoiceService.placeOnHold(r.getInvoice().getInvoiceId(),
                        reason.trim(), adminId);
                ok++;
            } catch (Exception e) { fail++; }
        }

        statusMessageLabel.setText("Held " + ok + " of " + selected.size()
                + (fail > 0 ? " — " + fail + " failed" : "")
                + ". Warning alerts created.");
        statusMessageLabel.setStyle("-fx-text-fill: #d97706;");
        refreshTable();
    }

    @FXML
    private void rejectSelected() {
        List<InvoiceRow> selected = selectedRows();
        if (selected.isEmpty()) { showInfo("Select at least one invoice to reject."); return; }

        String reason = promptForReason("Reject Invoice(s)",
                "Enter a rejection reason (applies to all " + selected.size() + " selected):");
        if (reason == null || reason.trim().isEmpty()) {
            showInfo("Rejection cancelled — a reason is required."); return;
        }

        if (!confirm("Reject " + selected.size() + " invoice(s)?",
                "Rejection is final and will be logged. Continue?")) return;

        int ok = 0, fail = 0;
        String adminId = SessionManager.getInstance().getCurrentUserId();

        for (InvoiceRow r : selected) {
            try {
                // Service: status update + audit log atomically.
                invoiceService.reject(r.getInvoice().getInvoiceId(),
                        reason.trim(), adminId);
                ok++;
            } catch (Exception e) { fail++; }
        }

        statusMessageLabel.setText("Rejected " + ok + " of " + selected.size()
                + (fail > 0 ? " — " + fail + " failed" : ""));
        statusMessageLabel.setStyle("-fx-text-fill: #dc2626;");
        refreshTable();
    }

    private List<InvoiceRow> selectedRows() {
        List<InvoiceRow> out = new ArrayList<>();
        for (InvoiceRow r : tableData) if (r.isSelected()) out.add(r);
        return out;
    }

    // Audit logging + hold-alert broadcasting both moved into InvoiceService —
    // the controller now delegates and stays focused on UI concerns.

    private String promptForReason(String title, String prompt) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle(title);
        dlg.setHeaderText(prompt);
        dlg.setContentText("Reason:");
        return dlg.showAndWait().orElse(null);
    }

    private boolean confirm(String title, String body) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(body);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    private void showInfo(String msg) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showError(String msg) {
        statusMessageLabel.setText(msg);
        statusMessageLabel.setStyle("-fx-text-fill: #dc2626;");
    }

    private void showBlockingDialog(String title, String msg) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    @FXML
    private void goBack() {
        try { SceneNavigator.navigateTo(SceneNavigator.DASHBOARD, 1280, 800); }
        catch (Exception e) { e.printStackTrace(); }
    }

    public static class InvoiceRow {
        private final Invoice invoice;
        private final String vendorName;
        private final SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
        private final int daysSinceImport;

        public InvoiceRow(Invoice inv, String vendorName) {
            this.invoice = inv;
            this.vendorName = vendorName;
            this.daysSinceImport = inv.getCreatedAt() == null ? 0
                    : (int) ChronoUnit.DAYS.between(
                    inv.getCreatedAt().toLocalDate(), LocalDate.now());
        }

        public Invoice getInvoice()           { return invoice; }
        public String getVendorName()         { return vendorName; }
        public int getDaysSinceImport()       { return daysSinceImport; }
        public boolean isSelected()           { return selected.get(); }
        public void setSelected(boolean v)    { selected.set(v); }
        public SimpleBooleanProperty selectedProperty() { return selected; }
    }
}