package ro.mihalea.deerkat.exception.repository;

/**
 * Error thrown when trying to add items to the repository goes wrong
 */
public class RepositoryCreateException extends Exception {
    public RepositoryCreateException(String message) {
        super(message);
    }

    public RepositoryCreateException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
