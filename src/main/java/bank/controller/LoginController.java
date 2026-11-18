package bank.controller;

import bank.dao.CustomerDAO;
import bank.model.Customer;

import java.util.HashMap;
import java.util.Map;

/**
 * Control class for logging in and out. Sits between the views and the Customer entity.
 * Errors are reported with exceptions whose messages are safe to show to the user.
 */
public class LoginController {

    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final CustomerDAO dao;
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private Customer currentCustomer;

    public LoginController(CustomerDAO dao) {
        this.dao = dao;
    }

    /** Returns the customer if the details are right; otherwise throws with a message. */
    public Customer login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Enter both username and password.");
        }
        String name = username.trim();
        if (failedAttempts.getOrDefault(name, 0) >= MAX_FAILED_ATTEMPTS) {
            throw new IllegalStateException("Too many failed attempts. Restart the application to try again.");
        }
        Customer customer = dao.findByUsername(name);
        if (customer != null && customer.verifyCredentials(name, password)) {
            failedAttempts.remove(name);
            currentCustomer = customer;
            return customer;
        }
        failedAttempts.merge(name, 1, Integer::sum);
        throw new IllegalArgumentException("Invalid username or password.");
    }

    public void logout() {
        currentCustomer = null;
    }

    public Customer getCurrentCustomer() {
        return currentCustomer;
    }
}
