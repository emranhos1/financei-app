package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.entity.Account;
import com.finance.entity.Transaction;
import com.finance.service.AccountService;
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

@Controller
@RequiredArgsConstructor
public class TransferController {
    private final SessionContext sessionContext;
    private final TransactionService transactionService;
    private final AccountService accountService;

    @FXML
    private ComboBox<Account> fromAccountComboBox;

    @FXML
    private ComboBox<Account> toAccountComboBox;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField amountField;

    @FXML
    private TextArea noteArea;

    @FXML
    private TableView<Transaction> transfersTable;

    @FXML
    private TableColumn<Transaction, Long> idColumn;

    @FXML
    private TableColumn<Transaction, LocalDate> dateColumn;

    @FXML
    private TableColumn<Transaction, BigDecimal> amountColumn;

    @FXML
    private TableColumn<Transaction, String> fromColumn;

    @FXML
    private TableColumn<Transaction, String> toColumn;

    @FXML
    private TableColumn<Transaction, String> noteColumn;

    @FXML
    public void initialize() {
        setupTable();
        loadAccounts();
        loadTransfers();
        datePicker.setValue(LocalDate.now());
    }

    private void loadAccounts() {
        Long userId = sessionContext.getCurrentUserId();
        List<Account> accounts = accountService.getAccountsByUserId(userId);
        fromAccountComboBox.setItems(FXCollections.observableArrayList(accounts));
        toAccountComboBox.setItems(FXCollections.observableArrayList(accounts));
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        
        fromColumn.setCellValueFactory(cellData -> {
            Transaction trans = cellData.getValue();
            if (trans.getFromAccountId() != null) {
                try {
                    Account account = accountService.getAccountById(trans.getFromAccountId());
                    return new javafx.beans.property.SimpleStringProperty(account.getName());
                } catch (Exception e) {
                    return new javafx.beans.property.SimpleStringProperty("N/A");
                }
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        
        toColumn.setCellValueFactory(cellData -> {
            Transaction trans = cellData.getValue();
            if (trans.getToAccountId() != null) {
                try {
                    Account account = accountService.getAccountById(trans.getToAccountId());
                    return new javafx.beans.property.SimpleStringProperty(account.getName());
                } catch (Exception e) {
                    return new javafx.beans.property.SimpleStringProperty("N/A");
                }
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));
    }

    private void loadTransfers() {
        Long userId = sessionContext.getCurrentUserId();
        List<Transaction> transfers = transactionService.getTransactionsByType(userId, Transaction.TransactionType.transfer);
        transfersTable.setItems(FXCollections.observableArrayList(transfers));
    }

    @FXML
    public void handleTransfer() {
        Account fromAccount = fromAccountComboBox.getValue();
        Account toAccount = toAccountComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String amountStr = amountField.getText().trim();
        String note = noteArea.getText().trim();

        if (fromAccount == null || toAccount == null || amountStr.isEmpty()) {
            showAlert("Validation Error", "All fields are required");
            return;
        }

        if (fromAccount.getId().equals(toAccount.getId())) {
            showAlert("Validation Error", "Source and destination accounts must be different");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr);
            Long userId = sessionContext.getCurrentUserId();

            transactionService.recordTransferTransaction(
                    userId,
                    date,
                    amount,
                    fromAccount.getId(),
                    toAccount.getId(),
                    note
            );

            clearFields();
            loadAccounts();
            loadTransfers();
            showAlert("Success", "Transfer completed successfully");
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
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
