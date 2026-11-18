package bank.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for every kind of bank account (abstraction).
 * Attributes are private (encapsulation) and only changed through methods
 * that enforce the rules.
 */
public abstract class Account {

    private final String accountNumber;
    private final String branch;
    private double balance;
    private final LocalDate openDate;
    private final List<Transaction> transactions = new ArrayList<>();

    protected Account(String accountNumber, String branch, double initialBalance) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required.");
        }
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative.");
        }
        this.accountNumber = accountNumber;
        this.branch = branch;
        this.openDate = LocalDate.now();
        // An opening deposit is recorded like any other deposit.
        if (initialBalance > 0) {
            deposit(initialBalance, "Opening deposit");
        }
    }

    /** Deposit with no note (method overloading: same name, different parameters). */
    public void deposit(double amount) {
        deposit(amount, null);
    }

    /** Deposit with a note that is stored on the transaction. */
    public void deposit(double amount, String note) {
        record(TransactionType.DEPOSIT, amount, note);
    }

    /**
     * Withdraws money. Returns true if it worked, false if the amount is not
     * valid or is more than the balance. Subclasses may override (SavingsAccount does).
     */
    public boolean withdraw(double amount) {
        if (amount <= 0 || amount > balance) {
            return false;
        }
        balance -= amount;
        transactions.add(new Transaction(TransactionType.WITHDRAWAL, amount, balance, null));
        return true;
    }

    /** Shared by deposit, salary and interest so the rules live in one place. */
    protected void record(TransactionType type, double amount, String note) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        balance += amount;
        transactions.add(new Transaction(type, amount, balance, note));
    }

    /** Short name of the account type, used by the GUI and the database. */
    public abstract String getAccountType();

    public String getAccountNumber() { return accountNumber; }
    public String getBranch() { return branch; }
    public double getBalance() { return balance; }
    public LocalDate getOpenDate() { return openDate; }

    /** Read-only view, so outside code cannot change the history. */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    @Override
    public String toString() {
        return String.format("%s %s | balance BWP %.2f", getAccountType(), accountNumber, balance);
    }
}
