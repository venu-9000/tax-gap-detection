package com.tax.repository;

import com.tax.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);
    boolean existsByTransactionId(String transactionId);
    List<Transaction> findByCustomerId(String customerId);
    List<Transaction> findByValidationStatus(String validationStatus);
}
