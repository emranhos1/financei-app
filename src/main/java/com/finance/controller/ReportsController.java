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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReportsController {
    private final SessionContext sessionContext;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    @FXML private ComboBox<String> periodComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpenseLabel;
    @FXML private Label netIncomeLabel;

    @FXML private TableView<CategoryRow> categoryTable;
    @FXML private TableColumn<CategoryRow, String> categoryNameColumn;
    @FXML private TableColumn<CategoryRow, BigDecimal> categoryAmountColumn;

    @FXML private TableView<AccountRow> accountTable;
    @FXML private TableColumn<AccountRow, String> accountNameColumn;
    @FXML private TableColumn<AccountRow, String> accountTypeColumn;
    @FXML private TableColumn<AccountRow, BigDecimal> accountBalanceColumn;

    @FXML private ComboBox<Account> accountLogComboBox;
    @FXML private TableView<AccountLog> accountLogTable;
    @FXML private TableColumn<AccountLog, LocalDateTime> logDateColumn;
    @FXML private TableColumn<AccountLog, String> logChangeTypeColumn;
    @FXML private TableColumn<AccountLog, String> logRefTypeColumn;
    @FXML private TableColumn<AccountLog, BigDecimal> logAmountColumn;
    @FXML private TableColumn<AccountLog, BigDecimal> logBalanceBeforeColumn;
    @FXML private TableColumn<AccountLog, BigDecimal> logBalanceAfterColumn;
    @FXML private TableColumn<AccountLog, String> logNoteColumn;

    @FXML
    public void initialize() {
        periodComboBox.setItems(FXCollections.observableArrayList("Today", "This Month", "This Year", "Custom"));
        periodComboBox.setValue("This Month");
        periodComboBox.setOnAction(e -> loadReport());

        categoryNameColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        categoryAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        accountNameColumn.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        accountTypeColumn.setCellValueFactory(new PropertyValueFactory<>("accountType"));
        accountBalanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));

        logDateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        logAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        logBalanceBeforeColumn.setCellValueFactory(new PropertyValueFactory<>("balanceBefore"));
        logBalanceAfterColumn.setCellValueFactory(new PropertyValueFactory<>("balanceAfter"));
        logNoteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));
        logChangeTypeColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getChangeType().name()));
        logRefTypeColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getReferenceType().name()));

        accountLogComboBox.setConverter(new StringConverter<Account>() {
            public String toString(Account a) { return a == null ? "" : a.getName(); }
            public Account fromString(String s) { return null; }
        });
        accountLogComboBox.setOnAction(e -> loadAccountLog());

        loadReport();
        loadAccountLogDropdown();
    }

    @FXML
    public void handleGenerateReport() { loadReport(); }

    private void loadReport() {
        Long userId = sessionContext.getCurrentUserId();
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;

        switch (periodComboBox.getValue()) {
            case "Today":      startDate = today; break;
            case "This Month": startDate = today.withDayOfMonth(1); break;
            case "This Year":  startDate = today.withDayOfYear(1); break;
            case "Custom":
                startDate = startDatePicker.getValue();
                endDate = endDatePicker.getValue();
                if (startDate == null || endDate == null) { showAlert("Error", "Please select start and end dates"); return; }
                break;
            default: startDate = today.withDayOfMonth(1);
        }

        BigDecimal totalIncome = transactionService.getTotalIncome(userId, startDate, endDate);
        BigDecimal totalExpense = transactionService.getTotalExpense(userId, startDate, endDate);
        totalIncomeLabel.setText(String.format("৳ %.2f", totalIncome));
        totalExpenseLabel.setText(String.format("৳ %.2f", totalExpense));
        netIncomeLabel.setText(String.format("৳ %.2f", totalIncome.subtract(totalExpense)));

        List<Category> expenseCategories = categoryService.getExpenseCategories(userId);
        List<CategoryRow> categoryRows = new ArrayList<>();
        for (Category cat : expenseCategories) {
            BigDecimal amount = transactionService.getExpenseByCategory(userId, cat.getId(), startDate, endDate);
            categoryRows.add(new CategoryRow(cat.getName(), amount));
        }
        categoryTable.setItems(FXCollections.observableArrayList(categoryRows));

        List<Account> accounts = accountService.getAccountsByUserId(userId);
        List<AccountRow> accountRows = new ArrayList<>();
        for (Account acc : accounts) {
            accountRows.add(new AccountRow(acc.getName(), acc.getAccountType().getName(), acc.getBalance()));
        }
        accountTable.setItems(FXCollections.observableArrayList(accountRows));
    }

    private void loadAccountLogDropdown() {
        List<Account> accounts = accountService.getAccountsByUserId(sessionContext.getCurrentUserId());
        accountLogComboBox.setItems(FXCollections.observableArrayList(accounts));
    }

    private void loadAccountLog() {
        Account selected = accountLogComboBox.getValue();
        if (selected == null) { accountLogTable.setItems(FXCollections.observableArrayList()); return; }
        List<AccountLog> logs = accountService.getAccountLogs(selected.getId());
        accountLogTable.setItems(FXCollections.observableArrayList(logs));
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