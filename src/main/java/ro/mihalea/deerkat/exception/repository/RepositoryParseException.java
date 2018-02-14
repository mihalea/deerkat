package ro.mihalea.deerkat.exception.repository;

public class RepositoryParseException extends Exception{
    public RepositoryParseException(String message) {
        super(message);
    }

    public RepositoryParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
