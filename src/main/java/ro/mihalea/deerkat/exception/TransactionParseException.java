package ro.mihalea.deerkat.exception;

/**
 * Exception thrown when failing to parse a transaction's date
 */
public class TransactionParseException extends Exception {
    public TransactionParseException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
