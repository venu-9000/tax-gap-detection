package com.tax.dto;

import lombok.Data;
import java.util.List;

/**
 * The full request body for POST /api/transactions/batch
 * Contains a list of transactions
 */
@Data
public class BatchRequest {
    private List<TransactionRequest> transactions;
}
