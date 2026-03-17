package com.tax.repository;

import com.tax.entity.TaxResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxResultRepository extends JpaRepository<TaxResult, Long> {
    TaxResult findByTransactionId(String transactionId);
    List<TaxResult> findByComplianceStatus(String complianceStatus);
}
