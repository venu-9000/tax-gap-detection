package com.tax.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Records every important action in the system.
 * eventType can be:
 *   INGESTION       - a transaction was received
 *   TAX_CALCULATION - tax was computed
 *   RULE_CHECK      - a rule was evaluated
 */
@Entity
@Table(name = "audit_logs")
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type")
    private String eventType;        // INGESTION / TAX_CALCULATION / RULE_CHECK

    @Column(name = "transaction_id")
    private String transactionId;    // which transaction this is about

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;          // what happened (plain English)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
