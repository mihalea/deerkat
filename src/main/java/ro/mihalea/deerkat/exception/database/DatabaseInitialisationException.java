package ro.mihalea.deerkat.exception.database;

/**
 * Exception thrown when failing to read the default SQL configuration file
 */
public class DatabaseInitialisationException extends Exception {
    public DatabaseInitialisationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public DatabaseInitialisationException(String s) {
        super(s);
    }
}
