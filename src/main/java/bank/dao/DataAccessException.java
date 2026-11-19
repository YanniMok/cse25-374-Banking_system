package bank.dao;

/**
 * Thrown when the database cannot be read or written. It extends IllegalStateException so the
 * screens show its message to the user instead of crashing.
 */
public class DataAccessException extends IllegalStateException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
