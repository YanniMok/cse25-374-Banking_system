package bank;

import bank.controller.AccountController;
import bank.controller.LoginController;
import bank.controller.RegistrationController;
import bank.dao.CustomerDAO;
import bank.dao.DatabaseManager;
import bank.dao.JdbcCustomerDAO;
import bank.dao.SampleData;
import bank.model.Account;
import bank.model.Customer;
import bank.model.Transaction;
import bank.model.TransactionType;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Integration test: checks that the controllers, the core model and the SQLite database work
 * together as one system. Every test makes the same calls the JavaFX screens make (the views
 * only forward to the controllers) and then reads the database again through a NEW connection
 * to prove the change was really saved.
 *
 * Run it from IntelliJ with the green arrow next to main(), or with:
 *   mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=bank.IntegrationTest
 * It uses its own file (integration-test.db), so bank.db is never touched.
 * It prints a results table and writes it to test-report.txt.
 */
public class IntegrationTest {

    private static final String DB_FILE = "integration-test.db";

    private record Result(String id, String name, String steps, String expected, String actual, boolean passed) { }

    private interface Body { String run() throws Exception; }

    private static final List<Result> results = new ArrayList<>();

    private static DatabaseManager database;
    private static CustomerDAO dao;
    private static LoginController login;
    private static RegistrationController registration;
    private static AccountController accounts;

    public static void main(String[] args) throws Exception {
        new File(DB_FILE).delete();
        database = new DatabaseManager(DB_FILE);
        database.initialize();
        dao = new JdbcCustomerDAO(database);
        login = new LoginController(dao);
        registration = new RegistrationController(dao);
        accounts = new AccountController(dao);

        runTests();
        printReport();
        new File(DB_FILE).delete();
        System.exit(results.stream().allMatch(Result::passed) ? 0 : 1);
    }

    private static void runTests() {

        test("IT-01", "Database set-up and sample data",
                "Create the tables from schema.sql, then load the sample customers.",
                "10 customers stored, with their accounts and transactions in the three tables.",
                () -> {
                    SampleData.seedIfEmpty(dao);
                    SampleData.seedIfEmpty(dao); // second call must not add duplicates
                    int c = rows("customer"), a = rows("account"), t = rows("transactions");
                    require(c == 10, "expected 10 customers but found " + c);
                    require(a == 19 && t > 20, "unexpected account or transaction count");
                    return c + " customers, " + a + " accounts, " + t + " transactions stored (no duplicates after seeding twice).";
                });

        test("IT-02", "Login with correct details",
                "LoginController.login(\"thabo\", \"pass123\").",
                "The customer is read from the database with all 3 accounts.",
                () -> {
                    Customer c = login.login("thabo", "pass123");
                    require(c.getFirstName().equals("Thabo"), "wrong customer");
                    require(c.getAccounts().size() == 3, "expected 3 accounts");
                    return "Logged in as " + c + " with " + c.getAccounts().size() + " accounts loaded from the database.";
                });

        test("IT-03", "Login with wrong details and lockout",
                "Wrong password, blank fields, then 3 wrong attempts for neo followed by the right password.",
                "Each is refused with a clear message; after 3 failures even the right password is blocked.",
                () -> {
                    String m1 = message(() -> login.login("thabo", "wrong"));
                    String m2 = message(() -> login.login("", ""));
                    for (int i = 0; i < 3; i++) message(() -> login.login("neo", "bad"));
                    String m3 = message(() -> login.login("neo", "secret1"));
                    require(m1.contains("Invalid"), "wrong password message: " + m1);
                    require(m2.contains("Enter both"), "blank message: " + m2);
                    require(m3.contains("Too many"), "lockout message: " + m3);
                    return "\"" + m1 + "\" / \"" + m2 + "\" / \"" + m3 + "\"";
                });

        test("IT-04", "Deposit is saved in the database",
                "Login as thabo, deposit BWP 100.00 into SAV-1001, then read the database again.",
                "Balance rises from 1500.75 to 1600.75 and a DEPOSIT transaction of 100.00 is stored.",
                () -> {
                    Customer t = login.login("thabo", "pass123");
                    accounts.deposit(t, "SAV-1001", 100.00);
                    Account saved = fresh("thabo").findAccount("SAV-1001");
                    Transaction last = lastTransaction(saved);
                    require(close(saved.getBalance(), 1600.75), "balance was " + saved.getBalance());
                    require(last.getType() == TransactionType.DEPOSIT && close(last.getAmount(), 100.00), "last transaction wrong");
                    return "SAV-1001 balance in database: " + money(saved.getBalance()) + "; last transaction " + last.getType() + " " + money(last.getAmount()) + ".";
                });

        test("IT-05", "Withdrawal from an investment account",
                "Withdraw BWP 300.00 from INV-1002, then read the database again.",
                "Balance falls from 2047.50 to 1747.50 and a WITHDRAWAL transaction is stored.",
                () -> {
                    Customer t = login.login("thabo", "pass123");
                    accounts.withdraw(t, "INV-1002", 300.00);
                    Account saved = fresh("thabo").findAccount("INV-1002");
                    require(close(saved.getBalance(), 1747.50), "balance was " + saved.getBalance());
                    require(lastTransaction(saved).getType() == TransactionType.WITHDRAWAL, "last transaction not a withdrawal");
                    return "INV-1002 balance in database: " + money(saved.getBalance()) + ".";
                });

        test("IT-06", "Business rules block invalid withdrawals",
                "Withdraw from the savings account SAV-1001; withdraw BWP 99999 from CHQ-1003; deposit -5.",
                "All three are refused with a message and the balances in the database do not change.",
                () -> {
                    Customer t = login.login("thabo", "pass123");
                    String m1 = message(() -> accounts.withdraw(t, "SAV-1001", 10));
                    String m2 = message(() -> accounts.withdraw(t, "CHQ-1003", 99999));
                    String m3 = message(() -> accounts.deposit(t, "SAV-1001", -5));
                    require(m1.contains("savings"), m1);
                    require(m2.contains("Insufficient"), m2);
                    require(m3.contains("greater than zero"), m3);
                    Customer f = fresh("thabo");
                    require(close(f.findAccount("SAV-1001").getBalance(), 1600.75), "savings balance changed");
                    require(close(f.findAccount("CHQ-1003").getBalance(), 7300.00), "cheque balance changed");
                    return "\"" + m1 + "\" / \"" + m2 + "\" / \"" + m3 + "\"; balances unchanged in the database.";
                });

        test("IT-07", "Salary credit to a cheque account",
                "Credit a salary of BWP 1500.00 to CHQ-1003; also try crediting a salary to a savings account.",
                "Cheque balance rises from 7300.00 to 8800.00 with a SALARY transaction; the savings request is refused.",
                () -> {
                    Customer t = login.login("thabo", "pass123");
                    accounts.creditSalary(t, "CHQ-1003", 1500.00);
                    String refused = message(() -> accounts.creditSalary(t, "SAV-1001", 100));
                    Account saved = fresh("thabo").findAccount("CHQ-1003");
                    require(close(saved.getBalance(), 8800.00), "balance was " + saved.getBalance());
                    require(lastTransaction(saved).getType() == TransactionType.SALARY, "last transaction not a salary");
                    require(refused.contains("cheque"), refused);
                    return "CHQ-1003 balance in database: " + money(saved.getBalance()) + "; savings salary refused (\"" + refused + "\").";
                });

        test("IT-08", "Monthly interest",
                "Run monthly interest for kagiso (cheque + investment) and boitumelo (savings + investment).",
                "Investment earns 5% (5250.00 -> 5512.50), savings 0.05% (800.40 -> 800.80), cheque earns nothing.",
                () -> {
                    Customer k = login.login("kagiso", "pass123");
                    int paidK = accounts.payMonthlyInterest(k);
                    Customer b = login.login("boitumelo", "pass123");
                    int paidB = accounts.payMonthlyInterest(b);
                    Customer fk = fresh("kagiso"), fb = fresh("boitumelo");
                    require(paidK == 1 && paidB == 2, "accounts paid: " + paidK + " and " + paidB);
                    require(close(fk.findAccount("INV-1008").getBalance(), 5512.50), "investment interest wrong");
                    require(close(fk.findAccount("CHQ-1007").getBalance(), 10500.00), "cheque must not earn interest");
                    require(close(fb.findAccount("SAV-1009").getBalance(), 800.80), "savings interest wrong");
                    return "Kagiso investment " + money(fk.findAccount("INV-1008").getBalance()) + ", cheque unchanged "
                            + money(fk.findAccount("CHQ-1007").getBalance()) + "; Boitumelo savings " + money(fb.findAccount("SAV-1009").getBalance()) + ".";
                });

        test("IT-09", "Rules for opening accounts",
                "As neo (not employed): open a cheque account, an investment with BWP 100, then an investment with BWP 600. As thabo: open a 4th account.",
                "The first, second and fourth are refused; the BWP 600 investment is opened and stored with a new unique number.",
                () -> {
                    Customer neo = fresh("neo");
                    String m1 = message(() -> accounts.openAccount(neo, "Cheque", 0));
                    String m2 = message(() -> accounts.openAccount(neo, "Investment", 100));
                    Account opened = accounts.openAccount(neo, "Investment", 600);
                    Customer thabo = fresh("thabo");
                    String m3 = message(() -> accounts.openAccount(thabo, "Savings", 10));
                    require(m1.contains("employed"), m1);
                    require(m2.contains("500"), m2);
                    require(m3.contains("at most"), m3);
                    require(opened.getAccountNumber().equals("INV-1020"), "number was " + opened.getAccountNumber());
                    require(fresh("neo").getAccounts().size() == 2, "new account not stored");
                    return "Refused: \"" + m1 + "\" / \"" + m2 + "\" / \"" + m3 + "\". Opened " + opened.getAccountNumber() + " with " + money(opened.getBalance()) + ".";
                });

        test("IT-10", "Registration and login of a new customer",
                "Register an employed customer, log in with the new details, then try the same username again.",
                "Customer C011 is saved and can log in; the duplicate username is refused.",
                () -> {
                    Customer n = registration.register("Lorato", "Kenosi", "Palapye", "lorato", "abcdef",
                            true, "Palapye Mills", "Industrial Area, Palapye");
                    Customer back = login.login("lorato", "abcdef");
                    String dup = message(() -> registration.register("A", "B", "C", "lorato", "abcdef", false, null, null));
                    require(n.getCustomerID().equals("C011"), "id was " + n.getCustomerID());
                    require(back.getEmployerName().equals("Palapye Mills"), "employer not saved");
                    require(dup.contains("taken"), dup);
                    require(!fresh("lorato").getPasswordHash().equals("abcdef"), "password stored as plain text");
                    return n.getCustomerID() + " saved and logged in; password stored as a hash; duplicate refused (\"" + dup + "\").";
                });

        test("IT-11", "Closing an account",
                "Withdraw everything from neo's INV-1020 and close it; close the savings account SAV-1004 that still has money.",
                "Both accounts are deleted from the database together with their transactions; the savings balance is paid out.",
                () -> {
                    Customer neo = fresh("neo");
                    String notEmpty = message(() -> accounts.closeAccount(neo, "INV-1020"));
                    accounts.withdraw(neo, "INV-1020", 600.00);
                    accounts.closeAccount(neo, "INV-1020");
                    double paidOut = accounts.closeAccount(neo, "SAV-1004");
                    require(notEmpty.contains("remaining balance"), notEmpty);
                    require(paidOut > 0, "savings balance should be paid out");
                    require(fresh("neo").getAccounts().isEmpty(), "accounts still in database");
                    require(count("SELECT COUNT(*) FROM transactions WHERE account_number IN ('INV-1020','SAV-1004')") == 0,
                            "transactions were not deleted with their accounts");
                    return "Closing a non-empty investment account refused; both accounts then deleted with their transactions; savings balance paid out: " + money(paidOut) + ".";
                });

        test("IT-12", "Database constraints protect the data",
                "Insert rows that break the rules directly with SQL: second savings account, missing customer, negative balance.",
                "The database itself rejects all three.",
                () -> {
                    require(sqlBlocked("INSERT INTO account VALUES ('X-1','C001','Savings','b',10,'2026-01-01')"), "duplicate account type allowed");
                    require(sqlBlocked("INSERT INTO account VALUES ('X-2','NOBODY','Savings','b',10,'2026-01-01')"), "foreign key not enforced");
                    require(sqlBlocked("INSERT INTO account VALUES ('X-3','C002','Savings','b',-5,'2026-01-01')"), "negative balance allowed");
                    return "UNIQUE (customer, type), FOREIGN KEY and CHECK (balance >= 0) all rejected the invalid rows.";
                });

        test("IT-13", "Full user journey across all layers",
                "Register, log in, open cheque and investment accounts, credit salary, withdraw, run interest, log out, then log in again with new objects.",
                "After logging in again the balances (cheque 5000.00, investment 840.00) and transaction histories are exactly as left.",
                () -> {
                    Customer reg = registration.register("Naledi", "Moremi", "Serowe", "naledi", "secret99",
                            true, "Serowe Clinic", "Main Road, Serowe");
                    Customer c = login.login("naledi", "secret99");
                    accounts.openAccount(c, "Cheque", 0);
                    accounts.openAccount(c, "Investment", 1000.00);
                    String chq = c.getAccounts().get(0).getAccountNumber();
                    String inv = c.getAccounts().get(1).getAccountNumber();
                    accounts.creditSalary(c, chq, 5000.00);
                    accounts.withdraw(c, inv, 200.00);
                    accounts.payMonthlyInterest(c);
                    login.logout();

                    // a completely new set of objects, as if the application was restarted
                    DatabaseManager db2 = new DatabaseManager(DB_FILE);
                    CustomerDAO dao2 = new JdbcCustomerDAO(db2);
                    LoginController login2 = new LoginController(dao2);
                    Customer again = login2.login("naledi", "secret99");
                    Account cheque = again.findAccount(chq), investment = again.findAccount(inv);
                    require(close(cheque.getBalance(), 5000.00), "cheque balance " + cheque.getBalance());
                    require(close(investment.getBalance(), 840.00), "investment balance " + investment.getBalance());
                    require(cheque.getTransactions().size() == 1, "cheque history size " + cheque.getTransactions().size());
                    require(investment.getTransactions().size() == 3, "investment history size " + investment.getTransactions().size());
                    return reg.getCustomerID() + " journey saved: cheque " + money(cheque.getBalance()) + " (1 transaction), investment "
                            + money(investment.getBalance()) + " (3 transactions: opening deposit, withdrawal, interest).";
                });
    }

    // ---------- tiny test framework ----------

    private static void test(String id, String name, String steps, String expected, Body body) {
        try {
            String actual = body.run();
            results.add(new Result(id, name, steps, expected, actual, true));
        } catch (Throwable t) {
            String why = t.getMessage() == null ? t.toString() : t.getMessage();
            results.add(new Result(id, name, steps, expected, "FAILED: " + why, false));
        }
    }

    private static void require(boolean condition, String messageIfFalse) {
        if (!condition) {
            throw new AssertionError(messageIfFalse);
        }
    }

    /** Runs something that should be refused and returns the message it was refused with. */
    private static String message(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            return e.getMessage();
        }
        throw new AssertionError("expected the action to be refused, but it worked");
    }

    /** Reads the customer again through a NEW database connection and DAO. */
    private static Customer fresh(String username) {
        return new JdbcCustomerDAO(new DatabaseManager(DB_FILE)).findByUsername(username);
    }

    private static Transaction lastTransaction(Account account) {
        return account.getTransactions().get(account.getTransactions().size() - 1);
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) < 0.005;
    }

    private static String money(double value) {
        return String.format("BWP %.2f", value);
    }

    private static int rows(String table) throws SQLException {
        return count("SELECT COUNT(*) FROM " + table);
    }

    private static int count(String sql) throws SQLException {
        try (Connection c = database.getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            r.next();
            return r.getInt(1);
        }
    }

    private static boolean sqlBlocked(String sql) throws SQLException {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.executeUpdate();
            return false;
        } catch (SQLException expected) {
            return true;
        }
    }

    private static void printReport() throws Exception {
        long passed = results.stream().filter(Result::passed).count();
        StringBuilder out = new StringBuilder();
        out.append("INTEGRATION TEST REPORT\n");
        out.append("Run on: ").append(LocalDateTime.now().withNano(0)).append("\n");
        out.append("Java: ").append(System.getProperty("java.version")).append("\n\n");
        for (Result r : results) {
            out.append(r.id()).append("  ").append(r.passed() ? "PASS" : "FAIL").append("  ").append(r.name()).append("\n");
            out.append("      Steps    : ").append(r.steps()).append("\n");
            out.append("      Expected : ").append(r.expected()).append("\n");
            out.append("      Actual   : ").append(r.actual()).append("\n\n");
        }
        out.append("SUMMARY: ").append(passed).append(" of ").append(results.size()).append(" tests passed.\n");
        System.out.print(out);
        try (PrintWriter w = new PrintWriter("test-report.txt")) {
            w.print(out);
        }
    }
}
