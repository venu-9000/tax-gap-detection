package com.tax.seeder;

import com.tax.entity.TaxRule;
import com.tax.repository.TaxRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Runs automatically when the app starts.
 * Seeds the 3 default tax rules into the database
 * only if the table is empty.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final TaxRuleRepository taxRuleRepository;

    @Override
    public void run(String... args) {

        if (taxRuleRepository.count() > 0) {
            log.info("Tax rules already exist in DB. Skipping seed.");
            return;
        }

        log.info("Seeding default tax rules into PostgreSQL...");

        // ── Rule 1: High Value Transaction ──────────────────
        TaxRule rule1 = new TaxRule();
        rule1.setRuleName("HIGH_VALUE");
        rule1.setDescription("Flag transactions where amount exceeds 1 lakh");
        rule1.setSeverity("HIGH");
        rule1.setEnabled(true);
        rule1.setThreshold(new BigDecimal("100000.00"));

        // ── Rule 2: Refund Too Large ─────────────────────────
        TaxRule rule2 = new TaxRule();
        rule2.setRuleName("REFUND_CHECK");
        rule2.setDescription("Refund amount must not exceed 50,000");
        rule2.setSeverity("HIGH");
        rule2.setEnabled(true);
        rule2.setMaxRefund(new BigDecimal("50000.00"));

        // ── Rule 3: GST Slab Violation ───────────────────────
        TaxRule rule3 = new TaxRule();
        rule3.setRuleName("GST_SLAB");
        rule3.setDescription("Amount above 10,000 must have tax rate >= 18%");
        rule3.setSeverity("MEDIUM");
        rule3.setEnabled(true);
        rule3.setSlabThreshold(new BigDecimal("10000.00"));
        rule3.setMinTaxRate(new BigDecimal("0.180000"));

        taxRuleRepository.saveAll(List.of(rule1, rule2, rule3));

        log.info("✅ 3 tax rules seeded successfully.");
        log.info("   - HIGH_VALUE    : amount > 1,00,000");
        log.info("   - REFUND_CHECK  : refund > 50,000");
        log.info("   - GST_SLAB      : amount > 10,000 needs taxRate >= 18%");
    }
}
