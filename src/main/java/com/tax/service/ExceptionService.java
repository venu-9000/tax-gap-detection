package com.tax.service;

import com.tax.entity.TaxException;
import com.tax.repository.TaxExceptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExceptionService {

    private final TaxExceptionRepository exceptionRepository;

    // Get ALL exceptions
    public List<TaxException> getAll() {
        return exceptionRepository.findAll();
    }

    // Filter exceptions by any combination of customerId, severity, ruleName
    public List<TaxException> filter(String customerId, String severity, String ruleName) {

        // All 3 filters
        if (customerId != null && severity != null && ruleName != null) {
            return exceptionRepository
                    .findByCustomerIdAndSeverityAndRuleName(customerId, severity, ruleName);
        }
        // customerId + severity
        if (customerId != null && severity != null) {
            return exceptionRepository.findByCustomerIdAndSeverity(customerId, severity);
        }
        // customerId + ruleName
        if (customerId != null && ruleName != null) {
            return exceptionRepository.findByCustomerIdAndRuleName(customerId, ruleName);
        }
        // severity + ruleName
        if (severity != null && ruleName != null) {
            return exceptionRepository.findBySeverityAndRuleName(severity, ruleName);
        }
        // customerId only
        if (customerId != null) {
            return exceptionRepository.findByCustomerId(customerId);
        }
        // severity only
        if (severity != null) {
            return exceptionRepository.findBySeverity(severity);
        }
        // ruleName only
        if (ruleName != null) {
            return exceptionRepository.findByRuleName(ruleName);
        }

        // No filters → return everything
        return exceptionRepository.findAll();
    }

}
