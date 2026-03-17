package com.tax.service;

import com.tax.dto.BatchRequest;
import com.tax.dto.TransactionRequest;
import com.tax.entity.*;
import com.tax.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TaxResultRepository taxResultRepository;
    private final TaxExceptionRepository taxExceptionRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final AuditLogRepository auditLogRepository;

    // ─────────────────────────────────────────────────
    // 1. Process a full batch of transactions
    // ─────────────────────────────────────────────────
    @Transactional
    public Map<String, Object> processBatch(BatchRequest request) {
        int success = 0;
        int failure = 0;
        List<Map<String, Object>> results = new ArrayList<>();

        for (TransactionRequest req : request.getTransactions()) {
            Map<String, Object> result = processSingle(req);
            results.add(result);

            if ("SUCCESS".equals(result.get("validationStatus"))) {
                success++;
            } else {
                failure++;
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalReceived", request.getTransactions().size());
        response.put("successCount", success);
        response.put("failureCount", failure);
        response.put("results", results);
        return response;
    }

    // ─────────────────────────────────────────────────
    // 2. Process ONE transaction
    // ─────────────────────────────────────────────────
    private Map<String, Object> processSingle(TransactionRequest req) {

        // Step A: Validate
        List<String> errors = validate(req);

        // Step B: Build the Transaction object
        Transaction tx = new Transaction();
        tx.setTransactionId(req.getTransactionId());
        tx.setCustomerId(req.getCustomerId());
        tx.setAmount(req.getAmount());
        tx.setTaxRate(req.getTaxRate());
        tx.setReportedTax(req.getReportedTax());
        tx.setTransactionType(req.getTransactionType());
        tx.setCreatedAt(LocalDateTime.now());

        // Parse the date
        if (req.getDate() != null && !req.getDate().isBlank()) {
            try {
                tx.setDate(LocalDate.parse(req.getDate()));
            } catch (Exception e) {
                errors.add("Invalid date format. Use yyyy-MM-dd (example: 2024-03-15)");
            }
        }

        // Step C: Set status based on validation
        if (errors.isEmpty()) {
            tx.setValidationStatus("SUCCESS");
        } else {
            tx.setValidationStatus("FAILURE");
            tx.setFailureReasons(String.join(" | ", errors));
        }

        // Step D: Save to PostgreSQL
        transactionRepository.save(tx);
        log.info("Transaction [{}] saved with status: {}", tx.getTransactionId(), tx.getValidationStatus());

        // Step E: Write audit log
        saveAuditLog("INGESTION", tx.getTransactionId(),
                "Transaction saved. Status=" + tx.getValidationStatus()
                + (tx.getFailureReasons() != null ? ". Errors: " + tx.getFailureReasons() : ""));

        // Step F: Only valid transactions get tax calc + rule checks
        if ("SUCCESS".equals(tx.getValidationStatus())) {
            calculateTax(tx);
            checkAllRules(tx);
        }

        // Return summary for this transaction
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("transactionId", tx.getTransactionId());
        result.put("customerId", tx.getCustomerId());
        result.put("validationStatus", tx.getValidationStatus());
        result.put("failureReasons", tx.getFailureReasons());
        return result;
    }

    // ─────────────────────────────────────────────────
    // 3. Validate fields
    // ─────────────────────────────────────────────────
    private List<String> validate(TransactionRequest req) {
        List<String> errors = new ArrayList<>();

        if (req.getTransactionId() == null || req.getTransactionId().isBlank())
            errors.add("transactionId is required");

        if (req.getCustomerId() == null || req.getCustomerId().isBlank())
            errors.add("customerId is required");

        if (req.getDate() == null || req.getDate().isBlank())
            errors.add("date is required");

        if (req.getTransactionType() == null || req.getTransactionType().isBlank())
            errors.add("transactionType is required (SALE / REFUND / EXPENSE)");

        if (req.getAmount() == null) {
            errors.add("amount is required");
        } else if (req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("amount must be greater than 0");
        }

        if (req.getTaxRate() == null)
            errors.add("taxRate is required");

        // Check for duplicate transactionId
        if (req.getTransactionId() != null
                && transactionRepository.existsByTransactionId(req.getTransactionId())) {
            errors.add("transactionId already exists: " + req.getTransactionId());
        }

        return errors;
    }

    // ─────────────────────────────────────────────────
    // 4. Tax Gap Calculation
    //    expectedTax = amount * taxRate
    //    taxGap      = expectedTax - reportedTax
    // ─────────────────────────────────────────────────
    public void calculateTax(Transaction tx) {

        // Formula: expectedTax = amount × taxRate
        BigDecimal expectedTax = tx.getAmount()
                .multiply(tx.getTaxRate())
                .setScale(4, RoundingMode.HALF_UP);

        BigDecimal taxGap;
        String complianceStatus;

        if (tx.getReportedTax() == null) {
            // Customer didn't report any tax → NON_COMPLIANT
            taxGap = expectedTax;
            complianceStatus = "NON_COMPLIANT";

        } else {
            // taxGap = expectedTax - reportedTax
            taxGap = expectedTax.subtract(tx.getReportedTax())
                    .setScale(4, RoundingMode.HALF_UP);

            BigDecimal absGap = taxGap.abs();

            if (absGap.compareTo(BigDecimal.ONE) <= 0) {
                complianceStatus = "COMPLIANT";       // gap ≤ 1 → fine
            } else if (taxGap.compareTo(BigDecimal.ZERO) > 0) {
                complianceStatus = "UNDERPAID";       // paid less than expected
            } else {
                complianceStatus = "OVERPAID";        // paid more than expected
            }
        }

        // Save tax result
        TaxResult taxResult = new TaxResult();
        taxResult.setTransactionId(tx.getTransactionId());
        taxResult.setExpectedTax(expectedTax);
        taxResult.setTaxGap(taxGap);
        taxResult.setComplianceStatus(complianceStatus);
        taxResult.setCalculatedAt(LocalDateTime.now());
        taxResultRepository.save(taxResult);

        log.info("Tax calculated for [{}]: expected={}, gap={}, status={}",
                tx.getTransactionId(), expectedTax, taxGap, complianceStatus);

        saveAuditLog("TAX_CALCULATION", tx.getTransactionId(),
                "expectedTax=" + expectedTax
                + ", taxGap=" + taxGap
                + ", complianceStatus=" + complianceStatus);
    }

    // ─────────────────────────────────────────────────
    // 5. Run All Active Rules
    // ─────────────────────────────────────────────────
    public void checkAllRules(Transaction tx) {

        List<TaxRule> activeRules = taxRuleRepository.findByEnabledTrue();

        for (TaxRule rule : activeRules) {
            boolean violated = false;
            String message = "";

            switch (rule.getRuleName()) {

                case "HIGH_VALUE":
                    // Flag if transaction amount exceeds threshold
                    if (tx.getAmount().compareTo(rule.getThreshold()) > 0) {
                        violated = true;
                        message = "Transaction amount " + tx.getAmount()
                                + " exceeds high-value threshold of " + rule.getThreshold();
                    }
                    break;

                case "REFUND_CHECK":
                    // Only applies to REFUND type transactions
                    if ("REFUND".equalsIgnoreCase(tx.getTransactionType())) {
                        if (tx.getAmount().compareTo(rule.getMaxRefund()) > 0) {
                            violated = true;
                            message = "Refund amount " + tx.getAmount()
                                    + " exceeds maximum allowed refund of " + rule.getMaxRefund();
                        }
                    }
                    break;

                case "GST_SLAB":
                    // If amount is above slab but tax rate is too low → violation
                    boolean aboveSlab = tx.getAmount().compareTo(rule.getSlabThreshold()) > 0;
                    boolean rateTooLow = tx.getTaxRate().compareTo(rule.getMinTaxRate()) < 0;
                    if (aboveSlab && rateTooLow) {
                        violated = true;
                        message = "Amount " + tx.getAmount()
                                + " is above GST slab threshold " + rule.getSlabThreshold()
                                + " but taxRate " + tx.getTaxRate()
                                + " is below minimum required " + rule.getMinTaxRate();
                    }
                    break;

                default:
                    log.warn("Unknown rule: {}", rule.getRuleName());
            }

            // Log every rule check
            saveAuditLog("RULE_CHECK", tx.getTransactionId(),
                    "Rule=" + rule.getRuleName()
                    + " | Violated=" + violated
                    + (violated ? " | " + message : ""));

            // If violated → create an exception record
            if (violated) {
                TaxException exception = new TaxException();
                exception.setTransactionId(tx.getTransactionId());
                exception.setCustomerId(tx.getCustomerId());
                exception.setRuleName(rule.getRuleName());
                exception.setSeverity(rule.getSeverity());
                exception.setMessage(message);
                exception.setCreatedAt(LocalDateTime.now());
                taxExceptionRepository.save(exception);

                log.warn("Exception created for [{}] - Rule: {} - {}",
                        tx.getTransactionId(), rule.getRuleName(), message);
            }
        }
    }

    // ─────────────────────────────────────────────────
    // 6. Helper: Save Audit Log
    // ─────────────────────────────────────────────────
    private void saveAuditLog(String eventType, String transactionId, String details) {
        AuditLog log = new AuditLog();
        log.setEventType(eventType);
        log.setTransactionId(transactionId);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    // ─────────────────────────────────────────────────
    // 7. Get all transactions
    // ─────────────────────────────────────────────────
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    // ─────────────────────────────────────────────────
    // 8. Get one transaction by its ID
    // ─────────────────────────────────────────────────
    public Transaction getByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId).orElse(null);
    }

}
