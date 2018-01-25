package ro.mihalea.deerkat.exception.repository;

/**
 * Exception thrown when failing to initialise the repository
 */
public class RepositoryInitialisationException extends Exception {
    public RepositoryInitialisationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public RepositoryInitialisationException(String s) {
        super(s);
    }
}
