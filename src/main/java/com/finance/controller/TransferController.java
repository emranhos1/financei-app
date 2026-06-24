package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.entity.Account;
import com.finance.entity.Transaction;
import com.finance.service.AccountService;
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

@Controller
@RequiredArgsConstructor
public class TransferController {
    private final SessionContext sessionContext;
    private final TransactionService transactionService;
    private final AccountService accountService;

    @FXML private ComboBox<Account> fromAccountComboBox;
    @FXML private ComboBox<Account> toAccountComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextField amountField;
    @FXML private TextArea noteArea;

    @FXML private TableView<Transaction> transfersTable;
    @FXML private TableColumn<Transaction, Long> idColumn;
    @FXML private TableColumn<Transaction, LocalDate> dateColumn;
    @FXML private TableColumn<Transaction, BigDecimal> amountColumn;
    @FXML private TableColumn<Transaction, String> fromColumn;
    @FXML private TableColumn<Transaction, String> toColumn;
    @FXML private TableColumn<Transaction, String> noteColumn;

    @FXML
    public void initialize() {
        setupAccountComboBoxes();
        setupTable();
        loadAccounts();
        loadTransfers();
        datePicker.setValue(LocalDate.now());
    }

    private void setupAccountComboBoxes() {
        StringConverter<Account> converter = new StringConverter<Account>() {
            public String toString(Account a) {
                return a == null ? "" : a.getName() + " [" + a.getAccountType().getName() + "]";
            }
            public Account fromString(String s) { return null; }
        };
        fromAccountComboBox.setConverter(converter);
        toAccountComboBox.setConverter(converter);
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));

        fromColumn.setCellValueFactory(cd -> {
            Long accId = cd.getValue().getFromAccountId();
            if (accId == null) return new SimpleStringProperty("-");
            try { return new SimpleStringProperty(accountService.getAccountById(accId).getName()); }
            catch (Exception e) { return new SimpleStringProperty("-"); }
        });

        toColumn.setCellValueFactory(cd -> {
            Long accId = cd.getValue().getToAccountId();
            if (accId == null) return new SimpleStringProperty("-");
            try { return new SimpleStringProperty(accountService.getAccountById(accId).getName()); }
            catch (Exception e) { return new SimpleStringProperty("-"); }
        });
    }

    private void loadAccounts() {
        List<Account> accounts = accountService.getAccountsByUserId(sessionContext.getCurrentUserId());
        fromAccountComboBox.setItems(FXCollections.observableArrayList(accounts));
        toAccountComboBox.setItems(FXCollections.observableArrayList(accounts));
    }

    private void loadTransfers() {
        List<Transaction> transfers = transactionService.getTransactionsByType(
                sessionContext.getCurrentUserId(), Transaction.TransactionType.TRANSFER);
        transfersTable.setItems(FXCollections.observableArrayList(transfers));
    }

    @FXML
    public void handleTransfer() {
        Account from = fromAccountComboBox.getValue();
        Account to = toAccountComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String amountStr = amountField.getText().trim();
        String note = noteArea.getText().trim();

        if (from == null || to == null || amountStr.isEmpty()) {
            showAlert("Validation Error", "All fields are required"); return;
        }
        if (from.getId().equals(to.getId())) {
            showAlert("Validation Error", "Source and destination accounts must be different"); return;
        }
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            transactionService.recordTransferTransaction(
                    sessionContext.getCurrentUserId(), date, amount, from.getId(), to.getId(), note);
            clearFields();
            loadAccounts();
            loadTransfers();
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Amount must be a valid number");
        } catch (Exception e) {
            showAlert("Error", "Transfer failed: " + e.getMessage());
        }
    }

    private void clearFields() {
        fromAccountComboBox.setValue(null);
        toAccountComboBox.setValue(null);
        datePicker.setValue(LocalDate.now());
        amountField.clear();
        noteArea.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}