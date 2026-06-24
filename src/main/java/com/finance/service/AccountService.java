package com.finance.service;

import com.finance.entity.Account;
import com.finance.entity.AccountType;
import com.finance.repository.AccountRepository;
import com.finance.repository.AccountTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;

    public Account createAccount(Long userId, String name, Long accountTypeId, BigDecimal initialBalance) {
        AccountType accountType = accountTypeRepository.findById(accountTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Account type not found"));
        return accountRepository.save(Account.builder()
                .userId(userId)
                .name(name)
                .accountType(accountType)
                .balance(initialBalance != null ? initialBalance : BigDecimal.ZERO)
                .build());
    }

    public Account getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    public Account updateAccount(Long accountId, String name, Long accountTypeId, BigDecimal balance) {
        Account account = getAccountById(accountId);
        AccountType accountType = accountTypeRepository.findById(accountTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Account type not found"));
        account.setName(name);
        account.setAccountType(accountType);
        account.setBalance(balance);
        return accountRepository.save(account);
    }

    public BigDecimal getNetWorth(Long userId) {
        return accountRepository.sumBalanceByUserId(userId);
    }

    public BigDecimal getBalanceByAccountType(Long userId, AccountType accountType) {
        return accountRepository.sumBalanceByUserIdAndAccountType(userId, accountType);
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