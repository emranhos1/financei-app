package com.finance.controller;

import com.finance.context.SessionContext;
import com.finance.entity.Account;
import com.finance.entity.AccountType;
import com.finance.entity.Category;
import com.finance.entity.Transaction;
import com.finance.service.AccountService;
import com.finance.service.AccountTypeService;
import com.finance.service.CategoryService;
import com.finance.service.TransactionService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter TX_DATE_FMT = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter GOAL_DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final String[] BADGE_PALETTE = {"badge-teal", "badge-gold", "badge-blue", "badge-purple", "badge-rose", "badge-mint"};

    @FXML private HBox balanceCardsBox;

    @FXML private Label netWorthAmountLabel;
    @FXML private FlowPane netWorthChipsBox;

    @FXML private VBox recentTxBox;

    @FXML private Label weeklyNetLabel;
    @FXML private HBox weeklyChartBox;

    @FXML private StackPane expenseRingStack;
    @FXML private Label expenseRingLabel;
    @FXML private Label topExpenseLabel;

    @FXML private Label trendAmountLabel;
    @FXML private StackPane trendChartPane;
    @FXML private HBox trendMonthLabelsBox;

    @FXML private FlowPane goalCardsBox;

    @FXML private Label todayIncomeLabel;
    @FXML private Label todayExpenseLabel;
    @FXML private Label monthIncomeLabel;
    @FXML private Label monthExpenseLabel;
    @FXML private Label yearIncomeLabel;
    @FXML private Label yearExpenseLabel;

    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private Label catTodayIncLabel;
    @FXML private Label catTodayExpLabel;
    @FXML private Label catMonthIncLabel;
    @FXML private Label catMonthExpLabel;
    @FXML private Label catYearIncLabel;
    @FXML private Label catYearExpLabel;

    private List<BigDecimal> trendValuesCache = new ArrayList<>();
    private List<String> trendLabelsCache = new ArrayList<>();

    @FXML
    public void initialize() {
        setupCategoryComboBox();
        trendChartPane.widthProperty().addListener((obs, o, n) -> drawTrendChart());
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

        List<Account> accounts = accountService.getAccountsByUserId(userId);

        buildBalanceCards(userId);
        buildNetWorthAndChips(accounts);
        buildRecentTransactions(userId, accounts);
        buildWeeklyChart(userId, today);
        buildExpenseRing(userId, monthStart, today);
        buildTopExpense(userId, monthStart, today);
        buildNetWorthTrend(userId, today);
        buildGoalCards(accounts);

        BigDecimal tInc = transactionService.getTotalIncome(userId, today, today);
        BigDecimal tExp = transactionService.getTotalExpense(userId, today, today);
        todayIncomeLabel.setText(fmt(tInc));
        todayExpenseLabel.setText(fmt(tExp));

        BigDecimal mInc = transactionService.getTotalIncome(userId, monthStart, today);
        BigDecimal mExp = transactionService.getTotalExpense(userId, monthStart, today);
        monthIncomeLabel.setText(fmt(mInc));
        monthExpenseLabel.setText(fmt(mExp));

        BigDecimal yInc = transactionService.getTotalIncome(userId, yearStart, today);
        BigDecimal yExp = transactionService.getTotalExpense(userId, yearStart, today);
        yearIncomeLabel.setText(fmt(yInc));
        yearExpenseLabel.setText(fmt(yExp));

        refreshCategoryCard();
    }

    private void buildBalanceCards(Long userId) {
        balanceCardsBox.getChildren().clear();
        List<AccountType> types = accountTypeService.getAccountTypesByUserId(userId);
        for (AccountType at : types) {
            BigDecimal balance = accountService.getBalanceByAccountType(userId, at);

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

    /** "Net worth" here means liquid funds: any account WITHOUT a maturity date (e.g. Bank, Cash type accounts).
     *  Accounts WITH a maturity date (e.g. DPS, FDR) are treated as goal accounts and shown separately below. */
    private void buildNetWorthAndChips(List<Account> accounts) {
        List<Account> liquidAccounts = new ArrayList<>();
        for (Account a : accounts) if (a.getMaturityDate() == null) liquidAccounts.add(a);

        BigDecimal total = BigDecimal.ZERO;
        Map<Long, AccountType> typeById = new LinkedHashMap<>();
        Map<Long, BigDecimal> sumByType = new LinkedHashMap<>();
        for (Account a : liquidAccounts) {
            total = total.add(a.getBalance());
            Long typeId = a.getAccountType().getId();
            typeById.putIfAbsent(typeId, a.getAccountType());
            sumByType.merge(typeId, a.getBalance(), BigDecimal::add);
        }
        netWorthAmountLabel.setText(fmt(total));

        netWorthChipsBox.getChildren().clear();
        for (Map.Entry<Long, BigDecimal> e : sumByType.entrySet()) {
            AccountType at = typeById.get(e.getKey());
            Label chip = new Label(at.getName() + "  " + fmt(e.getValue()));
            chip.getStyleClass().add("dash-chip");
            netWorthChipsBox.getChildren().add(chip);
        }
        if (sumByType.isEmpty()) {
            Label chip = new Label("No liquid accounts yet");
            chip.getStyleClass().add("dash-section-hint");
            netWorthChipsBox.getChildren().add(chip);
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
        String sub;
        String amountText;

        String dateStr = tx.getDate().format(TX_DATE_FMT);

        if (tx.getType() == Transaction.TransactionType.INCOME) {
            glyph = "+"; iconClass = "dash-tx-icon-in"; amountClass = "dash-tx-amount-pos";
            Category c = categoriesById.get(tx.getCategoryId());
            title = c != null ? c.getName() : "Income";
            Account acc = accountsById.get(tx.getToAccountId());
            sub = (acc != null ? acc.getName() : "") + " · " + dateStr;
            amountText = "+" + fmt(tx.getAmount());
        } else if (tx.getType() == Transaction.TransactionType.EXPENSE) {
            glyph = "−"; iconClass = "dash-tx-icon-out"; amountClass = "dash-tx-amount-neg";
            Category c = categoriesById.get(tx.getCategoryId());
            title = c != null ? c.getName() : "Expense";
            Account acc = accountsById.get(tx.getFromAccountId());
            sub = (acc != null ? acc.getName() : "") + " · " + dateStr;
            amountText = "-" + fmt(tx.getAmount());
        } else {
            glyph = "\u21C4"; iconClass = "dash-tx-icon-transfer"; amountClass = "dash-tx-amount-neutral";
            title = "Transfer";
            Account from = accountsById.get(tx.getFromAccountId());
            Account to = accountsById.get(tx.getToAccountId());
            sub = (from != null ? from.getName() : "") + " \u2192 " + (to != null ? to.getName() : "") + " · " + dateStr;
            amountText = fmt(tx.getAmount());
        }

        Label icon = new Label(glyph);
        icon.getStyleClass().addAll("dash-tx-icon", iconClass);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dash-tx-title");
        Label subLabel = new Label(sub);
        subLabel.getStyleClass().add("dash-tx-sub");
        VBox textBox = new VBox(1, titleLabel, subLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label amountLabel = new Label(amountText);
        amountLabel.getStyleClass().add(amountClass);

        HBox row = new HBox(10, icon, textBox, spacer, amountLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("dash-tx-row");
        return row;
    }

    private void buildWeeklyChart(Long userId, LocalDate today) {
        List<LocalDate> days = new ArrayList<>();
        List<BigDecimal> incomes = new ArrayList<>();
        List<BigDecimal> expenses = new ArrayList<>();
        BigDecimal weekNet = BigDecimal.ZERO;
        double max = 0.01;

        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            BigDecimal inc = transactionService.getTotalIncome(userId, d, d);
            BigDecimal exp = transactionService.getTotalExpense(userId, d, d);
            days.add(d); incomes.add(inc); expenses.add(exp);
            weekNet = weekNet.add(inc).subtract(exp);
            max = Math.max(max, Math.max(inc.doubleValue(), exp.doubleValue()));
        }
        weeklyNetLabel.setText(fmt(weekNet) + " net");

        weeklyChartBox.getChildren().clear();
        double maxBarHeight = 90;
        for (int i = 0; i < days.size(); i++) {
            double incH = Math.max(2, incomes.get(i).doubleValue() / max * maxBarHeight);
            double expH = Math.max(2, expenses.get(i).doubleValue() / max * maxBarHeight);
            if (incomes.get(i).compareTo(BigDecimal.ZERO) == 0) incH = 0;
            if (expenses.get(i).compareTo(BigDecimal.ZERO) == 0) expH = 0;

            Region incBar = new Region();
            incBar.getStyleClass().add("dash-bar-income");
            incBar.setPrefSize(9, incH);
            Region expBar = new Region();
            expBar.getStyleClass().add("dash-bar-expense");
            expBar.setPrefSize(9, expH);

            HBox bars = new HBox(3, incBar, expBar);
            bars.setAlignment(Pos.BOTTOM_CENTER);
            bars.setPrefHeight(maxBarHeight);

            Label dayLabel = new Label(String.valueOf(days.get(i).getDayOfMonth()));
            dayLabel.getStyleClass().add("dash-day-label");

            VBox dayBox = new VBox(4, bars, dayLabel);
            dayBox.setAlignment(Pos.BOTTOM_CENTER);
            weeklyChartBox.getChildren().add(dayBox);
        }
    }

    private void buildExpenseRing(Long userId, LocalDate monthStart, LocalDate today) {
        BigDecimal mInc = transactionService.getTotalIncome(userId, monthStart, today);
        BigDecimal mExp = transactionService.getTotalExpense(userId, monthStart, today);

        int pct;
        if (mInc.compareTo(BigDecimal.ZERO) <= 0) {
            pct = mExp.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        } else {
            BigDecimal ratio = mExp.divide(mInc, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            pct = ratio.setScale(0, RoundingMode.HALF_UP).intValue();
            if (pct > 100) pct = 100;
            if (pct < 0) pct = 0;
        }
        expenseRingLabel.setText(pct + "%");

        // remove any previously drawn ring shapes (keep the centered label)
        expenseRingStack.getChildren().removeIf(n -> n instanceof Circle);

        double radius = 38;
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

    private void buildTopExpense(Long userId, LocalDate monthStart, LocalDate today) {
        List<Category> expenseCategories = categoryService.getExpenseCategories(userId);
        Category topCategory = null;
        BigDecimal topAmount = BigDecimal.ZERO;
        for (Category c : expenseCategories) {
            BigDecimal amount = transactionService.getExpenseByCategory(userId, c.getId(), monthStart, today);
            if (amount.compareTo(topAmount) > 0) { topAmount = amount; topCategory = c; }
        }
        if (topCategory == null) {
            topExpenseLabel.setText("No expense this month");
        } else {
            topExpenseLabel.setText(topCategory.getName() + " — " + fmt(topAmount));
        }
    }

    private void buildNetWorthTrend(Long userId, LocalDate today) {
        BigDecimal currentNetWorth = accountService.getNetWorth(userId);
        YearMonth currentYm = YearMonth.from(today);

        BigDecimal[] vals = new BigDecimal[6];
        YearMonth cursor = currentYm;
        BigDecimal runningValue = currentNetWorth;
        vals[5] = runningValue;
        for (int idx = 4; idx >= 0; idx--) {
            BigDecimal change = monthlyNetChange(userId, cursor);
            runningValue = runningValue.subtract(change);
            vals[idx] = runningValue;
            cursor = cursor.minusMonths(1);
        }

        trendValuesCache = new ArrayList<>();
        trendLabelsCache = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            trendValuesCache.add(vals[i]);
            trendLabelsCache.add(currentYm.minusMonths(5 - i).format(MONTH_FMT));
        }

        trendAmountLabel.setText(fmt(currentNetWorth));

        trendMonthLabelsBox.getChildren().clear();
        for (String label : trendLabelsCache) {
            Label l = new Label(label);
            l.getStyleClass().add("dash-day-label");
            HBox.setHgrow(l, Priority.ALWAYS);
            l.setMaxWidth(Double.MAX_VALUE);
            l.setAlignment(Pos.CENTER);
            trendMonthLabelsBox.getChildren().add(l);
        }

        drawTrendChart();
    }

    private BigDecimal monthlyNetChange(Long userId, YearMonth m) {
        BigDecimal inc = transactionService.getTotalIncome(userId, m.atDay(1), m.atEndOfMonth());
        BigDecimal exp = transactionService.getTotalExpense(userId, m.atDay(1), m.atEndOfMonth());
        return inc.subtract(exp);
    }

    private void drawTrendChart() {
        if (trendValuesCache.isEmpty()) return;
        double width = trendChartPane.getWidth();
        if (width <= 0) width = 400;
        double height = 60;

        double min = trendValuesCache.stream().mapToDouble(BigDecimal::doubleValue).min().orElse(0);
        double max = trendValuesCache.stream().mapToDouble(BigDecimal::doubleValue).max().orElse(1);
        if (max - min < 0.01) { max = max + 1; min = min - 1; }

        int n = trendValuesCache.size();
        double padX = 4, padY = 6;
        Polyline line = new Polyline();
        line.getStyleClass().add("dash-trend-line");
        double lastX = 0, lastY = 0;
        for (int i = 0; i < n; i++) {
            double x = padX + (width - 2 * padX) * i / (n - 1);
            double v = trendValuesCache.get(i).doubleValue();
            double y = padY + (height - 2 * padY) * (1 - (v - min) / (max - min));
            line.getPoints().addAll(x, y);
            lastX = x; lastY = y;
        }

        Circle dot = new Circle(lastX, lastY, 3);
        dot.getStyleClass().add("dash-trend-dot");

        trendChartPane.getChildren().clear();
        javafx.scene.layout.Pane freeform = new javafx.scene.layout.Pane(line, dot);
        freeform.setPrefSize(width, height);
        trendChartPane.getChildren().add(freeform);
    }

    /** Goal accounts = any account with a maturity date set (typically DPS / FDR style accounts). */
    private void buildGoalCards(List<Account> accounts) {
        goalCardsBox.getChildren().clear();
        List<Account> goalAccounts = new ArrayList<>();
        for (Account a : accounts) if (a.getMaturityDate() != null) goalAccounts.add(a);

        if (goalAccounts.isEmpty()) {
            Label empty = new Label("No goal accounts yet. Add a DPS or FDR account with a maturity date from the Accounts page.");
            empty.getStyleClass().add("dash-section-hint");
            empty.setWrapText(true);
            goalCardsBox.getChildren().add(empty);
        }

        for (Account a : goalAccounts) {
            goalCardsBox.getChildren().add(buildGoalCard(a));
        }

        javafx.scene.control.Button addBtn = new javafx.scene.control.Button("+  Add account");
        addBtn.getStyleClass().add("dash-add-goal-btn");
        addBtn.setPrefSize(180, 110);
        addBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Add a goal account");
            alert.setHeaderText(null);
            alert.setContentText("Go to the Accounts page and create or edit an account with a maturity date (e.g. DPS or FDR). It will automatically appear here.");
            alert.showAndWait();
        });
        goalCardsBox.getChildren().add(addBtn);
    }

    private VBox buildGoalCard(Account a) {
        Label title = new Label(a.getName());
        title.getStyleClass().add("dash-goal-title");

        Label matures = new Label("Matures " + a.getMaturityDate().format(GOAL_DATE_FMT));
        matures.getStyleClass().add("dash-goal-sub");

        Label amount = new Label(fmt(a.getBalance()));
        amount.getStyleClass().add("dash-goal-amount");

        double fraction = 0;
        if (a.getCreatedAt() != null) {
            LocalDate start = a.getCreatedAt().toLocalDate();
            LocalDate end = a.getMaturityDate();
            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, end);
            long elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(start, LocalDate.now());
            if (totalDays > 0) fraction = Math.max(0, Math.min(1, elapsedDays / (double) totalDays));
            else fraction = 1;
        }
        ProgressBar progressBar = new ProgressBar(fraction);
        progressBar.getStyleClass().add("dash-goal-progress");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        VBox card = new VBox(6, title, matures, amount, progressBar);
        if (a.getInstallmentAmount() != null) {
            Label installment = new Label("Installment " + fmt(a.getInstallmentAmount()) + "/month");
            installment.getStyleClass().add("dash-goal-sub");
            card.getChildren().add(installment);
        }
        card.getStyleClass().add("dash-goal-card");
        card.setPrefWidth(200);
        return card;
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