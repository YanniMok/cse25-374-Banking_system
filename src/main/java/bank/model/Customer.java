package bank.model;

import bank.util.PasswordHasher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A bank customer. Owns up to three accounts (composition):
 * an account cannot exist without the customer that holds it.
 */
public class Customer {

    public static final int MAX_ACCOUNTS = 3;

    private final String customerID;
    private final String firstName;
    private final String surname;
    private final String address;
    private final String username;
    private final String passwordHash;
    private final boolean employed;
    private final String employerName;
    private final String employerAddress;
    private final List<Account> accounts = new ArrayList<>();

    /** For a new customer: the plain password is hashed here. */
    public Customer(String customerID, String firstName, String surname, String address,
                    String username, String plainPassword,
                    boolean employed, String employerName, String employerAddress) {
        this(customerID, firstName, surname, address, username, plainPassword, false,
                employed, employerName, employerAddress);
    }

    /** For loading from the database: the password is already a hash. */
    public static Customer fromDatabase(String customerID, String firstName, String surname,
                                        String address, String username, String passwordHash,
                                        boolean employed, String employerName, String employerAddress) {
        return new Customer(customerID, firstName, surname, address, username, passwordHash, true,
                employed, employerName, employerAddress);
    }

    private Customer(String customerID, String firstName, String surname, String address,
                     String username, String password, boolean alreadyHashed,
                     boolean employed, String employerName, String employerAddress) {
        if (employed && (employerName == null || employerName.isBlank()
                || employerAddress == null || employerAddress.isBlank())) {
            throw new IllegalArgumentException("An employed customer must give employer name and address.");
        }
        this.customerID = customerID;
        this.firstName = firstName;
        this.surname = surname;
        this.address = address;
        this.username = username;
        this.passwordHash = alreadyHashed ? password : PasswordHasher.hash(password);
        this.employed = employed;
        this.employerName = employerName;
        this.employerAddress = employerAddress;
    }

    /** Checks a login attempt. Used by LoginController. */
    public boolean verifyCredentials(String username, String plainPassword) {
        return this.username.equals(username)
                && this.passwordHash.equals(PasswordHasher.hash(plainPassword));
    }

    /**
     * Adds an account after checking the bank's rules: at most three accounts,
     * one of each type, and a cheque account only for an employed customer.
     */
    public void addAccount(Account account) {
        if (accounts.size() >= MAX_ACCOUNTS) {
            throw new IllegalStateException("A customer can hold at most " + MAX_ACCOUNTS + " accounts.");
        }
        for (Account existing : accounts) {
            if (existing.getAccountType().equals(account.getAccountType())) {
                throw new IllegalStateException("Customer already has a " + account.getAccountType() + " account.");
            }
        }
        if (account instanceof ChequeAccount && !employed) {
            throw new IllegalStateException("A cheque account can only be opened for an employed customer.");
        }
        accounts.add(account);
    }

    /** Finds one of this customer's accounts, or null if there is none with that number. */
    public Account findAccount(String accountNumber) {
        for (Account a : accounts) {
            if (a.getAccountNumber().equals(accountNumber)) {
                return a;
            }
        }
        return null;
    }

    /** Closes an account. The balance must be withdrawn (zero) first. */
    public boolean removeAccount(String accountNumber) {
        Account a = findAccount(accountNumber);
        if (a == null || a.getBalance() > 0) {
            return false;
        }
        return accounts.remove(a);
    }

    public List<Account> getAccounts() { return Collections.unmodifiableList(accounts); }

    public String getCustomerID() { return customerID; }
    public String getFirstName() { return firstName; }
    public String getSurname() { return surname; }
    public String getAddress() { return address; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEmployed() { return employed; }
    public String getEmployerName() { return employerName; }
    public String getEmployerAddress() { return employerAddress; }

    @Override
    public String toString() {
        return customerID + " " + firstName + " " + surname + " (" + username + ")";
    }
}
