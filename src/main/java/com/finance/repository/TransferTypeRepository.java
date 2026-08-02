package com.finance.repository;

import com.finance.entity.TransferType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferTypeRepository extends JpaRepository<TransferType, Long> {
    List<TransferType> findByUserId(Long userId);
    Optional<TransferType> findByUserIdAndName(Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);
}