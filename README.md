# Banking System (CSE202 OOAD with Java)

Core model (Step 4). The GUI, controllers and database are added in later steps.

## Structure
- `bank.model`: Customer, Account (abstract), SavingsAccount, InvestmentAccount,
  ChequeAccount, InterestBearing (interface), Transaction, TransactionType
- `bank.util`: PasswordHasher
- `bank.Main`: console demo of every business rule

## Run (Codespaces or any machine with JDK 17+ and Maven)
```
mvn compile exec:java
```

## Business rules
- Savings: no withdrawals, 0.05% monthly interest
- Investment: opening deposit at least BWP 500.00, 5% monthly interest
- Cheque: employed customers only, no interest, salary credits
- A customer holds at most three accounts, one of each type
