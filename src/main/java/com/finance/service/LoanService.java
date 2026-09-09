package com.finance.service;

import com.finance.entity.Loan;
import com.finance.entity.LoanPerson;
import com.finance.entity.Transaction;
import com.finance.repository.LoanPersonRepository;
import com.finance.repository.LoanRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {
    private final LoanRepository loanRepository;
    private final LoanPersonRepository loanPersonRepository;
    private final TransactionService transactionService;

    public LoanPerson createLoanPerson(Long userId, String name, String note) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Name cannot be empty");
        String trimmed = name.trim();
        if (loanPersonRepository.findByUserIdAndNameIgnoreCase(userId, trimmed).isPresent())
            throw new IllegalArgumentException("A loan account for \"" + trimmed + "\" already exists");
        return loanPersonRepository.save(LoanPerson.builder()
                .userId(userId).name(trimmed).note(note == null ? null : note.trim()).build());
    }

    public List<LoanPerson> getLoanPersonsByUserId(Long userId) {
        return loanPersonRepository.findByUserIdOrderByNameAsc(userId);
    }

    public void deleteLoanPerson(Long loanPersonId, Long userId) {
        LoanPerson person = loanPersonRepository.findById(loanPersonId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found"));
        if (!person.getUserId().equals(userId)) throw new SecurityException("Access denied");
        if (!loanRepository.findByUserIdAndLoanPersonIdOrderByLoanDateDesc(userId, loanPersonId).isEmpty())
            throw new IllegalArgumentException("Cannot delete - this person already has loan entries");
        loanPersonRepository.deleteById(loanPersonId);
    }

    public Loan createLoan(Long userId, Long loanPersonId, Loan.LoanType type, BigDecimal amount,
                            Long accountId, Long transferTypeId, Long categoryId,
                            LocalDate date, LocalDate dueDate, String note) {
        if (loanPersonId == null)
            throw new IllegalArgumentException("Person is required");
        LoanPerson person = loanPersonRepository.findById(loanPersonId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found"));
        if (!person.getUserId().equals(userId)) throw new SecurityException("Access denied");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be greater than zero");

        String autoNote = (type == Loan.LoanType.LENT ? "Loan given to " : "Loan received from ") + person.getName();
        String composedNote = (note == null || note.trim().isEmpty()) ? autoNote : autoNote + " - " + note.trim();

        Transaction tx;
        if (type == Loan.LoanType.LENT) {
            tx = transactionService.recordExpenseTransaction(userId, date, amount, accountId, categoryId, transferTypeId, composedNote);
        } else {
            tx = transactionService.recordIncomeTransaction(userId, date, amount, accountId, categoryId, transferTypeId, composedNote);
        }

        return loanRepository.save(Loan.builder()
                .userId(userId).personName(person.getName()).loanPersonId(loanPersonId).type(type)
                .principalAmount(amount).accountId(accountId).transferTypeId(transferTypeId).categoryId(categoryId)
                .initialTransactionId(tx.getId()).loanDate(date).dueDate(dueDate)
                .note(note).status(Loan.LoanStatus.OPEN).build());
    }

    public Loan recordRepayment(Long loanId, Long userId, BigDecimal amount, Long accountId, LocalDate date, String note) {
        Loan loan = getLoanById(loanId);
        if (!loan.getUserId().equals(userId)) throw new SecurityException("Access denied");
        if (loan.getStatus() == Loan.LoanStatus.SETTLED)
            throw new IllegalArgumentException("This loan is already settled");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Amount must be greater than zero");

        BigDecimal remaining = getRemaining(loan);
        if (amount.compareTo(remaining) > 0)
            throw new IllegalArgumentException("Repayment (৳ " + amount.toPlainString()
                    + ") exceeds remaining balance (৳ " + remaining.toPlainString() + ")");

        String autoNote = (loan.getType() == Loan.LoanType.LENT ? "Loan repayment from " : "Loan repayment to ") + loan.getPersonName();
        String composedNote = (note == null || note.trim().isEmpty()) ? autoNote : autoNote + " - " + note.trim();

        Transaction tx;
        if (loan.getType() == Loan.LoanType.LENT) {
            tx = transactionService.recordIncomeTransaction(userId, date, amount, accountId, null, composedNote);
        } else {
            tx = transactionService.recordExpenseTransaction(userId, date, amount, accountId, null, composedNote);
        }
        transactionService.linkTransactionToLoan(tx.getId(), loanId);

        if (remaining.subtract(amount).compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(Loan.LoanStatus.SETTLED);
            loanRepository.save(loan);
        }
        return loan;
    }

    public BigDecimal getRemaining(Loan loan) {
        BigDecimal repaid = transactionService.getLoanRepaidAmount(loan.getId());
        BigDecimal remaining = loan.getPrincipalAmount().subtract(repaid);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    public Loan getLoanById(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
    }

    public List<Loan> getLoansByUserId(Long userId) {
        return loanRepository.findByUserIdOrderByLoanDateDesc(userId);
    }

    public List<Loan> getLoansByUserIdAndStatus(Long userId, Loan.LoanStatus status) {
        return loanRepository.findByUserIdAndStatusOrderByLoanDateDesc(userId, status);
    }

    public List<Loan> getLoansByPerson(Long userId, Long loanPersonId) {
        return loanRepository.findByUserIdAndLoanPersonIdOrderByLoanDateDesc(userId, loanPersonId);
    }

    /** One summary row per person: totals and net remaining across all their loans, so the same
     *  person lending/borrowing multiple times over time no longer shows as repeated look-alike
     *  rows in the main table. */
    public List<PersonSummary> getPersonSummaries(Long userId) {
        List<PersonSummary> result = new ArrayList<>();
        for (LoanPerson person : getLoanPersonsByUserId(userId)) {
            List<Loan> loans = getLoansByPerson(userId, person.getId());
            BigDecimal totalLent = BigDecimal.ZERO;
            BigDecimal totalBorrowed = BigDecimal.ZERO;
            BigDecimal netRemaining = BigDecimal.ZERO;
            int openCount = 0;
            for (Loan loan : loans) {
                BigDecimal remaining = getRemaining(loan);
                if (loan.getType() == Loan.LoanType.LENT) {
                    totalLent = totalLent.add(loan.getPrincipalAmount());
                    netRemaining = netRemaining.add(remaining);
                } else {
                    totalBorrowed = totalBorrowed.add(loan.getPrincipalAmount());
                    netRemaining = netRemaining.subtract(remaining);
                }
                if (loan.getStatus() == Loan.LoanStatus.OPEN) openCount++;
            }
            result.add(new PersonSummary(person.getId(), person.getName(), totalLent, totalBorrowed, netRemaining, openCount));
        }
        return result;
    }

    @Data
    @AllArgsConstructor
    public static class PersonSummary {
        private Long personId;
        private String personName;
        private BigDecimal totalLent;
        private BigDecimal totalBorrowed;
        private BigDecimal netRemaining;
        private int openCount;
    }

    /** OPEN loans whose due date is within the next daysAhead days, or already overdue. */
    public List<Loan> getDueSoonOrOverdueLoans(Long userId, int daysAhead) {
        LocalDate threshold = LocalDate.now().plusDays(daysAhead);
        List<Loan> result = new ArrayList<>();
        for (Loan loan : getLoansByUserIdAndStatus(userId, Loan.LoanStatus.OPEN)) {
            if (loan.getDueDate() != null && !loan.getDueDate().isAfter(threshold)) {
                result.add(loan);
            }
        }
        return result;
    }

    /** Deleting a loan reverses every transaction it created - the initial lend/borrow and any
     *  repayments - so the account balance always matches what's left in the loans table, instead
     *  of silently drifting when only the roster row was removed. */
    public void deleteLoan(Long loanId, Long userId) {
        Loan loan = getLoanById(loanId);
        if (!loan.getUserId().equals(userId)) throw new SecurityException("Access denied");

        for (Transaction repayment : transactionService.getTransactionsByLoanId(loanId)) {
            transactionService.deleteTransaction(repayment.getId(), userId);
        }
        if (loan.getInitialTransactionId() != null) {
            transactionService.deleteTransaction(loan.getInitialTransactionId(), userId);
        }
        loanRepository.deleteById(loanId);
    }
}
