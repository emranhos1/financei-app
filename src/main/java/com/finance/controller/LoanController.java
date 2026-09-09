package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.entity.Account;
import com.finance.entity.Category;
import com.finance.entity.Loan;
import com.finance.entity.LoanPerson;
import com.finance.entity.TransferType;
import com.finance.service.AccountService;
import com.finance.service.CategoryService;
import com.finance.service.LoanService;
import com.finance.service.LoanService.PersonSummary;
import com.finance.service.TransferTypeService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class LoanController {
    private final SessionContext sessionContext;
    private final LoanService loanService;
    private final AccountService accountService;
    private final TransferTypeService transferTypeService;
    private final CategoryService categoryService;

    @FXML private VBox addLoanBox;
    @FXML private ComboBox<LoanPerson> personComboBox;
    @FXML private RadioButton lentRadio;
    @FXML private RadioButton borrowedRadio;
    @FXML private ComboBox<Account> accountComboBox;
    @FXML private ComboBox<TransferType> transferTypeComboBox;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private TextField amountField;
    @FXML private DatePicker datePicker;
    @FXML private DatePicker dueDatePicker;
    @FXML private TextArea noteArea;
    @FXML private Button saveLoanBtn;

    @FXML private VBox repaymentBox;
    @FXML private Label selectedLoanLabel;
    @FXML private TextField repayAmountField;
    @FXML private ComboBox<Account> repayAccountComboBox;
    @FXML private DatePicker repayDatePicker;
    @FXML private TextField repayNoteField;

    @FXML private VBox settledSelectionBox;
    @FXML private Label settledLoanLabel;

    @FXML private TableView<PersonSummary> personsSummaryTable;
    @FXML private TableColumn<PersonSummary, String> summaryPersonColumn;
    @FXML private TableColumn<PersonSummary, BigDecimal> summaryLentColumn;
    @FXML private TableColumn<PersonSummary, BigDecimal> summaryBorrowedColumn;
    @FXML private TableColumn<PersonSummary, BigDecimal> summaryNetColumn;
    @FXML private TableColumn<PersonSummary, Number> summaryOpenColumn;

    @FXML private Label historyTitleLabel;
    @FXML private TableView<Loan> loansTable;
    @FXML private TableColumn<Loan, String> typeColumn;
    @FXML private TableColumn<Loan, BigDecimal> principalColumn;
    @FXML private TableColumn<Loan, BigDecimal> remainingColumn;
    @FXML private TableColumn<Loan, String> statusColumn;
    @FXML private TableColumn<Loan, LocalDate> dueDateColumn;
    @FXML private TableColumn<Loan, String> noteColumn;

    @FXML private TextField personNameField;
    @FXML private TextArea personNoteArea;
    @FXML private Button addPersonBtn;
    @FXML private HBox personEditButtons;
    @FXML private TableView<LoanPerson> personsTable;
    @FXML private TableColumn<LoanPerson, String> personListNameColumn;
    @FXML private TableColumn<LoanPerson, String> personListNoteColumn;

    private Loan selectedLoan = null;
    private PersonSummary selectedPersonSummary = null;
    private LoanPerson selectedPersonForEdit = null;

    @FXML
    public void initialize() {
        setupAccountComboBoxes();
        setupPersonComboBox();
        setupTransferTypeComboBox();
        setupCategoryComboBox();
        setupSummaryTable();
        setupHistoryTable();
        setupPersonsTable();

        loadAccounts();
        loadTransferTypes();
        loadPersons();
        refreshCategories();
        lentRadio.selectedProperty().addListener((obs, o, sel) -> refreshCategories());
        borrowedRadio.selectedProperty().addListener((obs, o, sel) -> refreshCategories());
        loadPersonSummaries();
        resetLoanForm();
        resetSelection();
        resetPersonSelection();
    }

    private void setupAccountComboBoxes() {
        StringConverter<Account> converter = new StringConverter<Account>() {
            public String toString(Account a) {
                return a == null ? "" : a.getName() + " [" + a.getAccountType().getName() + "]";
            }
            public Account fromString(String s) { return null; }
        };
        accountComboBox.setConverter(converter);
        repayAccountComboBox.setConverter(converter);
    }

    private void setupPersonComboBox() {
        personComboBox.setConverter(new StringConverter<LoanPerson>() {
            public String toString(LoanPerson p) { return p == null ? "" : p.getName(); }
            public LoanPerson fromString(String s) { return null; }
        });
    }

    private void setupTransferTypeComboBox() {
        transferTypeComboBox.setConverter(new StringConverter<TransferType>() {
            public String toString(TransferType t) { return t == null ? "" : t.getName(); }
            public TransferType fromString(String s) { return null; }
        });
    }

    private void loadTransferTypes() {
        List<TransferType> types = transferTypeService.getTransferTypesByUserId(sessionContext.getCurrentUserId());
        transferTypeComboBox.setItems(FXCollections.observableArrayList(types));
    }

    private void setupCategoryComboBox() {
        categoryComboBox.setConverter(new StringConverter<Category>() {
            public String toString(Category c) { return c == null ? "" : c.getName(); }
            public Category fromString(String s) { return null; }
        });
    }

    private void refreshCategories() {
        Long userId = sessionContext.getCurrentUserId();
        List<Category> categories = lentRadio.isSelected()
                ? categoryService.getExpenseCategories(userId)
                : categoryService.getIncomeCategories(userId);
        categoryComboBox.setItems(FXCollections.observableArrayList(categories));
        categoryComboBox.setValue(null);
    }

    private void setupSummaryTable() {
        summaryPersonColumn.setCellValueFactory(new PropertyValueFactory<>("personName"));
        summaryLentColumn.setCellValueFactory(new PropertyValueFactory<>("totalLent"));
        summaryBorrowedColumn.setCellValueFactory(new PropertyValueFactory<>("totalBorrowed"));
        summaryNetColumn.setCellValueFactory(new PropertyValueFactory<>("netRemaining"));
        summaryOpenColumn.setCellValueFactory(new PropertyValueFactory<>("openCount"));

        personsSummaryTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) {
                selectedPersonSummary = sel;
                historyTitleLabel.setText("History - " + sel.getPersonName());
                loadHistoryForPerson(sel.getPersonId());
                resetSelection();
            }
        });
    }

    private void setupHistoryTable() {
        principalColumn.setCellValueFactory(new PropertyValueFactory<>("principalAmount"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));

        typeColumn.setCellValueFactory(cd -> new SimpleStringProperty(typeLabel(cd.getValue())));
        statusColumn.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus().name()));
        remainingColumn.setCellValueFactory(cd -> new SimpleObjectProperty<>(loanService.getRemaining(cd.getValue())));

        loansTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) populateSelection(sel);
        });
    }

    private void setupPersonsTable() {
        personListNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        personListNoteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));

        personsTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) {
                selectedPersonForEdit = sel;
                personNameField.setText(sel.getName());
                personNoteArea.setText(sel.getNote() == null ? "" : sel.getNote());
                addPersonBtn.setVisible(false);
                addPersonBtn.setManaged(false);
                personEditButtons.setVisible(true);
                personEditButtons.setManaged(true);
            }
        });
    }

    private void populateSelection(Loan loan) {
        selectedLoan = loan;
        addLoanBox.setVisible(false);
        addLoanBox.setManaged(false);

        boolean open = loan.getStatus() == Loan.LoanStatus.OPEN;
        repaymentBox.setVisible(open);
        repaymentBox.setManaged(open);
        settledSelectionBox.setVisible(!open);
        settledSelectionBox.setManaged(!open);

        if (open) {
            BigDecimal remaining = loanService.getRemaining(loan);
            selectedLoanLabel.setText(loan.getPersonName() + " - " + typeLabel(loan)
                    + " - Remaining: ৳ " + remaining.toPlainString());
            repayAmountField.clear();
            repayNoteField.clear();
            repayDatePicker.setValue(LocalDate.now());
            repayAccountComboBox.getItems().stream()
                    .filter(a -> a.getId().equals(loan.getAccountId())).findFirst()
                    .ifPresent(repayAccountComboBox::setValue);
        } else {
            settledLoanLabel.setText(loan.getPersonName() + " - " + typeLabel(loan)
                    + " - SETTLED (Principal ৳ " + loan.getPrincipalAmount().toPlainString() + ")");
        }
    }

    private String typeLabel(Loan loan) {
        return loan.getType() == Loan.LoanType.LENT ? "I Gave" : "I Took";
    }

    private void loadAccounts() {
        List<Account> accounts = accountService.getAccountsByUserId(sessionContext.getCurrentUserId());
        accountComboBox.setItems(FXCollections.observableArrayList(accounts));
        repayAccountComboBox.setItems(FXCollections.observableArrayList(accounts));
    }

    private void loadPersons() {
        List<LoanPerson> persons = loanService.getLoanPersonsByUserId(sessionContext.getCurrentUserId());
        personComboBox.setItems(FXCollections.observableArrayList(persons));
        personsTable.setItems(FXCollections.observableArrayList(persons));
    }

    private void loadPersonSummaries() {
        List<PersonSummary> summaries = loanService.getPersonSummaries(sessionContext.getCurrentUserId());
        personsSummaryTable.setItems(FXCollections.observableArrayList(summaries));
        loansTable.getItems().clear();
        historyTitleLabel.setText("History");
        selectedPersonSummary = null;
    }

    private void loadHistoryForPerson(Long loanPersonId) {
        List<Loan> loans = loanService.getLoansByPerson(sessionContext.getCurrentUserId(), loanPersonId);
        loansTable.setItems(FXCollections.observableArrayList(loans));
    }

    @FXML
    public void handleSaveLoan() {
        LoanPerson person = personComboBox.getValue();
        Loan.LoanType type = lentRadio.isSelected() ? Loan.LoanType.LENT : Loan.LoanType.BORROWED;
        Account account = accountComboBox.getValue();
        TransferType transferType = transferTypeComboBox.getValue();
        Category category = categoryComboBox.getValue();
        String amountStr = amountField.getText().trim();
        LocalDate date = datePicker.getValue();
        LocalDate dueDate = dueDatePicker.getValue();
        String note = noteArea.getText().trim();

        if (person == null || account == null || amountStr.isEmpty() || date == null) {
            showAlert("Validation Error", "Person, Account, Date and Amount are required"); return;
        }
        if (!confirm("Save this loan entry?")) return;
        try {
            loanService.createLoan(sessionContext.getCurrentUserId(), person.getId(), type,
                    new BigDecimal(amountStr), account.getId(),
                    transferType == null ? null : transferType.getId(),
                    category == null ? null : category.getId(), date, dueDate, note);
            resetLoanForm();
            loadAccounts();
            loadPersonSummaries();
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Amount must be a valid number");
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    public void handleSaveRepayment() {
        if (selectedLoan == null) return;
        String amountStr = repayAmountField.getText().trim();
        Account account = repayAccountComboBox.getValue();
        LocalDate date = repayDatePicker.getValue();
        String note = repayNoteField.getText().trim();

        if (amountStr.isEmpty() || account == null || date == null) {
            showAlert("Validation Error", "Amount, Account and Date are required"); return;
        }
        if (!confirm("Save this repayment?")) return;
        try {
            Long personId = selectedLoan.getLoanPersonId();
            loanService.recordRepayment(selectedLoan.getId(), sessionContext.getCurrentUserId(),
                    new BigDecimal(amountStr), account.getId(), date, note);
            resetSelection();
            loadAccounts();
            loadPersonSummaries();
            if (personId != null) loadHistoryForPerson(personId);
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Amount must be a valid number");
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    public void handleDeleteLoan() {
        if (selectedLoan == null) return;
        if (!confirm("Delete this loan? This will also reverse the original loan transaction "
                + "and any repayments, restoring the account balance(s).")) return;
        try {
            Long personId = selectedLoan.getLoanPersonId();
            loanService.deleteLoan(selectedLoan.getId(), sessionContext.getCurrentUserId());
            resetSelection();
            loadPersonSummaries();
            if (personId != null) loadHistoryForPerson(personId);
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    public void handleCancelSelection() {
        resetSelection();
    }

    @FXML
    public void handleAddPerson() {
        String name = personNameField.getText().trim();
        String note = personNoteArea.getText().trim();
        if (name.isEmpty()) {
            showAlert("Validation Error", "Name is required"); return;
        }
        try {
            loanService.createLoanPerson(sessionContext.getCurrentUserId(), name, note);
            resetPersonForm();
            loadPersons();
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    public void handleDeletePerson() {
        if (selectedPersonForEdit == null) return;
        if (!confirm("Delete this loan account?")) return;
        try {
            loanService.deleteLoanPerson(selectedPersonForEdit.getId(), sessionContext.getCurrentUserId());
            resetPersonSelection();
            loadPersons();
            loadPersonSummaries();
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    public void handleCancelPersonSelection() {
        resetPersonSelection();
    }

    private void resetLoanForm() {
        personComboBox.setValue(null);
        lentRadio.setSelected(true);
        accountComboBox.setValue(null);
        transferTypeComboBox.setValue(null);
        categoryComboBox.setValue(null);
        amountField.clear();
        datePicker.setValue(LocalDate.now());
        dueDatePicker.setValue(null);
        noteArea.clear();
    }

    private void resetSelection() {
        selectedLoan = null;
        loansTable.getSelectionModel().clearSelection();
        addLoanBox.setVisible(true);
        addLoanBox.setManaged(true);
        repaymentBox.setVisible(false);
        repaymentBox.setManaged(false);
        settledSelectionBox.setVisible(false);
        settledSelectionBox.setManaged(false);
    }

    private void resetPersonForm() {
        personNameField.clear();
        personNoteArea.clear();
    }

    private void resetPersonSelection() {
        selectedPersonForEdit = null;
        personsTable.getSelectionModel().clearSelection();
        resetPersonForm();
        addPersonBtn.setVisible(true);
        addPersonBtn.setManaged(true);
        personEditButtons.setVisible(false);
        personEditButtons.setManaged(false);
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
