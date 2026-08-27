package com.finance.service;

import com.finance.entity.Account;
import com.finance.entity.MonthlyExpenseOverride;
import com.finance.entity.User;
import com.finance.repository.MonthlyExpenseOverrideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MonthlyExpenseOverrideService {
    private static final String[] TRACKED_ACCOUNT_TYPES = {"CASH", "BANK"};

    private final MonthlyExpenseOverrideRepository monthlyExpenseOverrideRepository;
    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    public Map<Integer, MonthlyExpenseOverride> getRecordsForYear(Long userId, String accountTypeName, int year) {
        Map<Integer, MonthlyExpenseOverride> byMonth = new LinkedHashMap<>();
        for (MonthlyExpenseOverride o : monthlyExpenseOverrideRepository
                .findByUserIdAndAccountTypeNameAndExpenseYear(userId, accountTypeName, year)) {
            byMonth.put(o.getExpenseMonth(), o);
        }
        return byMonth;
    }

    /** User-entered figure (via the ✎ edit button). Always wins - the daily auto-recalculation
     *  of the current month will never overwrite a value saved through this method. */
    public void saveManualOverride(Long userId, String accountTypeName, int year, int month, BigDecimal amount) {
        MonthlyExpenseOverride record = findOrNew(userId, accountTypeName, year, month);
        record.setAmount(amount);
        record.setIsManual(true);
        monthlyExpenseOverrideRepository.save(record);
    }

    /** Keeps the current month's row in sync with the live transaction total. No-op if the user
     *  has already manually corrected this month's figure. Past months are never touched here -
     *  once a month is no longer the current month, its saved row is left as-is (frozen). */
    public void autoSaveCurrentMonth(Long userId, String accountTypeName, int year, int month, BigDecimal calculatedAmount) {
        MonthlyExpenseOverride existing = monthlyExpenseOverrideRepository
                .findByUserIdAndAccountTypeNameAndExpenseYearAndExpenseMonth(userId, accountTypeName, year, month)
                .orElse(null);
        if (existing != null && Boolean.TRUE.equals(existing.getIsManual())) return;

        MonthlyExpenseOverride record = existing != null ? existing : findOrNew(userId, accountTypeName, year, month);
        record.setAmount(calculatedAmount);
        record.setIsManual(false);
        monthlyExpenseOverrideRepository.save(record);
    }

    /**
     * One-time, idempotent backfill: for every user and every tracked account type (CASH, BANK),
     * computes each past month's expense total from existing transaction history and saves it as
     * an auto (non-manual) record - but only for months that don't already have a saved row, so
     * it never touches a month the live auto-save or a manual ✎ edit has already produced.
     * Safe to call on every app startup; after the first run it's a no-op.
     */
    public void backfillHistoricalMonths() {
        for (User user : userService.getAllUsers()) {
            LocalDate earliest = transactionService.getEarliestTransactionDate(user.getId());
            if (earliest == null) continue;

            List<Account> accounts = accountService.getAccountsByUserId(user.getId());
            YearMonth start = YearMonth.from(earliest);
            YearMonth end = YearMonth.now();

            for (String typeName : TRACKED_ACCOUNT_TYPES) {
                List<Long> accountIds = new ArrayList<>();
                for (Account a : accounts) {
                    if (typeName.equalsIgnoreCase(a.getAccountType().getName())) accountIds.add(a.getId());
                }
                if (accountIds.isEmpty()) continue;

                for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
                    boolean exists = monthlyExpenseOverrideRepository
                            .findByUserIdAndAccountTypeNameAndExpenseYearAndExpenseMonth(
                                    user.getId(), typeName, ym.getYear(), ym.getMonthValue())
                            .isPresent();
                    if (exists) continue;

                    BigDecimal amount = transactionService.getEffectiveExpense(
                            user.getId(), accounts, typeName, ym.atDay(1), ym.atEndOfMonth());

                    MonthlyExpenseOverride record = MonthlyExpenseOverride.builder()
                            .userId(user.getId())
                            .accountTypeName(typeName)
                            .expenseYear(ym.getYear())
                            .expenseMonth(ym.getMonthValue())
                            .amount(amount)
                            .isManual(false)
                            .build();
                    monthlyExpenseOverrideRepository.save(record);
                }
            }
        }
        log.info("Monthly expense backfill check completed");
    }

    private MonthlyExpenseOverride findOrNew(Long userId, String accountTypeName, int year, int month) {
        return monthlyExpenseOverrideRepository
                .findByUserIdAndAccountTypeNameAndExpenseYearAndExpenseMonth(userId, accountTypeName, year, month)
                .orElseGet(() -> MonthlyExpenseOverride.builder()
                        .userId(userId)
                        .accountTypeName(accountTypeName)
                        .expenseYear(year)
                        .expenseMonth(month)
                        .build());
    }
}