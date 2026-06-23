package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.entity.Account;
import com.finance.entity.Category;
import com.finance.service.AccountService;
import com.finance.service.CategoryService;
import com.finance.service.TransactionService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ReportsController {
    private final SessionContext sessionContext;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    @FXML
    private ComboBox<String> periodComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Label totalIncomeLabel;

    @FXML
    private Label totalExpenseLabel;

    @FXML
    private Label netIncomeLabel;

    @FXML
    private TableView<CategoryExpense> categoryTable;

    @FXML
    private TableColumn<CategoryExpense, String> categoryNameColumn;

    @FXML
    private TableColumn<CategoryExpense, BigDecimal> categoryAmountColumn;

    @FXML
    private TableView<AccountBalance> accountTable;

    @FXML
    private TableColumn<AccountBalance, String> accountNameColumn;

    @FXML
    private TableColumn<AccountBalance, String> accountTypeColumn;

    @FXML
    private TableColumn<AccountBalance, BigDecimal> accountBalanceColumn;

    @FXML
    public void initialize() {
        setupComboBox();
        setupTables();
        loadReport();
    }

    private void setupComboBox() {
        periodComboBox.setItems(FXCollections.observableArrayList("Today", "This Month", "This Year", "Custom"));
        periodComboBox.setValue("This Month");
        periodComboBox.setOnAction(e -> loadReport());
    }

    private void setupTables() {
        categoryNameColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        categoryAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        accountNameColumn.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        accountTypeColumn.setCellValueFactory(new PropertyValueFactory<>("accountType"));
        accountBalanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
    }

    @FXML
    public void handleGenerateReport() {
        loadReport();
    }

    private void loadReport() {
        Long userId = sessionContext.getCurrentUserId();
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = today;

        String period = periodComboBox.getValue();
        switch (period) {
            case "Today":
                startDate = today;
                break;
            case "This Month":
                startDate = today.withDayOfMonth(1);
                break;
            case "This Year":
                startDate = today.withDayOfYear(1);
                break;
            case "Custom":
                startDate = startDatePicker.getValue();
                endDate = endDatePicker.getValue();
                if (startDate == null || endDate == null) {
                    showAlert("Error", "Please select start and end dates");
                    return;
                }
                break;
            default:
                startDate = today.withDayOfMonth(1);
        }

        // Get income and expense
        BigDecimal totalIncome = transactionService.getTotalIncome(userId, startDate, endDate);
        BigDecimal totalExpense = transactionService.getTotalExpense(userId, startDate, endDate);
        BigDecimal netIncome = totalIncome.subtract(totalExpense);

        totalIncomeLabel.setText(String.format("৳ %.2f", totalIncome));
        totalExpenseLabel.setText(String.format("৳ %.2f", totalExpense));
        netIncomeLabel.setText(String.format("৳ %.2f", netIncome));

        // Load category breakdown
        List<Category> expenseCategories = categoryService.getExpenseCategories(userId);
        var categoryExpenses = new java.util.ArrayList<CategoryExpense>();
        for (Category cat : expenseCategories) {
            BigDecimal amount = transactionService.getExpenseByCategory(userId, cat.getId(), startDate, endDate);
            categoryExpenses.add(new CategoryExpense(cat.getName(), amount));
        }
        categoryTable.setItems(FXCollections.observableArrayList(categoryExpenses));

        // Load account balances
        List<Account> accounts = accountService.getAccountsByUserId(userId);
        var accountBalances = new java.util.ArrayList<AccountBalance>();
        for (Account acc : accounts) {
            accountBalances.add(new AccountBalance(acc.getName(), acc.getType().toString(), acc.getBalance()));
        }
        accountTable.setItems(FXCollections.observableArrayList(accountBalances));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class CategoryExpense {
        private final String categoryName;
        private final BigDecimal amount;

        public CategoryExpense(String categoryName, BigDecimal amount) {
            this.categoryName = categoryName;
            this.amount = amount;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }

    public static class AccountBalance {
        private final String accountName;
        private final String accountType;
        private final BigDecimal balance;

        public AccountBalance(String accountName, String accountType, BigDecimal balance) {
            this.accountName = accountName;
            this.accountType = accountType;
            this.balance = balance;
        }

        public String getAccountName() {
            return accountName;
        }

        public String getAccountType() {
            return accountType;
        }

        public BigDecimal getBalance() {
            return balance;
        }
    }
}
