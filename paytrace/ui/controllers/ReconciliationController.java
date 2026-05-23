package com.paytrace.ui.controllers;

import com.paytrace.models.Invoice;
import com.paytrace.models.Payment;
import com.paytrace.models.enums.PaymentStatus;
import com.paytrace.patterns.command.CommandHistory;
import com.paytrace.services.InvoiceService;
import com.paytrace.services.PaymentService;
import com.paytrace.services.ReconciliationService;
import com.paytrace.services.InvoiceService;
import com.paytrace.services.PaymentService;
import com.paytrace.services.ReconciliationService;
import com.paytrace.ui.utils.SceneNavigator;
import com.paytrace.ui.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static com.paytrace.patterns.command.MatchCommand.invoiceDAO;
import static com.paytrace.patterns.command.MatchCommand.paymentDAO;

public class ReconciliationController {

    @FXML private Label totalInvoicesLabel;
    @FXML private Label totalPaymentsLabel;
    @FXML private Label matchedLabel;
    @FXML private Label refundedLabel;

    @FXML private TextField amountToleranceField;
    @FXML private TextField dateToleranceField;
    @FXML private Button    runButton;
    @FXML private Button    undoButton;
    @FXML private Button    redoButton;
    @FXML private ProgressIndicator progressSpinner;
    @FXML private TextArea  logArea;

    /**
     * Engine orchestration is fully encapsulated in the service layer
     * (UC-04 / UC-05 / UC-06). This controller's only job is to build a
     * thread-safe log callback, hand it to the service on a worker thread,
     * and update the UI when the run finishes.
     */
    private final ReconciliationService reconciliationService = new ReconciliationService();
    private final InvoiceService        invoiceService        = new InvoiceService();
    private final PaymentService        paymentService        = new PaymentService();

    /** Used directly for undo / redo — Command pattern lives outside the service. */
    private final CommandHistory commandHistory = CommandHistory.getInstance();

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();


        refreshKPIs();
        refreshUndoRedoButtons();
        appendLog("Engine ready. Configure tolerances and click 'Run Reconciliation' to start.\n");
    }

    private void refreshKPIs() {
        try {
            String vendorId = SessionManager.getInstance().getContextId();
            if (vendorId == null) return;

            int openInv = 0;
            for (Invoice i : invoiceService.findByOwner(vendorId)) {
                if (i.getRemainingBalance() != null
                        && i.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0) openInv++;
            }
            int unmatched = (int) paymentService.findByOwner(vendorId).stream()
                    .filter(p -> p.getStatus() != PaymentStatus.FULLY_ALLOCATED)
                    .count();

            totalInvoicesLabel.setText(String.valueOf(openInv));
            totalPaymentsLabel.setText(String.valueOf(unmatched));
        } catch (Exception e) {
            appendLog("[KPI ERROR] " + e.getMessage() + "\n");
        }
    }

    @FXML
    private void runReconciliation() throws SQLException {
        BigDecimal amtTol;
        int dateTol;
        try {
            amtTol  = new BigDecimal(amountToleranceField.getText().trim());
            dateTol = Integer.parseInt(dateToleranceField.getText().trim());
        } catch (Exception e) {
            warn("Tolerance values must be numeric.");
            return;
        }

        runButton.setDisable(true);
        progressSpinner.setVisible(true);
        logArea.clear();
        appendLog("════════════════════════════════════════════\n");
        appendLog("  RECONCILIATION ENGINE — STARTED\n");
        appendLog("  " + LocalDateTime.now() + "\n");
        appendLog("════════════════════════════════════════════\n\n");

        final BigDecimal amtTolFinal = amtTol;
        final int dateTolFinal = dateTol;
        final String vendorId = SessionManager.getInstance().getContextId();
        if (vendorId == null) {
            warn("No vendor context — cannot run reconciliation.");
            runButton.setDisable(false);
            progressSpinner.setVisible(false);
            return;
        }
        int unmatched = invoiceDAO.countUnreconciled(vendorId);
        int unmatchedPay = paymentDAO.countByStatus(vendorId, PaymentStatus.UNMATCHED);
        if (unmatched == 0 && unmatchedPay == 0) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Nothing to reconcile — all current invoices and payments are already matched. "
                            + "Import new data or wait for new payments before running again.")
                    .showAndWait();
            return;
        }
        // Worker thread + service call. The service knows nothing about JavaFX;
        // we hand it a thread-safe log callback that marshals onto the FX thread.
        Thread worker = new Thread(() -> {
            try {
                ReconciliationService.RunSummary s = reconciliationService.runAutoReconciliation(
                        vendorId, amtTolFinal, dateTolFinal,
                        msg -> Platform.runLater(() -> appendLog(msg)));

                Platform.runLater(() -> {
                    appendLog(
                            "\n════════════════════════════════════════════\n"
                                    + "  RECONCILIATION COMPLETE\n"
                                    + "  Matched: " + s.matchedCount + "\n"
                                    + "  Overpayments refunded: " + s.refundedOverpaymentCount + "\n"
                                    + "════════════════════════════════════════════\n");
                    finishRun(s.matchedCount, s.refundedOverpaymentCount);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    appendLog("\n[FATAL] " + ex.getMessage() + "\n");
                    runButton.setDisable(false);
                    progressSpinner.setVisible(false);
                });
            }
        }, "recon-engine");
        worker.setDaemon(true);
        worker.start();
    }

    private void finishRun(int matched, int refunded) {
        runButton.setDisable(false);
        progressSpinner.setVisible(false);
        matchedLabel.setText(String.valueOf(matched));
        refundedLabel.setText(String.valueOf(refunded));
        refreshKPIs();
        refreshUndoRedoButtons();
    }

    @FXML
    private void undoLast() {
        try {
            String desc = commandHistory.undoLast();
            appendLog("\n[UNDO] " + desc + "\n");
            refreshKPIs();
            refreshUndoRedoButtons();
        } catch (Exception e) {
            warn("Undo failed: " + e.getMessage());
        }
    }

    @FXML
    private void redoLast() {
        try {
            String desc = commandHistory.redoLast();
            appendLog("\n[REDO] " + desc + "\n");
            refreshKPIs();
            refreshUndoRedoButtons();
        } catch (Exception e) {
            warn("Redo failed: " + e.getMessage());
        }
    }

    private void refreshUndoRedoButtons() {
        undoButton.setDisable(!commandHistory.canUndo());
        redoButton.setDisable(!commandHistory.canRedo());
    }

    private void appendLog(String msg) {
        logArea.appendText(msg);
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    private void warn(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    @FXML
    private void goBack() {
        try {SceneNavigator.loadIntoShell(SceneNavigator.DASHBOARD_HOME); }
        catch (Exception e) { e.printStackTrace(); }
    }
}