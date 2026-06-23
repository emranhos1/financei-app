package com.finance.service;

import com.finance.entity.Account;
import com.finance.repository.AccountRepository;
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

    public Account createAccount(Long userId, String name, Account.AccountType type, BigDecimal initialBalance) {
        Account account = Account.builder()
                .userId(userId)
                .name(name)
                .type(type)
                .balance(initialBalance != null ? initialBalance : BigDecimal.ZERO)
                .build();
        return accountRepository.save(account);
    }

    public Account getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    public List<Account> getAccountsByUserAndType(Long userId, Account.AccountType type) {
        return accountRepository.findByUserIdAndType(userId, type);
    }

    public Account updateAccount(Long accountId, String name, Account.AccountType type) {
        Account account = getAccountById(accountId);
        account.setName(name);
        account.setType(type);
        return accountRepository.save(account);
    }

    public BigDecimal getNetWorth(Long userId) {
        return accountRepository.sumBalanceByUserId(userId);
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
