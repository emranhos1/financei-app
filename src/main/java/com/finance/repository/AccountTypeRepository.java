package com.finance.repository;

import com.finance.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountTypeRepository extends JpaRepository<AccountType, Long> {
    List<AccountType> findByUserId(Long userId);
    Optional<AccountType> findByUserIdAndName(Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);
}