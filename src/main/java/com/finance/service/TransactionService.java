package com.finance.service;

import com.finance.entity.Account;
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

    public Transaction recordIncomeTransaction(Long userId, LocalDate date, BigDecimal amount,
                                               Long toAccountId, Long categoryId, String note) {
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new IllegalArgumentException("To account not found"));
        if (!toAccount.getUserId().equals(userId)) throw new IllegalArgumentException("Account does not belong to user");

        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);

        return transactionRepository.save(Transaction.builder()
                .userId(userId).date(date).type(Transaction.TransactionType.income)
                .amount(amount).toAccountId(toAccountId).categoryId(categoryId).note(note).build());
    }

    public Transaction recordExpenseTransaction(Long userId, LocalDate date, BigDecimal amount,
                                                Long fromAccountId, Long categoryId, String note) {
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new IllegalArgumentException("From account not found"));
        if (!fromAccount.getUserId().equals(userId)) throw new IllegalArgumentException("Account does not belong to user");

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountRepository.save(fromAccount);

        return transactionRepository.save(Transaction.builder()
                .userId(userId).date(date).type(Transaction.TransactionType.expense)
                .amount(amount).fromAccountId(fromAccountId).categoryId(categoryId).note(note).build());
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

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        return transactionRepository.save(Transaction.builder()
                .userId(userId).date(date).type(Transaction.TransactionType.transfer)
                .amount(amount).fromAccountId(fromAccountId).toAccountId(toAccountId).note(note).build());
    }

    public Transaction updateTransaction(Long transactionId, Long userId, LocalDate date,
                                         BigDecimal newAmount, Long categoryId, String note) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        if (!tx.getUserId().equals(userId)) throw new IllegalArgumentException("Access denied");

        BigDecimal diff = newAmount.subtract(tx.getAmount());

        // Reverse old and apply new balance difference
        if (tx.getType() == Transaction.TransactionType.income) {
            Account acc = accountRepository.findById(tx.getToAccountId()).orElseThrow();
            acc.setBalance(acc.getBalance().add(diff));
            accountRepository.save(acc);
        } else if (tx.getType() == Transaction.TransactionType.expense) {
            Account acc = accountRepository.findById(tx.getFromAccountId()).orElseThrow();
            acc.setBalance(acc.getBalance().subtract(diff));
            accountRepository.save(acc);
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

        // Reverse balance effect
        if (tx.getType() == Transaction.TransactionType.income && tx.getToAccountId() != null) {
            accountRepository.findById(tx.getToAccountId()).ifPresent(acc -> {
                acc.setBalance(acc.getBalance().subtract(tx.getAmount()));
                accountRepository.save(acc);
            });
        } else if (tx.getType() == Transaction.TransactionType.expense && tx.getFromAccountId() != null) {
            accountRepository.findById(tx.getFromAccountId()).ifPresent(acc -> {
                acc.setBalance(acc.getBalance().add(tx.getAmount()));
                accountRepository.save(acc);
            });
        } else if (tx.getType() == Transaction.TransactionType.transfer) {
            if (tx.getFromAccountId() != null) accountRepository.findById(tx.getFromAccountId()).ifPresent(acc -> {
                acc.setBalance(acc.getBalance().add(tx.getAmount()));
                accountRepository.save(acc);
            });
            if (tx.getToAccountId() != null) accountRepository.findById(tx.getToAccountId()).ifPresent(acc -> {
                acc.setBalance(acc.getBalance().subtract(tx.getAmount()));
                accountRepository.save(acc);
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

    public BigDecimal getNetIncome(Long userId, LocalDate startDate, LocalDate endDate) {
        return getTotalIncome(userId, startDate, endDate).subtract(getTotalExpense(userId, startDate, endDate));
    }
}