# Banking System (CSE202 OOAD with Java)

## Layers (separation of concerns)
| Package | Role | Rule |
|---|---|---|
| `bank.model` | Entity classes: Customer, Account (abstract), SavingsAccount, InvestmentAccount, ChequeAccount, InterestBearing (interface), Transaction | Business rules; no GUI code |
| `bank.view` | Boundary classes: LoginView, RegisterView, AccountView, Navigator | Display and input only; talk to controllers only |
| `bank.controller` | Control classes: LoginController, RegistrationController, AccountController | Validate input, call the model, call the DAO |
| `bank.dao` | CustomerDAO (interface), InMemoryCustomerDAO, SampleData | Storage. Step 7 adds a JDBC version behind the same interface |
| `bank.BankingApp` | JavaFX start-up and screen switching | |
| `bank.Main` | Console demo of the model | |

## Run
- Console demo (works in Codespaces): `mvn compile exec:java`
- GUI (run on a computer with a screen, JDK 17+ and Maven): `mvn javafx:run`

## Sample logins
| Username | Password | Accounts |
|---|---|---|
| thabo | pass123 | Savings, Investment, Cheque (employed) |
| neo | secret1 | Savings (not employed) |

## Business rules
- Savings: no withdrawals, 0.05% monthly interest
- Investment: opening deposit at least BWP 500.00, 5% monthly interest
- Cheque: employed customers only, no interest, salary credits
- A customer holds at most three accounts, one of each type
- Closing an account: balance must be withdrawn first (a savings balance is paid out on closure)
- Three failed logins lock that username until the application restarts
