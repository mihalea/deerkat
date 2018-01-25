package ro.mihalea.deerkat.exception.repository;

/**
 * Exception thrown when the repository fails to read an items
 */
public class RepositoryReadException extends Exception {
    public RepositoryReadException(String s) {
        super(s);
    }

    public RepositoryReadException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
