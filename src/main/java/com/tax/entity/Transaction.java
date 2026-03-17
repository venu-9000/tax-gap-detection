package com.tax.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stores every transaction uploaded by users.
 * One row = one financial transaction.
 */
@Entity
@Table(name = "transactions")
@Data
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;         // e.g. TX-001

    @Column(name = "date")
    private LocalDate date;               // transaction date

    @Column(name = "customer_id")
    private String customerId;            // e.g. CUST-001

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;            // transaction amount

    @Column(name = "tax_rate", precision = 10, scale = 6)
    private BigDecimal taxRate;           // e.g. 0.18 = 18%

    @Column(name = "reported_tax", precision = 19, scale = 4)
    private BigDecimal reportedTax;       // what customer says they paid (optional)

    @Column(name = "transaction_type")
    private String transactionType;       // SALE / REFUND / EXPENSE

    @Column(name = "validation_status")
    private String validationStatus;      // SUCCESS or FAILURE

    @Column(name = "failure_reasons", columnDefinition = "TEXT")
    private String failureReasons;        // if FAILURE, why?

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
