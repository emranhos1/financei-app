package com.finance.service;

import com.finance.entity.Account;
import com.finance.entity.AccountLog;
import com.finance.entity.Loan;
import com.finance.entity.Transaction;
import com.finance.repository.AccountLogRepository;
import com.finance.repository.AccountRepository;
import com.finance.repository.LoanRepository;
import com.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Read-only diagnostic checks over the whole database - never modifies any data. Mirrors the
 *  manual SQL sanity checks (account balance vs. account_logs chain continuity, dangling loan_id
 *  references) that were run by hand while debugging the Loan feature. */
@Service
@RequiredArgsConstructor
public class DataIntegrityService {
    private final AccountRepository accountRepository;
    private final AccountLogRepository accountLogRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;

    public List<String> runFullCheck() {
        List<String> issues = new ArrayList<>();
        issues.addAll(checkAccountBalances());
        issues.addAll(checkDanglingLoanReferences());
        return issues;
    }

    /** For each account with at least one log entry: every log's balanceAfter must equal the next
     *  log's balanceBefore (no gap in the chain), and the last log's balanceAfter must equal the
     *  account's current balance. Accounts with zero logs (an untouched opening balance) are
     *  skipped - that's not an error. */
    public List<String> checkAccountBalances() {
        List<String> issues = new ArrayList<>();
        for (Account account : accountRepository.findAll()) {
            List<AccountLog> logs = accountLogRepository.findByAccountIdOrderByIdAsc(account.getId());
            if (logs.isEmpty()) continue;

            for (int i = 0; i < logs.size() - 1; i++) {
                BigDecimal thisAfter = logs.get(i).getBalanceAfter();
                BigDecimal nextBefore = logs.get(i + 1).getBalanceBefore();
                if (thisAfter.compareTo(nextBefore) != 0) {
                    issues.add("Account \"" + account.getName() + "\": log chain gap between log #"
                            + logs.get(i).getId() + " and #" + logs.get(i + 1).getId()
                            + " (" + thisAfter + " vs " + nextBefore + ")");
                }
            }

            BigDecimal lastLogBalance = logs.get(logs.size() - 1).getBalanceAfter();
            if (lastLogBalance.compareTo(account.getBalance()) != 0) {
                issues.add("Account \"" + account.getName() + "\": current balance (" + account.getBalance()
                        + ") does not match its last log entry (" + lastLogBalance + ")");
            }
        }
        return issues;
    }

    /** A transaction tagged with a loan_id whose loan no longer exists (e.g. left over from before
     *  loan deletion was fixed to cascade-reverse its transactions). */
    public List<String> checkDanglingLoanReferences() {
        List<String> issues = new ArrayList<>();
        Set<Long> loanIds = new HashSet<>();
        for (Loan loan : loanRepository.findAll()) loanIds.add(loan.getId());

        for (Transaction tx : transactionRepository.findAll()) {
            if (tx.getLoanId() != null && !loanIds.contains(tx.getLoanId())) {
                issues.add("Transaction #" + tx.getId() + " references a loan (id " + tx.getLoanId()
                        + ") that no longer exists");
            }
        }
        return issues;
    }
}
