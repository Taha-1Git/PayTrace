package com.paytrace.ui.controllers;

import com.paytrace.models.Invoice;
import com.paytrace.models.Payment;
import com.paytrace.models.PaymentAllocation;
import com.paytrace.models.Vendor;
import com.paytrace.models.enums.InvoiceStatus;
import com.paytrace.services.InvoiceService;
import com.paytrace.services.PaymentService;
import com.paytrace.services.ReportService;
import com.paytrace.services.InvoiceService;
import com.paytrace.services.PaymentService;
import com.paytrace.services.ReportService;
import com.paytrace.ui.utils.SceneNavigator;
import com.paytrace.ui.utils.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SplitPaymentController {



    @FXML private Label paymentTxnLabel;
    @FXML private Label paymentVendorLabel;
    @FXML private Label paymentAmountLabel;

    @FXML private TableView<AllocationRow> allocationTable;
    @FXML private TableColumn<AllocationRow, String> allocInvoiceCol;
    @FXML private TableColumn<AllocationRow, String> allocVendorCol;
    @FXML private TableColumn<AllocationRow, String> allocOutstandingCol;
    @FXML private TableColumn<AllocationRow, String> allocAmountCol;

    @FXML private Label totalAllocatedLabel;
    @FXML private Label residualLabel;
    @FXML private Label splitStatusLabel;

    /** UC-08 (Split Payment): everything routed through the service layer. */
    private final InvoiceService invoiceService = new InvoiceService();
    private final PaymentService paymentService = new PaymentService();
    private final ReportService  reportService  = new ReportService();

    private Payment payment;
    private final ObservableList<AllocationRow> rows = FXCollections.observableArrayList();
    private final Map<String, String> vendorNameCache = new HashMap<>();

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();

        payment = SplitPaymentContext.getSelectedPayment();
        if (payment == null) {
            splitStatusLabel.setText("No payment selected. Return to Manual Reconciliation.");
            return;
        }

        paymentTxnLabel.setText(payment.getTransactionId());
        paymentVendorLabel.setText(payment.getVendorName() == null
                ? "—" : payment.getVendorName());
        paymentAmountLabel.setText(payment.getAmountPaid() != null
                ? payment.getAmountPaid().toPlainString() : "0");

        configureTable();
        loadInvoicesForVendor();
        recalculate();
    }

    private void configureTable() {
        allocInvoiceCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().invoiceNumber));
        allocVendorCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().vendorName));
        allocOutstandingCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().outstanding == null ? "0"
                                : d.getValue().outstanding.toPlainString()));
        allocAmountCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().allocated == null ? "0"
                                : d.getValue().allocated.toPlainString()));

        // Editable allocation column
        allocAmountCol.setCellFactory(
                TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        allocAmountCol.setOnEditCommit(ev -> {
            AllocationRow row = ev.getRowValue();
            try {
                row.allocated = new BigDecimal(ev.getNewValue().trim());
            } catch (Exception e) {
                row.allocated = BigDecimal.ZERO;
            }
            recalculate();
        });

        allocationTable.setItems(rows);
    }

    private void loadInvoicesForVendor() {
        rows.clear();
        try {
            // Find approved invoices for the same vendor (by normalized name match)
            String normPayVendor = payment.getNormalizedVendorName() == null
                    ? "" : payment.getNormalizedVendorName().toLowerCase();

            List<Invoice> approved = invoiceService.findByStatus(InvoiceStatus.APPROVED);
            for (Invoice inv : approved) {
                String iv = inv.getNormalizedVendorName() == null
                        ? "" : inv.getNormalizedVendorName().toLowerCase();
                if (!iv.isEmpty() && iv.equals(normPayVendor)) {
                    AllocationRow r = new AllocationRow();
                    r.invoice        = inv;
                    r.invoiceNumber  = inv.getInvoiceNumber();
                    r.vendorName     = resolveVendorName(inv.getVendorId());
                    r.outstanding    = inv.getRemainingBalance() != null
                            ? inv.getRemainingBalance() : inv.getAmountDue();
                    r.allocated      = BigDecimal.ZERO;
                    rows.add(r);
                }
            }

            // Proportional defaults if total outstanding > 0
            BigDecimal totalOut = BigDecimal.ZERO;
            for (AllocationRow r : rows)
                if (r.outstanding != null) totalOut = totalOut.add(r.outstanding);

            if (totalOut.compareTo(BigDecimal.ZERO) > 0 && payment.getAmountPaid() != null) {
                for (AllocationRow r : rows) {
                    BigDecimal share = r.outstanding
                            .divide(totalOut, 6, RoundingMode.HALF_UP)
                            .multiply(payment.getAmountPaid())
                            .setScale(2, RoundingMode.HALF_UP);
                    // Don't allocate more than the invoice outstanding
                    r.allocated = share.min(r.outstanding);
                }
            }
        } catch (Exception e) {
            splitStatusLabel.setText("Load failed: " + e.getMessage());
        }
    }

    @FXML
    private void recalculate() {
        BigDecimal total = BigDecimal.ZERO;
        for (AllocationRow r : rows) {
            if (r.allocated != null) total = total.add(r.allocated);
        }
        totalAllocatedLabel.setText(total.toPlainString());

        BigDecimal payAmt = payment == null || payment.getAmountPaid() == null
                ? BigDecimal.ZERO : payment.getAmountPaid();
        BigDecimal residual = payAmt.subtract(total);
        residualLabel.setText(residual.toPlainString());

        // Color coding
        if (residual.compareTo(BigDecimal.ZERO) == 0) {
            residualLabel.setStyle("-fx-text-fill: #16a34a;");
        } else if (residual.compareTo(BigDecimal.ZERO) < 0) {
            residualLabel.setStyle("-fx-text-fill: #dc2626;");
        } else {
            residualLabel.setStyle("-fx-text-fill: #d97706;");
        }

        allocationTable.refresh();
    }

    @FXML
    private void applySplit() {
        if (payment == null) return;

        BigDecimal total = BigDecimal.ZERO;
        for (AllocationRow r : rows)
            if (r.allocated != null) total = total.add(r.allocated);

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            showAlert("Please enter at least one non-zero allocation.");
            return;
        }
        if (total.compareTo(payment.getAmountPaid()) > 0) {
            showAlert("Total allocated (" + total.toPlainString()
                    + ") exceeds payment amount (" + payment.getAmountPaid().toPlainString() + ").");
            return;
        }

        try {
            String userId = SessionManager.getInstance().getCurrentUserId();

            // 1. Build per-invoice allocation drafts; update each invoice's
            //    remaining balance through the InvoiceService.
            java.util.List<PaymentAllocation> allocations = new java.util.ArrayList<>();
            for (AllocationRow r : rows) {
                if (r.allocated == null
                        || r.allocated.compareTo(BigDecimal.ZERO) <= 0) continue;

                PaymentAllocation alloc = new PaymentAllocation();
                alloc.setInvoiceId(r.invoice.getInvoiceId());
                alloc.setAllocatedAmount(r.allocated);
                alloc.setResidual(false);
                alloc.setAllocationDate(LocalDateTime.now());
                allocations.add(alloc);

                BigDecimal newBal = r.outstanding.subtract(r.allocated);
                if (newBal.compareTo(BigDecimal.ZERO) < 0) newBal = BigDecimal.ZERO;
                invoiceService.updateRemainingBalance(r.invoice.getInvoiceId(), newBal);
            }

            // 2. Residual goes in as an allocation against no invoice.
            BigDecimal residual = payment.getAmountPaid().subtract(total);
            if (residual.compareTo(BigDecimal.ZERO) > 0) {
                PaymentAllocation res = new PaymentAllocation();
                res.setInvoiceId(null);
                res.setAllocatedAmount(residual);
                res.setResidual(true);
                res.setAllocationDate(LocalDateTime.now());
                allocations.add(res);
            }

            // 3. PaymentService persists allocations + marks payment FULLY_ALLOCATED
            //    + writes a single PAYMENT_SPLIT audit entry.
            paymentService.splitPayment(payment.getPaymentId(), allocations, userId);

            splitStatusLabel.setText("Split applied: " + rows.size()
                    + " allocation(s) saved" + (residual.compareTo(BigDecimal.ZERO) > 0
                    ? ", residual " + residual.toPlainString() + " recorded." : "."));
            splitStatusLabel.setStyle("-fx-text-fill: #16a34a;");
        } catch (Exception ex) {
            splitStatusLabel.setText("Apply failed: " + ex.getMessage());
            splitStatusLabel.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    private String resolveVendorName(String vendorId) {
        if (vendorId == null) return "—";
        if (vendorNameCache.containsKey(vendorId)) return vendorNameCache.get(vendorId);
        try {
            Optional<Vendor> v = reportService.vendor(vendorId);
            String name = v.map(Vendor::getVendorName).orElse("—");
            vendorNameCache.put(vendorId, name);
            return name;
        } catch (Exception e) { return "—"; }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    @FXML
    private void goBack() {
        try { SceneNavigator.navigateTo(SceneNavigator.MANUAL_RECON, 1280, 800); }
        catch (Exception e) { e.printStackTrace(); }
    }

    public static class AllocationRow {
        public Invoice invoice;
        public String invoiceNumber;
        public String vendorName;
        public BigDecimal outstanding;
        public BigDecimal allocated;
    }
}