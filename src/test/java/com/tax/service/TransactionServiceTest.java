package com.tax.service;

import com.tax.dto.BatchRequest;
import com.tax.dto.TransactionRequest;
import com.tax.entity.*;
import com.tax.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private TaxResultRepository taxResultRepository;
    @Mock private TaxExceptionRepository taxExceptionRepository;
    @Mock private TaxRuleRepository taxRuleRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private TransactionService service;

    private TransactionRequest validRequest;

    @BeforeEach
    void setUp() {
        // Set up all mocks leniently so every test gets a clean slate
        when(transactionRepository.existsByTransactionId(any())).thenReturn(false);
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.findAll()).thenReturn(new ArrayList<>());
        when(transactionRepository.findByValidationStatus(any())).thenReturn(new ArrayList<>());
        when(transactionRepository.findByTransactionId(any())).thenReturn(Optional.empty());
        when(taxResultRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(taxExceptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(auditLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(taxRuleRepository.findByEnabledTrue()).thenReturn(new ArrayList<>());

        validRequest = new TransactionRequest();
        validRequest.setTransactionId("TX-001");
        validRequest.setDate("2024-03-15");
        validRequest.setCustomerId("CUST-001");
        validRequest.setAmount(new BigDecimal("10000"));
        validRequest.setTaxRate(new BigDecimal("0.18"));
        validRequest.setReportedTax(new BigDecimal("1800"));
        validRequest.setTransactionType("SALE");
    }

    // ─── Validation Tests ────────────────────────────────────

    @Test
    void test01_validTransaction_returnsSuccess() {
        BatchRequest batch = new BatchRequest();
        batch.setTransactions(List.of(validRequest));

        Map<String, Object> result = service.processBatch(batch);

        assertEquals(1, result.get("totalReceived"));
        assertEquals(1, result.get("successCount"));
        assertEquals(0, result.get("failureCount"));
    }

    @Test
    void test02_duplicateTransactionId_returnsFailure() {
        when(transactionRepository.existsByTransactionId("TX-001")).thenReturn(true);

        BatchRequest batch = new BatchRequest();
        batch.setTransactions(List.of(validRequest));

        Map<String, Object> result = service.processBatch(batch);
        assertEquals(0, result.get("successCount"));
        assertEquals(1, result.get("failureCount"));
    }

    @Test
    void test03_nullAmount_returnsFailure() {
        validRequest.setAmount(null);

        BatchRequest batch = new BatchRequest();
        batch.setTransactions(List.of(validRequest));

        Map<String, Object> result = service.processBatch(batch);
        assertEquals(1, result.get("failureCount"));
    }

    @Test
    void test04_zeroAmount_returnsFailure() {
        validRequest.setAmount(BigDecimal.ZERO);

        BatchRequest batch = new BatchRequest();
        batch.setTransactions(List.of(validRequest));

        Map<String, Object> result = service.processBatch(batch);
        assertEquals(1, result.get("failureCount"));
    }

    @Test
    void test05_negativeAmount_returnsFailure() {
        validRequest.setAmount(new BigDecimal("-500"));

        BatchRequest batch = new BatchRequest();
        batch.setTransactions(List.of(validRequest));

        Map<String, Object> result = service.processBatch(batch);
        assertEquals(1, result.get("failureCount"));
    }

    @Test
    void test06_missingCustomerId_returnsFailure() {
        validRequest.setCustomerId(null);

        BatchRequest batch = new BatchRequest();
        batch.setTransactions(List.of(validRequest));

        Map<String, Object> result = service.processBatch(batch);
        assertEquals(1, result.get("failureCount"));
    }

    @Test
    void test07_invalidDate_returnsFailure() {
        validRequest.setDate("not-a-date");

        BatchRequest batch = new BatchRequest();
        batch.setTransactions(List.of(validRequest));

        Map<String, Object> result = service.processBatch(batch);
        assertEquals(1, result.get("failureCount"));
    }

    @Test
    void test08_missingTransactionId_returnsFailure() {
        validRequest.setTransactionId(null);

        BatchRequest batch = new BatchRequest();
        batch.setTransactions(List.of(validRequest));

        Map<String, Object> result = service.processBatch(batch);
        assertEquals(1, result.get("failureCount"));
    }

    @Test
    void test09_missingDate_returnsFailure() {
        validRequest.setDate(null);

        BatchRequest batch = new BatchRequest();
        batch.setTransactions(List.of(validRequest));

        Map<String, Object> result = service.processBatch(batch);
        assertEquals(1, result.get("failureCount"));
    }

    @Test
    void test10_missingTaxRate_returnsFailure() {
        validRequest.setTaxRate(null);

        BatchRequest batch = new BatchRequest();
        batch.setTransactions(List.of(validRequest));

        Map<String, Object> result = service.processBatch(batch);
        assertEquals(1, result.get("failureCount"));
    }

    @Test
    void test11_missingTransactionType_returnsFailure() {
        validRequest.setTransactionType(null);

        BatchRequest batch = new BatchRequest();
        batch.setTransactions(List.of(validRequest));

        Map<String, Object> result = service.processBatch(batch);
        assertEquals(1, result.get("failureCount"));
    }

    @Test
    void test12_multipleMixed_correctCounts() {
        // TX-001 is valid, TX-002 has no amount
        TransactionRequest bad = new TransactionRequest();
        bad.setTransactionId("TX-002");
        bad.setDate("2024-03-15");
        bad.setCustomerId("CUST-001");
        bad.setAmount(null);
        bad.setTaxRate(new BigDecimal("0.18"));
        bad.setTransactionType("SALE");

        BatchRequest batch = new BatchRequest();
        batch.setTransactions(List.of(validRequest, bad));

        Map<String, Object> result = service.processBatch(batch);
        assertEquals(2, result.get("totalReceived"));
        assertEquals(1, result.get("successCount"));
        assertEquals(1, result.get("failureCount"));
    }

    // ─── Tax Calculation Tests ──────────────────────────────

    @Test
    void test13_exactTaxPaid_isCompliant() {
        // expected = 10000 * 0.18 = 1800, reported = 1800, gap = 0
        Transaction tx = buildTx("10000", "0.18", "1800");
        service.calculateTax(tx);

        verify(taxResultRepository).save(argThat(r ->
                "COMPLIANT".equals(r.getComplianceStatus())));
    }

    @Test
    void test14_underpaidTax_isUnderpaid() {
        // expected = 1800, reported = 500, gap = 1300 > 1
        Transaction tx = buildTx("10000", "0.18", "500");
        service.calculateTax(tx);

        verify(taxResultRepository).save(argThat(r ->
                "UNDERPAID".equals(r.getComplianceStatus())));
    }

    @Test
    void test15_overpaidTax_isOverpaid() {
        // expected = 1800, reported = 5000, gap = -3200 < -1
        Transaction tx = buildTx("10000", "0.18", "5000");
        service.calculateTax(tx);

        verify(taxResultRepository).save(argThat(r ->
                "OVERPAID".equals(r.getComplianceStatus())));
    }

    @Test
    void test16_noReportedTax_isNonCompliant() {
        // reportedTax = null
        Transaction tx = buildTx("10000", "0.18", null);
        service.calculateTax(tx);

        verify(taxResultRepository).save(argThat(r ->
                "NON_COMPLIANT".equals(r.getComplianceStatus())));
    }

    @Test
    void test17_taxGapWithinTolerance_isCompliant() {
        // expected = 1800, reported = 1800.50, gap = -0.50 → within ±1
        Transaction tx = buildTx("10000", "0.18", "1800.50");
        service.calculateTax(tx);

        verify(taxResultRepository).save(argThat(r ->
                "COMPLIANT".equals(r.getComplianceStatus())));
    }

    @Test
    void test18_taxGapExactlyOne_isCompliant() {
        // gap of exactly 1.00 → still COMPLIANT (≤ 1)
        Transaction tx = buildTx("10000", "0.18", "1799");
        service.calculateTax(tx);

        verify(taxResultRepository).save(argThat(r ->
                "COMPLIANT".equals(r.getComplianceStatus())));
    }

    // ─── Rule Tests ─────────────────────────────────────────

    @Test
    void test19_highValueRule_firesWhenAmountExceedsThreshold() {
        Transaction tx = buildTxFull("200000", "0.18", "SALE");
        TaxRule rule = buildRule("HIGH_VALUE", "HIGH");
        rule.setThreshold(new BigDecimal("100000"));

        when(taxRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        service.checkAllRules(tx);

        verify(taxExceptionRepository, times(1)).save(any());
    }

    @Test
    void test20_highValueRule_doesNotFireWhenAmountSmall() {
        Transaction tx = buildTxFull("5000", "0.18", "SALE");
        TaxRule rule = buildRule("HIGH_VALUE", "HIGH");
        rule.setThreshold(new BigDecimal("100000"));

        when(taxRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        service.checkAllRules(tx);

        verify(taxExceptionRepository, never()).save(any());
    }

    @Test
    void test21_highValueRule_doesNotFireWhenAmountEqualsThreshold() {
        // amount == threshold → NOT > threshold → no violation
        Transaction tx = buildTxFull("100000", "0.18", "SALE");
        TaxRule rule = buildRule("HIGH_VALUE", "HIGH");
        rule.setThreshold(new BigDecimal("100000"));

        when(taxRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        service.checkAllRules(tx);

        verify(taxExceptionRepository, never()).save(any());
    }

    @Test
    void test22_refundRule_firesForLargeRefund() {
        Transaction tx = buildTxFull("80000", "0.18", "REFUND");
        TaxRule rule = buildRule("REFUND_CHECK", "HIGH");
        rule.setMaxRefund(new BigDecimal("50000"));

        when(taxRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        service.checkAllRules(tx);

        verify(taxExceptionRepository, times(1)).save(any());
    }

    @Test
    void test23_refundRule_doesNotFireForSale() {
        Transaction tx = buildTxFull("80000", "0.18", "SALE");
        TaxRule rule = buildRule("REFUND_CHECK", "HIGH");
        rule.setMaxRefund(new BigDecimal("50000"));

        when(taxRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        service.checkAllRules(tx);

        verify(taxExceptionRepository, never()).save(any());
    }

    @Test
    void test24_refundRule_doesNotFireForExpense() {
        Transaction tx = buildTxFull("80000", "0.18", "EXPENSE");
        TaxRule rule = buildRule("REFUND_CHECK", "HIGH");
        rule.setMaxRefund(new BigDecimal("50000"));

        when(taxRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        service.checkAllRules(tx);

        verify(taxExceptionRepository, never()).save(any());
    }

    @Test
    void test25_refundRule_doesNotFireWhenAmountBelowMax() {
        Transaction tx = buildTxFull("30000", "0.18", "REFUND");
        TaxRule rule = buildRule("REFUND_CHECK", "HIGH");
        rule.setMaxRefund(new BigDecimal("50000"));

        when(taxRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        service.checkAllRules(tx);

        verify(taxExceptionRepository, never()).save(any());
    }

    @Test
    void test26_gstSlabRule_firesWhenRateTooLow() {
        // amount > slab AND taxRate < min
        Transaction tx = buildTxFull("20000", "0.05", "SALE");
        TaxRule rule = buildRule("GST_SLAB", "MEDIUM");
        rule.setSlabThreshold(new BigDecimal("10000"));
        rule.setMinTaxRate(new BigDecimal("0.18"));

        when(taxRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        service.checkAllRules(tx);

        verify(taxExceptionRepository, times(1)).save(any());
    }

    @Test
    void test27_gstSlabRule_doesNotFireWhenRateCorrect() {
        Transaction tx = buildTxFull("20000", "0.18", "SALE");
        TaxRule rule = buildRule("GST_SLAB", "MEDIUM");
        rule.setSlabThreshold(new BigDecimal("10000"));
        rule.setMinTaxRate(new BigDecimal("0.18"));

        when(taxRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        service.checkAllRules(tx);

        verify(taxExceptionRepository, never()).save(any());
    }

    @Test
    void test28_gstSlabRule_doesNotFireWhenAmountBelowSlab() {
        Transaction tx = buildTxFull("5000", "0.05", "SALE");
        TaxRule rule = buildRule("GST_SLAB", "MEDIUM");
        rule.setSlabThreshold(new BigDecimal("10000"));
        rule.setMinTaxRate(new BigDecimal("0.18"));

        when(taxRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        service.checkAllRules(tx);

        verify(taxExceptionRepository, never()).save(any());
    }

    @Test
    void test29_unknownRule_isSkippedGracefully() {
        Transaction tx = buildTxFull("99999", "0.18", "SALE");
        TaxRule rule = buildRule("UNKNOWN_RULE", "LOW");
        rule.setThreshold(new BigDecimal("1"));

        when(taxRuleRepository.findByEnabledTrue()).thenReturn(List.of(rule));

        // Should not throw any exception
        assertDoesNotThrow(() -> service.checkAllRules(tx));
        verify(taxExceptionRepository, never()).save(any());
    }

    @Test
    void test30_multipleRules_allViolated_createsMultipleExceptions() {
        Transaction tx = buildTxFull("200000", "0.05", "REFUND");

        TaxRule r1 = buildRule("HIGH_VALUE", "HIGH");
        r1.setThreshold(new BigDecimal("100000"));

        TaxRule r2 = buildRule("REFUND_CHECK", "HIGH");
        r2.setMaxRefund(new BigDecimal("50000"));

        TaxRule r3 = buildRule("GST_SLAB", "MEDIUM");
        r3.setSlabThreshold(new BigDecimal("10000"));
        r3.setMinTaxRate(new BigDecimal("0.18"));

        when(taxRuleRepository.findByEnabledTrue()).thenReturn(List.of(r1, r2, r3));

        service.checkAllRules(tx);

        // All 3 rules should fire → 3 exceptions saved
        verify(taxExceptionRepository, times(3)).save(any());
    }

    @Test
    void test31_getAllTransactions_returnsList() {
        Transaction tx = new Transaction();
        tx.setTransactionId("TX-001");
        when(transactionRepository.findAll()).thenReturn(List.of(tx));

        List<Transaction> result = service.getAllTransactions();
        assertEquals(1, result.size());
        assertEquals("TX-001", result.get(0).getTransactionId());
    }

    @Test
    void test32_getByTransactionId_found_returnsTransaction() {
        Transaction tx = new Transaction();
        tx.setTransactionId("TX-001");
        when(transactionRepository.findByTransactionId("TX-001"))
                .thenReturn(Optional.of(tx));

        Transaction result = service.getByTransactionId("TX-001");
        assertNotNull(result);
        assertEquals("TX-001", result.getTransactionId());
    }

    @Test
    void test33_getByTransactionId_notFound_returnsNull() {
        when(transactionRepository.findByTransactionId("UNKNOWN"))
                .thenReturn(Optional.empty());

        Transaction result = service.getByTransactionId("UNKNOWN");
        assertNull(result);
    }

    // ─── Helper Methods ─────────────────────────────────────

    private Transaction buildTx(String amount, String taxRate, String reportedTax) {
        Transaction tx = new Transaction();
        tx.setTransactionId("TX-TEST");
        tx.setCustomerId("CUST-001");
        tx.setAmount(new BigDecimal(amount));
        tx.setTaxRate(new BigDecimal(taxRate));
        tx.setReportedTax(reportedTax != null ? new BigDecimal(reportedTax) : null);
        tx.setTransactionType("SALE");
        return tx;
    }

    private Transaction buildTxFull(String amount, String taxRate, String type) {
        Transaction tx = new Transaction();
        tx.setTransactionId("TX-TEST");
        tx.setCustomerId("CUST-001");
        tx.setAmount(new BigDecimal(amount));
        tx.setTaxRate(new BigDecimal(taxRate));
        tx.setTransactionType(type);
        return tx;
    }

    private TaxRule buildRule(String ruleName, String severity) {
        TaxRule rule = new TaxRule();
        rule.setRuleName(ruleName);
        rule.setSeverity(severity);
        rule.setEnabled(true);
        return rule;
    }
}
