package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.entity.Account;
import com.finance.entity.AccountLog;
import com.finance.entity.Category;
import com.finance.service.AccountService;
import com.finance.service.CategoryService;
import com.finance.service.TransactionService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ReportsController {
    private final SessionContext sessionContext;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpenseLabel;
    @FXML private Label netIncomeLabel;

    @FXML private LineChart<String, Number> trendChart;

    @FXML private DatePicker compareAStartPicker;
    @FXML private DatePicker compareAEndPicker;
    @FXML private DatePicker compareBStartPicker;
    @FXML private DatePicker compareBEndPicker;
    @FXML private Label compareIncomeALabel;
    @FXML private Label compareIncomeBLabel;
    @FXML private Label compareIncomeChangeLabel;
    @FXML private Label compareExpenseALabel;
    @FXML private Label compareExpenseBLabel;
    @FXML private Label compareExpenseChangeLabel;
    @FXML private Label compareNetALabel;
    @FXML private Label compareNetBLabel;
    @FXML private Label compareNetChangeLabel;

    @FXML private TableView<CategoryRow> expenseCategoryTable;
    @FXML private TableColumn<CategoryRow, String> expCategoryNameColumn;
    @FXML private TableColumn<CategoryRow, BigDecimal> expCategoryAmountColumn;

    @FXML private TableView<CategoryRow> incomeCategoryTable;
    @FXML private TableColumn<CategoryRow, String> incCategoryNameColumn;
    @FXML private TableColumn<CategoryRow, BigDecimal> incCategoryAmountColumn;

    @FXML private TableView<AccountRow> accountTable;
    @FXML private TableColumn<AccountRow, String> accountNameColumn;
    @FXML private TableColumn<AccountRow, String> accountTypeColumn;
    @FXML private TableColumn<AccountRow, BigDecimal> accountBalanceColumn;

    @FXML private MenuButton accountLogMenuButton;
    @FXML private TableView<AccountLog> accountLogTable;
    @FXML private TableColumn<AccountLog, String> logDateColumn;
    @FXML private TableColumn<AccountLog, String> logChangeTypeColumn;
    @FXML private TableColumn<AccountLog, String> logRefTypeColumn;
    @FXML private TableColumn<AccountLog, BigDecimal> logAmountColumn;
    @FXML private TableColumn<AccountLog, BigDecimal> logBalanceBeforeColumn;
    @FXML private TableColumn<AccountLog, BigDecimal> logBalanceAfterColumn;
    @FXML private TableColumn<AccountLog, String> logNoteColumn;
    @FXML private Button logPrevPageBtn;
    @FXML private Button logNextPageBtn;
    @FXML private Label logPageLabel;
    @FXML private ComboBox<Integer> logPageSizeComboBox;

    private List<Account> allAccounts = new ArrayList<>();
    private final Map<Long, CheckMenuItem> accountLogCheckItems = new LinkedHashMap<>();
    private CheckMenuItem allAccountsLogItem;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<Integer> PAGE_SIZE_OPTIONS = Arrays.asList(5, 10, 20, 50, 100);
    private int logCurrentPage = 0;
    private int logTotalPages = 1;

    @FXML
    public void initialize() {
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today.withDayOfMonth(1));
        endDatePicker.setValue(today.withDayOfMonth(today.lengthOfMonth()));

        expCategoryNameColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        expCategoryAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        incCategoryNameColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        incCategoryAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        accountNameColumn.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        accountTypeColumn.setCellValueFactory(new PropertyValueFactory<>("accountType"));
        accountBalanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));

        logDateColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCreatedAt().format(DATE_FORMAT)));
        logAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        logBalanceBeforeColumn.setCellValueFactory(new PropertyValueFactory<>("balanceBefore"));
        logBalanceAfterColumn.setCellValueFactory(new PropertyValueFactory<>("balanceAfter"));
        logNoteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));
        logChangeTypeColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getChangeType().name()));
        logRefTypeColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getReferenceType().name()));

        logPageSizeComboBox.setItems(FXCollections.observableArrayList(PAGE_SIZE_OPTIONS));
        logPageSizeComboBox.setValue(20);
        logPageSizeComboBox.setOnAction(e -> loadAccountLogPage(0));

        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate lastMonth = today.minusMonths(1);
        compareAStartPicker.setValue(lastMonth.withDayOfMonth(1));
        compareAEndPicker.setValue(lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()));
        compareBStartPicker.setValue(thisMonthStart);
        compareBEndPicker.setValue(today.withDayOfMonth(today.lengthOfMonth()));

        loadReport();
        loadAccountLogDropdown();
        loadTrendChart();
        handleCompare();
    }

    @FXML
    public void handleCompare() {
        LocalDate aStart = compareAStartPicker.getValue();
        LocalDate aEnd = compareAEndPicker.getValue();
        LocalDate bStart = compareBStartPicker.getValue();
        LocalDate bEnd = compareBEndPicker.getValue();
        if (aStart == null || aEnd == null || bStart == null || bEnd == null) {
            showAlert("Error", "Please select both periods' start and end dates"); return;
        }

        Long userId = sessionContext.getCurrentUserId();
        BigDecimal incomeA = transactionService.getTotalIncome(userId, aStart, aEnd);
        BigDecimal expenseA = transactionService.getTotalExpense(userId, aStart, aEnd);
        BigDecimal netA = incomeA.subtract(expenseA);

        BigDecimal incomeB = transactionService.getTotalIncome(userId, bStart, bEnd);
        BigDecimal expenseB = transactionService.getTotalExpense(userId, bStart, bEnd);
        BigDecimal netB = incomeB.subtract(expenseB);

        compareIncomeALabel.setText(String.format("৳ %.2f", incomeA));
        compareIncomeBLabel.setText(String.format("৳ %.2f", incomeB));
        compareIncomeChangeLabel.setText(formatChange(incomeA, incomeB));

        compareExpenseALabel.setText(String.format("৳ %.2f", expenseA));
        compareExpenseBLabel.setText(String.format("৳ %.2f", expenseB));
        compareExpenseChangeLabel.setText(formatChange(expenseA, expenseB));

        compareNetALabel.setText(String.format("৳ %.2f", netA));
        compareNetBLabel.setText(String.format("৳ %.2f", netB));
        compareNetChangeLabel.setText(formatChange(netA, netB));
    }

    private String formatChange(BigDecimal from, BigDecimal to) {
        if (from.compareTo(BigDecimal.ZERO) == 0) {
            return to.compareTo(BigDecimal.ZERO) == 0 ? "0%" : "N/A";
        }
        BigDecimal percent = to.subtract(from).divide(from.abs(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        String sign = percent.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
        return sign + percent.setScale(1, java.math.RoundingMode.HALF_UP) + "%";
    }

    private void loadTrendChart() {
        Long userId = sessionContext.getCurrentUserId();
        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Income");
        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Expense");

        LocalDate now = LocalDate.now();
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MMM yyyy");
        for (int i = 11; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            LocalDate monthStart = monthDate.withDayOfMonth(1);
            LocalDate monthEnd = monthDate.withDayOfMonth(monthDate.lengthOfMonth());
            String label = monthDate.format(monthFormat);

            BigDecimal income = transactionService.getTotalIncome(userId, monthStart, monthEnd);
            BigDecimal expense = transactionService.getTotalExpense(userId, monthStart, monthEnd);
            incomeSeries.getData().add(new XYChart.Data<>(label, income));
            expenseSeries.getData().add(new XYChart.Data<>(label, expense));
        }
        trendChart.getData().setAll(incomeSeries, expenseSeries);
        if (incomeSeries.getNode() != null) incomeSeries.getNode().setStyle("-fx-stroke: #E74C3C; -fx-stroke-width: 2;");
        if (expenseSeries.getNode() != null) expenseSeries.getNode().setStyle("-fx-stroke: #F39C12; -fx-stroke-width: 2; -fx-stroke-dash-array: 6 4;");
        for (XYChart.Data<String, Number> data : incomeSeries.getData()) {
            if (data.getNode() != null) data.getNode().setStyle("-fx-background-color: #E74C3C, white;");
        }
        for (XYChart.Data<String, Number> data : expenseSeries.getData()) {
            if (data.getNode() != null) data.getNode().setStyle("-fx-background-color: #F39C12, white;");
        }
    }

    @FXML
    public void handleGenerateReport() { loadReport(); }

    private void loadReport() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        if (startDate == null || endDate == null) {
            showAlert("Error", "Please select start and end dates"); return;
        }
        Long userId = sessionContext.getCurrentUserId();

        BigDecimal totalIncome = transactionService.getTotalIncome(userId, startDate, endDate);
        BigDecimal totalExpense = transactionService.getTotalExpense(userId, startDate, endDate);
        totalIncomeLabel.setText(String.format("৳ %.2f", totalIncome));
        totalExpenseLabel.setText(String.format("৳ %.2f", totalExpense));
        netIncomeLabel.setText(String.format("৳ %.2f", totalIncome.subtract(totalExpense)));

        List<CategoryRow> expenseRows = new ArrayList<>();
        for (Category cat : categoryService.getExpenseCategories(userId)) {
            BigDecimal amount = transactionService.getExpenseByCategory(userId, cat.getId(), startDate, endDate);
            if (amount.compareTo(BigDecimal.ZERO) != 0) expenseRows.add(new CategoryRow(cat.getName(), amount));
        }
        expenseCategoryTable.setItems(FXCollections.observableArrayList(expenseRows));

        List<CategoryRow> incomeRows = new ArrayList<>();
        for (Category cat : categoryService.getIncomeCategories(userId)) {
            BigDecimal amount = transactionService.getIncomeByCategory(userId, cat.getId(), startDate, endDate);
            if (amount.compareTo(BigDecimal.ZERO) != 0) incomeRows.add(new CategoryRow(cat.getName(), amount));
        }
        incomeCategoryTable.setItems(FXCollections.observableArrayList(incomeRows));

        List<AccountRow> accountRows = new ArrayList<>();
        for (Account acc : accountService.getAccountsByUserId(userId)) {
            if (acc.getBalance().compareTo(BigDecimal.ZERO) != 0)
                accountRows.add(new AccountRow(acc.getName(), acc.getAccountType().getName(), acc.getBalance()));
        }
        accountTable.setItems(FXCollections.observableArrayList(accountRows));
    }

    private void loadAccountLogDropdown() {
        allAccounts = accountService.getAccountsByUserId(sessionContext.getCurrentUserId());
        accountLogMenuButton.getItems().clear();
        accountLogCheckItems.clear();

        allAccountsLogItem = new CheckMenuItem("All Accounts");
        allAccountsLogItem.setSelected(true);
        allAccountsLogItem.setOnAction(e -> {
            boolean select = allAccountsLogItem.isSelected();
            for (CheckMenuItem item : accountLogCheckItems.values()) item.setSelected(select);
            onAccountLogSelectionChanged();
        });
        accountLogMenuButton.getItems().add(allAccountsLogItem);
        accountLogMenuButton.getItems().add(new SeparatorMenuItem());

        for (Account acc : allAccounts) {
            CheckMenuItem item = new CheckMenuItem(acc.getName());
            item.setSelected(true);
            item.setOnAction(e -> {
                if (!item.isSelected()) allAccountsLogItem.setSelected(false);
                else if (accountLogCheckItems.values().stream().allMatch(CheckMenuItem::isSelected)) allAccountsLogItem.setSelected(true);
                onAccountLogSelectionChanged();
            });
            accountLogCheckItems.put(acc.getId(), item);
            accountLogMenuButton.getItems().add(item);
        }

        onAccountLogSelectionChanged();
    }

    private List<Account> getSelectedLogAccounts() {
        List<Account> selected = new ArrayList<>();
        for (Account acc : allAccounts) {
            CheckMenuItem item = accountLogCheckItems.get(acc.getId());
            if (item != null && item.isSelected()) selected.add(acc);
        }
        return selected;
    }

    private void onAccountLogSelectionChanged() {
        List<Account> selected = getSelectedLogAccounts();
        if (selected.isEmpty()) {
            accountLogMenuButton.setText("Select accounts");
        } else if (selected.size() == allAccounts.size()) {
            accountLogMenuButton.setText("All Accounts");
        } else if (selected.size() == 1) {
            accountLogMenuButton.setText(selected.get(0).getName());
        } else {
            accountLogMenuButton.setText(selected.size() + " accounts selected");
        }
        loadAccountLogPage(0);
    }

    private void loadAccountLogPage(int page) {
        List<Account> selected = getSelectedLogAccounts();
        if (selected.isEmpty()) {
            accountLogTable.setItems(FXCollections.observableArrayList());
            logCurrentPage = 0;
            logTotalPages = 1;
            logPageLabel.setText("Page 0 of 0 (0 total)");
            logPrevPageBtn.setDisable(true);
            logNextPageBtn.setDisable(true);
            return;
        }
        List<Long> accountIds = new ArrayList<>();
        for (Account acc : selected) accountIds.add(acc.getId());

        int pageSize = logPageSizeComboBox.getValue() != null ? logPageSizeComboBox.getValue() : 20;
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<AccountLog> result = accountService.getAccountLogsPage(accountIds, pageable);

        logCurrentPage = result.getNumber();
        logTotalPages = Math.max(result.getTotalPages(), 1);
        accountLogTable.setItems(FXCollections.observableArrayList(result.getContent()));
        logPageLabel.setText("Page " + (logCurrentPage + 1) + " of " + logTotalPages + " (" + result.getTotalElements() + " total)");
        logPrevPageBtn.setDisable(logCurrentPage <= 0);
        logNextPageBtn.setDisable(logCurrentPage >= logTotalPages - 1);
    }

    @FXML
    public void handleLogPrevPage() {
        if (logCurrentPage > 0) loadAccountLogPage(logCurrentPage - 1);
    }

    @FXML
    public void handleLogNextPage() {
        if (logCurrentPage < logTotalPages - 1) loadAccountLogPage(logCurrentPage + 1);
    }

    @FXML
    public void handleExportCSV() {
        List<Account> selectedAccounts = getSelectedLogAccounts();
        if (selectedAccounts.isEmpty()) { showAlert("Error", "Select at least one account to export"); return; }

        Map<String, List<AccountLog>> exportMap = new LinkedHashMap<>();
        for (Account acc : selectedAccounts) {
            List<AccountLog> logs = accountService.getAccountLogs(acc.getId());
            if (!logs.isEmpty()) exportMap.put(acc.getName(), logs);
        }
        if (exportMap.isEmpty()) { showAlert("Error", "No log data found"); return; }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Account Log");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName(selectedAccounts.size() == 1 ? selectedAccounts.get(0).getName() + "_log.csv" : "accounts_log.csv");
        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        try (FileWriter fw = new FileWriter(file)) {
            for (Map.Entry<String, List<AccountLog>> entry : exportMap.entrySet()) {
                fw.write("Account: " + entry.getKey() + "\n");
                fw.write("Date,Change,Reference,Amount,Balance Before,Balance After,Note\n");
                for (AccountLog l : entry.getValue()) {
                    fw.write(l.getCreatedAt().format(DATE_FORMAT) + ","
                            + l.getChangeType().name() + ","
                            + l.getReferenceType().name() + ","
                            + l.getAmount() + ","
                            + l.getBalanceBefore() + ","
                            + l.getBalanceAfter() + ","
                            + (l.getNote() != null ? l.getNote().replace(",", ";") : "") + "\n");
                }
                fw.write("\n");
            }
            showAlert("Success", "Exported to " + file.getName());
        } catch (IOException e) {
            showAlert("Error", "Failed to export: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait();
    }

    public static class CategoryRow {
        private final String categoryName;
        private final BigDecimal amount;
        public CategoryRow(String categoryName, BigDecimal amount) { this.categoryName = categoryName; this.amount = amount; }
        public String getCategoryName() { return categoryName; }
        public BigDecimal getAmount() { return amount; }
    }

    public static class AccountRow {
        private final String accountName;
        private final String accountType;
        private final BigDecimal balance;
        public AccountRow(String accountName, String accountType, BigDecimal balance) {
            this.accountName = accountName; this.accountType = accountType; this.balance = balance;
        }
        public String getAccountName() { return accountName; }
        public String getAccountType() { return accountType; }
        public BigDecimal getBalance() { return balance; }
    }
}