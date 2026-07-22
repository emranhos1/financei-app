package com.finance.service;

import com.finance.entity.Account;
import com.finance.entity.AccountLog;
import com.finance.entity.AccountType;
import com.finance.repository.AccountLogRepository;
import com.finance.repository.AccountRepository;
import com.finance.repository.AccountTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;
    private final AccountLogRepository accountLogRepository;

    public Account createAccount(Long userId, String name, Long accountTypeId, BigDecimal initialBalance,
                                 LocalDate maturityDate, BigDecimal installmentAmount, boolean showInGoals) {
        AccountType accountType = accountTypeRepository.findById(accountTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Account type not found"));
        BigDecimal balance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
        Account account = accountRepository.save(Account.builder()
                .userId(userId).name(name).accountType(accountType).balance(balance)
                .maturityDate(maturityDate).installmentAmount(installmentAmount).showInGoals(showInGoals).build());
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            accountLogRepository.save(AccountLog.builder()
                    .accountId(account.getId()).userId(userId)
                    .changeType(AccountLog.ChangeType.CREDIT)
                    .amount(balance).balanceBefore(BigDecimal.ZERO).balanceAfter(balance)
                    .referenceType(AccountLog.ReferenceType.MANUAL)
                    .note("Initial balance").build());
        }
        return account;
    }

    public Account getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    public Account updateAccount(Long accountId, String name, Long accountTypeId, BigDecimal newBalance,
                                 LocalDate maturityDate, BigDecimal installmentAmount, boolean showInGoals) {
        Account account = getAccountById(accountId);
        AccountType accountType = accountTypeRepository.findById(accountTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Account type not found"));
        BigDecimal oldBalance = account.getBalance();
        account.setName(name);
        account.setAccountType(accountType);
        account.setBalance(newBalance);
        account.setMaturityDate(maturityDate);
        account.setInstallmentAmount(installmentAmount);
        account.setShowInGoals(showInGoals);
        accountRepository.save(account);
        if (oldBalance.compareTo(newBalance) != 0) {
            BigDecimal diff = newBalance.subtract(oldBalance);
            accountLogRepository.save(AccountLog.builder()
                    .accountId(accountId).userId(account.getUserId())
                    .changeType(diff.compareTo(BigDecimal.ZERO) > 0 ? AccountLog.ChangeType.CREDIT : AccountLog.ChangeType.DEBIT)
                    .amount(diff.abs()).balanceBefore(oldBalance).balanceAfter(newBalance)
                    .referenceType(AccountLog.ReferenceType.MANUAL)
                    .note("Manual adjustment").build());
        }
        return account;
    }

    public void logBalanceChange(Long accountId, Long userId, BigDecimal balanceBefore, BigDecimal balanceAfter,
                                 AccountLog.ChangeType changeType, AccountLog.ReferenceType referenceType,
                                 Long referenceId, String note) {
        accountLogRepository.save(AccountLog.builder()
                .accountId(accountId).userId(userId)
                .changeType(changeType)
                .amount(balanceAfter.subtract(balanceBefore).abs())
                .balanceBefore(balanceBefore).balanceAfter(balanceAfter)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .note(note).build());
    }

    public List<AccountLog> getAccountLogs(Long accountId) {
        return accountLogRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    public List<AccountLog> getAllLogsByUserId(Long userId) {
        return accountLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public BigDecimal getNetWorth(Long userId) {
        return accountRepository.sumBalanceByUserId(userId);
    }

    public BigDecimal getBalanceByAccountType(Long userId, AccountType accountType) {
        return accountRepository.sumBalanceByUserIdAndAccountType(userId, accountType);
    }

    public void setShowInGoals(Long accountId, boolean show) {
        Account account = getAccountById(accountId);
        account.setShowInGoals(show);
        accountRepository.save(account);
    }

    public void updateAccountBalance(Long accountId, BigDecimal newBalance) {
        Account account = getAccountById(accountId);
        account.setBalance(newBalance);
        accountRepository.save(account);
    }

    public void deleteAccount(Long accountId) {
        accountRepository.deleteById(accountId);
    }
}