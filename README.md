# Banking System (CSE202 OOAD with Java)

## Layers (separation of concerns)
| Package | Role | Rule |
|---|---|---|
| `bank.model` | Entity classes: Customer, Account (abstract), SavingsAccount, InvestmentAccount, ChequeAccount, InterestBearing (interface), Transaction | Business rules; no GUI or database code |
| `bank.view` | Boundary classes: LoginView, RegisterView, AccountView, Navigator | Display and input only; talk to controllers only |
| `bank.controller` | Control classes: LoginController, RegistrationController, AccountController | Validate input, call the model, call the DAO |
| `bank.dao` | CustomerDAO (interface), JdbcCustomerDAO, AccountDAO, TransactionDAO, DatabaseManager, SampleData | All JDBC code lives here |
| `src/main/resources/schema.sql` | Database schema (SQLite) | |
| `bank.BankingApp` | JavaFX start-up and screen switching | |
| `bank.Main`, `bank.DatabaseDemo` | Console demos of the model and of the database | |

## Database
SQLite, stored in one file (`bank.db`, created in the project folder on first run; delete it to reset).
Tables: `customer`, `account` (one table for all account types, `account_type` says which subclass),
`transactions`. Foreign keys with ON DELETE CASCADE, CHECK constraints and a UNIQUE (customer, type)
constraint mirror the business rules. 10 sample customers are added when the database is empty.

## Run
- GUI (needs a screen, JDK 17+ and Maven): `mvn javafx:run`
- Database demo (CRUD with the 10 sample customers): `mvn compile exec:java -Dexec.mainClass=bank.DatabaseDemo`
- Model demo: `mvn compile exec:java`

## Integration test
`src/test/java/bank/IntegrationTest.java` runs 13 tests through the controllers, model and SQLite database
(it uses its own `integration-test.db`). In IntelliJ, open the file and click the green arrow next to `main()`.
Results are printed and saved in `test-report.txt`.

## Sample logins (10 customers)
Password `pass123` for all, except `neo` (`secret1`).

| Username | Employed | Accounts |
|---|---|---|
| thabo | yes | Savings, Investment, Cheque |
| neo | no | Savings |
| lesego | yes | Cheque, Savings |
| kagiso | yes | Cheque, Investment |
| boitumelo | no | Savings, Investment |
| tebogo | yes | Savings, Investment, Cheque |
| masego | no | Savings |
| onalenna | yes | Cheque |
| gofaone | no | Investment, Savings |
| mpho | yes | Cheque, Investment |

## Business rules
- Savings: no withdrawals, 0.05% monthly interest
- Investment: opening deposit at least BWP 500.00, 5% monthly interest
- Cheque: employed customers only, no interest, salary credits
- A customer holds at most three accounts, one of each type
- Closing an account: balance must be withdrawn first (a savings balance is paid out on closure)
- Three failed logins lock that username until the application restarts
