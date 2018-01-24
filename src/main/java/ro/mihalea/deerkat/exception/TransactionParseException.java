package ro.mihalea.deerkat.exception;

public class TransactionParseException extends Exception {
    public TransactionParseException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
