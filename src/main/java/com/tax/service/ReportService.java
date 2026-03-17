package com.tax.service;

import com.tax.entity.TaxException;
import com.tax.entity.TaxResult;
import com.tax.entity.Transaction;
import com.tax.repository.TaxExceptionRepository;
import com.tax.repository.TaxResultRepository;
import com.tax.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final TaxResultRepository taxResultRepository;
    private final TaxExceptionRepository taxExceptionRepository;

    // ─────────────────────────────────────────────────
    // Report 1: Customer Tax Summary
    // Groups all data by customer and calculates totals
    // ─────────────────────────────────────────────────
    public List<Map<String, Object>> getCustomerTaxSummary() {

        // Get all successful transactions
        List<Transaction> allTransactions =
                transactionRepository.findByValidationStatus("SUCCESS");

        // Group by customerId
        Map<String, List<Transaction>> byCustomer = new LinkedHashMap<>();
        for (Transaction tx : allTransactions) {
            byCustomer
                .computeIfAbsent(tx.getCustomerId(), k -> new ArrayList<>())
                .add(tx);
        }

        List<Map<String, Object>> report = new ArrayList<>();

        for (Map.Entry<String, List<Transaction>> entry : byCustomer.entrySet()) {
            String customerId = entry.getKey();
            List<Transaction> txList = entry.getValue();

            BigDecimal totalAmount      = BigDecimal.ZERO;
            BigDecimal totalReportedTax = BigDecimal.ZERO;
            BigDecimal totalExpectedTax = BigDecimal.ZERO;
            BigDecimal totalTaxGap      = BigDecimal.ZERO;
            int total                   = txList.size();
            int nonCompliant            = 0;

            for (Transaction tx : txList) {
                totalAmount = totalAmount.add(tx.getAmount());

                if (tx.getReportedTax() != null) {
                    totalReportedTax = totalReportedTax.add(tx.getReportedTax());
                }

                TaxResult result = taxResultRepository.findByTransactionId(tx.getTransactionId());
                if (result != null) {
                    totalExpectedTax = totalExpectedTax.add(result.getExpectedTax());
                    totalTaxGap      = totalTaxGap.add(result.getTaxGap());
                    if (!"COMPLIANT".equals(result.getComplianceStatus())) {
                        nonCompliant++;
                    }
                }
            }

            // complianceScore = 100 - (nonCompliant / total * 100)
            double complianceScore = total == 0 ? 100.0
                    : BigDecimal.valueOf(100.0 - ((double) nonCompliant / total * 100.0))
                        .setScale(2, RoundingMode.HALF_UP).doubleValue();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("customerId",               customerId);
            row.put("totalTransactions",         total);
            row.put("totalAmount",              totalAmount);
            row.put("totalReportedTax",         totalReportedTax);
            row.put("totalExpectedTax",         totalExpectedTax);
            row.put("totalTaxGap",              totalTaxGap);
            row.put("nonCompliantTransactions", nonCompliant);
            row.put("complianceScore",          complianceScore);
            report.add(row);
        }

        return report;
    }

    // ─────────────────────────────────────────────────
    // Report 2: Exception Summary
    // Total exceptions grouped by severity + customer
    // ─────────────────────────────────────────────────
    public Map<String, Object> getExceptionSummary() {

        List<TaxException> all = taxExceptionRepository.findAll();

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        Map<String, Long> byCustomer = new LinkedHashMap<>();

        for (TaxException ex : all) {
            bySeverity.merge(ex.getSeverity(), 1L, Long::sum);
            byCustomer.merge(ex.getCustomerId(), 1L, Long::sum);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalExceptions", (long) all.size());
        summary.put("countBySeverity", bySeverity);
        summary.put("countByCustomer", byCustomer);
        return summary;
    }

}
