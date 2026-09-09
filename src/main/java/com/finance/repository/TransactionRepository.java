package com.finance.repository;

import com.finance.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByDateDesc(Long userId);
    List<Transaction> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    List<Transaction> findByUserIdAndType(Long userId, Transaction.TransactionType type);
    List<Transaction> findByUserIdAndCategoryId(Long userId, Long categoryId);
    Page<Transaction> findByUserIdAndTypeOrderByDateDescIdDesc(Long userId, Transaction.TransactionType type, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.type IN :types " +
           "AND (:categoryId IS NULL OR t.categoryId = :categoryId) " +
           "AND (:date IS NULL OR t.date = :date) " +
           "AND (:accountId IS NULL OR t.fromAccountId = :accountId OR t.toAccountId = :accountId) " +
           "AND (:filterType IS NULL OR t.type = :filterType) " +
           "ORDER BY t.date DESC, t.id DESC")
    Page<Transaction> findTransactionsPage(@Param("userId") Long userId, @Param("types") List<Transaction.TransactionType> types,
                                            @Param("categoryId") Long categoryId, @Param("date") LocalDate date,
                                            @Param("accountId") Long accountId, @Param("filterType") Transaction.TransactionType filterType,
                                            Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = 'income' AND t.date BETWEEN :startDate AND :endDate")
    BigDecimal sumIncomeByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = 'expense' AND t.date BETWEEN :startDate AND :endDate")
    BigDecimal sumExpenseByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.categoryId = :categoryId AND t.type = 'income' AND t.date BETWEEN :startDate AND :endDate")
    BigDecimal sumIncomeByCategory(@Param("userId") Long userId, @Param("categoryId") Long categoryId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.categoryId = :categoryId AND t.type = 'expense' AND t.date BETWEEN :startDate AND :endDate")
    BigDecimal sumExpenseByCategory(@Param("userId") Long userId, @Param("categoryId") Long categoryId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = 'expense' AND t.fromAccountId IN :accountIds AND t.date BETWEEN :startDate AND :endDate")
    BigDecimal sumExpenseByFromAccountIdsAndDateRange(@Param("userId") Long userId, @Param("accountIds") List<Long> accountIds, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT MIN(t.date) FROM Transaction t WHERE t.userId = :userId")
    LocalDate findEarliestDateByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.type = 'income' AND t.toAccountId IN :accountIds AND t.date BETWEEN :startDate AND :endDate")
    BigDecimal sumIncomeByToAccountIdsAndDateRange(@Param("userId") Long userId, @Param("accountIds") List<Long> accountIds, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.categoryId = :categoryId AND t.type = 'expense' AND t.fromAccountId IN :accountIds AND t.date BETWEEN :startDate AND :endDate")
    BigDecimal sumExpenseByCategoryAndFromAccountIds(@Param("userId") Long userId, @Param("categoryId") Long categoryId, @Param("accountIds") List<Long> accountIds, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.loanId = :loanId")
    BigDecimal sumAmountByLoanId(@Param("loanId") Long loanId);

    List<Transaction> findByLoanId(Long loanId);
}