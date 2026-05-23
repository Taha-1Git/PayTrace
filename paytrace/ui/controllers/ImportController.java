package com.paytrace.ui.controllers;

import com.paytrace.models.Payment;
import com.paytrace.services.ImportService;
import com.paytrace.services.ImportService;
import com.paytrace.ui.utils.SceneNavigator;
import com.paytrace.ui.utils.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportController {



    @FXML private TextField bankFileField;
    @FXML private TableView<String[]> bankPreviewTable;
    @FXML private TableColumn<String[], String> bankTxnCol;
    @FXML private TableColumn<String[], String> bankSenderCol;
    @FXML private TableColumn<String[], String> bankCounterpartyCol;
    @FXML private TableColumn<String[], String> bankAmountCol;
    @FXML private TableColumn<String[], String> bankDateCol;
    @FXML private Label bankStatusLabel;

    @FXML private VBox  resultsSection;
    @FXML private Label successCountLabel;
    @FXML private Label failureCountLabel;
    @FXML private Label resultMessageLabel;

    private File selectedFile;
    private List<String[]> rawRows = new ArrayList<>();
    /** Header column name (lowercased) → column index in the row array. */
    private Map<String, Integer> columnIndex = new HashMap<>();

    /** Single business-logic dependency — handles validation, persistence,
     *  and ImportSession bookkeeping in one call. */
    private final ImportService importService = new ImportService();

    @FXML
    public void initialize() {
        SessionManager sm = SessionManager.getInstance();

        // Wire preview columns generically by index
        bankTxnCol.setCellValueFactory(d -> cellAt(d.getValue(), "transaction id", "txn id", "txn"));
        bankSenderCol.setCellValueFactory(d -> cellAt(d.getValue(), "sender account", "from account", "account"));
        bankCounterpartyCol.setCellValueFactory(d -> cellAt(d.getValue(), "counterparty", "payer", "vendor name", "name"));
        bankAmountCol.setCellValueFactory(d -> cellAt(d.getValue(), "amount paid", "amount"));
        bankDateCol.setCellValueFactory(d -> cellAt(d.getValue(), "payment date", "date"));
    }

    private javafx.beans.value.ObservableValue<String> cellAt(String[] row, String... aliases) {
        for (String alias : aliases) {
            Integer idx = columnIndex.get(alias);
            if (idx != null && idx < row.length) {
                return new SimpleStringProperty(row[idx]);
            }
        }
        return new SimpleStringProperty("");
    }

    @FXML
    private void browseBankFile() {
        File file = openFileChooser();
        if (file == null) return;

        selectedFile = file;
        bankFileField.setText(file.getName());
        rawRows = readFile(file);
        if (rawRows.isEmpty()) {
            bankStatusLabel.setText("File is empty.");
            bankStatusLabel.setStyle("-fx-text-fill: #dc2626;");
            return;
        }

        // First row is the header — index columns by name (lowercased)
        columnIndex.clear();
        String[] header = rawRows.get(0);
        for (int i = 0; i < header.length; i++) {
            String h = header[i] == null ? "" : header[i].trim().toLowerCase();
            columnIndex.put(h, i);
        }

        // Show first 10 data rows in preview
        ObservableList<String[]> preview = FXCollections.observableArrayList();
        for (int i = 1; i < Math.min(11, rawRows.size()); i++) preview.add(rawRows.get(i));
        bankPreviewTable.setItems(preview);

        bankStatusLabel.setText((rawRows.size() - 1) + " data rows detected. " +
                "Review the preview, then click Import.");
        bankStatusLabel.setStyle("-fx-text-fill: #2563eb;");
    }

    @FXML
    private void importBankStatement() {
        if (selectedFile == null) {
            warn("Select a file first.");
            return;
        }

        SessionManager sm = SessionManager.getInstance();
        String vendorId = sm.getContextId();
        if (vendorId == null) {
            warn("No vendor context. Log in as Administrator with your vendor code.");
            return;
        }
        if (!sm.isAdmin() && !sm.isAdministrator()) {
            warn("Only Admin or Administrator can import bank statements.");
            return;
        }

        // Validate required columns are present
        if (!columnIndex.containsKey("transaction id") && !columnIndex.containsKey("txn id")) {
            warn("CSV must have a 'Transaction ID' column."); return;
        }
        if (!columnIndex.containsKey("amount paid") && !columnIndex.containsKey("amount")) {
            warn("CSV must have an 'Amount Paid' column."); return;
        }
        if (!columnIndex.containsKey("payment date") && !columnIndex.containsKey("date")) {
            warn("CSV must have a 'Payment Date' column."); return;
        }
        if (!columnIndex.containsKey("sender account")
                && !columnIndex.containsKey("from account")
                && !columnIndex.containsKey("account")) {
            warn("CSV must have a 'Sender Account' column. " +
                    "This is what reconciliation matches against each user's account number.");
            return;
        }

        // Parse rows into Payment drafts. The controller owns parsing
        // (it's a UI/file-format concern); the service owns validation,
        // persistence, and ImportSession bookkeeping.
        List<Payment> drafts = new ArrayList<>();
        int parseFailures = 0;
        for (int i = 1; i < rawRows.size(); i++) {
            String[] row = rawRows.get(i);
            try {
                String txnId  = pick(row, "transaction id", "txn id", "txn");
                String sender = pick(row, "sender account", "from account", "account");
                String party  = pick(row, "counterparty", "payer", "vendor name", "name");
                String amtStr = pick(row, "amount paid", "amount");
                String dtStr  = pick(row, "payment date", "date");

                Payment pay = new Payment();
                pay.setTransactionId(txnId);
                pay.setSenderAccount(sender);
                pay.setCounterparty(party);
                pay.setNormalizedParty(party == null ? null : party.toLowerCase());
                if (!amtStr.isEmpty()) {
                    pay.setAmountPaid(new BigDecimal(amtStr));
                    pay.setUnallocatedAmount(new BigDecimal(amtStr));
                }
                if (!dtStr.isEmpty()) pay.setPaymentDate(parseDate(dtStr));
                drafts.add(pay);
            } catch (Exception ex) {
                parseFailures++;
            }
        }

        try {
            String performedBy = sm.getCurrentUserId();
            ImportService.ImportResult result = importService.importBankStatement(
                    vendorId, drafts, selectedFile.getName(), performedBy);
            showResults(result.successCount, result.failureCount + parseFailures);
        } catch (Exception ex) {
            warn("Import failed: " + ex.getMessage());
        }
    }

    private String pick(String[] row, String... aliases) {
        for (String alias : aliases) {
            Integer idx = columnIndex.get(alias);
            if (idx != null && idx < row.length && row[idx] != null)
                return row[idx].trim();
        }
        return "";
    }

    // ── File reading ──

    private File openFileChooser() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Bank Statement");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Excel/CSV", "*.xlsx", "*.xls", "*.csv"));
        return fc.showOpenDialog(null);
    }

    private List<String[]> readFile(File file) {
        List<String[]> rows = new ArrayList<>();
        try {
            if (file.getName().toLowerCase().endsWith(".csv")) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        rows.add(parseCsvLine(line));
                    }
                }
            } else {
                try (FileInputStream fis = new FileInputStream(file);
                     Workbook wb = WorkbookFactory.create(fis)) {
                    Sheet sheet = wb.getSheetAt(0);
                    for (Row row : sheet) {
                        int last = row.getLastCellNum();
                        if (last <= 0) continue;
                        String[] cells = new String[last];
                        for (int c = 0; c < last; c++) {
                            Cell cell = row.getCell(c);
                            cells[c] = cell == null ? "" : cell.toString().trim();
                        }
                        rows.add(cells);
                    }
                }
            }
        } catch (Exception e) {
            warn("Read error: " + e.getMessage());
        }
        return rows;
    }

    /** Splits a CSV line, respecting double-quoted values. */
    private String[] parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"'); i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString().trim());
        return out.toArray(new String[0]);
    }

    private LocalDate parseDate(String s) {
        String[] formats = { "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy",
                "dd-MM-yyyy", "MM-dd-yyyy" };
        for (String fmt : formats) {
            try { return LocalDate.parse(s, DateTimeFormatter.ofPattern(fmt)); }
            catch (Exception ignored) {}
        }
        return LocalDate.now();
    }

    // saveImportSession() removed — ImportService now writes the session record
    // automatically as part of importBankStatement().

    // ── Result display ──

    private void showResults(int success, int fail) {
        resultsSection.setVisible(true);
        resultsSection.setManaged(true);
        successCountLabel.setText(String.valueOf(success));
        failureCountLabel.setText(String.valueOf(fail));
        resultMessageLabel.setText("Imported " + success + " payment(s)" +
                (fail > 0 ? " — " + fail + " row(s) failed validation." : "."));
        resultMessageLabel.setStyle(fail == 0
                ? "-fx-text-fill: #16a34a;" : "-fx-text-fill: #d97706;");
        bankStatusLabel.setText("Done.");
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