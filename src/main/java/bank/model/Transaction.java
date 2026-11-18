package bank.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A record of one deposit, withdrawal, salary credit or interest payment.
 * Entity class: it is immutable, so a recorded transaction can never be changed.
 */
public class Transaction {

    private final String transactionID;
    private final TransactionType type;
    private final double amount;
    private final LocalDateTime timestamp;
    private final double balanceAfter;
    private final String note;

    /** Used when a new transaction happens (ID and time are created here). */
    public Transaction(TransactionType type, double amount, double balanceAfter, String note) {
        this(UUID.randomUUID().toString(), type, amount, LocalDateTime.now(), balanceAfter, note);
    }

    /** Used when a transaction is loaded back from the database later. */
    public Transaction(String transactionID, TransactionType type, double amount,
                       LocalDateTime timestamp, double balanceAfter, String note) {
        this.transactionID = transactionID;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
        this.balanceAfter = balanceAfter;
        this.note = note;
    }

    public String getTransactionID() { return transactionID; }
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public double getBalanceAfter() { return balanceAfter; }
    public String getNote() { return note; }

    public String getDetails() {
        String text = String.format("%s | %-10s | BWP %10.2f | balance BWP %10.2f",
                timestamp.withNano(0), type, amount, balanceAfter);
        return (note == null || note.isBlank()) ? text : text + " | " + note;
    }

    @Override
    public String toString() {
        return getDetails();
    }
}
