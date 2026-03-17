package com.tax.controller;

import com.tax.dto.BatchRequest;
import com.tax.entity.Transaction;
import com.tax.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /api/transactions/batch
     * Upload a batch of transactions
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> uploadBatch(
            @RequestBody BatchRequest request) {

        // Validate request
        if (request.getTransactions() == null || request.getTransactions().isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "No transactions provided in request body");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> data = transactionService.processBatch(request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Batch processed successfully");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/transactions
     * Get all transactions
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTransactions() {
        List<Transaction> list = transactionService.getAllTransactions();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("count", list.size());
        response.put("data", list);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/transactions/{transactionId}
     * Get a single transaction by ID
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<Map<String, Object>> getOne(
            @PathVariable String transactionId) {

        Transaction tx = transactionService.getByTransactionId(transactionId);

        Map<String, Object> response = new LinkedHashMap<>();
        if (tx == null) {
            response.put("success", false);
            response.put("message", "Transaction not found: " + transactionId);
            return ResponseEntity.status(404).body(response);
        }

        response.put("success", true);
        response.put("data", tx);
        return ResponseEntity.ok(response);
    }

}
