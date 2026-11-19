package bank.dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Opens connections to the SQLite database and creates the tables from schema.sql.
 * The database is one file (bank.db by default), so it needs no server and works the same
 * on a laptop and in Codespaces.
 */
public class DatabaseManager {

    private final String url;

    public DatabaseManager(String filePath) {
        this.url = "jdbc:sqlite:" + filePath;
    }

    /** Uses bank.db in the working folder, or the file named by -Dbank.db=... */
    public static DatabaseManager forDefaultFile() {
        return new DatabaseManager(System.getProperty("bank.db", "bank.db"));
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON"); // SQLite ignores foreign keys unless asked
        }
        return connection;
    }

    /** Creates the tables if they do not exist yet. Safe to call every time the program starts. */
    public void initialize() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : readSchemaStatements()) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not set up the database: " + e.getMessage(), e);
        }
    }

    private String[] readSchemaStatements() {
        try (InputStream in = DatabaseManager.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new DataAccessException("schema.sql was not found.", null);
            }
            String text = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines()
                    .filter(line -> !line.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"));
            return java.util.Arrays.stream(text.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        } catch (IOException e) {
            throw new DataAccessException("Could not read schema.sql: " + e.getMessage(), e);
        }
    }
}
