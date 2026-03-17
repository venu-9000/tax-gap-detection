package com.tax.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Stores rule violations (exceptions).
 * Example: transaction amount > 1 lakh → exception created.
 */
@Entity
@Table(name = "tax_exceptions")
@Data
public class TaxException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id")
    private String transactionId;    // which transaction triggered this

    @Column(name = "customer_id")
    private String customerId;       // which customer

    @Column(name = "rule_name")
    private String ruleName;         // which rule was violated

    @Column(name = "severity")
    private String severity;         // HIGH / MEDIUM / LOW

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;          // description of the violation

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
