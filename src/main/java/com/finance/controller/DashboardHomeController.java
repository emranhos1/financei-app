package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.service.AccountService;
import com.finance.service.TransactionService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class DashboardHomeController {
    private final SessionContext sessionContext;
    private final AccountService accountService;
    private final TransactionService transactionService;

    @FXML private Label netWorthLabel;
    @FXML private Label cashLabel;
    @FXML private Label bankLabel;
    @FXML private Label dpsLabel;

    @FXML private Label todayIncomeLabel;
    @FXML private Label todayExpenseLabel;
    @FXML private Label todayNetLabel;

    @FXML private Label monthIncomeLabel;
    @FXML private Label monthExpenseLabel;
    @FXML private Label monthNetLabel;

    @FXML private Label yearIncomeLabel;
    @FXML private Label yearExpenseLabel;
    @FXML private Label yearNetLabel;

    @FXML
    public void initialize() {
        refreshDashboard();
    }

    public void refreshDashboard() {
        Long userId = sessionContext.getCurrentUserId();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);

        // Net Worth
        BigDecimal netWorth = accountService.getNetWorth(userId);
        netWorthLabel.setText(String.format("৳ %.2f", netWorth));

        // Per-type balances
        BigDecimal cash = accountService.getBalanceByType(userId, "cash");
        BigDecimal bank = accountService.getBalanceByType(userId, "bank");
        BigDecimal dps  = accountService.getBalanceByType(userId, "dps");
        cashLabel.setText(String.format("৳ %.2f", cash));
        bankLabel.setText(String.format("৳ %.2f", bank));
        dpsLabel.setText(String.format("৳ %.2f", dps));

        // Today
        BigDecimal tInc = transactionService.getTotalIncome(userId, today, today);
        BigDecimal tExp = transactionService.getTotalExpense(userId, today, today);
        todayIncomeLabel.setText(String.format("৳ %.2f", tInc));
        todayExpenseLabel.setText(String.format("৳ %.2f", tExp));
        todayNetLabel.setText(String.format("৳ %.2f", tInc.subtract(tExp)));

        // Month
        BigDecimal mInc = transactionService.getTotalIncome(userId, monthStart, today);
        BigDecimal mExp = transactionService.getTotalExpense(userId, monthStart, today);
        monthIncomeLabel.setText(String.format("৳ %.2f", mInc));
        monthExpenseLabel.setText(String.format("৳ %.2f", mExp));
        monthNetLabel.setText(String.format("৳ %.2f", mInc.subtract(mExp)));

        // Year
        BigDecimal yInc = transactionService.getTotalIncome(userId, yearStart, today);
        BigDecimal yExp = transactionService.getTotalExpense(userId, yearStart, today);
        yearIncomeLabel.setText(String.format("৳ %.2f", yInc));
        yearExpenseLabel.setText(String.format("৳ %.2f", yExp));
        yearNetLabel.setText(String.format("৳ %.2f", yInc.subtract(yExp)));
    }
}