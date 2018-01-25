package ro.mihalea.deerkat.exception.database;

/**
 * Exception thrown when the JDBC can't parse an SQL query
 */
public class DatabaseStatementException extends Exception {
    public DatabaseStatementException(String s) {
        super(s);
    }

    public DatabaseStatementException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
