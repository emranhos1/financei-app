package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.entity.Account;
import com.finance.entity.Category;
import com.finance.entity.Transaction;
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
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TransactionController {
    private final SessionContext sessionContext;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    @FXML private ComboBox<String> typeComboBox;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<Account> accountComboBox;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private TextField amountField;
    @FXML private TextArea noteArea;

    @FXML private TableView<Transaction> transactionsTable;
    @FXML private TableColumn<Transaction, Long> idColumn;
    @FXML private TableColumn<Transaction, LocalDate> dateColumn;
    @FXML private TableColumn<Transaction, String> typeColumn;
    @FXML private TableColumn<Transaction, String> accountColumn;
    @FXML private TableColumn<Transaction, String> categoryColumn;
    @FXML private TableColumn<Transaction, BigDecimal> amountColumn;

    @FXML private TableView<Category> categoriesTable;
    @FXML private TableColumn<Category, Long> catIdColumn;
    @FXML private TableColumn<Category, String> catNameColumn;
    @FXML private TableColumn<Category, String> catTypeColumn;
    @FXML private TextField catNameField;
    @FXML private ComboBox<String> catTypeComboBox;

    private Transaction selectedTransaction = null;

    @FXML
    public void initialize() {
        setupAccountComboBox();
        setupCategoryComboBox();
        setupTransactionTable();
        setupCategoryTable();

        typeComboBox.setItems(FXCollections.observableArrayList("Income", "Expense"));
        typeComboBox.setOnAction(e -> refreshDropdowns());
        datePicker.setValue(LocalDate.now());
        catTypeComboBox.setItems(FXCollections.observableArrayList("income", "expense", "both"));

        loadTransactions();
        loadCategories();
    }

    // ============================================================
    // DROPDOWN SETUP
    // ============================================================

    private void setupAccountComboBox() {
        accountComboBox.setConverter(new StringConverter<>() {
            public String toString(Account a) { return a == null ? "" : a.getName() + " [" + a.getType() + "]"; }
            public Account fromString(String s) { return null; }
        });
    }

    private void setupCategoryComboBox() {
        categoryComboBox.setConverter(new StringConverter<>() {
            public String toString(Category c) { return c == null ? "" : c.getName(); }
            public Category fromString(String s) { return null; }
        });
    }

    private void refreshDropdowns() {
        Long userId = sessionContext.getCurrentUserId();
        String type = typeComboBox.getValue();

        List<Account> accounts = accountService.getAccountsByUserId(userId);
        accountComboBox.setItems(FXCollections.observableArrayList(accounts));
        accountComboBox.setValue(null);

        if ("Income".equals(type)) {
            categoryComboBox.setItems(FXCollections.observableArrayList(categoryService.getIncomeCategories(userId)));
        } else if ("Expense".equals(type)) {
            categoryComboBox.setItems(FXCollections.observableArrayList(categoryService.getExpenseCategories(userId)));
        }
        categoryComboBox.setValue(null);
    }

    // ============================================================
    // TRANSACTION TABLE
    // ============================================================

    private void setupTransactionTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        typeColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getType().name()));

        accountColumn.setCellValueFactory(cd -> {
            Transaction tx = cd.getValue();
            Long accId = tx.getType() == Transaction.TransactionType.income
                    ? tx.getToAccountId() : tx.getFromAccountId();
            if (accId == null) return new SimpleStringProperty("-");
            try { return new SimpleStringProperty(accountService.getAccountById(accId).getName()); }
            catch (Exception e) { return new SimpleStringProperty("-"); }
        });

        categoryColumn.setCellValueFactory(cd -> {
            Long catId = cd.getValue().getCategoryId();
            if (catId == null) return new SimpleStringProperty("-");
            try { return new SimpleStringProperty(categoryService.getCategoryById(catId).getName()); }
            catch (Exception e) { return new SimpleStringProperty("-"); }
        });

        transactionsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) populateFormForEdit(sel);
        });
    }

    private void loadTransactions() {
        transactionsTable.setItems(FXCollections.observableArrayList(
                transactionService.getTransactionsByUserId(sessionContext.getCurrentUserId())));
    }

    private void populateFormForEdit(Transaction tx) {
        selectedTransaction = tx;
        String type = tx.getType() == Transaction.TransactionType.income ? "Income" : "Expense";
        typeComboBox.setValue(type);
        refreshDropdowns();
        datePicker.setValue(tx.getDate());
        amountField.setText(tx.getAmount().toPlainString());
        noteArea.setText(tx.getNote() != null ? tx.getNote() : "");

        Long accId = tx.getType() == Transaction.TransactionType.income ? tx.getToAccountId() : tx.getFromAccountId();
        if (accId != null) accountComboBox.getItems().stream()
                .filter(a -> a.getId().equals(accId)).findFirst().ifPresent(accountComboBox::setValue);

        if (tx.getCategoryId() != null) categoryComboBox.getItems().stream()
                .filter(c -> c.getId().equals(tx.getCategoryId())).findFirst().ifPresent(categoryComboBox::setValue);
    }

    @FXML
    public void handleSaveTransaction() {
        String type = typeComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String amountStr = amountField.getText().trim();
        Account account = accountComboBox.getValue();
        Category category = categoryComboBox.getValue();
        String note = noteArea.getText().trim();

        if (type == null || date == null || amountStr.isEmpty() || account == null) {
            showAlert("Validation Error", "Type, Date, Account and Amount are required"); return;
        }
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            Long userId = sessionContext.getCurrentUserId();
            Long categoryId = category != null ? category.getId() : null;

            if (selectedTransaction != null) {
                transactionService.updateTransaction(selectedTransaction.getId(), userId, date, amount, categoryId, note);
            } else {
                if ("Income".equals(type)) {
                    transactionService.recordIncomeTransaction(userId, date, amount, account.getId(), categoryId, note);
                } else {
                    transactionService.recordExpenseTransaction(userId, date, amount, account.getId(), categoryId, note);
                }
            }
            clearTransactionForm();
            loadTransactions();
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Amount must be a valid number");
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    public void handleDeleteTransaction() {
        Transaction sel = transactionsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Error", "Select a transaction to delete"); return; }
        if (confirm("Delete this transaction? The account balance will be reversed.")) {
            try {
                transactionService.deleteTransaction(sel.getId(), sessionContext.getCurrentUserId());
                clearTransactionForm();
                loadTransactions();
            } catch (Exception e) { showAlert("Error", e.getMessage()); }
        }
    }

    @FXML
    public void handleClearForm() { clearTransactionForm(); }

    private void clearTransactionForm() {
        selectedTransaction = null;
        typeComboBox.setValue(null);
        datePicker.setValue(LocalDate.now());
        amountField.clear();
        accountComboBox.setItems(FXCollections.observableArrayList());
        accountComboBox.setValue(null);
        categoryComboBox.setItems(FXCollections.observableArrayList());
        categoryComboBox.setValue(null);
        noteArea.clear();
        transactionsTable.getSelectionModel().clearSelection();
    }

    // ============================================================
    // CATEGORIES TAB
    // ============================================================

    private void setupCategoryTable() {
        catIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        catNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        catTypeColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getType().name()));

        categoriesTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) {
                catNameField.setText(sel.getName());
                catTypeComboBox.setValue(sel.getType().name());
            }
        });
    }

    private void loadCategories() {
        categoriesTable.setItems(FXCollections.observableArrayList(
                categoryService.getCategoriesByUserId(sessionContext.getCurrentUserId())));
    }

    @FXML
    public void handleAddCategory() {
        String name = catNameField.getText().trim();
        String type = catTypeComboBox.getValue();
        if (name.isEmpty() || type == null) { showAlert("Validation Error", "Name and type are required"); return; }
        try {
            categoryService.createCategory(sessionContext.getCurrentUserId(), name, Category.CategoryType.valueOf(type));
            clearCategoryForm();
            loadCategories();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML
    public void handleUpdateCategory() {
        Category sel = categoriesTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Error", "Select a category to update"); return; }
        String name = catNameField.getText().trim();
        String type = catTypeComboBox.getValue();
        if (name.isEmpty() || type == null) { showAlert("Validation Error", "Name and type are required"); return; }
        try {
            categoryService.updateCategory(sel.getId(), sessionContext.getCurrentUserId(), name, Category.CategoryType.valueOf(type));
            clearCategoryForm();
            loadCategories();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML
    public void handleDeleteCategory() {
        Category sel = categoriesTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Error", "Select a category to delete"); return; }
        if (confirm("Delete category '" + sel.getName() + "'?")) {
            try {
                categoryService.deleteCategory(sel.getId());
                clearCategoryForm();
                loadCategories();
            } catch (Exception e) { showAlert("Error", e.getMessage()); }
        }
    }

    private void clearCategoryForm() {
        catNameField.clear(); catTypeComboBox.setValue(null);
        categoriesTable.getSelectionModel().clearSelection();
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm"); a.setHeaderText(null); a.setContentText(msg);
        Optional<ButtonType> r = a.showAndWait(); return r.isPresent() && r.get() == ButtonType.OK;
    }
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}