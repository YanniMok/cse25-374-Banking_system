package bank.dao;

import bank.model.Account;
import bank.model.ChequeAccount;
import bank.model.InvestmentAccount;
import bank.model.SavingsAccount;
import bank.model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JDBC data access for the account table. The account_type column says which subclass
 * (Savings, Investment or Cheque) to rebuild when a row is read.
 */
public class AccountDAO {

    private final TransactionDAO transactionDAO = new TransactionDAO();

    /** Create or update: saves the account row and any transactions not yet stored. */
    public void save(Connection connection, String customerId, Account account) throws SQLException {
        String sql = "INSERT INTO account (account_number, customer_id, account_type, branch, balance, open_date) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(account_number) DO UPDATE SET balance = excluded.balance, branch = excluded.branch";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, account.getAccountNumber());
            ps.setString(2, customerId);
            ps.setString(3, account.getAccountType());
            ps.setString(4, account.getBranch());
            ps.setDouble(5, account.getBalance());
            ps.setString(6, account.getOpenDate().toString());
            ps.executeUpdate();
        }
        for (Transaction t : account.getTransactions()) {
            transactionDAO.insertIfMissing(connection, account.getAccountNumber(), t);
        }
    }

    /** Read: all accounts of one customer, in the order they were opened. */
    public List<Account> findByCustomer(Connection connection, String customerId) throws SQLException {
        String sql = "SELECT account_number, account_type, branch, balance, open_date "
                + "FROM account WHERE customer_id = ? ORDER BY rowid";
        List<Account> accounts = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accounts.add(rebuild(connection, rs));
                }
            }
        }
        return accounts;
    }

    /** Read: one account by its number, or null. */
    public Account findByNumber(Connection connection, String accountNumber) throws SQLException {
        String sql = "SELECT account_number, account_type, branch, balance, open_date "
                + "FROM account WHERE account_number = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rebuild(connection, rs) : null;
            }
        }
    }

    /** Delete: removes one account (its transactions go with it). */
    public void delete(Connection connection, String accountNumber) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM account WHERE account_number = ?")) {
            ps.setString(1, accountNumber);
            ps.executeUpdate();
        }
    }

    /** Delete: removes the customer's accounts that are not in the list (accounts that were closed). */
    public void deleteAllExcept(Connection connection, String customerId, Collection<String> keep) throws SQLException {
        StringBuilder sql = new StringBuilder("DELETE FROM account WHERE customer_id = ?");
        if (!keep.isEmpty()) {
            sql.append(" AND account_number NOT IN (");
            sql.append(String.join(", ", java.util.Collections.nCopies(keep.size(), "?")));
            sql.append(")");
        }
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            ps.setString(1, customerId);
            int i = 2;
            for (String number : keep) {
                ps.setString(i++, number);
            }
            ps.executeUpdate();
        }
    }

    private Account rebuild(Connection connection, ResultSet rs) throws SQLException {
        String number = rs.getString("account_number");
        String type = rs.getString("account_type");
        String branch = rs.getString("branch");
        double balance = rs.getDouble("balance");
        LocalDate openDate = LocalDate.parse(rs.getString("open_date"));
        List<Transaction> history = transactionDAO.findByAccount(connection, number);
        return switch (type) {
            case "Savings" -> SavingsAccount.fromDatabase(number, branch, balance, openDate, history);
            case "Investment" -> InvestmentAccount.fromDatabase(number, branch, balance, openDate, history);
            case "Cheque" -> ChequeAccount.fromDatabase(number, branch, balance, openDate, history);
            default -> throw new SQLException("Unknown account type in database: " + type);
        };
    }
}
