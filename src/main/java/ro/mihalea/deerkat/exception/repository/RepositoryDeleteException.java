package ro.mihalea.deerkat.exception.repository;

/**
 * Exception thrown when repositories fail to delete one or more fields
 */
public class RepositoryDeleteException extends Exception {
    public RepositoryDeleteException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public RepositoryDeleteException(String message) {
        super(message);
    }
}
