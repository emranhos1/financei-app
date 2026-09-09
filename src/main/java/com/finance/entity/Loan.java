package com.finance.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A roster entry for money lent to or borrowed from a person. The actual money movement goes
 *  through the normal Transaction/AccountLog flow (via TransactionService); this entity only
 *  tracks who, how much, and whether it's settled. Remaining balance is derived as
 *  principalAmount minus the sum of Transactions tagged with this loan's id (repayments only -
 *  the initial lend/borrow transaction is not tagged, since this row already records it). */
@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "person_name", nullable = false, length = 150)
    private String personName;

    @Column(name = "loan_person_id")
    private Long loanPersonId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanType type;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "transfer_type_id")
    private Long transferTypeId;

    @Column(name = "category_id")
    private Long categoryId;

    /** The initial lend/borrow Transaction this loan created. Deleting the loan reverses and
     *  deletes this transaction (and any repayment transactions tagged with this loan's id) so
     *  the account balance never drifts away from what the loans table shows. */
    @Column(name = "initial_transaction_id")
    private Long initialTransactionId;

    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(length = 500)
    private String note;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status = LoanStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum LoanType {
        LENT, BORROWED
    }

    public enum LoanStatus {
        OPEN, SETTLED
    }
}
