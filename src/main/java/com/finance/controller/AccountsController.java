package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.entity.Account;
import com.finance.entity.AccountType;
import com.finance.service.AccountService;
import com.finance.service.AccountTypeService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AccountsController {
    private final SessionContext sessionContext;
    private final AccountService accountService;
    private final AccountTypeService accountTypeService;

    @FXML private TableView<Account> accountsTable;
    @FXML private TableColumn<Account, Long> idColumn;
    @FXML private TableColumn<Account, String> nameColumn;
    @FXML private TableColumn<Account, String> typeColumn;
    @FXML private TableColumn<Account, BigDecimal> balanceColumn;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField balanceField;

    @FXML private TableView<AccountType> typesTable;
    @FXML private TableColumn<AccountType, Long> typeIdColumn;
    @FXML private TableColumn<AccountType, String> typeNameColumn;
    @FXML private TextField typeNameField;

    @FXML
    public void initialize() {
        setupAccountsTable();
        setupTypesTable();
        loadAccountTypes();
        loadAccounts();
    }

    private void setupAccountsTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));
        accountsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) {
                nameField.setText(sel.getName());
                typeComboBox.setValue(sel.getType());
                balanceField.setText(sel.getBalance().toPlainString());
            }
        });
    }

    private void loadAccounts() {
        accountsTable.setItems(FXCollections.observableArrayList(
                accountService.getAccountsByUserId(sessionContext.getCurrentUserId())));
    }

    private void loadAccountTypes() {
        Long userId = sessionContext.getCurrentUserId();
        List<AccountType> types = accountTypeService.getAccountTypesByUserId(userId);
        typeComboBox.setItems(FXCollections.observableArrayList(
                types.stream().map(AccountType::getName).collect(Collectors.toList())));
        typesTable.setItems(FXCollections.observableArrayList(types));
    }

    @FXML public void handleAddAccount() {
        String name = nameField.getText().trim();
        String type = typeComboBox.getValue();
        String balStr = balanceField.getText().trim();
        if (name.isEmpty() || type == null || balStr.isEmpty()) { showAlert("Validation Error", "All fields are required"); return; }
        try {
            accountService.createAccount(sessionContext.getCurrentUserId(), name, type, new BigDecimal(balStr));
            clearAccountFields(); loadAccounts();
        } catch (NumberFormatException e) { showAlert("Validation Error", "Balance must be a valid number");
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML public void handleUpdateAccount() {
        Account sel = accountsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Error", "Select an account to update"); return; }
        String name = nameField.getText().trim(); String type = typeComboBox.getValue();
        if (name.isEmpty() || type == null) { showAlert("Validation Error", "Name and type are required"); return; }
        try { accountService.updateAccount(sel.getId(), name, type); clearAccountFields(); loadAccounts();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML public void handleDeleteAccount() {
        Account sel = accountsTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Error", "Select an account to delete"); return; }
        if (confirm("Delete account '" + sel.getName() + "'?")) {
            try { accountService.deleteAccount(sel.getId()); clearAccountFields(); loadAccounts();
            } catch (Exception e) { showAlert("Error", "Failed to delete: " + e.getMessage()); }
        }
    }

    private void clearAccountFields() {
        nameField.clear(); balanceField.clear(); typeComboBox.setValue(null);
        accountsTable.getSelectionModel().clearSelection();
    }

    private void setupTypesTable() {
        typeIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        typeNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typesTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) typeNameField.setText(sel.getName());
        });
    }

    @FXML public void handleAddType() {
        String name = typeNameField.getText().trim();
        if (name.isEmpty()) { showAlert("Validation Error", "Type name cannot be empty"); return; }
        try { accountTypeService.createAccountType(sessionContext.getCurrentUserId(), name); clearTypeForm(); loadAccountTypes();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML public void handleUpdateType() {
        AccountType sel = typesTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Error", "Select a type to update"); return; }
        String name = typeNameField.getText().trim();
        if (name.isEmpty()) { showAlert("Validation Error", "Name cannot be empty"); return; }
        try { accountTypeService.updateAccountType(sel.getId(), sessionContext.getCurrentUserId(), name); clearTypeForm(); loadAccountTypes();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML public void handleDeleteType() {
        AccountType sel = typesTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Error", "Select a type to delete"); return; }
        if (confirm("Delete account type '" + sel.getName() + "'?")) {
            try { accountTypeService.deleteAccountType(sel.getId(), sessionContext.getCurrentUserId()); clearTypeForm(); loadAccountTypes();
            } catch (Exception e) { showAlert("Error", "Failed to delete: " + e.getMessage()); }
        }
    }

    private void clearTypeForm() { typeNameField.clear(); typesTable.getSelectionModel().clearSelection(); }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION); a.setTitle("Confirm"); a.setHeaderText(null); a.setContentText(msg);
        Optional<ButtonType> r = a.showAndWait(); return r.isPresent() && r.get() == ButtonType.OK;
    }
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}