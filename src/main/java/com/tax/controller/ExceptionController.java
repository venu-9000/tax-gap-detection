package com.tax.controller;

import com.tax.entity.TaxException;
import com.tax.service.ExceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/exceptions")
@RequiredArgsConstructor
public class ExceptionController {

    private final ExceptionService exceptionService;

    /**
     * GET /api/exceptions
     * GET /api/exceptions?customerId=CUST-001
     * GET /api/exceptions?severity=HIGH
     * GET /api/exceptions?ruleName=HIGH_VALUE
     * GET /api/exceptions?customerId=CUST-001&severity=HIGH
     * GET /api/exceptions?customerId=CUST-001&severity=HIGH&ruleName=HIGH_VALUE
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getExceptions(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String ruleName) {

        List<TaxException> exceptions =
                exceptionService.filter(customerId, severity, ruleName);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("count", exceptions.size());
        response.put("filters", buildFiltersMap(customerId, severity, ruleName));
        response.put("data", exceptions);
        return ResponseEntity.ok(response);
    }

    // Shows which filters were applied in the response
    private Map<String, String> buildFiltersMap(
            String customerId, String severity, String ruleName) {
        Map<String, String> filters = new LinkedHashMap<>();
        if (customerId != null) filters.put("customerId", customerId);
        if (severity   != null) filters.put("severity",   severity);
        if (ruleName   != null) filters.put("ruleName",   ruleName);
        return filters;
    }

}
