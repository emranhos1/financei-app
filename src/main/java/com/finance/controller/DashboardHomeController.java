package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.entity.AccountType;
import com.finance.entity.Category;
import com.finance.service.AccountService;
import com.finance.service.AccountTypeService;
import com.finance.service.CategoryService;
import com.finance.service.TransactionService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardHomeController {
    private final SessionContext sessionContext;
    private final AccountService accountService;
    private final AccountTypeService accountTypeService;
    private final TransactionService transactionService;
    private final CategoryService categoryService;

    @FXML private HBox balanceCardsBox;
    @FXML private Label todayIncomeLabel;
    @FXML private Label todayExpenseLabel;
    @FXML private Label todayNetLabel;
    @FXML private Label monthIncomeLabel;
    @FXML private Label monthExpenseLabel;
    @FXML private Label monthNetLabel;
    @FXML private Label yearIncomeLabel;
    @FXML private Label yearExpenseLabel;
    @FXML private Label yearNetLabel;
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private Label catTodayIncLabel;
    @FXML private Label catTodayExpLabel;
    @FXML private Label catMonthIncLabel;
    @FXML private Label catMonthExpLabel;
    @FXML private Label catYearIncLabel;
    @FXML private Label catYearExpLabel;

    @FXML
    public void initialize() {
        setupCategoryComboBox();
        refreshDashboard();
    }

    private void setupCategoryComboBox() {
        categoryComboBox.setConverter(new StringConverter<Category>() {
            public String toString(Category c) { return c == null ? "" : c.getName() + " (" + c.getType().name() + ")"; }
            public Category fromString(String s) { return null; }
        });
        categoryComboBox.setItems(FXCollections.observableArrayList(
                categoryService.getCategoriesByUserId(sessionContext.getCurrentUserId())));
        categoryComboBox.setOnAction(e -> refreshCategoryCard());
    }

    public void refreshDashboard() {
        Long userId = sessionContext.getCurrentUserId();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);

        buildBalanceCards(userId);

        BigDecimal tInc = transactionService.getTotalIncome(userId, today, today);
        BigDecimal tExp = transactionService.getTotalExpense(userId, today, today);
        todayIncomeLabel.setText(fmt(tInc));
        todayExpenseLabel.setText(fmt(tExp));
        todayNetLabel.setText(fmt(tInc.subtract(tExp)));

        BigDecimal mInc = transactionService.getTotalIncome(userId, monthStart, today);
        BigDecimal mExp = transactionService.getTotalExpense(userId, monthStart, today);
        monthIncomeLabel.setText(fmt(mInc));
        monthExpenseLabel.setText(fmt(mExp));
        monthNetLabel.setText(fmt(mInc.subtract(mExp)));

        BigDecimal yInc = transactionService.getTotalIncome(userId, yearStart, today);
        BigDecimal yExp = transactionService.getTotalExpense(userId, yearStart, today);
        yearIncomeLabel.setText(fmt(yInc));
        yearExpenseLabel.setText(fmt(yExp));
        yearNetLabel.setText(fmt(yInc.subtract(yExp)));

        refreshCategoryCard();
    }

    private void buildBalanceCards(Long userId) {
        balanceCardsBox.getChildren().clear();
        List<AccountType> types = accountTypeService.getAccountTypesByUserId(userId);
        for (AccountType at : types) {
            // FK join — exact name from account_types table, no string matching
            BigDecimal balance = accountService.getBalanceByAccountType(userId, at);

            Label typeLabel = new Label(at.getName());
            typeLabel.getStyleClass().add("balance-card-title");

            Label amtLabel = new Label(fmt(balance));
            amtLabel.getStyleClass().add("balance-card-amount");

            VBox card = new VBox(4, typeLabel, amtLabel);
            card.getStyleClass().add("balance-card");
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
            balanceCardsBox.getChildren().add(card);
        }
    }

    private void refreshCategoryCard() {
        Category cat = categoryComboBox.getValue();
        if (cat == null) { clearCategoryLabels(); return; }
        Long userId = sessionContext.getCurrentUserId();
        Long catId = cat.getId();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);

        catTodayIncLabel.setText(fmt(transactionService.getIncomeByCategory(userId, catId, today, today)));
        catTodayExpLabel.setText(fmt(transactionService.getExpenseByCategory(userId, catId, today, today)));
        catMonthIncLabel.setText(fmt(transactionService.getIncomeByCategory(userId, catId, monthStart, today)));
        catMonthExpLabel.setText(fmt(transactionService.getExpenseByCategory(userId, catId, monthStart, today)));
        catYearIncLabel.setText(fmt(transactionService.getIncomeByCategory(userId, catId, yearStart, today)));
        catYearExpLabel.setText(fmt(transactionService.getExpenseByCategory(userId, catId, yearStart, today)));
    }

    private void clearCategoryLabels() {
        String z = "৳ 0.00";
        catTodayIncLabel.setText(z); catTodayExpLabel.setText(z);
        catMonthIncLabel.setText(z); catMonthExpLabel.setText(z);
        catYearIncLabel.setText(z); catYearExpLabel.setText(z);
    }

    private String fmt(BigDecimal v) { return String.format("৳ %.2f", v); }
}