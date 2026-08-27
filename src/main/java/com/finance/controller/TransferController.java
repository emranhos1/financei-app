package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.entity.Account;
import com.finance.entity.Transaction;
import com.finance.entity.TransferType;
import com.finance.service.AccountService;
import com.finance.service.TransactionService;
import com.finance.service.TransferTypeService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TransferController {
    private final SessionContext sessionContext;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final TransferTypeService transferTypeService;

    @FXML private DatePicker datePicker;
    @FXML private ComboBox<Account> fromAccountComboBox;
    @FXML private ComboBox<Account> toAccountComboBox;
    @FXML private ComboBox<TransferType> transferTypeComboBox;
    @FXML private TextField amountField;
    @FXML private TextArea noteArea;
    @FXML private Button saveTransferBtn;
    @FXML private HBox txEditButtons;

    @FXML private TableView<Transaction> transfersTable;
    @FXML private TableColumn<Transaction, LocalDate> dateColumn;
    @FXML private TableColumn<Transaction, String> typeColumn;
    @FXML private TableColumn<Transaction, String> fromColumn;
    @FXML private TableColumn<Transaction, String> toColumn;
    @FXML private TableColumn<Transaction, String> transferTypeColumn;
    @FXML private TableColumn<Transaction, BigDecimal> amountColumn;
    @FXML private TableColumn<Transaction, String> noteColumn;

    @FXML private TableView<TransferType> typesTable;
    @FXML private TableColumn<TransferType, Long> typeIdColumn;
    @FXML private TableColumn<TransferType, String> typeNameColumn;
    @FXML private TextField typeNameField;
    @FXML private Button typeAddBtn;
    @FXML private HBox typeEditButtons;

    private Transaction selectedTransfer = null;

    @FXML
    public void initialize() {
        setupAccountComboBoxes();
        setupTable();
        setupTypesTable();
        loadAccounts();
        loadTypes();
        loadTransfers();
        datePicker.setValue(LocalDate.now());
        resetForm();
        resetTypeForm();
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

        transferTypeComboBox.setConverter(new StringConverter<TransferType>() {
            public String toString(TransferType t) { return t == null ? "" : t.getName(); }
            public TransferType fromString(String s) { return null; }
        });
    }

    private void setupTable() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));

        typeColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getType().name()));

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

        transferTypeColumn.setCellValueFactory(cd -> {
            Long typeId = cd.getValue().getTransferTypeId();
            if (typeId == null) return new SimpleStringProperty("-");
            try { return new SimpleStringProperty(transferTypeService.getTransferTypeById(typeId).getName()); }
            catch (Exception e) { return new SimpleStringProperty("-"); }
        });

        transfersTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) populateFormForEdit(sel);
        });
    }

    private void populateFormForEdit(Transaction tx) {
        selectedTransfer = tx;
        datePicker.setValue(tx.getDate());
        amountField.setText(tx.getAmount().toPlainString());
        noteArea.setText(tx.getNote() != null ? tx.getNote() : "");

        if (tx.getFromAccountId() != null) fromAccountComboBox.getItems().stream()
                .filter(a -> a.getId().equals(tx.getFromAccountId())).findFirst().ifPresent(fromAccountComboBox::setValue);
        if (tx.getToAccountId() != null) toAccountComboBox.getItems().stream()
                .filter(a -> a.getId().equals(tx.getToAccountId())).findFirst().ifPresent(toAccountComboBox::setValue);
        if (tx.getTransferTypeId() != null) transferTypeComboBox.getItems().stream()
                .filter(t -> t.getId().equals(tx.getTransferTypeId())).findFirst().ifPresent(transferTypeComboBox::setValue);
        else transferTypeComboBox.setValue(null);

        saveTransferBtn.setVisible(false);
        saveTransferBtn.setManaged(false);
        txEditButtons.setVisible(true);
        txEditButtons.setManaged(true);
    }

    private void loadAccounts() {
        List<Account> accounts = accountService.getAccountsByUserId(sessionContext.getCurrentUserId());
        fromAccountComboBox.setItems(FXCollections.observableArrayList(accounts));
        toAccountComboBox.setItems(FXCollections.observableArrayList(accounts));
    }

    private void loadTransfers() {
        List<Transaction> transfers = transactionService.getTransactionsByType(
                sessionContext.getCurrentUserId(), Transaction.TransactionType.TRANSFER);
        transfers.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        transfersTable.setItems(FXCollections.observableArrayList(transfers));
    }

    @FXML
    public void handleSaveTransfer() {
        Account from = fromAccountComboBox.getValue();
        Account to = toAccountComboBox.getValue();
        TransferType transferType = transferTypeComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String amountStr = amountField.getText().trim();
        String note = noteArea.getText().trim();
        if (from == null || to == null || amountStr.isEmpty() || transferType == null) {
            showAlert("Validation Error", "From Account, To Account, Transfer Type and Amount are required"); return;
        }
        if (from.getId().equals(to.getId())) {
            showAlert("Validation Error", "Source and destination accounts must be different"); return;
        }
        if (!confirm("Save this transfer?")) return;
        try {
            transactionService.recordTransferTransaction(
                    sessionContext.getCurrentUserId(), date, new BigDecimal(amountStr), from.getId(), to.getId(),
                    transferType.getId(), note);
            resetForm(); loadAccounts(); loadTransfers();
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Amount must be a valid number");
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML
    public void handleUpdateTransfer() {
        if (selectedTransfer == null) return;
        LocalDate date = datePicker.getValue();
        String amountStr = amountField.getText().trim();
        String note = noteArea.getText().trim();
        TransferType transferType = transferTypeComboBox.getValue();
        if (amountStr.isEmpty() || transferType == null) {
            showAlert("Validation Error", "Amount and Transfer Type are required"); return;
        }
        if (!confirm("Update this transfer?")) return;
        try {
            transactionService.updateTransaction(selectedTransfer.getId(), sessionContext.getCurrentUserId(),
                    date, new BigDecimal(amountStr), null, transferType.getId(), note);
            resetForm(); loadAccounts(); loadTransfers();
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Amount must be a valid number");
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML
    public void handleDeleteTransfer() {
        if (selectedTransfer == null) return;
        if (!confirm("Delete this transfer? The account balances will be reversed.")) return;
        try {
            transactionService.deleteTransaction(selectedTransfer.getId(), sessionContext.getCurrentUserId());
            resetForm(); loadAccounts(); loadTransfers();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML
    public void handleCancelTransfer() { resetForm(); }

    private void resetForm() {
        selectedTransfer = null;
        datePicker.setValue(LocalDate.now());
        fromAccountComboBox.setValue(null);
        toAccountComboBox.setValue(null);
        transferTypeComboBox.setValue(null);
        amountField.clear();
        noteArea.clear();
        transfersTable.getSelectionModel().clearSelection();
        saveTransferBtn.setVisible(true);
        saveTransferBtn.setManaged(true);
        txEditButtons.setVisible(false);
        txEditButtons.setManaged(false);
    }

    private void setupTypesTable() {
        typeIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        typeNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typesTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) {
                typeNameField.setText(sel.getName());
                typeAddBtn.setVisible(false); typeAddBtn.setManaged(false);
                typeEditButtons.setVisible(true); typeEditButtons.setManaged(true);
            }
        });
    }

    private void loadTypes() {
        List<TransferType> types = transferTypeService.getTransferTypesByUserId(sessionContext.getCurrentUserId());
        transferTypeComboBox.setItems(FXCollections.observableArrayList(types));
        typesTable.setItems(FXCollections.observableArrayList(types));
    }

    @FXML public void handleAddType() {
        String name = typeNameField.getText().trim();
        if (name.isEmpty()) { showAlert("Validation Error", "Type name cannot be empty"); return; }
        if (!confirm("Add transfer type '" + name + "'?")) return;
        try { transferTypeService.createTransferType(sessionContext.getCurrentUserId(), name); resetTypeForm(); loadTypes();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML public void handleUpdateType() {
        TransferType sel = typesTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String name = typeNameField.getText().trim();
        if (name.isEmpty()) { showAlert("Validation Error", "Name cannot be empty"); return; }
        if (!confirm("Update type '" + sel.getName() + "' to '" + name + "'?")) return;
        try { transferTypeService.updateTransferType(sel.getId(), sessionContext.getCurrentUserId(), name); resetTypeForm(); loadTypes();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML public void handleDeleteType() {
        TransferType sel = typesTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        if (!confirm("Delete type '" + sel.getName() + "'?")) return;
        try { transferTypeService.deleteTransferType(sel.getId(), sessionContext.getCurrentUserId()); resetTypeForm(); loadTypes();
        } catch (Exception e) { showAlert("Error", e.getMessage()); }
    }

    @FXML public void handleCancelType() { resetTypeForm(); }

    private void resetTypeForm() {
        typeNameField.clear();
        typesTable.getSelectionModel().clearSelection();
        typeAddBtn.setVisible(true); typeAddBtn.setManaged(true);
        typeEditButtons.setVisible(false); typeEditButtons.setManaged(false);
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