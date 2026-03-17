package com.tax.controller;

import com.tax.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * GET /api/reports/customer-summary
     * Returns tax summary grouped per customer:
     * totalAmount, totalReportedTax, totalExpectedTax,
     * totalTaxGap, complianceScore
     */
    @GetMapping("/customer-summary")
    public ResponseEntity<Map<String, Object>> customerSummary() {
        List<Map<String, Object>> data = reportService.getCustomerTaxSummary();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("count", data.size());
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/reports/exception-summary
     * Returns:
     *   totalExceptions
     *   countBySeverity  (HIGH, MEDIUM, LOW)
     *   countByCustomer  (per customer)
     */
    @GetMapping("/exception-summary")
    public ResponseEntity<Map<String, Object>> exceptionSummary() {
        Map<String, Object> data = reportService.getExceptionSummary();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

}
