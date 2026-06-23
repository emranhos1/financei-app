package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.service.AccountService;
import com.finance.service.TransactionService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Controller
@RequiredArgsConstructor
public class DashboardHomeController {
    private final SessionContext sessionContext;
    private final AccountService accountService;
    private final TransactionService transactionService;

    @FXML
    private Label netWorthLabel;

    @FXML
    private Label todayIncomeLabel;

    @FXML
    private Label todayExpenseLabel;

    @FXML
    private Label todayNetLabel;

    @FXML
    private Label monthIncomeLabel;

    @FXML
    private Label monthExpenseLabel;

    @FXML
    private Label monthNetLabel;

    @FXML
    private Label yearIncomeLabel;

    @FXML
    private Label yearExpenseLabel;

    @FXML
    private Label yearNetLabel;

    @FXML
    private VBox summaryVBox;

    @FXML
    public void initialize() {
        refreshDashboard();
    }

    public void refreshDashboard() {
        Long userId = sessionContext.getCurrentUserId();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);

        // Net worth
        BigDecimal netWorth = accountService.getNetWorth(userId);
        netWorthLabel.setText(String.format("৳ %.2f", netWorth));

        // Today
        BigDecimal todayIncome = transactionService.getTotalIncome(userId, today, today);
        BigDecimal todayExpense = transactionService.getTotalExpense(userId, today, today);
        BigDecimal todayNet = todayIncome.subtract(todayExpense);
        todayIncomeLabel.setText(String.format("৳ %.2f", todayIncome));
        todayExpenseLabel.setText(String.format("৳ %.2f", todayExpense));
        todayNetLabel.setText(String.format("৳ %.2f", todayNet));

        // Month
        BigDecimal monthIncome = transactionService.getTotalIncome(userId, monthStart, today);
        BigDecimal monthExpense = transactionService.getTotalExpense(userId, monthStart, today);
        BigDecimal monthNet = monthIncome.subtract(monthExpense);
        monthIncomeLabel.setText(String.format("৳ %.2f", monthIncome));
        monthExpenseLabel.setText(String.format("৳ %.2f", monthExpense));
        monthNetLabel.setText(String.format("৳ %.2f", monthNet));

        // Year
        BigDecimal yearIncome = transactionService.getTotalIncome(userId, yearStart, today);
        BigDecimal yearExpense = transactionService.getTotalExpense(userId, yearStart, today);
        BigDecimal yearNet = yearIncome.subtract(yearExpense);
        yearIncomeLabel.setText(String.format("৳ %.2f", yearIncome));
        yearExpenseLabel.setText(String.format("৳ %.2f", yearExpense));
        yearNetLabel.setText(String.format("৳ %.2f", yearNet));
    }
}
