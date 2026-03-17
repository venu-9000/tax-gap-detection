package com.tax.service;

import com.tax.entity.TaxException;
import com.tax.repository.TaxExceptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExceptionServiceTest {

    @Mock private TaxExceptionRepository exceptionRepository;
    @InjectMocks private ExceptionService service;

    private TaxException makeEx(String customerId, String severity, String ruleName) {
        TaxException ex = new TaxException();
        ex.setId(1L);
        ex.setTransactionId("TX-001");
        ex.setCustomerId(customerId);
        ex.setSeverity(severity);
        ex.setRuleName(ruleName);
        ex.setMessage("Test violation");
        ex.setCreatedAt(LocalDateTime.now());
        return ex;
    }

    @Test
    void test01_getAll_returnsAll() {
        when(exceptionRepository.findAll())
            .thenReturn(List.of(
                makeEx("CUST-001", "HIGH",   "HIGH_VALUE"),
                makeEx("CUST-002", "MEDIUM", "GST_SLAB")
            ));

        List<TaxException> result = service.getAll();
        assertEquals(2, result.size());
    }

    @Test
    void test02_filterByCustomerId() {
        when(exceptionRepository.findByCustomerId("CUST-001"))
            .thenReturn(List.of(makeEx("CUST-001", "HIGH", "HIGH_VALUE")));

        List<TaxException> result = service.filter("CUST-001", null, null);
        assertEquals(1, result.size());
        verify(exceptionRepository).findByCustomerId("CUST-001");
    }

    @Test
    void test03_filterBySeverity() {
        when(exceptionRepository.findBySeverity("HIGH"))
            .thenReturn(List.of(makeEx("CUST-001", "HIGH", "HIGH_VALUE")));

        List<TaxException> result = service.filter(null, "HIGH", null);
        assertEquals(1, result.size());
        verify(exceptionRepository).findBySeverity("HIGH");
    }

    @Test
    void test04_filterByRuleName() {
        when(exceptionRepository.findByRuleName("GST_SLAB"))
            .thenReturn(List.of(makeEx("CUST-002", "MEDIUM", "GST_SLAB")));

        List<TaxException> result = service.filter(null, null, "GST_SLAB");
        assertEquals(1, result.size());
        verify(exceptionRepository).findByRuleName("GST_SLAB");
    }

    @Test
    void test05_filterByCustomerAndSeverity() {
        when(exceptionRepository.findByCustomerIdAndSeverity("CUST-001", "HIGH"))
            .thenReturn(List.of(makeEx("CUST-001", "HIGH", "HIGH_VALUE")));

        List<TaxException> result = service.filter("CUST-001", "HIGH", null);
        assertEquals(1, result.size());
        verify(exceptionRepository).findByCustomerIdAndSeverity("CUST-001", "HIGH");
    }

    @Test
    void test06_filterByCustomerAndRuleName() {
        when(exceptionRepository.findByCustomerIdAndRuleName("CUST-001", "HIGH_VALUE"))
            .thenReturn(List.of(makeEx("CUST-001", "HIGH", "HIGH_VALUE")));

        List<TaxException> result = service.filter("CUST-001", null, "HIGH_VALUE");
        assertEquals(1, result.size());
        verify(exceptionRepository).findByCustomerIdAndRuleName("CUST-001", "HIGH_VALUE");
    }

    @Test
    void test07_filterBySeverityAndRuleName() {
        when(exceptionRepository.findBySeverityAndRuleName("HIGH", "HIGH_VALUE"))
            .thenReturn(List.of(makeEx("CUST-001", "HIGH", "HIGH_VALUE")));

        List<TaxException> result = service.filter(null, "HIGH", "HIGH_VALUE");
        assertEquals(1, result.size());
        verify(exceptionRepository).findBySeverityAndRuleName("HIGH", "HIGH_VALUE");
    }

    @Test
    void test08_filterByAllThree() {
        when(exceptionRepository
            .findByCustomerIdAndSeverityAndRuleName("CUST-001", "HIGH", "HIGH_VALUE"))
            .thenReturn(List.of(makeEx("CUST-001", "HIGH", "HIGH_VALUE")));

        List<TaxException> result = service.filter("CUST-001", "HIGH", "HIGH_VALUE");
        assertEquals(1, result.size());
        verify(exceptionRepository)
            .findByCustomerIdAndSeverityAndRuleName("CUST-001", "HIGH", "HIGH_VALUE");
    }

    @Test
    void test09_noFilters_returnsAll() {
        when(exceptionRepository.findAll()).thenReturn(List.of(
            makeEx("CUST-001", "HIGH",   "HIGH_VALUE"),
            makeEx("CUST-002", "MEDIUM", "GST_SLAB")
        ));

        List<TaxException> result = service.filter(null, null, null);
        assertEquals(2, result.size());
        verify(exceptionRepository).findAll();
    }

    @Test
    void test10_emptyResult_returnsEmptyList() {
        when(exceptionRepository.findBySeverity("LOW")).thenReturn(List.of());

        List<TaxException> result = service.filter(null, "LOW", null);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

}
