package com.tax.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Stores configurable tax rules in the database.
 * Rules can be enabled/disabled without changing code.
 *
 * 3 rules are auto-seeded on first startup:
 *   1. HIGH_VALUE   - flags large transactions
 *   2. REFUND_CHECK - flags large refunds
 *   3. GST_SLAB     - flags under-taxed transactions
 */
@Entity
@Table(name = "tax_rules")
@Data
public class TaxRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", unique = true)
    private String ruleName;         // HIGH_VALUE / REFUND_CHECK / GST_SLAB

    @Column(name = "description")
    private String description;

    @Column(name = "severity")
    private String severity;         // HIGH / MEDIUM / LOW

    @Column(name = "enabled")
    private boolean enabled;         // true = active rule

    // Each rule uses different fields:
    @Column(name = "threshold", precision = 19, scale = 4)
    private BigDecimal threshold;    // HIGH_VALUE: flag if amount > this

    @Column(name = "max_refund", precision = 19, scale = 4)
    private BigDecimal maxRefund;    // REFUND_CHECK: flag if refund > this

    @Column(name = "slab_threshold", precision = 19, scale = 4)
    private BigDecimal slabThreshold; // GST_SLAB: amount must exceed this

    @Column(name = "min_tax_rate", precision = 10, scale = 6)
    private BigDecimal minTaxRate;   // GST_SLAB: taxRate must be >= this

}
