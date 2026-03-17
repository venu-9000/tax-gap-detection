package com.tax.repository;

import com.tax.entity.TaxException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxExceptionRepository extends JpaRepository<TaxException, Long> {
    List<TaxException> findByCustomerId(String customerId);
    List<TaxException> findBySeverity(String severity);
    List<TaxException> findByRuleName(String ruleName);
    List<TaxException> findByCustomerIdAndSeverity(String customerId, String severity);
    List<TaxException> findByCustomerIdAndRuleName(String customerId, String ruleName);
    List<TaxException> findBySeverityAndRuleName(String severity, String ruleName);
    List<TaxException> findByCustomerIdAndSeverityAndRuleName(String customerId, String severity, String ruleName);
}
