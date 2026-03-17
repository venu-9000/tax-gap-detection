package com.tax.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stores the tax gap calculation result for each valid transaction.
 * One row = one tax calculation.
 */
@Entity
@Table(name = "tax_results")
@Data
public class TaxResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id")
    private String transactionId;          // links back to transactions table

    @Column(name = "expected_tax", precision = 19, scale = 4)
    private BigDecimal expectedTax;        // amount * taxRate

    @Column(name = "tax_gap", precision = 19, scale = 4)
    private BigDecimal taxGap;             // expectedTax - reportedTax

    @Column(name = "compliance_status")
    private String complianceStatus;       // COMPLIANT / UNDERPAID / OVERPAID / NON_COMPLIANT

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

}
