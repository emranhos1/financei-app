package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.entity.Account;
import com.finance.entity.Category;
import com.finance.entity.Transaction;
import com.finance.service.AccountService;
import com.finance.service.CategoryService;
import com.finance.service.TransactionService;
import com.finance.service.TransferTypeService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TransactionController {
    private final SessionContext sessionContext;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final TransferTypeService transferTypeService;

    @FXML private RadioButton incomeRadio;
    @FXML private RadioButton expenseRadio;
    @FXML private ToggleGroup typeToggleGroup;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<Account> accountComboBox;
    @FXML private Label accountBalanceLabel;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private TextField amountField;
    @FXML private TextArea noteArea;
    @FXML private Button saveTransactionBtn;
    @FXML private HBox txEditButtons;

    @FXML private TableView<Transaction> transactionsTable;
    @FXML private TableColumn<Transaction, LocalDate> dateColumn;
    @FXML private TableColumn<Transaction, String> typeColumn;
    @FXML private TableColumn<Transaction, String> accountColumn;
    @FXML private TableColumn<Transaction, String> categoryColumn;
    @FXML private TableColumn<Transaction, BigDecimal> amountColumn;
    @FXML private TableColumn<Transaction, String> noteColumn;
    @FXML private ComboBox<Category> categoryFilterComboBox;
    @FXML private DatePicker dateFilter;
    @FXML private ComboBox<Account> accountFilterComboBox;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private Button txPrevPageBtn;
    @FXML private Button txNextPageBtn;
    @FXML private Label txPageLabel;
    @FXML private ComboBox<Integer> txPageSizeComboBox;

    private static final Category ALL_CATEGORIES_OPTION = Category.builder().id(null).name("All Categories").build();
    private static final Account ALL_ACCOUNTS_OPTION = Account.builder().id(null).name("All Accounts").build();
    private static final List<Integer> PAGE_SIZE_OPTIONS = Arrays.asList(5, 10, 20, 50, 100);

    private int txCurrentPage = 0;
    private int txTotalPages = 1;

    @FXML private TableView<Category> categoriesTable;
    @FXML private TableColumn<Category, Long> catIdColumn;
    @FXML private TableColumn<Category, String> catNameColumn;
    @FXML private TableColumn<Category, String> catTypeColumn;
    @FXML private TextField catNameField;
    @FXML private ComboBox<Category.CategoryType> catTypeComboBox;
    @FXML private Button catAddBtn;
    @FXML private HBox catEditButtons;

    private Transaction selectedTransaction = null;

    @FXML
    public void initialize() {
        setupAccountComboBox();
        setupCategoryComboBox();
        setupTransactionTable();
        setupCategoryTable();

        typeToggleGroup.selectedToggleProperty().addListener((obs, o, n) -> refreshDropdowns());
        datePicker.setValue(LocalDate.now());

        setupCategoryFilterComboBox();
        txPageSizeComboBox.setItems(FXCollections.observableArrayList(PAGE_SIZE_OPTIONS));
        txPageSizeComboBox.setValue(20);
        txPageSizeComboBox.setOnAction(e -> loadTransactionsPage(0));

        catTypeComboBox.setItems(FXCollections.observableArrayList(Category.CategoryType.values()));
        catTypeComboBox.setConverter(new StringConverter<Category.CategoryType>() {
            public String toString(Category.CategoryType t) { return t == null ? "" : t.name(); }
            public Category.CategoryType fromString(String s) { return null; }
        });

        loadTransactions();
        loadCategories();
        resetTransactionForm();
        resetCategoryForm();
    }

    private void setupAccountComboBox() {
        accountComboBox.setConverter(new StringConverter<Account>() {
            public String toString(Account a) {
                return a == null ? "" : a.getName() + " [" + a.getAccountType().getName() + "]";
            }
            public Account fromString(String s) { return null; }
        });
        accountComboBox.valueProperty().addListener((obs, o, selected) -> updateAccountBalanceLabel(selected));
    }

    private void updateAccountBalanceLabel(Account account) {
        if (account == null) { accountBalanceLabel.setText(""); return; }
        accountBalanceLabel.setText("Available balance: \u09f3 " + account.getBalance().toPlainString());
    }

    private void setupCategoryComboBox() {
        categoryComboBox.setConverter(new StringConverter<Category>() {
            public String toString(Category c) { return c == null ? "" : c.getName(); }
            public Category fromString(String s) { return null; }
        });
    }

    private String getSelectedType() {
        if (incomeRadio.isSelected()) return "INCOME";
        if (expenseRadio.isSelected()) return "EXPENSE";
        return null;
    }

    private void refreshDropdowns() {
        Long userId = sessionContext.getCurrentUserId();
        String type = getSelectedType();
        accountComboBox.setItems(FXCollections.observableArrayList(accountService.getAccountsByUserId(userId)));
        accountComboBox.setValue(null);
        if ("INCOME".equals(type)) {
            categoryComboBox.setItems(FXCollections.observableArrayList(categoryService.getIncomeCategories(userId)));
        } else if ("EXPENSE".equals(type)) {
            categoryComboBox.setItems(FXCollections.observableArrayList(categoryService.getExpenseCategories(userId)));
        }
        categoryComboBox.setValue(null);
    }

    private void setupTransactionTable() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));

        typeColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getType().name().toUpperCase()));

        accountColumn.setCellValueFactory(cd -> {
            Transaction tx = cd.getValue();
            Long accId = tx.getType() == Transaction.TransactionType.INCOME ? tx.getToAccountId() : tx.getFromAccountId();
            if (accId == null) return new SimpleStringProperty("-");
            try { return new SimpleStringProperty(accountService.getAccountById(accId).getName()); }
            catch (Exception e) { return new SimpleStringProperty("-"); }
        });

        categoryColumn.setCellValueFactory(cd -> {
            Transaction tx = cd.getValue();
            if (tx.getType() == Transaction.TransactionType.TRANSFER) {
                Long typeId = tx.getTransferTypeId();
                if (typeId == null) return new SimpleStringProperty("-");
                try { return new SimpleStringProperty(transferTypeService.getTransferTypeById(typeId).getName()); }
                catch (Exception e) { return new SimpleStringProperty("-"); }
            }
            Long catId = tx.getCategoryId();
            if (catId == null) return new SimpleStringProperty("-");
            try { return new SimpleStringProperty(categoryService.getCategoryById(catId).getName()); }
            catch (Exception e) { return new SimpleStringProperty("-"); }
        });

        transactionsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) populateFormForEdit(sel);
        });
    }

    private void setupCategoryFilterComboBox() {
        categoryFilterComboBox.setConverter(new StringConverter<Category>() {
            public String toString(Category c) { return c == null ? "" : c.getName(); }
            public Category fromString(String s) { return null; }
        });
        categoryFilterComboBox.setOnAction(e -> loadTransactionsPage(0));
        dateFilter.valueProperty().addListener((obs, o, n) -> loadTransactionsPage(0));

        accountFilterComboBox.setConverter(new StringConverter<Account>() {
            public String toString(Account a) {
                if (a == null) return "";
                return a.getAccountType() == null ? a.getName() : a.getName() + " [" + a.getAccountType().getName() + "]";
            }
            public Account fromString(String s) { return null; }
        });
        accountFilterComboBox.setOnAction(e -> loadTransactionsPage(0));

        typeFilterComboBox.setItems(FXCollections.observableArrayList("All Types", "INCOME", "EXPENSE"));
        typeFilterComboBox.setValue("All Types");
        typeFilterComboBox.setOnAction(e -> loadTransactionsPage(0));
    }

    private void loadTransactions() {
        Long userId = sessionContext.getCurrentUserId();
        List<Category> userCategories = categoryService.getCategoriesByUserId(userId);
        List<Category> filterOptions = new ArrayList<>();
        filterOptions.add(ALL_CATEGORIES_OPTION);
        filterOptions.addAll(userCategories);
        categoryFilterComboBox.setItems(FXCollections.observableArrayList(filterOptions));
        categoryFilterComboBox.setValue(ALL_CATEGORIES_OPTION);

        List<Account> accountOptions = new ArrayList<>();
        accountOptions.add(ALL_ACCOUNTS_OPTION);
        accountOptions.addAll(accountService.getAccountsByUserId(userId));
        accountFilterComboBox.setItems(FXCollections.observableArrayList(accountOptions));
        accountFilterComboBox.setValue(ALL_ACCOUNTS_OPTION);

        loadTransactionsPage(0);
    }

    private void loadTransactionsPage(int page) {
        Category selectedCategory = categoryFilterComboBox.getValue();
        Long categoryId = (selectedCategory != null && selectedCategory.getId() != null) ? selectedCategory.getId() : null;
        LocalDate selectedDate = dateFilter.getValue();
        Account selectedAccount = accountFilterComboBox.getValue();
        Long accountId = (selectedAccount != null && selectedAccount.getId() != null) ? selectedAccount.getId() : null;
        String selectedTypeStr = typeFilterComboBox.getValue();
        Transaction.TransactionType filterType = (selectedTypeStr == null || "All Types".equals(selectedTypeStr))
                ? null : Transaction.TransactionType.valueOf(selectedTypeStr);
        int pageSize = txPageSizeComboBox.getValue() != null ? txPageSizeComboBox.getValue() : 20;

        Pageable pageable = PageRequest.of(page, pageSize);
        Page<Transaction> result = transactionService.getTransactionsPage(sessionContext.getCurrentUserId(),
                Arrays.asList(Transaction.TransactionType.INCOME, Transaction.TransactionType.EXPENSE),
                categoryId, selectedDate, accountId, filterType, pageable);

        txCurrentPage = result.getNumber();
        txTotalPages = Math.max(result.getTotalPages(), 1);
        transactionsTable.setItems(FXCollections.observableArrayList(result.getContent()));
        txPageLabel.setText("Page " + (txCurrentPage + 1) + " of " + txTotalPages + " (" + result.getTotalElements() + " total)");
        txPrevPageBtn.setDisable(txCurrentPage <= 0);
        txNextPageBtn.setDisable(txCurrentPage >= txTotalPages - 1);
    }

    @FXML
    public void handleTxPrevPage() {
        if (txCurrentPage > 0) loadTransactionsPage(txCurrentPage - 1);
    }

    @FXML
    public void handleTxNextPage() {
        if (txCurrentPage < txTotalPages - 1) loadTransactionsPage(txCurrentPage + 1);
    }

    @FXML
    public void handleClearFilters() {
        categoryFilterComboBox.setValue(ALL_CATEGORIES_OPTION);
        dateFilter.setValue(null);
        accountFilterComboBox.setValue(ALL_ACCOUNTS_OPTION);
        typeFilterComboBox.setValue("All Types");
        loadTransactionsPage(0);
    }

    private void populateFormForEdit(Transaction tx) {
        selectedTransaction = tx;
        if (tx.getType() == Transaction.TransactionType.INCOME) {
            incomeRadio.setSelected(true);
        } else {
            expenseRadio.setSelected(true);
        }
        refreshDropdowns();
        datePicker.setValue(tx.getDate());
        amountField.setText(tx.getAmount().toPlainString());
        noteArea.setText(tx.getNote() != null ? tx.getNote() : "");

        Long accId = tx.getType() == Transaction.TransactionType.INCOME ? tx.getToAccountId() : tx.getFromAccountId();
        if (accId != null) accountComboBox.getItems().stream()
                .filter(a -> a.getId().equals(accId)).findFirst().ifPresent(accountComboBox::setValue);
        if (tx.getCategoryId() != null) categoryComboBox.getItems().stream()
                .filter(c -> c.getId().equals(tx.getCategoryId())).findFirst().ifPresent(categoryComboBox::setValue);

        saveTransactionBtn.setVisible(false);
        saveTransactionBtn.setManaged(false);
        txEditButtons.setVisible(true);
        txEditButtons.setManaged(true);
    }

    @FXML
    public void handleSaveTransaction() {
        String type = getSelectedType();
        LocalDate date = datePicker.getValue();
        String amountStr = amountField.getText().trim();
        Account account = accountComboBox.getValue();
        Category category = categoryComboBox.getValue();
        String note = noteArea.getText().trim();
        if (type == null || date == null || amountStr.isEmpty() || account == null || category == null) {
            showAlert("Validation Error", "Type, Date, Account, Category and Amount are required"); return;
        }
        if (!confirm("Save this transaction?")) return;
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            Long userId = sessionContext.getCurrentUserId();
            Long categoryId = category.getId();
            if ("INCOME".equals(type)) {
                transactionService.recordIncomeTransaction(userId, date, amount, account.getId(), categoryId, note);
            } else {
                transactionService.recordExpenseTransaction(userId, date, amount, account.getId(), categoryId, note);
            }
            resetTransactionForm();
            loadTransactions();
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Amount must be a valid number");
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML
    public void handleUpdateTransaction() {
        if (selectedTransaction == null) return;
        String type = getSelectedType();
        LocalDate date = datePicker.getValue();
        String amountStr = amountField.getText().trim();
        Account account = accountComboBox.getValue();
        Category category = categoryComboBox.getValue();
        String note = noteArea.getText().trim();
        if (type == null || date == null || amountStr.isEmpty() || account == null || category == null) {
            showAlert("Validation Error", "Type, Date, Account, Category and Amount are required"); return;
        }
        if (!confirm("Update this transaction?")) return;
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            Long categoryId = category.getId();
            transactionService.updateTransaction(selectedTransaction.getId(), sessionContext.getCurrentUserId(), date, amount, categoryId, null, note);
            resetTransactionForm();
            loadTransactions();
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Amount must be a valid number");
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML
    public void handleDeleteTransaction() {
        if (selectedTransaction == null) return;
        if (!confirm("Delete this transaction? The account balance will be reversed.")) return;
        try {
            transactionService.deleteTransaction(selectedTransaction.getId(), sessionContext.getCurrentUserId());
            resetTransactionForm();
            loadTransactions();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML
    public void handleCancelTransaction() { resetTransactionForm(); }

    private void resetTransactionForm() {
        selectedTransaction = null;
        typeToggleGroup.selectToggle(null);
        datePicker.setValue(LocalDate.now());
        amountField.clear();
        accountComboBox.setItems(FXCollections.observableArrayList());
        accountComboBox.setValue(null);
        categoryComboBox.setItems(FXCollections.observableArrayList());
        categoryComboBox.setValue(null);
        noteArea.clear();
        transactionsTable.getSelectionModel().clearSelection();
        saveTransactionBtn.setVisible(true);
        saveTransactionBtn.setManaged(true);
        txEditButtons.setVisible(false);
        txEditButtons.setManaged(false);
    }

    private void setupCategoryTable() {
        catIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        catNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        catTypeColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getType().name()));

        categoriesTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) {
                catNameField.setText(sel.getName());
                catTypeComboBox.setValue(sel.getType());
                catAddBtn.setVisible(false);
                catAddBtn.setManaged(false);
                catEditButtons.setVisible(true);
                catEditButtons.setManaged(true);
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
        Category.CategoryType type = catTypeComboBox.getValue();
        if (name.isEmpty() || type == null) { showAlert("Validation Error", "Name and type are required"); return; }
        try {
            categoryService.createCategory(sessionContext.getCurrentUserId(), name, type);
            resetCategoryForm(); loadCategories();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML
    public void handleUpdateCategory() {
        Category sel = categoriesTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String name = catNameField.getText().trim();
        Category.CategoryType type = catTypeComboBox.getValue();
        if (name.isEmpty() || type == null) { showAlert("Validation Error", "Name and type are required"); return; }
        if (!confirm("Update category '" + sel.getName() + "'?")) return;
        try {
            categoryService.updateCategory(sel.getId(), sessionContext.getCurrentUserId(), name, type);
            resetCategoryForm(); loadCategories();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML
    public void handleDeleteCategory() {
        Category sel = categoriesTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        if (!confirm("Delete category '" + sel.getName() + "'?")) return;
        try {
            categoryService.deleteCategory(sel.getId()); resetCategoryForm(); loadCategories();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML
    public void handleCancelCategory() { resetCategoryForm(); }

    private void resetCategoryForm() {
        catNameField.clear();
        catTypeComboBox.setValue(null);
        categoriesTable.getSelectionModel().clearSelection();
        catAddBtn.setVisible(true);
        catAddBtn.setManaged(true);
        catEditButtons.setVisible(false);
        catEditButtons.setManaged(false);
    }

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