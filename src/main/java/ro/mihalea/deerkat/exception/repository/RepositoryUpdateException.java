package ro.mihalea.deerkat.exception.repository;

public class RepositoryUpdateException extends Exception {
    public RepositoryUpdateException(String message) {
        super(message);
    }

    public RepositoryUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
