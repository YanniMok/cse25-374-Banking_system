package bank.dao;

import bank.model.Customer;

import java.util.List;

/**
 * Data access interface for customers (with their accounts and transactions).
 * Step 6 uses an in-memory version; Step 7 adds a JDBC version behind the same interface,
 * so the controllers and views do not change.
 */
public interface CustomerDAO {

    /** Inserts the customer, or updates it (and its accounts and transactions) if it exists. */
    void save(Customer customer);

    /** Returns the customer with this username, or null if there is none. */
    Customer findByUsername(String username);

    List<Customer> findAll();
}
