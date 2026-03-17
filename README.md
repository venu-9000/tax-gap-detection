# Tax Gap Detection & Compliance Validation Service

A Spring Boot REST API for tax auditors to validate transactions,
detect tax gaps, and enforce compliance rules.

---

## Tech Stack

| | |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Tests | JUnit 5 + Mockito |
| Coverage | JaCoCo |

---

## Project Structure

```
src/main/java/com/tax/
├── TaxApplication.java          ← Main entry point
├── entity/                      ← 5 database table classes
│   ├── Transaction.java
│   ├── TaxResult.java
│   ├── TaxException.java
│   ├── TaxRule.java
│   └── AuditLog.java
├── repository/                  ← Spring Data JPA (DB access)
├── service/                     ← Business logic
│   ├── TransactionService.java  ← Validation + Tax calc + Rules
│   ├── ExceptionService.java    ← Filter exceptions
│   └── ReportService.java       ← Summary reports
├── controller/                  ← REST endpoints
│   ├── TransactionController.java
│   ├── ExceptionController.java
│   └── ReportController.java
├── dto/                         ← Request objects
│   ├── TransactionRequest.java
│   └── BatchRequest.java
└── seeder/
    └── DataSeeder.java          ← Auto-seeds rules on startup
```

---

## Database Setup

### Step 1 — Install PostgreSQL
Download: https://www.postgresql.org/download/

### Step 2 — Create Database
```bash
psql -U postgres
```
```sql
CREATE DATABASE taxdb;
\q
```

### Step 3 — Configure application.properties
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/taxdb
spring.datasource.username=postgres
spring.datasource.password=postgres   ← change if yours differs
spring.jpa.hibernate.ddl-auto=update
server.port=8080
```

> Tables are created **automatically** on first startup. No SQL scripts needed.

---

## How to Run

```bash
# Step 1: Build
mvn clean package -DskipTests

# Step 2: Run
java -jar target/tax-gap-service-1.0.0.jar
```

App starts at: **http://localhost:8080**

On startup you will see:
```
Hibernate: create table transactions ...
Hibernate: create table tax_results ...
Hibernate: create table tax_exceptions ...
Hibernate: create table tax_rules ...
Hibernate: create table audit_logs ...
✅ 3 tax rules seeded successfully.
```

---

## API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/transactions/batch | Upload batch of transactions |
| GET | /api/transactions | Get all transactions |
| GET | /api/transactions/{id} | Get one transaction |
| GET | /api/exceptions | Get all exceptions |
| GET | /api/exceptions?severity=HIGH | Filter exceptions |
| GET | /api/exceptions?customerId=CUST-001 | Filter by customer |
| GET | /api/reports/customer-summary | Tax summary per customer |
| GET | /api/reports/exception-summary | Exception count report |

---

## Sample curl Calls

### Upload Transactions
```bash
curl -X POST http://localhost:8080/api/transactions/batch \
  -H "Content-Type: application/json" \
  -d @sample-transactions.json
```

### Get All Transactions
```bash
curl http://localhost:8080/api/transactions
```

### Get One Transaction
```bash
curl http://localhost:8080/api/transactions/TX-001
```

### Get All Exceptions
```bash
curl http://localhost:8080/api/exceptions
```

### Filter Exceptions by Severity
```bash
curl "http://localhost:8080/api/exceptions?severity=HIGH"
```

### Filter by Customer + Severity
```bash
curl "http://localhost:8080/api/exceptions?customerId=CUST-001&severity=HIGH"
```

### Customer Tax Summary
```bash
curl http://localhost:8080/api/reports/customer-summary
```

### Exception Summary
```bash
curl http://localhost:8080/api/reports/exception-summary
```

---

## Tax Gap Logic

```
expectedTax = amount × taxRate
taxGap      = expectedTax - reportedTax
```

| Condition | Status |
|-----------|--------|
| reportedTax is missing | NON_COMPLIANT |
| \|taxGap\| ≤ 1 | COMPLIANT |
| taxGap > 1 | UNDERPAID |
| taxGap < -1 | OVERPAID |

---

## Default Rules (auto-seeded)

| Rule | Condition | Severity |
|------|-----------|----------|
| HIGH_VALUE | amount > 1,00,000 | HIGH |
| REFUND_CHECK | refund amount > 50,000 | HIGH |
| GST_SLAB | amount > 10,000 AND taxRate < 18% | MEDIUM |

---

## Run Tests

```bash
mvn test
```

View coverage report:
```
target/site/jacoco/index.html
```

---

## Verify in PostgreSQL

```sql
-- All tables
\dt

-- Transactions
SELECT transaction_id, customer_id, amount, validation_status FROM transactions;

-- Tax results
SELECT transaction_id, expected_tax, tax_gap, compliance_status FROM tax_results;

-- Exceptions (rule violations)
SELECT transaction_id, rule_name, severity, message FROM tax_exceptions;

-- Audit trail
SELECT event_type, transaction_id, details FROM audit_logs ORDER BY created_at;

-- Rules
SELECT rule_name, severity, enabled FROM tax_rules;
```
