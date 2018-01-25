package ro.mihalea.deerkat.exception.database;

/**
 * Exception thrown when failing to connect to the database and construct the repository
 */
public class DatabaseConnectionException extends Exception {
    public DatabaseConnectionException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
