package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.entity.Account;
import com.finance.entity.AccountType;
import com.finance.entity.Category;
import com.finance.entity.Loan;
import com.finance.entity.MonthlyExpenseOverride;
import com.finance.entity.Transaction;
import com.finance.service.AccountService;
import com.finance.service.AccountTypeService;
import com.finance.service.CategoryService;
import com.finance.service.LoanService;
import com.finance.service.MonthlyExpenseOverrideService;
import com.finance.service.TransactionService;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardHomeController {
    private final SessionContext sessionContext;
    private final AccountService accountService;
    private final AccountTypeService accountTypeService;
    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final MonthlyExpenseOverrideService monthlyExpenseOverrideService;
    private final LoanService loanService;
    private final DashboardController dashboardController;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter FULL_MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final String[] BADGE_PALETTE = {"badge-teal", "badge-gold", "badge-blue", "badge-purple", "badge-rose", "badge-mint"};

    @FXML private HBox loanReminderBanner;
    @FXML private Label loanReminderLabel;

    @FXML private HBox maturityReminderBanner;
    @FXML private Label maturityReminderLabel;

    @FXML private HBox lowBalanceReminderBanner;
    @FXML private Label lowBalanceReminderLabel;

    @FXML private HBox balanceCardsBox;


    @FXML private VBox recentTxBox;

    @FXML private StackPane expenseRingStack;
    @FXML private Label expenseRingLabel;
    @FXML private Label topExpenseLabel;
    @FXML private PieChart dashExpensePieChart;

    @FXML private VBox loansCard;
    @FXML private VBox loansBox;
    @FXML private HBox loansTotalBox;

    @FXML private VBox cashMonthlyExpenseBox;
    @FXML private VBox bankMonthlyExpenseBox;
    @FXML private VBox totalMonthlyExpenseBox;

    @FXML
    public void initialize() {
        refreshDashboard();
        showLoanReminderToast();
        loanReminderBanner.setCursor(javafx.scene.Cursor.HAND);
        loanReminderBanner.setOnMouseClicked(e -> dashboardController.goToLoansTab());

        showMaturityReminderToast();
        maturityReminderBanner.setCursor(javafx.scene.Cursor.HAND);
        maturityReminderBanner.setOnMouseClicked(e -> dashboardController.goToAccountsTab());

        showLowBalanceToast();
        lowBalanceReminderBanner.setCursor(javafx.scene.Cursor.HAND);
        lowBalanceReminderBanner.setOnMouseClicked(e -> dashboardController.goToAccountsTab());

        loansCard.setCursor(javafx.scene.Cursor.HAND);
        loansCard.setOnMouseClicked(e -> dashboardController.goToLoansTab());
    }

    private void showLoanReminderToast() {
        List<Loan> dueLoans = loanService.getDueSoonOrOverdueLoans(sessionContext.getCurrentUserId(), 3);
        if (dueLoans.isEmpty()) {
            loanReminderBanner.setVisible(false);
            loanReminderBanner.setManaged(false);
            return;
        }

        LocalDate today = LocalDate.now();
        Loan first = dueLoans.get(0);
        long daysDiff = ChronoUnit.DAYS.between(today, first.getDueDate());
        String status = daysDiff < 0 ? "overdue by " + (-daysDiff) + " day(s)"
                : daysDiff == 0 ? "due today"
                : "due in " + daysDiff + " day(s)";
        String message = dueLoans.size() == 1
                ? "Loan with " + first.getPersonName() + " is " + status + " — check the Loans tab."
                : dueLoans.size() + " loans are due soon or overdue — check the Loans tab.";
        loanReminderLabel.setText(message);
        loanReminderBanner.setVisible(true);
        loanReminderBanner.setManaged(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(6));
        pause.setOnFinished(e -> {
            loanReminderBanner.setVisible(false);
            loanReminderBanner.setManaged(false);
        });
        pause.play();
    }

    private void showMaturityReminderToast() {
        List<Account> maturingAccounts = accountService.getUpcomingMaturities(sessionContext.getCurrentUserId(), 3);
        if (maturingAccounts.isEmpty()) {
            maturityReminderBanner.setVisible(false);
            maturityReminderBanner.setManaged(false);
            return;
        }

        LocalDate today = LocalDate.now();
        Account first = maturingAccounts.get(0);
        long daysDiff = ChronoUnit.DAYS.between(today, first.getMaturityDate());
        String status = daysDiff < 0 ? "matured " + (-daysDiff) + " day(s) ago"
                : daysDiff == 0 ? "matures today"
                : "matures in " + daysDiff + " day(s)";
        String message = maturingAccounts.size() == 1
                ? first.getName() + " " + status + " — check the Accounts tab."
                : maturingAccounts.size() + " accounts are maturing soon or overdue — check the Accounts tab.";
        maturityReminderLabel.setText(message);
        maturityReminderBanner.setVisible(true);
        maturityReminderBanner.setManaged(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(6));
        pause.setOnFinished(e -> {
            maturityReminderBanner.setVisible(false);
            maturityReminderBanner.setManaged(false);
        });
        pause.play();
    }

    private void showLowBalanceToast() {
        List<Account> lowAccounts = accountService.getAccountsBelowThreshold(sessionContext.getCurrentUserId());
        if (lowAccounts.isEmpty()) {
            lowBalanceReminderBanner.setVisible(false);
            lowBalanceReminderBanner.setManaged(false);
            return;
        }

        Account first = lowAccounts.get(0);
        String message = lowAccounts.size() == 1
                ? first.getName() + " balance (৳ " + first.getBalance().toPlainString()
                        + ") is below your ৳ " + first.getLowBalanceThreshold().toPlainString() + " alert — check the Accounts tab."
                : lowAccounts.size() + " accounts are below their low-balance alert — check the Accounts tab.";
        lowBalanceReminderLabel.setText(message);
        lowBalanceReminderBanner.setVisible(true);
        lowBalanceReminderBanner.setManaged(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(6));
        pause.setOnFinished(e -> {
            lowBalanceReminderBanner.setVisible(false);
            lowBalanceReminderBanner.setManaged(false);
        });
        pause.play();
    }

    public void refreshDashboard() {
        Long userId = sessionContext.getCurrentUserId();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        List<Account> accounts = accountService.getAccountsByUserId(userId);

        buildBalanceCards(userId);
        buildLoans(userId);
        buildRecentTransactions(userId, accounts);
        buildExpenseRing(userId, accounts, monthStart, today);
        buildTopExpense(userId, accounts, monthStart, today);
        BigDecimal[] cashByMonth = buildMonthlyExpenseByType(cashMonthlyExpenseBox, userId, accounts, "CASH");
        BigDecimal[] bankByMonth = buildMonthlyExpenseByType(bankMonthlyExpenseBox, userId, accounts, "BANK");
        buildMonthlyExpenseCombined(totalMonthlyExpenseBox, cashByMonth, bankByMonth);
    }

    private void buildBalanceCards(Long userId) {
        balanceCardsBox.getChildren().clear();
        List<AccountType> types = accountTypeService.getAccountTypesByUserId(userId);
        for (AccountType at : types) {
            BigDecimal balance = accountService.getBalanceByAccountType(userId, at);
            if (balance.compareTo(BigDecimal.ZERO) == 0) continue;

            Label badge = new Label(initials(at.getName()));
            badge.getStyleClass().addAll("dash-badge", paletteClass(at.getName()));

            Label typeLabel = new Label(at.getName());
            typeLabel.getStyleClass().add("dash-account-type");

            Label amtLabel = new Label(fmt(balance));
            amtLabel.getStyleClass().add("dash-account-amount");

            VBox textBox = new VBox(2, typeLabel, amtLabel);
            HBox card = new HBox(10, badge, textBox);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("dash-account-card");
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
            balanceCardsBox.getChildren().add(card);
        }
    }

    private void buildRecentTransactions(Long userId, List<Account> accounts) {
        Map<Long, Account> accountsById = new LinkedHashMap<>();
        for (Account a : accounts) accountsById.put(a.getId(), a);
        Map<Long, Category> categoriesById = new LinkedHashMap<>();
        for (Category c : categoryService.getCategoriesByUserId(userId)) categoriesById.put(c.getId(), c);

        List<Transaction> recent = transactionService.getTransactionsByUserId(userId);
        recentTxBox.getChildren().clear();

        if (recent.isEmpty()) {
            Label empty = new Label("No transactions yet");
            empty.getStyleClass().add("dash-section-hint");
            recentTxBox.getChildren().add(empty);
            return;
        }

        int limit = Math.min(3, recent.size());
        for (int i = 0; i < limit; i++) {
            Transaction tx = recent.get(i);
            recentTxBox.getChildren().add(buildTxRow(tx, accountsById, categoriesById));
        }
    }

    private HBox buildTxRow(Transaction tx, Map<Long, Account> accountsById, Map<Long, Category> categoriesById) {
        String glyph;
        String iconClass;
        String amountClass;
        String title;
        String amountText;

        if (tx.getType() == Transaction.TransactionType.INCOME) {
            glyph = "+"; iconClass = "dash-tx-icon-in"; amountClass = "dash-tx-amount-pos";
            Category c = categoriesById.get(tx.getCategoryId());
            title = c != null ? c.getName() : "Income";
            amountText = "+" + fmt(tx.getAmount());
        } else if (tx.getType() == Transaction.TransactionType.EXPENSE) {
            glyph = "−"; iconClass = "dash-tx-icon-out"; amountClass = "dash-tx-amount-neg";
            Category c = categoriesById.get(tx.getCategoryId());
            title = c != null ? c.getName() : "Expense";
            amountText = "-" + fmt(tx.getAmount());
        } else {
            glyph = "\u21C4"; iconClass = "dash-tx-icon-transfer"; amountClass = "dash-tx-amount-neutral";
            title = "Transfer";
            amountText = fmt(tx.getAmount());
        }

        Label icon = new Label(glyph);
        icon.getStyleClass().addAll("dash-tx-icon", iconClass);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dash-tx-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label amountLabel = new Label(amountText);
        amountLabel.getStyleClass().add(amountClass);

        HBox row = new HBox(10, icon, titleLabel, spacer, amountLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dash-tx-row");
        return row;
    }

    /**
     * "Expense of this month's income" ring - scoped to Bank + Cash only (like the Net worth
     * card), not every account type. mInc is income received into Bank/Cash accounts this month.
     * mExp is EXPENSE transactions paid from Bank/Cash accounts this month (transfers excluded).
     */
    private void buildExpenseRing(Long userId, List<Account> accounts, LocalDate monthStart, LocalDate today) {
        List<Long> bankAndCashIds = new ArrayList<>();
        for (Account a : accounts) {
            String atName = a.getAccountType().getName();
            if ("BANK".equalsIgnoreCase(atName) || "CASH".equalsIgnoreCase(atName)) bankAndCashIds.add(a.getId());
        }
        BigDecimal mInc = transactionService.getIncomeByAccountIds(userId, bankAndCashIds, monthStart, today);
        BigDecimal mExp = transactionService.getEffectiveExpense(userId, accounts, "BANK", monthStart, today)
                .add(transactionService.getEffectiveExpense(userId, accounts, "CASH", monthStart, today));

        BigDecimal pctPrecise;
        if (mInc.compareTo(BigDecimal.ZERO) <= 0) {
            pctPrecise = mExp.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        } else {
            pctPrecise = mExp.divide(mInc, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            if (pctPrecise.compareTo(BigDecimal.valueOf(100)) > 0) pctPrecise = BigDecimal.valueOf(100);
            if (pctPrecise.compareTo(BigDecimal.ZERO) < 0) pctPrecise = BigDecimal.ZERO;
        }
        int pct = pctPrecise.setScale(0, RoundingMode.HALF_UP).intValue();
        expenseRingLabel.setText(pctPrecise.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%");

        // remove any previously drawn ring shapes (keep the centered label)
        expenseRingStack.getChildren().removeIf(n -> n instanceof Circle);

        double radius = 22;
        Circle track = new Circle(radius, radius, radius);
        track.getStyleClass().add("dash-ring-track");

        Circle progress = new Circle(radius, radius, radius);
        progress.getStyleClass().add("dash-ring-progress");
        progress.setStrokeLineCap(StrokeLineCap.ROUND);
        double circumference = 2 * Math.PI * radius;
        double dash = Math.max(0.01, circumference * pct / 100.0);
        progress.getStrokeDashArray().setAll(dash, circumference);
        progress.setRotate(-90);

        expenseRingStack.getChildren().add(0, progress);
        expenseRingStack.getChildren().add(0, track);
    }

    /** Scoped to Bank + Cash accounts only, matching Net worth and the expense ring. */
    private void buildTopExpense(Long userId, List<Account> accounts, LocalDate monthStart, LocalDate today) {
        List<Long> bankAndCashIds = new ArrayList<>();
        for (Account a : accounts) {
            String atName = a.getAccountType().getName();
            if ("BANK".equalsIgnoreCase(atName) || "CASH".equalsIgnoreCase(atName)) bankAndCashIds.add(a.getId());
        }

        List<Category> expenseCategories = categoryService.getExpenseCategories(userId);
        Category topCategory = null;
        BigDecimal topAmount = BigDecimal.ZERO;
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Category c : expenseCategories) {
            BigDecimal amount = transactionService.getExpenseByCategoryForAccounts(userId, c.getId(), bankAndCashIds, monthStart, today);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                pieData.add(new PieChart.Data(c.getName(), amount.doubleValue()));
            }
            if (amount.compareTo(topAmount) > 0) { topAmount = amount; topCategory = c; }
        }
        dashExpensePieChart.setData(pieData);

        for (PieChart.Data slice : pieData) {
            Tooltip tooltip = new Tooltip(slice.getName() + "\n" + fmt(BigDecimal.valueOf(slice.getPieValue())));
            Tooltip.install(slice.getNode(), tooltip);
        }

        if (topCategory == null) {
            topExpenseLabel.setText("No expense this month");
        } else {
            topExpenseLabel.setText(topCategory.getName() + " — " + fmt(topAmount));
        }
    }

    /** "Given" (people who owe the user, LENT) and "Taken" (people the user owes, BORROWED)
     *  sections, one line per person with a non-zero outstanding amount, largest first. The two
     *  sides are not netted against each other. The whole card is hidden when both are empty. */
    private void buildLoans(Long userId) {
        List<LoanService.PersonSummary> summaries = loanService.getPersonSummaries(userId);
        List<LoanService.PersonSummary> given = new ArrayList<>();
        List<LoanService.PersonSummary> taken = new ArrayList<>();
        for (LoanService.PersonSummary ps : summaries) {
            if (ps.getLentRemaining().compareTo(BigDecimal.ZERO) > 0) given.add(ps);
            if (ps.getBorrowedRemaining().compareTo(BigDecimal.ZERO) > 0) taken.add(ps);
        }
        given.sort((a, b) -> b.getLentRemaining().compareTo(a.getLentRemaining()));
        taken.sort((a, b) -> b.getBorrowedRemaining().compareTo(a.getBorrowedRemaining()));

        loansBox.getChildren().clear();
        loansTotalBox.getChildren().clear();
        loansCard.setVisible(!given.isEmpty() || !taken.isEmpty());
        if (!loansCard.isVisible()) return;

        // totals sit next to the card heading: Given in green, Taken in red
        if (!given.isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            for (LoanService.PersonSummary ps : given) total = total.add(ps.getLentRemaining());
            Label givenTotal = new Label(fmt(total));
            givenTotal.getStyleClass().add("dash-tx-amount-pos");
            Tooltip.install(givenTotal, new Tooltip("Total given - still to receive"));
            loansTotalBox.getChildren().add(givenTotal);
        }
        if (!taken.isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            for (LoanService.PersonSummary ps : taken) total = total.add(ps.getBorrowedRemaining());
            if (!given.isEmpty()) loansTotalBox.getChildren().add(new Label("/"));
            Label takenTotal = new Label(fmt(total));
            takenTotal.getStyleClass().add("dash-tx-amount-neg");
            Tooltip.install(takenTotal, new Tooltip("Total taken - still to pay back"));
            loansTotalBox.getChildren().add(takenTotal);
        }

        if (!given.isEmpty()) {
            for (LoanService.PersonSummary ps : given) {
                BigDecimal repaid = ps.getTotalLent().subtract(ps.getLentRemaining());
                loansBox.getChildren().add(buildLoanPersonRow(ps.getPersonName(), ps.getLentRemaining(),
                        "dash-tx-amount-pos", "Given " + fmt(ps.getTotalLent()) + " · Repaid " + fmt(repaid)));
            }
        }
        if (!taken.isEmpty()) {
            for (LoanService.PersonSummary ps : taken) {
                BigDecimal repaid = ps.getTotalBorrowed().subtract(ps.getBorrowedRemaining());
                loansBox.getChildren().add(buildLoanPersonRow(ps.getPersonName(), ps.getBorrowedRemaining(),
                        "dash-tx-amount-neg", "Taken " + fmt(ps.getTotalBorrowed()) + " · Repaid " + fmt(repaid)));
            }
        }
    }

    /** One line per person; the given/taken/repaid breakdown is kept in a tooltip to save height. */
    private HBox buildLoanPersonRow(String name, BigDecimal remaining, String amountClass, String detail) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("dash-tx-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label amountLabel = new Label(fmt(remaining));
        amountLabel.getStyleClass().add(amountClass);
        HBox row = new HBox(6, nameLabel, spacer, amountLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dash-tx-row");
        Tooltip.install(row, new Tooltip(detail));
        return row;
    }

    /**
     * Shows the current year's Jan-Dec expense total for every account of the given account type
     * (e.g. "CASH" or "BANK"), read from the {@code monthly_expense_overrides} table, plus a
     * Total and a 12-month Average row.
     * <p>
     * Behavior:
     * - The CURRENT month is recalculated live from EXPENSE transactions (paid from an account of
     *   this type) every time the dashboard loads, and that figure is auto-saved into the table -
     *   unless the user has manually corrected this month's figure, in which case the manual
     *   value wins and is left untouched.
     * - Every OTHER month (past or future) is never recalculated - it simply shows whatever is
     *   saved in the table (0 if nothing was ever saved for it). This is what makes a month
     *   "freeze" once it's no longer current: only a manual ✎ edit can change it after that.
     * Returns the 12 figures shown (index 0 = Jan) so the Cash + Bank card can add them up.
     */
    private BigDecimal[] buildMonthlyExpenseByType(VBox container, Long userId, List<Account> accounts, String typeName) {
        container.getChildren().clear();
        BigDecimal[] byMonth = new BigDecimal[12];

        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int currentMonth = today.getMonthValue();

        Map<Integer, MonthlyExpenseOverride> records =
                monthlyExpenseOverrideService.getRecordsForYear(userId, typeName, year);

        BigDecimal total = BigDecimal.ZERO;
        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(year, month);
            MonthlyExpenseOverride existing = records.get(month);
            BigDecimal amount;
            boolean isManual = existing != null && Boolean.TRUE.equals(existing.getIsManual());

            if (month == currentMonth && !isManual) {
                BigDecimal calculated = transactionService.getEffectiveExpense(userId, accounts, typeName, ym.atDay(1), ym.atEndOfMonth());
                monthlyExpenseOverrideService.autoSaveCurrentMonth(userId, typeName, year, month, calculated);
                amount = calculated;
            } else {
                amount = existing != null ? existing.getAmount() : BigDecimal.ZERO;
            }

            total = total.add(amount);
            byMonth[month - 1] = amount;
            container.getChildren().add(buildMonthlyExpenseRow(ym, typeName, amount, isManual, userId));
        }

        BigDecimal avg = total.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        container.getChildren().add(buildSummaryRow("Total", fmt(total), "dash-monthly-row-total"));
        container.getChildren().add(buildSummaryRow("AVG", fmt(avg), "dash-monthly-row-avg"));
        return byMonth;
    }

    /** Cash + Bank per month: the sum of the two cards' shown figures (manual ✎ values included),
     *  so it always agrees with them. Read-only - corrections are made in the Cash/Bank cards. */
    private void buildMonthlyExpenseCombined(VBox container, BigDecimal[] cashByMonth, BigDecimal[] bankByMonth) {
        container.getChildren().clear();
        int year = LocalDate.now().getYear();
        BigDecimal total = BigDecimal.ZERO;
        for (int month = 1; month <= 12; month++) {
            BigDecimal amount = cashByMonth[month - 1].add(bankByMonth[month - 1]);
            total = total.add(amount);

            Label nameLabel = new Label(YearMonth.of(year, month).format(MONTH_FMT).toUpperCase(Locale.ENGLISH));
            nameLabel.getStyleClass().add("dash-card-label");
            nameLabel.setPrefWidth(40);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label valueLabel = new Label(fmt(amount));
            valueLabel.getStyleClass().add("dash-card-label");
            // invisible, zero-width stand-in for the ✎ button: keeps the row as tall as the
            // Cash/Bank rows (so all three cards line up) without leaving a gap after the amount
            Button placeholder = new Button("✎");
            placeholder.getStyleClass().add("dash-monthly-edit-btn");
            placeholder.setVisible(false);
            placeholder.setMinWidth(0);
            placeholder.setPrefWidth(0);
            placeholder.setMaxWidth(0);

            HBox row = new HBox(0, nameLabel, spacer, valueLabel, placeholder);
            row.getStyleClass().add("dash-monthly-row");
            row.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().add(row);
        }

        BigDecimal avg = total.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        container.getChildren().add(buildSummaryRow("Total", fmt(total), "dash-monthly-row-total"));
        container.getChildren().add(buildSummaryRow("AVG", fmt(avg), "dash-monthly-row-avg"));
    }

    private HBox buildMonthlyExpenseRow(YearMonth ym, String typeName, BigDecimal amount, boolean isSaved, Long userId) {
        Label nameLabel = new Label(ym.format(MONTH_FMT).toUpperCase(Locale.ENGLISH));
        nameLabel.getStyleClass().add("dash-card-label");
        nameLabel.setPrefWidth(40);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label(fmt(amount));
        valueLabel.getStyleClass().add(isSaved ? "dash-monthly-value-saved" : "dash-card-label");
        if (isSaved) {
            Tooltip.install(valueLabel, new Tooltip("Manually saved value — overrides the calculated total"));
        }

        Button editBtn = new Button("✎");
        editBtn.getStyleClass().add("dash-monthly-edit-btn");
        Tooltip.install(editBtn, new Tooltip("Manually save this month's actual " + typeName + " expense"));
        editBtn.setOnAction(e -> handleEditMonthlyExpense(ym, typeName, amount, userId));

        HBox row = new HBox(6, nameLabel, spacer, valueLabel, editBtn);
        row.getStyleClass().add("dash-monthly-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void handleEditMonthlyExpense(YearMonth ym, String typeName, BigDecimal currentAmount, Long userId) {
        TextInputDialog dialog = new TextInputDialog(currentAmount.setScale(2, RoundingMode.HALF_UP).toPlainString());
        dialog.setTitle("Save month's actual expense");
        dialog.setHeaderText(null);
        dialog.setContentText(ym.format(FULL_MONTH_FMT) + " — " + typeName + " expense (৳):");
        dialog.showAndWait().ifPresent(input -> {
            BigDecimal parsed;
            try {
                parsed = new BigDecimal(input.trim());
            } catch (NumberFormatException ex) {
                showValidationError("Please enter a valid amount (e.g. 47780 or 47780.00).");
                return;
            }
            if (parsed.compareTo(BigDecimal.ZERO) < 0) {
                showValidationError("Amount cannot be negative.");
                return;
            }
            monthlyExpenseOverrideService.saveManualOverride(userId, typeName, ym.getYear(), ym.getMonthValue(), parsed);
            refreshDashboard();
        });
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle("Invalid amount");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private HBox buildSummaryRow(String label, String value, String rowStyleClass) {
        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("dash-card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("dash-card-title");

        HBox row = new HBox(nameLabel, spacer, valueLabel);
        row.getStyleClass().add(rowStyleClass);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String trimmed = name.trim();
        return trimmed.substring(0, 1).toUpperCase(Locale.ENGLISH);
    }

    private String paletteClass(String name) {
        int idx = Math.abs((name == null ? "" : name).hashCode()) % BADGE_PALETTE.length;
        return BADGE_PALETTE[idx];
    }

    private String fmt(BigDecimal v) { return String.format("৳ %.2f", v); }
}
