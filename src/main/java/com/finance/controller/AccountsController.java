package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.entity.Account;
import com.finance.service.AccountService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AccountsController {
    private final SessionContext sessionContext;
    private final AccountService accountService;

    @FXML
    private TableView<Account> accountsTable;

    @FXML
    private TableColumn<Account, Long> idColumn;

    @FXML
    private TableColumn<Account, String> nameColumn;

    @FXML
    private TableColumn<Account, Account.AccountType> typeColumn;

    @FXML
    private TableColumn<Account, BigDecimal> balanceColumn;

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<Account.AccountType> typeComboBox;

    @FXML
    private TextField balanceField;

    @FXML
    public void initialize() {
        setupTable();
        loadAccounts();
        typeComboBox.setItems(FXCollections.observableArrayList(Account.AccountType.values()));
    }

    private void setupTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
    }

    private void loadAccounts() {
        Long userId = sessionContext.getCurrentUserId();
        List<Account> accounts = accountService.getAccountsByUserId(userId);
        accountsTable.setItems(FXCollections.observableArrayList(accounts));
    }

    @FXML
    public void handleAddAccount() {
        String name = nameField.getText().trim();
        Account.AccountType type = typeComboBox.getValue();
        String balanceStr = balanceField.getText().trim();

        if (name.isEmpty() || type == null || balanceStr.isEmpty()) {
            showAlert("Validation Error", "All fields are required");
            return;
        }

        try {
            BigDecimal balance = new BigDecimal(balanceStr);
            Long userId = sessionContext.getCurrentUserId();
            accountService.createAccount(userId, name, type, balance);
            clearFields();
            loadAccounts();
            showAlert("Success", "Account created successfully");
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Balance must be a valid number");
        } catch (Exception e) {
            showAlert("Error", "Failed to create account: " + e.getMessage());
        }
    }

    @FXML
    public void handleUpdateAccount() {
        Account selected = accountsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select an account to update");
            return;
        }

        String name = nameField.getText().trim();
        Account.AccountType type = typeComboBox.getValue();

        if (name.isEmpty() || type == null) {
            showAlert("Validation Error", "Name and type are required");
            return;
        }

        try {
            accountService.updateAccount(selected.getId(), name, type);
            clearFields();
            loadAccounts();
            showAlert("Success", "Account updated successfully");
        } catch (Exception e) {
            showAlert("Error", "Failed to update account: " + e.getMessage());
        }
    }

    @FXML
    public void handleDeleteAccount() {
        Account selected = accountsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select an account to delete");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText("Are you sure you want to delete this account?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                accountService.deleteAccount(selected.getId());
                clearFields();
                loadAccounts();
                showAlert("Success", "Account deleted successfully");
            } catch (Exception e) {
                showAlert("Error", "Failed to delete account: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleSelectAccount() {
        Account selected = accountsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            nameField.setText(selected.getName());
            typeComboBox.setValue(selected.getType());
            balanceField.setText(selected.getBalance().toString());
        }
    }

    private void clearFields() {
        nameField.clear();
        balanceField.clear();
        typeComboBox.setValue(null);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
