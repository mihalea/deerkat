package ro.mihalea.deerkat.exception.repository;

/**
 * Exception thrown when the developer calls an unimplemented method
 */
public class UnimplementedMethodException extends Exception {
    public UnimplementedMethodException(String s) {
        super(s);
    }
}
