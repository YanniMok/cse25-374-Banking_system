package bank.dao;

import bank.model.Account;
import bank.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of CustomerDAO. Saving a customer stores the customer, its accounts and
 * their transactions in one database transaction, so either everything is saved or nothing is.
 */
public class JdbcCustomerDAO implements CustomerDAO {

    private static final String SELECT_CUSTOMER =
            "SELECT customer_id, first_name, surname, address, username, password_hash, "
                    + "employed, employer_name, employer_address FROM customer";

    private final DatabaseManager database;
    private final AccountDAO accountDAO = new AccountDAO();

    public JdbcCustomerDAO(DatabaseManager database) {
        this.database = database;
    }

    /** Create or update. */
    @Override
    public void save(Customer customer) {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                upsertCustomer(connection, customer);
                List<String> keep = new ArrayList<>();
                for (Account account : customer.getAccounts()) {
                    accountDAO.save(connection, customer.getCustomerID(), account);
                    keep.add(account.getAccountNumber());
                }
                accountDAO.deleteAllExcept(connection, customer.getCustomerID(), keep);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the customer: " + e.getMessage(), e);
        }
    }

    /** Read. */
    @Override
    public Customer findByUsername(String username) {
        return findOne("WHERE username = ?", username);
    }

    /** Read. */
    @Override
    public Customer findById(String customerId) {
        return findOne("WHERE customer_id = ?", customerId);
    }

    /** Read. */
    @Override
    public List<Customer> findAll() {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_CUSTOMER + " ORDER BY rowid");
             ResultSet rs = ps.executeQuery()) {
            List<Customer> customers = new ArrayList<>();
            while (rs.next()) {
                customers.add(readCustomer(rs));
            }
            for (Customer customer : customers) {
                attachAccounts(connection, customer);
            }
            return customers;
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the customers: " + e.getMessage(), e);
        }
    }

    /** Delete: removes the customer, its accounts and their transactions. */
    @Override
    public void delete(String customerId) {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM customer WHERE customer_id = ?")) {
            ps.setString(1, customerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the customer: " + e.getMessage(), e);
        }
    }

    // ----- helpers -----

    private Customer findOne(String whereClause, String value) {
        try (Connection connection = database.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT_CUSTOMER + " " + whereClause)) {
            ps.setString(1, value);
            Customer customer = null;
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    customer = readCustomer(rs);
                }
            }
            if (customer != null) {
                attachAccounts(connection, customer);
            }
            return customer;
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the customer: " + e.getMessage(), e);
        }
    }

    private void upsertCustomer(Connection connection, Customer c) throws SQLException {
        String sql = "INSERT INTO customer (customer_id, first_name, surname, address, username, "
                + "password_hash, employed, employer_name, employer_address) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(customer_id) DO UPDATE SET first_name = excluded.first_name, "
                + "surname = excluded.surname, address = excluded.address, username = excluded.username, "
                + "password_hash = excluded.password_hash, employed = excluded.employed, "
                + "employer_name = excluded.employer_name, employer_address = excluded.employer_address";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getCustomerID());
            ps.setString(2, c.getFirstName());
            ps.setString(3, c.getSurname());
            ps.setString(4, c.getAddress());
            ps.setString(5, c.getUsername());
            ps.setString(6, c.getPasswordHash());
            ps.setInt(7, c.isEmployed() ? 1 : 0);
            ps.setString(8, c.getEmployerName());
            ps.setString(9, c.getEmployerAddress());
            ps.executeUpdate();
        }
    }

    private Customer readCustomer(ResultSet rs) throws SQLException {
        return Customer.fromDatabase(
                rs.getString("customer_id"), rs.getString("first_name"), rs.getString("surname"),
                rs.getString("address"), rs.getString("username"), rs.getString("password_hash"),
                rs.getInt("employed") == 1, rs.getString("employer_name"), rs.getString("employer_address"));
    }

    private void attachAccounts(Connection connection, Customer customer) throws SQLException {
        for (Account account : accountDAO.findByCustomer(connection, customer.getCustomerID())) {
            customer.addAccount(account);
        }
    }
}
