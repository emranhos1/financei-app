package com.finance.repository;

import com.finance.entity.AccountLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountLogRepository extends JpaRepository<AccountLog, Long> {
    List<AccountLog> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    List<AccountLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<AccountLog> findByAccountIdInOrderByCreatedAtDescIdDesc(List<Long> accountIds, Pageable pageable);
}
