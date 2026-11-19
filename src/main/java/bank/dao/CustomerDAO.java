package bank.dao;

import bank.model.Customer;

import java.util.List;

/**
 * Data access interface for customers (with their accounts and transactions).
 * InMemoryCustomerDAO keeps data in memory (useful for quick tests); JdbcCustomerDAO stores it in
 * the SQLite database. The controllers and views work with either one.
 */
public interface CustomerDAO {

    /** Inserts the customer, or updates it (and its accounts and transactions) if it exists. */
    void save(Customer customer);

    /** Returns the customer with this username, or null if there is none. */
    Customer findByUsername(String username);

    /** Returns the customer with this ID, or null if there is none. */
    Customer findById(String customerId);

    List<Customer> findAll();

    /** Deletes the customer together with its accounts and their transactions. */
    void delete(String customerId);
}
