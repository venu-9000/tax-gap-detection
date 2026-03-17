package com.tax.service;

import com.tax.entity.TaxException;
import com.tax.entity.TaxResult;
import com.tax.entity.Transaction;
import com.tax.repository.TaxExceptionRepository;
import com.tax.repository.TaxResultRepository;
import com.tax.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private TaxResultRepository taxResultRepository;
    @Mock private TaxExceptionRepository taxExceptionRepository;

    @InjectMocks private ReportService reportService;

    // ─── Customer Summary Tests ──────────────────────────────

    @Test
    void test01_customerSummary_noTransactions_returnsEmptyList() {
        when(transactionRepository.findByValidationStatus("SUCCESS"))
                .thenReturn(List.of());

        List<Map<String, Object>> result = reportService.getCustomerTaxSummary();

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void test02_customerSummary_oneCompliantCustomer() {
        Transaction tx = makeTx("TX-001", "CUST-001", "10000", "0.18", "1800");
        when(transactionRepository.findByValidationStatus("SUCCESS"))
                .thenReturn(List.of(tx));

        TaxResult result = makeTaxResult("TX-001", "1800", "0", "COMPLIANT");
        when(taxResultRepository.findByTransactionId("TX-001"))
                .thenReturn(result);

        List<Map<String, Object>> report = reportService.getCustomerTaxSummary();

        assertEquals(1, report.size());
        Map<String, Object> row = report.get(0);
        assertEquals("CUST-001", row.get("customerId"));
        assertEquals(1, row.get("totalTransactions"));
        assertEquals(0, row.get("nonCompliantTransactions"));
        assertEquals(100.0, row.get("complianceScore"));
    }

    @Test
    void test03_customerSummary_oneNonCompliantCustomer() {
        Transaction tx = makeTx("TX-002", "CUST-002", "10000", "0.18", null);
        when(transactionRepository.findByValidationStatus("SUCCESS"))
                .thenReturn(List.of(tx));

        TaxResult result = makeTaxResult("TX-002", "1800", "1800", "NON_COMPLIANT");
        when(taxResultRepository.findByTransactionId("TX-002"))
                .thenReturn(result);

        List<Map<String, Object>> report = reportService.getCustomerTaxSummary();

        assertEquals(1, report.size());
        Map<String, Object> row = report.get(0);
        assertEquals(1, row.get("nonCompliantTransactions"));
        assertEquals(0.0, row.get("complianceScore"));
    }

    @Test
    void test04_customerSummary_multipleCustomers() {
        Transaction tx1 = makeTx("TX-001", "CUST-001", "5000", "0.18", "900");
        Transaction tx2 = makeTx("TX-002", "CUST-002", "8000", "0.18", "1440");

        when(transactionRepository.findByValidationStatus("SUCCESS"))
                .thenReturn(List.of(tx1, tx2));

        when(taxResultRepository.findByTransactionId("TX-001"))
                .thenReturn(makeTaxResult("TX-001", "900", "0", "COMPLIANT"));
        when(taxResultRepository.findByTransactionId("TX-002"))
                .thenReturn(makeTaxResult("TX-002", "1440", "0", "COMPLIANT"));

        List<Map<String, Object>> report = reportService.getCustomerTaxSummary();
        assertEquals(2, report.size());
    }

    @Test
    void test05_customerSummary_complianceScore_50percent() {
        // 2 transactions: 1 compliant, 1 non-compliant → score = 50%
        Transaction tx1 = makeTx("TX-001", "CUST-001", "5000", "0.18", "900");
        Transaction tx2 = makeTx("TX-002", "CUST-001", "5000", "0.18", "100");

        when(transactionRepository.findByValidationStatus("SUCCESS"))
                .thenReturn(List.of(tx1, tx2));
        when(taxResultRepository.findByTransactionId("TX-001"))
                .thenReturn(makeTaxResult("TX-001", "900", "0", "COMPLIANT"));
        when(taxResultRepository.findByTransactionId("TX-002"))
                .thenReturn(makeTaxResult("TX-002", "900", "800", "UNDERPAID"));

        List<Map<String, Object>> report = reportService.getCustomerTaxSummary();
        assertEquals(1, report.size());
        assertEquals(50.0, report.get(0).get("complianceScore"));
    }

    @Test
    void test06_customerSummary_nullTaxResult_handledGracefully() {
        Transaction tx = makeTx("TX-001", "CUST-001", "5000", "0.18", "900");
        when(transactionRepository.findByValidationStatus("SUCCESS"))
                .thenReturn(List.of(tx));
        when(taxResultRepository.findByTransactionId("TX-001"))
                .thenReturn(null);  // no tax result yet

        List<Map<String, Object>> report = reportService.getCustomerTaxSummary();
        assertEquals(1, report.size());
        assertEquals(100.0, report.get(0).get("complianceScore"));
    }

    // ─── Exception Summary Tests ─────────────────────────────

    @Test
    void test07_exceptionSummary_noExceptions() {
        when(taxExceptionRepository.findAll()).thenReturn(List.of());

        Map<String, Object> summary = reportService.getExceptionSummary();

        assertEquals(0L, summary.get("totalExceptions"));
        Map<?, ?> bySeverity = (Map<?, ?>) summary.get("countBySeverity");
        assertTrue(bySeverity.isEmpty());
    }

    @Test
    void test08_exceptionSummary_countsCorrectly() {
        List<TaxException> exceptions = List.of(
            makeEx("CUST-001", "HIGH"),
            makeEx("CUST-001", "HIGH"),
            makeEx("CUST-002", "MEDIUM"),
            makeEx("CUST-003", "LOW")
        );
        when(taxExceptionRepository.findAll()).thenReturn(exceptions);

        Map<String, Object> summary = reportService.getExceptionSummary();

        assertEquals(4L, summary.get("totalExceptions"));

        Map<?, ?> bySeverity = (Map<?, ?>) summary.get("countBySeverity");
        assertEquals(2L, bySeverity.get("HIGH"));
        assertEquals(1L, bySeverity.get("MEDIUM"));
        assertEquals(1L, bySeverity.get("LOW"));

        Map<?, ?> byCustomer = (Map<?, ?>) summary.get("countByCustomer");
        assertEquals(2L, byCustomer.get("CUST-001"));
        assertEquals(1L, byCustomer.get("CUST-002"));
        assertEquals(1L, byCustomer.get("CUST-003"));
    }

    @Test
    void test09_exceptionSummary_singleException() {
        when(taxExceptionRepository.findAll())
                .thenReturn(List.of(makeEx("CUST-001", "HIGH")));

        Map<String, Object> summary = reportService.getExceptionSummary();
        assertEquals(1L, summary.get("totalExceptions"));
    }

    @Test
    void test10_exceptionSummary_allSameSeverity() {
        List<TaxException> exceptions = List.of(
            makeEx("CUST-001", "HIGH"),
            makeEx("CUST-002", "HIGH"),
            makeEx("CUST-003", "HIGH")
        );
        when(taxExceptionRepository.findAll()).thenReturn(exceptions);

        Map<String, Object> summary = reportService.getExceptionSummary();
        Map<?, ?> bySeverity = (Map<?, ?>) summary.get("countBySeverity");

        assertEquals(3L, bySeverity.get("HIGH"));
        assertNull(bySeverity.get("MEDIUM"));
        assertNull(bySeverity.get("LOW"));
    }

    // ─── Helpers ─────────────────────────────────────────────

    private Transaction makeTx(String txId, String custId,
                                String amount, String taxRate, String reportedTax) {
        Transaction tx = new Transaction();
        tx.setTransactionId(txId);
        tx.setCustomerId(custId);
        tx.setAmount(new BigDecimal(amount));
        tx.setTaxRate(new BigDecimal(taxRate));
        tx.setReportedTax(reportedTax != null ? new BigDecimal(reportedTax) : null);
        tx.setTransactionType("SALE");
        tx.setValidationStatus("SUCCESS");
        return tx;
    }

    private TaxResult makeTaxResult(String txId, String expected,
                                     String gap, String status) {
        TaxResult r = new TaxResult();
        r.setTransactionId(txId);
        r.setExpectedTax(new BigDecimal(expected));
        r.setTaxGap(new BigDecimal(gap));
        r.setComplianceStatus(status);
        r.setCalculatedAt(LocalDateTime.now());
        return r;
    }

    private TaxException makeEx(String customerId, String severity) {
        TaxException ex = new TaxException();
        ex.setTransactionId("TX-001");
        ex.setCustomerId(customerId);
        ex.setSeverity(severity);
        ex.setRuleName("HIGH_VALUE");
        ex.setMessage("Test");
        ex.setCreatedAt(LocalDateTime.now());
        return ex;
    }
}
