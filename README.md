# Kovelya Extreme Accounting

Kovelya Extreme Accounting is a domain-driven accounting engine written in Java.

It models core financial concepts such as accounts, journals, invoices, payments, aging, and financial statements, and currently runs with an in-memory persistence layer and a console demo application.

---

## Features

- Double-entry general ledger (journal + ledger entries)
- Typed chart of accounts:
    - `ASSET`, `LIABILITY`, `EQUITY`, `INCOME`, `EXPENSE`
- Sales and purchase invoices:
    - Draft / issued / partially paid / paid / cancelled
- VAT handling:
    - Collected VAT on sales
    - Deductible VAT on purchases
- Accounting periods:
    - Open, closed, archived
- Financial reports:
    - Trial balance
    - Income statement (profit & loss)
    - Balance sheet (assets, liabilities, derived equity)
- Aging reports:
    - Customer receivables aging
    - Supplier payables aging
    - Based on **outstanding balances** (supports partial payments)
- Payments:
    - Customer payments and supplier payments
    - Full or partial settlement of invoices
    - Dated bank postings into the ledger

The engine currently runs **entirely in memory**, which makes it easy to experiment and test.

---

## Project Structure

Package root: `fr.kovelya.accounting`

```text
fr.kovelya.accounting
├── application       # Application services (use cases)
│   ├── dto           # Data transfer objects
│   ├── report        # View models for reporting
│   └── service       # Service interfaces + implementations
├── bootstrap         # Console demo (ConsoleApp)
├── domain            # Domain model (DDD-style)
│   ├── account
│   ├── customer
│   ├── invoice
│   ├── ledger
│   ├── payment
│   ├── period
│   ├── purchase
│   ├── shared
│   ├── supplier
│   └── tax
└── infrastructure
    └── persistence
        └── memory    # In-memory repository implementations
```

## Domain Layer
### The `domain` package contains the core business concepts:

`account`
* `Account`
* `AccountId`
* `AccountType`

`ledger`
* `LedgerEntry (single debit/credit line)`
* `JournalTransaction (multi-line, balanced transaction)`
* `JournalType (GENERAL, SALES, PURCHASES, BANK, etc.)`

`invoice`
* `SalesInvoice`
* `InvoiceLine`
* `InvoiceStatus`

`purchase`
* `PurchaseInvoice`
* `PurchaseInvoiceLine`
* `PurchaseInvoiceStatus`


`customer` / `supplier`
* `Customer`, `CustomerId`
* `Supplier`, `SupplierId`

`payment`
* `CustomerPayment`
* `SupplierPayment`

`period`
* `AccountingPeriod`
* `AccountingPeriodId`
* `PeriodStatus`

`tax`
* `TaxCategory`
* `VatRate`

`shared`
* `Money`: amount + currency, with safe arithmetic

### All important rules (validations, state transitions, balance checks) live in this layer.

## Application Layer
The `application` package orchestrates the domain through services:

`AccountingService`
* Open accounts
* Post journal transactions
* Transfers and bank operations
* Compute balances per account (global or per period)
* Manage accounting periods
* Produce a trial balance

`InvoicingService`
* Manage customers
* Create sales invoices in draft status
* List invoices

`PurchasingService`
* Manage suppliers
* Create purchase invoices in draft status
* List purchase invoices

`InvoicePostingService` / `PurchaseInvoicePostingService` <br/>
Post invoices into the general ledger:
* <b>Sales</b>
  * Debit: receivable
  * Credit: revenue + VAT collected
* <b>Purchases</b>
  * Debit: expense + deductible VAT
  * Credit: payable

`InvoicePaymentService` / `PurchaseInvoicePaymentService`
* Record full or partial payments on invoices
* Post dated bank transfers during `postTransfer`
* Create `CustomerPayment` / `SupplierPayment` records
* Update invoice status (`ISSUED` -> `PARTIALLY_PAID` -> `PAID`)

`ReceivablesAgingService` / `PayablesAgingService` <br/>
Compute aged balances based on outstanding amounts:

```text
outstanding = invoice.total() - sum(payments)
```

Buckets:
- `NOT_DUE`
- `0_30`
- `31_60`
- `61_90`
- `90+` days

---

`FinancialStatementsService`
* Income statement for a period
* Balance sheet for a period (assets, liabilities, derived equity)

`application.report.*` classes (e.g, `IncomeStatementView`, `BlanaceSheetView`, `CustomerReceivableAgingView`) are simple read models used to expose data.
