package com.tax.repository;

import com.tax.entity.TaxRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxRuleRepository extends JpaRepository<TaxRule, Long> {
    // Only return rules that are switched ON
    List<TaxRule> findByEnabledTrue();
}
