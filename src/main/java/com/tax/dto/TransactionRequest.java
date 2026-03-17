package com.tax.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * What the user sends for ONE transaction
 */
@Data
public class TransactionRequest {
    private String transactionId;    // e.g. TX-001
    private String date;             // e.g. 2024-03-15
    private String customerId;       // e.g. CUST-001
    private BigDecimal amount;       // e.g. 50000.00
    private BigDecimal taxRate;      // e.g. 0.18
    private BigDecimal reportedTax;  // optional
    private String transactionType;  // SALE / REFUND / EXPENSE
}
