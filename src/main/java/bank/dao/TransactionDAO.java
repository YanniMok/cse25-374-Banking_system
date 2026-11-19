package bank.dao;

import bank.model.Transaction;
import bank.model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC data access for the transactions table. The methods take the Connection so that
 * a whole customer can be saved in one database transaction.
 */
public class TransactionDAO {

    /** Create: stores the transaction unless it is already there (transactions never change). */
    public void insertIfMissing(Connection connection, String accountNumber, Transaction t) throws SQLException {
        String sql = "INSERT OR IGNORE INTO transactions "
                + "(transaction_id, account_number, type, amount, transaction_time, balance_after, note) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, t.getTransactionID());
            ps.setString(2, accountNumber);
            ps.setString(3, t.getType().name());
            ps.setDouble(4, t.getAmount());
            ps.setString(5, t.getTimestamp().toString());
            ps.setDouble(6, t.getBalanceAfter());
            ps.setString(7, t.getNote());
            ps.executeUpdate();
        }
    }

    /** Read: the transactions of one account, oldest first. */
    public List<Transaction> findByAccount(Connection connection, String accountNumber) throws SQLException {
        String sql = "SELECT transaction_id, type, amount, transaction_time, balance_after, note "
                + "FROM transactions WHERE account_number = ? ORDER BY rowid";
        List<Transaction> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Transaction(
                            rs.getString("transaction_id"),
                            TransactionType.valueOf(rs.getString("type")),
                            rs.getDouble("amount"),
                            LocalDateTime.parse(rs.getString("transaction_time")),
                            rs.getDouble("balance_after"),
                            rs.getString("note")));
                }
            }
        }
        return result;
    }

    /** Delete: removes all transactions of an account. */
    public void deleteByAccount(Connection connection, String accountNumber) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM transactions WHERE account_number = ?")) {
            ps.setString(1, accountNumber);
            ps.executeUpdate();
        }
    }
}
