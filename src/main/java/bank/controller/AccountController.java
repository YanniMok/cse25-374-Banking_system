package bank.controller;

import bank.dao.CustomerDAO;
import bank.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Control class for everything a customer does with accounts. Views call these methods and
 * never touch the model directly. Problems are reported with exceptions whose messages are
 * safe to show to the user.
 */
public class AccountController {

    private static final String BRANCH = "Main Mall";

    private final CustomerDAO dao;

    public AccountController(CustomerDAO dao) {
        this.dao = dao;
    }

    public List<Account> getAccounts(Customer customer) {
        return new ArrayList<>(customer.getAccounts());
    }

    /** Finds accounts whose number or type contains the text. Blank text returns all accounts. */
    public List<Account> searchAccounts(Customer customer, String criteria) {
        if (criteria == null || criteria.isBlank()) {
            return getAccounts(customer);
        }
        String text = criteria.trim().toLowerCase(Locale.ROOT);
        List<Account> found = new ArrayList<>();
        for (Account a : customer.getAccounts()) {
            if (a.getAccountNumber().toLowerCase(Locale.ROOT).contains(text)
                    || a.getAccountType().toLowerCase(Locale.ROOT).contains(text)) {
                found.add(a);
            }
        }
        return found;
    }

    public List<Transaction> getTransactions(Account account) {
        return new ArrayList<>(account.getTransactions());
    }

    /** Opens an account of the given type ("Savings", "Investment" or "Cheque"). */
    public Account openAccount(Customer customer, String type, double initialDeposit) {
        if (initialDeposit < 0) {
            throw new IllegalArgumentException("The opening deposit cannot be negative.");
        }
        int next = nextAccountNumber();
        Account account;
        switch (type) {
            case "Savings" -> account = new SavingsAccount("SAV-" + next, BRANCH, initialDeposit);
            case "Investment" -> account = new InvestmentAccount("INV-" + next, BRANCH, initialDeposit);
            case "Cheque" -> account = new ChequeAccount("CHQ-" + next, BRANCH, initialDeposit);
            default -> throw new IllegalArgumentException("Unknown account type.");
        }
        customer.addAccount(account); // enforces the rules on number, type and employment
        dao.save(customer);
        return account;
    }

    public void deposit(Customer customer, String accountNumber, double amount) {
        Account account = find(customer, accountNumber);
        requirePositive(amount);
        account.deposit(amount);
        dao.save(customer);
    }

    public void withdraw(Customer customer, String accountNumber, double amount) {
        Account account = find(customer, accountNumber);
        requirePositive(amount);
        if (account instanceof SavingsAccount) {
            throw new IllegalStateException("Withdrawals are not allowed from a savings account.");
        }
        if (!account.withdraw(amount)) {
            throw new IllegalStateException("Insufficient funds.");
        }
        dao.save(customer);
    }

    public void creditSalary(Customer customer, String accountNumber, double amount) {
        Account account = find(customer, accountNumber);
        requirePositive(amount);
        if (!(account instanceof ChequeAccount cheque)) {
            throw new IllegalStateException("A salary can only be credited to a cheque account.");
        }
        cheque.creditSalary(amount);
        dao.save(customer);
    }

    /** Closes an account and returns the balance paid out (only a savings account can have one). */
    public double closeAccount(Customer customer, String accountNumber) {
        Account account = find(customer, accountNumber);
        double paidOut = account.getBalance();
        if (!customer.removeAccount(accountNumber)) {
            throw new IllegalStateException("Withdraw the remaining balance before closing this account.");
        }
        dao.save(customer);
        return paidOut;
    }

    /** Pays this month's interest into every interest-bearing account. Returns how many were paid. */
    public int payMonthlyInterest(Customer customer) {
        int paid = 0;
        for (Account a : customer.getAccounts()) {
            if (a instanceof InterestBearing interestBearing) {
                interestBearing.applyMonthlyInterest();
                paid++;
            }
        }
        dao.save(customer);
        return paid;
    }

    private Account find(Customer customer, String accountNumber) {
        Account account = customer.findAccount(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account not found.");
        }
        return account;
    }

    private void requirePositive(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
    }

    /** Next free number, so new account numbers never repeat. */
    private int nextAccountNumber() {
        int highest = 1000;
        for (Customer c : dao.findAll()) {
            for (Account a : c.getAccounts()) {
                String number = a.getAccountNumber();
                int dash = number.lastIndexOf('-');
                if (dash >= 0) {
                    try {
                        highest = Math.max(highest, Integer.parseInt(number.substring(dash + 1)));
                    } catch (NumberFormatException ignored) {
                        // not a number we generated; skip it
                    }
                }
            }
        }
        return highest + 1;
    }
}
