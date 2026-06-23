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
import java.time.YearMonth;
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
        
        if (!toAccount.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Account does not belong to user");
        }

        // Double-entry: Credit account (increase balance)
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);

        // Record transaction
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .date(date)
                .type(Transaction.TransactionType.income)
                .amount(amount)
                .toAccountId(toAccountId)
                .categoryId(categoryId)
                .note(note)
                .build();

        return transactionRepository.save(transaction);
    }

    public Transaction recordExpenseTransaction(Long userId, LocalDate date, BigDecimal amount,
                                                Long fromAccountId, Long categoryId, String note) {
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new IllegalArgumentException("From account not found"));

        if (!fromAccount.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Account does not belong to user");
        }

        // Double-entry: Debit account (decrease balance)
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountRepository.save(fromAccount);

        // Record transaction
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .date(date)
                .type(Transaction.TransactionType.expense)
                .amount(amount)
                .fromAccountId(fromAccountId)
                .categoryId(categoryId)
                .note(note)
                .build();

        return transactionRepository.save(transaction);
    }

    public Transaction recordTransferTransaction(Long userId, LocalDate date, BigDecimal amount,
                                                 Long fromAccountId, Long toAccountId, String note) {
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new IllegalArgumentException("From account not found"));
        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new IllegalArgumentException("To account not found"));

        if (!fromAccount.getUserId().equals(userId) || !toAccount.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Accounts do not belong to user");
        }

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Double-entry: Debit source, credit destination
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Record transaction
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .date(date)
                .type(Transaction.TransactionType.transfer)
                .amount(amount)
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .note(note)
                .build();

        return transactionRepository.save(transaction);
    }

    public Transaction getTransactionById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }

    public List<Transaction> getTransactionsByUserId(Long userId) {
        return transactionRepository.findByUserId(userId);
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
        BigDecimal income = getTotalIncome(userId, startDate, endDate);
        BigDecimal expense = getTotalExpense(userId, startDate, endDate);
        return income.subtract(expense);
    }

    public void deleteTransaction(Long transactionId) {
        transactionRepository.deleteById(transactionId);
    }
}
