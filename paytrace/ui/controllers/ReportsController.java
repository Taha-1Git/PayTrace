package com.paytrace.ui.controllers;

import com.paytrace.models.AuditEntry;
import com.paytrace.models.Invoice;
import com.paytrace.models.Payment;
import com.paytrace.models.User;
import com.paytrace.models.UserConnection;
import com.paytrace.models.Vendor;
import com.paytrace.services.ReportService;
import com.paytrace.services.ReportService;
import com.paytrace.ui.utils.SceneNavigator;
import com.paytrace.ui.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ReportsController {



    @FXML private ComboBox<String> reportTypeBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private TextField vendorFilterField;
    @FXML private ComboBox<String> formatBox;

    @FXML private ProgressIndicator reportSpinner;
    @FXML private VBox resultsSection;
    @FXML private TextArea previewArea;
    @FXML private Label reportStatusLabel;

    /**
     * Single business-logic dependency for the entire reports module.
     * Hides the six DAOs that used to be wired in here behind a single
     * domain-flavoured facade ({@link ReportService}).
     */
    private final ReportService reportService = new ReportService();

    private String lastGeneratedContent;
    private String lastReportType;

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();

        // Different reports for different roles
        if (sm.isAdmin()) {
            reportTypeBox.getItems().addAll(
                    "System Overview",
                    "All Vendors Summary",
                    "All Users Summary",
                    "Audit Trail");
        } else if (sm.isAdministrator()) {
            reportTypeBox.getItems().addAll(
                    "Vendor Summary",
                    "Per-User Payment Status",
                    "Invoice Status Report",
                    "Audit Trail");
        } else {
            // Regular user
            reportTypeBox.getItems().addAll(
                    "My Payment History");
        }

        if (!reportTypeBox.getItems().isEmpty()) {
            reportTypeBox.setValue(reportTypeBox.getItems().get(0));
        }

        formatBox.getItems().addAll("Text (.txt)", "CSV (.csv)");
        formatBox.setValue("Text (.txt)");

        fromDatePicker.setValue(LocalDate.now().minusMonths(1));
        toDatePicker.setValue(LocalDate.now());
    }

    @FXML
    private void generateReport() {
        String type = reportTypeBox.getValue();
        if (type == null) { warn("Pick a report type."); return; }

        reportSpinner.setVisible(true);
        try {
            String content;
            switch (type) {
                case "System Overview":         content = systemOverview(); break;
                case "All Vendors Summary":     content = allVendorsSummary(); break;
                case "All Users Summary":      content = allUsersSummary(); break;
                case "Vendor Summary":          content = vendorSummary(); break;
                case "Per-User Payment Status": content = perUserStatus(); break;
                case "Invoice Status Report":   content = invoiceStatus(); break;
                case "My Payment History":     content = myPaymentHistory(); break;
                case "Audit Trail":            content = auditTrail(); break;
                default: content = "Unknown report type.";
            }

            lastGeneratedContent = content;
            lastReportType = type;
            previewArea.setText(content);
            resultsSection.setVisible(true);
            resultsSection.setManaged(true);
            reportStatusLabel.setText("✓ Report generated. Click 'Save to File' to export.");
            reportStatusLabel.setStyle("-fx-text-fill: #16a34a;");
        } catch (Exception e) {
            reportStatusLabel.setText("Failed: " + e.getMessage());
            reportStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        } finally {
            reportSpinner.setVisible(false);
        }
    }

    @FXML
    private void saveReport() {
        if (lastGeneratedContent == null) { warn("Generate a report first."); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Report");
        String ext = formatBox.getValue() != null && formatBox.getValue().contains("CSV")
                ? ".csv" : ".txt";
        fc.setInitialFileName(lastReportType.replace(' ', '_') + "_" +
                LocalDate.now() + ext);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Reports", "*" + ext));
        java.io.File f = fc.showSaveDialog(null);
        if (f == null) return;
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.write(lastGeneratedContent);
            reportStatusLabel.setText("✓ Saved: " + f.getAbsolutePath());
            reportStatusLabel.setStyle("-fx-text-fill: #16a34a;");
        } catch (Exception e) {
            reportStatusLabel.setText("Save failed: " + e.getMessage());
            reportStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    // ── Admin reports ──

    private String systemOverview() throws Exception {
        StringBuilder sb = banner("PAYTRACE — SYSTEM OVERVIEW");
        List<Vendor> vendors = reportService.allVendors();
        List<User>   users   = reportService.allUsers();
        List<Invoice> invoices = reportService.allInvoices();
        List<Payment> payments = reportService.allPayments();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Payment p : payments)
            if (p.getAmountPaid() != null) totalRevenue = totalRevenue.add(p.getAmountPaid());

        sb.append("Vendors registered:    ").append(vendors.size()).append("\n");
        sb.append("Users registered:      ").append(users.size()).append("\n");
        sb.append("Invoices issued:       ").append(invoices.size()).append("\n");
        sb.append("Payments processed:    ").append(payments.size()).append("\n");
        sb.append("Total revenue (PKR):   ").append(totalRevenue.toPlainString()).append("\n");
        return sb.toString();
    }

    private String allVendorsSummary() throws Exception {
        StringBuilder sb = banner("PAYTRACE — ALL VENDORS SUMMARY");
        for (Vendor v : reportService.allVendors()) {
            int userCount = reportService.vendorConnections(v.getVendorId()).size();
            List<Invoice> invs = reportService.vendorInvoices(v.getVendorId());
            List<Payment> pays = reportService.vendorPayments(v.getVendorId());
            BigDecimal billed = BigDecimal.ZERO;
            BigDecimal paid   = BigDecimal.ZERO;
            for (Invoice i : invs)
                if (i.getAmountDue() != null) billed = billed.add(i.getAmountDue());
            for (Payment p : pays)
                if (p.getAmountPaid() != null) paid = paid.add(p.getAmountPaid());

            sb.append("─── ").append(v.getVendorName()).append(" (")
                    .append(v.getAccessCode()).append(") ───\n");
            sb.append("  Account #:        ").append(v.getAccountNumber()).append("\n");
            sb.append("  Connected users:  ").append(userCount).append("\n");
            sb.append("  Invoices issued:  ").append(invs.size()).append("\n");
            sb.append("  Total billed:     PKR ").append(billed.toPlainString()).append("\n");
            sb.append("  Total received:   PKR ").append(paid.toPlainString()).append("\n");
            sb.append("  Outstanding:      PKR ")
                    .append(billed.subtract(paid).toPlainString()).append("\n\n");
        }
        return sb.toString();
    }

    private String allUsersSummary() throws Exception {
        StringBuilder sb = banner("PAYTRACE — ALL USERS SUMMARY");
        for (User u : reportService.allUsers()) {
            if (u.getAccountType() != com.paytrace.models.enums.AccountType.USER) continue;
            sb.append("─── ").append(u.getFullName()).append(" (")
                    .append(u.getEmail()).append(") ───\n");
            List<UserConnection> conns = reportService.userConnections(u.getUserId());
            sb.append("  Connected to ").append(conns.size()).append(" vendor(s)\n");
            for (UserConnection c : conns) {
                Optional<Vendor> v = reportService.vendor(c.getTargetId());
                if (v.isEmpty()) continue;
                List<Invoice> invs = reportService.vendorInvoicesForUser(
                        v.get().getVendorId(), u.getUserId());
                BigDecimal billed = BigDecimal.ZERO, owed = BigDecimal.ZERO;
                for (Invoice i : invs) {
                    if (i.getAmountDue() != null) billed = billed.add(i.getAmountDue());
                    if (i.getRemainingBalance() != null)
                        owed = owed.add(i.getRemainingBalance());
                }
                sb.append("    • ").append(v.get().getVendorName())
                        .append(" — ").append(invs.size()).append(" invoices, ")
                        .append("billed ").append(billed.toPlainString())
                        .append(", owes ").append(owed.toPlainString()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // ── Administrator reports ──

    private String vendorSummary() throws Exception {
        SessionManager sm = SessionManager.getInstance();
        String vendorId = sm.getContextId();
        if (vendorId == null) return "No vendor context.";
        Optional<Vendor> v = reportService.vendor(vendorId);
        if (v.isEmpty()) return "Vendor not found.";

        StringBuilder sb = banner("VENDOR SUMMARY — " + v.get().getVendorName());
        int userCount = reportService.vendorConnections(vendorId).size();
        List<Invoice> invs = reportService.vendorInvoices(vendorId);
        List<Payment> pays = reportService.vendorPayments(vendorId);

        BigDecimal billed = BigDecimal.ZERO, paid = BigDecimal.ZERO;
        for (Invoice i : invs)
            if (i.getAmountDue() != null) billed = billed.add(i.getAmountDue());
        for (Payment p : pays)
            if (p.getAmountPaid() != null) paid = paid.add(p.getAmountPaid());

        int confirmed = 0;
        for (Payment p : pays) if (p.isConfirmed()) confirmed++;

        sb.append("Vendor:              ").append(v.get().getVendorName()).append("\n");
        sb.append("Access code:         ").append(v.get().getAccessCode()).append("\n");
        sb.append("Account #:           ").append(v.get().getAccountNumber()).append("\n");
        sb.append("Email:               ").append(v.get().getVendorEmail()).append("\n\n");
        sb.append("Connected users:     ").append(userCount).append("\n");
        sb.append("Invoices issued:     ").append(invs.size()).append("\n");
        sb.append("Payments received:   ").append(pays.size())
                .append(" (").append(confirmed).append(" confirmed)").append("\n");
        sb.append("Total billed:        PKR ").append(billed.toPlainString()).append("\n");
        sb.append("Total received:      PKR ").append(paid.toPlainString()).append("\n");
        sb.append("Outstanding:         PKR ")
                .append(billed.subtract(paid).toPlainString()).append("\n");
        return sb.toString();
    }

    private String perUserStatus() throws Exception {
        SessionManager sm = SessionManager.getInstance();
        String vendorId = sm.getContextId();
        if (vendorId == null) return "No vendor context.";

        StringBuilder sb = banner("PER-USER PAYMENT STATUS");
        for (UserConnection conn : reportService.vendorConnections(vendorId)) {
            Optional<User> u = reportService.userById(conn.getUserId());
            if (u.isEmpty()) continue;
            List<Invoice> invs = reportService.vendorInvoicesForUser(vendorId, u.get().getUserId());
            BigDecimal billed = BigDecimal.ZERO, owed = BigDecimal.ZERO;
            int paidCount = 0, partialCount = 0, unpaidCount = 0, overdueCount = 0;
            LocalDate today = LocalDate.now();

            for (Invoice i : invs) {
                BigDecimal due = i.getAmountDue() != null ? i.getAmountDue() : BigDecimal.ZERO;
                BigDecimal rem = i.getRemainingBalance() != null
                        ? i.getRemainingBalance() : due;
                billed = billed.add(due);
                owed   = owed.add(rem);
                if (rem.compareTo(BigDecimal.ZERO) <= 0) paidCount++;
                else if (rem.compareTo(due) < 0) partialCount++;
                else unpaidCount++;
                if (rem.compareTo(BigDecimal.ZERO) > 0
                        && i.getDueDate() != null
                        && today.isAfter(i.getDueDate())) overdueCount++;
            }

            sb.append("─── ").append(u.get().getFullName()).append(" (")
                    .append(u.get().getEmail()).append(") ───\n");
            sb.append("  Total invoices:   ").append(invs.size()).append("\n");
            sb.append("  Fully paid:       ").append(paidCount).append("\n");
            sb.append("  Partial:          ").append(partialCount).append("\n");
            sb.append("  Unpaid:           ").append(unpaidCount).append("\n");
            sb.append("  Overdue:          ").append(overdueCount).append("\n");
            sb.append("  Total billed:     PKR ").append(billed.toPlainString()).append("\n");
            sb.append("  Outstanding:      PKR ").append(owed.toPlainString()).append("\n\n");
        }
        return sb.toString();
    }

    private String invoiceStatus() throws Exception {
        SessionManager sm = SessionManager.getInstance();
        String vendorId = sm.getContextId();
        if (vendorId == null) return "No vendor context.";

        StringBuilder sb = banner("INVOICE STATUS REPORT");
        for (Invoice i : reportService.vendorInvoices(vendorId)) {
            Optional<User> u = reportService.userById(i.getUserId());
            String userName = u.map(User::getFullName).orElse("—");
            sb.append(i.getInvoiceNumber()).append(" | ")
                    .append(userName).append(" | ")
                    .append("Due: ").append(i.getAmountDue()).append(" | ")
                    .append("Remaining: ").append(i.getRemainingBalance()).append(" | ")
                    .append(i.getDueDate()).append(" | ")
                    .append(i.getDescription() == null ? "" : i.getDescription())
                    .append("\n");
        }
        return sb.toString();
    }

    // ── User report ──

    private String myPaymentHistory() throws Exception {
        SessionManager sm = SessionManager.getInstance();
        String userId = sm.getCurrentUserId();
        StringBuilder sb = banner("MY PAYMENT HISTORY");

        for (UserConnection c : reportService.userConnections(userId)) {
            Optional<Vendor> v = reportService.vendor(c.getTargetId());
            if (v.isEmpty()) continue;
            sb.append("─── ").append(v.get().getVendorName()).append(" ───\n");
            List<Invoice> invs = reportService.vendorInvoicesForUser(
                    v.get().getVendorId(), userId);
            for (Invoice i : invs) {
                sb.append("  ").append(i.getInvoiceNumber())
                        .append(" — billed ").append(i.getAmountDue())
                        .append(", remaining ").append(i.getRemainingBalance())
                        .append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // ── Audit trail ──

    private String auditTrail() throws Exception {
        StringBuilder sb = banner("AUDIT TRAIL");
        LocalDate from = fromDatePicker.getValue();
        LocalDate to   = toDatePicker.getValue();
        List<AuditEntry> entries = reportService.auditTrail(from, to);
        for (AuditEntry e : entries) {
            sb.append(e.getPerformedAt()).append(" | ")
                    .append(e.getEventType()).append(" | ")
                    .append(e.getPerformedBy()).append(" | ")
                    .append(e.getEntityType()).append(":").append(e.getEntityId())
                    .append("\n");
        }
        sb.append("\n").append(entries.size()).append(" total entries.\n");
        return sb.toString();
    }

    private StringBuilder banner(String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("════════════════════════════════════════════\n");
        sb.append("  ").append(title).append("\n");
        sb.append("════════════════════════════════════════════\n");
        sb.append("Generated: ").append(LocalDateTime.now()).append("\n");
        sb.append("By:        ")
                .append(SessionManager.getInstance().getCurrentUserId()).append("\n\n");
        return sb;
    }

    private void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    @FXML
    private void goBack() {
        try { SceneNavigator.navigateTo(SceneNavigator.DASHBOARD, 1280, 800); }
        catch (Exception e) { e.printStackTrace(); }
    }
}