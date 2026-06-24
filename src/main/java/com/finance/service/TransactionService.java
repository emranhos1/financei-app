package com.finance.service;

import com.finance.entity.Account;
import com.finance.entity.AccountLog;
import com.finance.entity.Transaction;
import com.finance.repository.AccountRepository;
import com.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    public Transaction recordIncomeTransaction(Long userId, LocalDate date, BigDecimal amount,
                                               Long toAccountId, Long categoryId, String note) {
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new IllegalArgumentException("To account not found"));
        if (!toAccount.getUserId().equals(userId)) throw new IllegalArgumentException("Account does not belong to user");

        BigDecimal before = toAccount.getBalance();
        toAccount.setBalance(before.add(amount));
        accountRepository.save(toAccount);

        Transaction tx = transactionRepository.save(Transaction.builder()
                .userId(userId).date(date).type(Transaction.TransactionType.INCOME)
                .amount(amount).toAccountId(toAccountId).categoryId(categoryId).note(note).build());

        accountService.logBalanceChange(toAccountId, userId, before, toAccount.getBalance(),
                AccountLog.ChangeType.CREDIT, AccountLog.ReferenceType.INCOME, tx.getId(), note);
        return tx;
    }

    public Transaction recordExpenseTransaction(Long userId, LocalDate date, BigDecimal amount,
                                                Long fromAccountId, Long categoryId, String note) {
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new IllegalArgumentException("From account not found"));
        if (!fromAccount.getUserId().equals(userId)) throw new IllegalArgumentException("Account does not belong to user");

        BigDecimal before = fromAccount.getBalance();
        fromAccount.setBalance(before.subtract(amount));
        accountRepository.save(fromAccount);

        Transaction tx = transactionRepository.save(Transaction.builder()
                .userId(userId).date(date).type(Transaction.TransactionType.EXPENSE)
                .amount(amount).fromAccountId(fromAccountId).categoryId(categoryId).note(note).build());

        accountService.logBalanceChange(fromAccountId, userId, before, fromAccount.getBalance(),
                AccountLog.ChangeType.DEBIT, AccountLog.ReferenceType.EXPENSE, tx.getId(), note);
        return tx;
    }

    public Transaction recordTransferTransaction(Long userId, LocalDate date, BigDecimal amount,
                                                 Long fromAccountId, Long toAccountId, String note) {
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new IllegalArgumentException("From account not found"));
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new IllegalArgumentException("To account not found"));

        if (!fromAccount.getUserId().equals(userId) || !toAccount.getUserId().equals(userId))
            throw new IllegalArgumentException("Accounts do not belong to user");
        if (fromAccount.getId().equals(toAccount.getId()))
            throw new IllegalArgumentException("Cannot transfer to the same account");

        BigDecimal fromBefore = fromAccount.getBalance();
        BigDecimal toBefore = toAccount.getBalance();

        fromAccount.setBalance(fromBefore.subtract(amount));
        toAccount.setBalance(toBefore.add(amount));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction tx = transactionRepository.save(Transaction.builder()
                .userId(userId).date(date).type(Transaction.TransactionType.TRANSFER)
                .amount(amount).fromAccountId(fromAccountId).toAccountId(toAccountId).note(note).build());

        accountService.logBalanceChange(fromAccountId, userId, fromBefore, fromAccount.getBalance(),
                AccountLog.ChangeType.DEBIT, AccountLog.ReferenceType.TRANSFER, tx.getId(), note);
        accountService.logBalanceChange(toAccountId, userId, toBefore, toAccount.getBalance(),
                AccountLog.ChangeType.CREDIT, AccountLog.ReferenceType.TRANSFER, tx.getId(), note);
        return tx;
    }

    public Transaction updateTransaction(Long transactionId, Long userId, LocalDate date,
                                         BigDecimal newAmount, Long categoryId, String note) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        if (!tx.getUserId().equals(userId)) throw new IllegalArgumentException("Access denied");

        BigDecimal diff = newAmount.subtract(tx.getAmount());

        if (tx.getType() == Transaction.TransactionType.INCOME) {
            Account acc = accountRepository.findById(tx.getToAccountId()).orElseThrow();
            BigDecimal before = acc.getBalance();
            acc.setBalance(before.add(diff));
            accountRepository.save(acc);
            accountService.logBalanceChange(acc.getId(), userId, before, acc.getBalance(),
                    AccountLog.ChangeType.ADJUSTMENT, AccountLog.ReferenceType.INCOME, transactionId, note);
        } else if (tx.getType() == Transaction.TransactionType.EXPENSE) {
            Account acc = accountRepository.findById(tx.getFromAccountId()).orElseThrow();
            BigDecimal before = acc.getBalance();
            acc.setBalance(before.subtract(diff));
            accountRepository.save(acc);
            accountService.logBalanceChange(acc.getId(), userId, before, acc.getBalance(),
                    AccountLog.ChangeType.ADJUSTMENT, AccountLog.ReferenceType.EXPENSE, transactionId, note);
        } else if (tx.getType() == Transaction.TransactionType.TRANSFER) {
            Account from = accountRepository.findById(tx.getFromAccountId()).orElseThrow();
            Account to = accountRepository.findById(tx.getToAccountId()).orElseThrow();
            BigDecimal fromBefore = from.getBalance();
            BigDecimal toBefore = to.getBalance();
            from.setBalance(fromBefore.subtract(diff));
            to.setBalance(toBefore.add(diff));
            accountRepository.save(from);
            accountRepository.save(to);
            accountService.logBalanceChange(from.getId(), userId, fromBefore, from.getBalance(),
                    AccountLog.ChangeType.ADJUSTMENT, AccountLog.ReferenceType.TRANSFER, transactionId, note);
            accountService.logBalanceChange(to.getId(), userId, toBefore, to.getBalance(),
                    AccountLog.ChangeType.ADJUSTMENT, AccountLog.ReferenceType.TRANSFER, transactionId, note);
        }

        tx.setDate(date);
        tx.setAmount(newAmount);
        tx.setCategoryId(categoryId);
        tx.setNote(note);
        return transactionRepository.save(tx);
    }

    public void deleteTransaction(Long transactionId, Long userId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        if (!tx.getUserId().equals(userId)) throw new IllegalArgumentException("Access denied");

        if (tx.getType() == Transaction.TransactionType.INCOME && tx.getToAccountId() != null) {
            accountRepository.findById(tx.getToAccountId()).ifPresent(acc -> {
                BigDecimal before = acc.getBalance();
                acc.setBalance(before.subtract(tx.getAmount()));
                accountRepository.save(acc);
                accountService.logBalanceChange(acc.getId(), userId, before, acc.getBalance(),
                        AccountLog.ChangeType.DEBIT, AccountLog.ReferenceType.INCOME, transactionId, "Transaction deleted");
            });
        } else if (tx.getType() == Transaction.TransactionType.EXPENSE && tx.getFromAccountId() != null) {
            accountRepository.findById(tx.getFromAccountId()).ifPresent(acc -> {
                BigDecimal before = acc.getBalance();
                acc.setBalance(before.add(tx.getAmount()));
                accountRepository.save(acc);
                accountService.logBalanceChange(acc.getId(), userId, before, acc.getBalance(),
                        AccountLog.ChangeType.CREDIT, AccountLog.ReferenceType.EXPENSE, transactionId, "Transaction deleted");
            });
        } else if (tx.getType() == Transaction.TransactionType.TRANSFER) {
            if (tx.getFromAccountId() != null) accountRepository.findById(tx.getFromAccountId()).ifPresent(acc -> {
                BigDecimal before = acc.getBalance();
                acc.setBalance(before.add(tx.getAmount()));
                accountRepository.save(acc);
                accountService.logBalanceChange(acc.getId(), userId, before, acc.getBalance(),
                        AccountLog.ChangeType.CREDIT, AccountLog.ReferenceType.TRANSFER, transactionId, "Transaction deleted");
            });
            if (tx.getToAccountId() != null) accountRepository.findById(tx.getToAccountId()).ifPresent(acc -> {
                BigDecimal before = acc.getBalance();
                acc.setBalance(before.subtract(tx.getAmount()));
                accountRepository.save(acc);
                accountService.logBalanceChange(acc.getId(), userId, before, acc.getBalance(),
                        AccountLog.ChangeType.DEBIT, AccountLog.ReferenceType.TRANSFER, transactionId, "Transaction deleted");
            });
        }
        transactionRepository.deleteById(transactionId);
    }

    public Transaction getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }

    public List<Transaction> getTransactionsByUserId(Long userId) {
        return transactionRepository.findByUserIdOrderByDateDesc(userId);
    }

    public List<Transaction> getTransactionsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
    }

    public List<Transaction> getTransactionsByType(Long userId, Transaction.TransactionType type) {
        return transactionRepository.findByUserIdAndType(userId, type);
    }

    public List<Transaction> getTransactionsByCategory(Long userId, Long categoryId) {
        return transactionRepository.findByUserIdAndCategoryId(userId, categoryId);
    }

    public BigDecimal getTotalIncome(Long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.sumIncomeByUserAndDateRange(userId, startDate, endDate);
    }

    public BigDecimal getTotalExpense(Long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.sumExpenseByUserAndDateRange(userId, startDate, endDate);
    }

    public BigDecimal getExpenseByCategory(Long userId, Long categoryId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.sumExpenseByCategory(userId, categoryId, startDate, endDate);
    }

    public BigDecimal getIncomeByCategory(Long userId, Long categoryId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.sumIncomeByCategory(userId, categoryId, startDate, endDate);
    }

    public BigDecimal getNetIncome(Long userId, LocalDate startDate, LocalDate endDate) {
        return getTotalIncome(userId, startDate, endDate).subtract(getTotalExpense(userId, startDate, endDate));
    }
}