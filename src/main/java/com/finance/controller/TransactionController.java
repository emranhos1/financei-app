package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.entity.Account;
import com.finance.entity.Category;
import com.finance.entity.Transaction;
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
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class TransactionController {
    private final SessionContext sessionContext;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<Account> accountComboBox;

    @FXML
    private ComboBox<Category> categoryComboBox;

    @FXML
    private TextArea noteArea;

    @FXML
    private TableView<Transaction> transactionsTable;

    @FXML
    private TableColumn<Transaction, Long> idColumn;

    @FXML
    private TableColumn<Transaction, LocalDate> dateColumn;

    @FXML
    private TableColumn<Transaction, Transaction.TransactionType> typeColumn;

    @FXML
    private TableColumn<Transaction, BigDecimal> amountColumn;

    @FXML
    private TableColumn<Transaction, String> categoryColumn;

    @FXML
    private TableColumn<Transaction, String> noteColumn;

    @FXML
    public void initialize() {
        setupTable();
        setupTypeComboBox();
        loadTransactions();
        datePicker.setValue(LocalDate.now());
    }

    private void setupTypeComboBox() {
        typeComboBox.setItems(FXCollections.observableArrayList("Income", "Expense"));
        typeComboBox.setOnAction(e -> updateAccountAndCategoryComboBoxes());
    }

    private void updateAccountAndCategoryComboBoxes() {
        Long userId = sessionContext.getCurrentUserId();
        String selectedType = typeComboBox.getValue();

        if ("Income".equals(selectedType)) {
            List<Account> accounts = accountService.getAccountsByUserId(userId);
            accountComboBox.setItems(FXCollections.observableArrayList(accounts));
            List<Category> categories = categoryService.getIncomeCategories(userId);
            categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        } else if ("Expense".equals(selectedType)) {
            List<Account> accounts = accountService.getAccountsByUserId(userId);
            accountComboBox.setItems(FXCollections.observableArrayList(accounts));
            List<Category> categories = categoryService.getExpenseCategories(userId);
            categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        }
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        
        categoryColumn.setCellValueFactory(cellData -> {
            Transaction trans = cellData.getValue();
            if (trans.getCategoryId() != null) {
                try {
                    Category cat = categoryService.getCategoryById(trans.getCategoryId());
                    return new javafx.beans.property.SimpleStringProperty(cat.getName());
                } catch (Exception e) {
                    return new javafx.beans.property.SimpleStringProperty("N/A");
                }
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));
    }

    private void loadTransactions() {
        Long userId = sessionContext.getCurrentUserId();
        List<Transaction> transactions = transactionService.getTransactionsByUserId(userId);
        transactionsTable.setItems(FXCollections.observableArrayList(transactions));
    }

    @FXML
    public void handleAddTransaction() {
        String type = typeComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String amountStr = amountField.getText().trim();
        Account account = accountComboBox.getValue();
        Category category = categoryComboBox.getValue();
        String note = noteArea.getText().trim();

        if (type == null || date == null || amountStr.isEmpty() || account == null) {
            showAlert("Validation Error", "Type, Date, Amount, and Account are required");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr);
            Long userId = sessionContext.getCurrentUserId();
            Long categoryId = category != null ? category.getId() : null;

            if ("Income".equals(type)) {
                transactionService.recordIncomeTransaction(userId, date, amount, account.getId(), categoryId, note);
            } else if ("Expense".equals(type)) {
                transactionService.recordExpenseTransaction(userId, date, amount, account.getId(), categoryId, note);
            }

            clearFields();
            loadTransactions();
            showAlert("Success", "Transaction recorded successfully");
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Amount must be a valid number");
        } catch (Exception e) {
            showAlert("Error", "Failed to record transaction: " + e.getMessage());
        }
    }

    private void clearFields() {
        typeComboBox.setValue(null);
        datePicker.setValue(LocalDate.now());
        amountField.clear();
        accountComboBox.setValue(null);
        categoryComboBox.setValue(null);
        noteArea.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
