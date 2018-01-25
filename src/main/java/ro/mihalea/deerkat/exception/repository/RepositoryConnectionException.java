package ro.mihalea.deerkat.exception.repository;

/**
 * Exception thrown when failing to connect to the repository and construct the repository
 */
public class RepositoryConnectionException extends Exception {
    public RepositoryConnectionException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
