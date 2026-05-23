package com.paytrace.ui.controllers;

import com.paytrace.dao.NotificationDAO;
import com.paytrace.models.Invoice;
import com.paytrace.models.Notification;
import com.paytrace.models.User;
import com.paytrace.models.UserConnection;
import com.paytrace.models.enums.AlertStatus;
import com.paytrace.services.AlertService;
import com.paytrace.services.InvoiceService;
import com.paytrace.services.ReportService;
import com.paytrace.ui.observer.DashboardAlertObserver;
import com.paytrace.ui.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import com.paytrace.ui.utils.KpiAnimator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class DashboardHomeController {

    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;

    @FXML private Label totalInvoicesLabel;
    @FXML private Label totalPaymentsLabel;
    @FXML private Label matchRateLabel;
    @FXML private Label unreconciledLabel;
    @FXML private Label activeAlertsLabel;
    @FXML private Label kpi1Label;
    @FXML private Label kpi2Label;
    @FXML private Label kpi3Label;
    @FXML private Label kpi4Label;
    @FXML private Label kpi5Label;

    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> agingBarChart;

    @FXML private VBox userStatusCard;
    @FXML private Label userStatusCountLabel;
    @FXML private TableView<UserStatusRow> userStatusTable;
    @FXML private TableColumn<UserStatusRow, String> userNameCol;
    @FXML private TableColumn<UserStatusRow, String> userEmailCol;
    @FXML private TableColumn<UserStatusRow, String> userAccountCol;
    @FXML private TableColumn<UserStatusRow, String> userInvoicesCol;
    @FXML private TableColumn<UserStatusRow, String> userBilledCol;
    @FXML private TableColumn<UserStatusRow, String> userPaidCol;
    @FXML private TableColumn<UserStatusRow, String> userOwedCol;
    @FXML private TableColumn<UserStatusRow, String> userOverdueCol;

    @FXML private ListView<String> notificationsList;
    @FXML private Label unreadBadgeLabel;

    /** Dashboard pulls from many domains — route through services so the
     *  controller depends on five business interfaces, not six DAOs. */
    private final InvoiceService      invoiceService = new InvoiceService();
    private final AlertService        alertService   = AlertService.getInstance();
    private final ReportService       reportService  = new ReportService();
    /** Notification reads stay direct — no read-side service exists yet. */
    private final NotificationDAO notifDAO       = new NotificationDAO();

    @FXML
    public void initialize() {
        userStatusTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        SessionManager sm = SessionManager.getInstance();
        boolean isVendorAdmin = sm.isAdministrator() && sm.getContextId() != null;

        if (sm.isUser() && sm.getContextName() != null) {
            pageTitle.setText("My Dashboard — " + sm.getContextName());
            pageSubtitle.setText("Your invoices, quotes and service requests at "
                    + sm.getContextName());
        } else if (isVendorAdmin) {
            pageTitle.setText(sm.getContextName() + " — Vendor Dashboard");
            pageSubtitle.setText("Your vendor's payments, invoices and user activity");
        } else if (sm.isAdmin()) {
            pageTitle.setText("System Dashboard");
            pageSubtitle.setText("Cross-vendor overview");
        }

        userStatusCard.setVisible(isVendorAdmin);
        userStatusCard.setManaged(isVendorAdmin);

        configureUserStatusTable();
        loadKPIs();
        loadPieChart();
        loadAgingChart();
        if (isVendorAdmin) loadUserStatusTable();
        loadNotifications();

        // ── Observer Pattern (UC-09) ──────────────────────────────────
        // Register the dashboard observer once and bind the alerts label
        // to its observable count. AlertService now broadcasts to every
        // observer when an alert is raised — no more polling.
        DashboardAlertObserver obs = DashboardAlertObserver.getInstance();
        AlertService.getInstance().addObserver(obs);
        obs.activeCountProperty().addListener((src, oldVal, newVal) ->
                activeAlertsLabel.setText(String.valueOf(newVal)));
        try { AlertService.getInstance().refreshAll(); } catch (Exception ignored) {}
    }

    private void loadKPIs() {
        try {
            SessionManager sm = SessionManager.getInstance();
            if (sm.isUser()) loadUserKPIs(); else loadAdminKPIs();
        } catch (Exception e) {
            System.err.println("KPI load error: " + e.getMessage());
        }
    }

    private void loadUserKPIs() throws Exception {
        SessionManager sm = SessionManager.getInstance();
        String userId   = sm.getCurrentUserId();
        String vendorId = sm.getContextId();

        kpi1Label.setText("My Invoices");
        kpi2Label.setText("Approved");
        kpi3Label.setText("Total Owed (PKR)");
        kpi4Label.setText("Reconciled");
        kpi5Label.setText("Unread Notifications");

        int invCount = 0, approvedCount = 0, reconciledCount = 0;
        BigDecimal owed = BigDecimal.ZERO;
        if (vendorId != null) {
            for (Invoice inv : invoiceService.findByOwnerAndUser(vendorId, userId)) {
                invCount++;
                if (inv.getStatus() == com.paytrace.models.enums.InvoiceStatus.APPROVED)
                    approvedCount++;
                if (inv.getRemainingBalance() != null
                        && inv.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0) {
                    owed = owed.add(inv.getRemainingBalance());
                } else {
                    reconciledCount++;
                }
            }
        }
        KpiAnimator.animateInt(totalInvoicesLabel, invCount);
        KpiAnimator.animateInt(totalPaymentsLabel, approvedCount);
        matchRateLabel.setText(owed.toPlainString());
        KpiAnimator.animateInt(unreconciledLabel, reconciledCount);

        int unread = 0;
        try { unread = notifDAO.countUnread(userId); } catch (Exception ignored) {}
        KpiAnimator.animateInt(activeAlertsLabel, unread);
    }

    private void loadAdminKPIs() throws Exception {
        SessionManager sm = SessionManager.getInstance();
        String vendorId = sm.getContextId();

        kpi1Label.setText("Total Invoices");
        kpi2Label.setText("Payments Received");
        kpi3Label.setText("Confirmed %");
        kpi4Label.setText("Pending Review");
        kpi5Label.setText("Active Alerts");

        // ── Use invoice data as the single source of truth ─────────────
        // The user-status table below is also invoice-driven, so the KPI
        // bar must use the same source to stay consistent.
        List<Invoice> invs = vendorId != null && sm.isAdministrator()
                ? invoiceService.findByOwner(vendorId) : invoiceService.findAll();

        int totalInv  = invs.size();
        int fullyPaid = 0;   // remaining == 0  → "cleared / payment received"
        int pending   = 0;   // remaining  > 0  → still outstanding / pending review

        for (Invoice inv : invs) {
            BigDecimal rem = inv.getRemainingBalance();
            BigDecimal due = inv.getAmountDue();
            // Treat null remaining as fully outstanding (same logic as the pie-chart)
            if (rem == null) { pending++; continue; }
            if (rem.compareTo(BigDecimal.ZERO) <= 0) fullyPaid++;
            else pending++;
        }

        // Confirmed % = share of invoices fully cleared
        double rate = totalInv > 0 ? (fullyPaid * 100.0 / totalInv) : 0.0;

        int alerts = alertService.findByStatus(AlertStatus.ACTIVE).size();

        KpiAnimator.animateInt(totalInvoicesLabel, totalInv);
        KpiAnimator.animateInt(totalPaymentsLabel, fullyPaid);   // "Payments Received" = cleared invoices
        KpiAnimator.animatePercent(matchRateLabel, rate);        // "Confirmed %" from invoice balances
        KpiAnimator.animateInt(unreconciledLabel, pending);      // "Pending Review"  = not yet cleared
        KpiAnimator.animateInt(activeAlertsLabel, alerts);
    }

    private void loadPieChart() {
        try {
            SessionManager sm = SessionManager.getInstance();
            List<Invoice> invs = sm.getContextId() != null
                    ? invoiceService.findByOwner(sm.getContextId()) : invoiceService.findAll();
            int paid = 0, partial = 0, unpaid = 0;
            for (Invoice inv : invs) {
                BigDecimal rem = inv.getRemainingBalance();
                BigDecimal due = inv.getAmountDue();
                if (rem == null || due == null) { unpaid++; continue; }
                if (rem.compareTo(BigDecimal.ZERO) <= 0) paid++;
                else if (rem.compareTo(due) < 0) partial++;
                else unpaid++;
            }
            statusPieChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Paid", paid),
                    new PieChart.Data("Partial", partial),
                    new PieChart.Data("Unpaid", unpaid)));
        } catch (Exception e) { System.err.println("Pie chart: " + e.getMessage()); }
    }

    private void loadAgingChart() {
        try {
            SessionManager sm = SessionManager.getInstance();
            List<Invoice> invs = sm.getContextId() != null
                    ? invoiceService.findByOwner(sm.getContextId()) : invoiceService.findOverdue();
            int b1=0, b2=0, b3=0, b4=0;
            for (Invoice inv : invs) {
                BigDecimal rem = inv.getRemainingBalance();
                if (rem == null || rem.compareTo(BigDecimal.ZERO) <= 0) continue;
                if (inv.getDueDate() == null) continue;
                long days = ChronoUnit.DAYS.between(inv.getDueDate(), LocalDate.now());
                if (days < 0) continue;
                if (days <= 30) b1++; else if (days <= 60) b2++;
                else if (days <= 90) b3++; else b4++;
            }
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.getData().add(new XYChart.Data<>("0-30",  b1));
            s.getData().add(new XYChart.Data<>("31-60", b2));
            s.getData().add(new XYChart.Data<>("61-90", b3));
            s.getData().add(new XYChart.Data<>("90+",   b4));
            agingBarChart.getData().clear();
            agingBarChart.getData().add(s);
        } catch (Exception e) { System.err.println("Aging chart: " + e.getMessage()); }
    }

    private void configureUserStatusTable() {
        userNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().fullName));
        userEmailCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().email));
        userAccountCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().accountNumber));
        userInvoicesCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().invoiceCount)));
        userBilledCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().totalBilled.toPlainString()));
        userPaidCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().totalPaid.toPlainString()));
        userOwedCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().outstanding.toPlainString()));
        userOverdueCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().overdueCount)));
    }

    private void loadUserStatusTable() {
        try {
            String vendorId = SessionManager.getInstance().getContextId();
            ObservableList<UserStatusRow> rows = FXCollections.observableArrayList();
            for (UserConnection uc : reportService.vendorConnections(vendorId)) {
                Optional<User> u = reportService.userById(uc.getUserId());
                if (u.isEmpty()) continue;
                List<Invoice> invs = invoiceService.findByOwnerAndUser(vendorId, u.get().getUserId());
                BigDecimal billed = BigDecimal.ZERO, paid = BigDecimal.ZERO, owed = BigDecimal.ZERO;
                int overdue = 0;
                LocalDate today = LocalDate.now();
                for (Invoice inv : invs) {
                    BigDecimal due = inv.getAmountDue() != null ? inv.getAmountDue() : BigDecimal.ZERO;
                    BigDecimal rem = inv.getRemainingBalance() != null ? inv.getRemainingBalance() : due;
                    billed = billed.add(due);
                    owed   = owed.add(rem);
                    paid   = paid.add(due.subtract(rem));
                    if (rem.compareTo(BigDecimal.ZERO) > 0
                            && inv.getDueDate() != null && today.isAfter(inv.getDueDate())) overdue++;
                }
                UserStatusRow r = new UserStatusRow();
                r.fullName = u.get().getFullName();
                r.email = u.get().getEmail();
                r.accountNumber = uc.getUserAccountNumber() == null ? "—" : uc.getUserAccountNumber();
                r.invoiceCount = invs.size();
                r.totalBilled = billed; r.totalPaid = paid; r.outstanding = owed; r.overdueCount = overdue;
                rows.add(r);
            }
            userStatusTable.setItems(rows);
            userStatusCountLabel.setText(rows.size() + " connected user(s)");
        } catch (Exception e) { System.err.println("User status: " + e.getMessage()); }
    }

    private void loadNotifications() {
        try {
            String userId = SessionManager.getInstance().getCurrentUserId();
            if (userId == null) return;
            var notifs = notifDAO.findRecent(userId, 20);
            int unread = notifDAO.countUnread(userId);
            ObservableList<String> items = FXCollections.observableArrayList();
            for (var n : notifs) {
                String prefix = n.isRead() ? "  " : "● ";
                String time = n.getCreatedAt() != null
                        ? n.getCreatedAt().toString().substring(0, 16) : "";
                items.add(prefix + "[" + time + "] " + n.getTitle()
                        + "\n    " + n.getMessage().replace("\n", " "));
            }
            notificationsList.setItems(items);
            unreadBadgeLabel.setText(unread > 0 ? "(" + unread + " unread)" : "");
            unreadBadgeLabel.setStyle(unread > 0
                    ? "-fx-text-fill: #dc2626; -fx-font-weight: bold;" : "");
        } catch (Exception e) { System.err.println("Notifications: " + e.getMessage()); }
    }

    @FXML private void refreshNotifications() { loadNotifications(); }

    @FXML
    private void markAllNotificationsRead() {
        try {
            String userId = SessionManager.getInstance().getCurrentUserId();
            if (userId == null) return;
            notifDAO.markAllAsRead(userId);
            loadNotifications();
        } catch (Exception e) { System.err.println("Mark all read: " + e.getMessage()); }
    }

    public static class UserStatusRow {
        public String fullName, email, accountNumber;
        public int invoiceCount, overdueCount;
        public BigDecimal totalBilled = BigDecimal.ZERO;
        public BigDecimal totalPaid   = BigDecimal.ZERO;
        public BigDecimal outstanding = BigDecimal.ZERO;
    }
}